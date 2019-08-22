
import uuid
import os
import os.path
import logging
import sys
import random
import json
from argparse import ArgumentParser


import requests

import vm.vmutils as vmutils
import vm.vm_descriptor as vm_descriptor


MAX_INSTANCES = 1000
NUM_SEPARATOR = "-"
XML_VM_TEMPLATE = "vm/vm_template.xml"

# Base names for the TUN/TAP virtual interfaces on the data node that handle the VM interfaces.
CONTROL_TUN_PREFIX = "vnucont"
DATA_TUN_PREFIX = "vnudata"

API_CLONE_PORT = "5000"
BASE_CLONE_API_URL = "clone"
INSTANCE_PATH_KEY = "instance_path"
API_CLONE_METHOD = "POST"
API_CLEAN_METHOD = "DELETE"

# Global logger.
logger = None


def setup_custom_logger(name):
    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)

    formatter = logging.Formatter(fmt='%(asctime)s %(levelname)-8s %(message)s',
                                  datefmt='%Y-%m-%d %H:%M:%S')
    #handler = logging.FileHandler(file_path, mode='w')
    #handler.setFormatter(formatter)
    #logger.addHandler(handler)

    screen_handler = logging.StreamHandler(stream=sys.stderr)
    screen_handler.setFormatter(formatter)
    logger.addHandler(screen_handler)
    return logger


def create_and_start_umbox(data_node_ip, umbox_id, image_name, image_file_name, control_bridge, data_bridge):
    umbox = VmUmbox(umbox_id, image_name, image_file_name, control_bridge, data_bridge)
    #umbox.create_linked_image()
    umbox.start(data_node_ip)
    logger.info("Umbox started.")

    return umbox


def stop_umbox(data_node_ip, umbox_id, image_name):
    """Stops a running instance of an umbox."""
    umbox = VmUmbox(umbox_id, image_name, None)
    umbox.stop(data_node_ip)
    logger.info("Umbox stopped.")


def generate_mac(instance_id):
    """Generate a mac from the instance id. We are using te 00163e prefix used by Xensource."""
    mac = [
        0x00, 0x16, 0x3e,
        random.randint(0x00, 0x7f),
        int(instance_id) // MAX_INSTANCES,
        int(instance_id) % 100
    ]
    return ':'.join(map(lambda x: "%02x" % x, mac))


class VmUmbox(object):
    """Class that stores information about a VM that is working as a umbox."""

    def __init__(self, umbox_id, image_name, image_file_name, control_bridge=None, data_bridge=None):
        """Default constructor."""
        self.umbox_id = umbox_id
        self.image_name = image_name
        self.image_file_name = image_file_name
        self.instance_name = image_name + NUM_SEPARATOR + self.umbox_id

        self.control_bridge = control_bridge
        self.data_bridge = data_bridge
        self.control_iface_name = CONTROL_TUN_PREFIX + self.umbox_id
        self.data_in_iface_name = DATA_TUN_PREFIX + "_in_" + self.umbox_id
        self.data_out_iface_name = DATA_TUN_PREFIX + "_out_" + self.umbox_id

        # Only to be used for newly started VMs.
        self.control_mac_address = generate_mac(self.umbox_id)
        self.data_in_mac_address = generate_mac(self.umbox_id)
        self.data_out_mac_address = generate_mac(self.umbox_id)

        logger.info("VM name: " + self.instance_name)

    def get_updated_descriptor(self, xml_descriptor_string):
        """Updates an XML containing the description of the VM with the current info of this VM."""

        # Get the descriptor and inflate it to something we can work with.
        xml_descriptor = vm_descriptor.VirtualMachineDescriptor(xml_descriptor_string)

        xml_descriptor.set_uuid(str(uuid.uuid4()))
        xml_descriptor.set_name(self.instance_name)

        xml_descriptor.set_disk_image(self.instance_path, 'qcow2')

        logger.info('Adding test network interface on libvirts default network')
        xml_descriptor.add_internal_nic_interface()

        logger.info('Adding control plane network interface, using tap: ' + self.control_iface_name)
        xml_descriptor.add_bridge_interface(self.control_bridge, self.control_mac_address, target=self.control_iface_name)

        logger.info('Adding OVS connected network interface, incoming, using tap: ' + self.data_in_iface_name)
        xml_descriptor.add_bridge_interface(self.data_bridge, self.data_in_mac_address, target=self.data_in_iface_name, ovs=True)

        logger.info('Adding OVS connected network interface, outgoing, using tap: ' + self.data_out_iface_name)
        xml_descriptor.add_bridge_interface(self.data_bridge, self.data_out_mac_address, target=self.data_out_iface_name, ovs=True)

        # Remove seclabel item, which tends to generate issues when the VM is executed.
        xml_descriptor.remove_sec_label()

        updated_xml_descriptor_string = xml_descriptor.get_as_string()
        return updated_xml_descriptor_string

    def _connect_to_remote_hypervisor(self, hypervisor_host_ip):
        """Explicitly connect to hypervisor to ensure we are getting to remote libvirtd."""
        vmutils.VirtualMachine.get_hypervisor_instance(is_system_level=True, host_name=hypervisor_host_ip, transport='tcp')

    def start(self, hypervisor_host_ip):
        """Creates a new VM using the XML template plus the information set up for this umbox."""
        self._connect_to_remote_hypervisor(hypervisor_host_ip)

        # First clone remote image for new instance.
        json_reply = self.__send_api_command(hypervisor_host_ip, API_CLONE_METHOD, "{0}/{1}".format(self.image_file_name, self.instance_name))
        self.instance_path = json.loads(json_reply)[INSTANCE_PATH_KEY]

        # Set up VM information from template and umbox data.
        template_xml_file = os.path.abspath(XML_VM_TEMPLATE)
        with open(template_xml_file, 'r') as xml_file:
            template_xml = xml_file.read().replace('\n', '')
        updated_xml = self.get_updated_descriptor(template_xml)
        logger.info(updated_xml)

        # Check if the VM is already running.
        vm = vmutils.VirtualMachine()
        try:
            # If it is, connect and destroy it, before starting a new one.
            vm.connect_to_virtual_machine_by_name(self.instance_name)
            logger.info("VM with same name was already running; destroying it.")
            vm.destroy()
            logger.info("VM destroyed.")
        except vmutils.VirtualMachineException, ex:
            logger.warning("VM was not running.")
            vm = vmutils.VirtualMachine()

        # Then create and start the VM itself.
        logger.info("Starting new VM.")
        vm.create_and_start_vm(updated_xml)
        logger.info("New VM started.")

    def pause(self, hypervisor_host_ip):
        self._connect_to_remote_hypervisor(hypervisor_host_ip)
        vm = vmutils.VirtualMachine()
        try:
            vm.connect_to_virtual_machine_by_name(self.instance_name)
            vm.pause()
        except:
            logger.error("VM not found.")

    def unpause(self, hypervisor_host_ip):
        self._connect_to_remote_hypervisor(hypervisor_host_ip)
        vm = vmutils.VirtualMachine()
        try:
            vm.connect_to_virtual_machine_by_name(self.instance_name)
            vm.unpause()
        except:
            logger.error("VM not found.")

    def stop(self, hypervisor_host_ip):
        self._connect_to_remote_hypervisor(hypervisor_host_ip)
        vm = vmutils.VirtualMachine()
        try:
            vm.connect_to_virtual_machine_by_name(self.instance_name)
            vm.destroy()

            # Destroy instance image file.
            json_reply = self.__send_api_command(hypervisor_host_ip, API_CLEAN_METHOD, "{0}/{1}".format("non", self.instance_name))
            print "Remote instance image deletion response: " + json_reply
        except:
            logger.warning("VM not found.")

    def __send_api_command(self, host, method, command):
        remote_url = 'http://{0}:{1}/{2}/{3}'.format(host, API_CLONE_PORT, BASE_CLONE_API_URL, command)
        print remote_url

        req = requests.Request(method, remote_url)
        prepared = req.prepare()
        session = requests.Session()
        response = session.send(prepared)

        if response.status_code != requests.codes.ok:
            raise Exception('Error sending request {}: {} - {}'.format(command, response.status_code, response.text))

        return response.text


def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument("-c", "--command", dest="command", required=True, help="Command: start or stop")
    parser.add_argument("-s", "--server", dest="datanodeip", required=True, help="IP of the data node server")
    parser.add_argument("-u", "--umbox", dest="umboxid", required=False, help="id of the umbox instance")
    parser.add_argument("-i", "--image", dest="imagename", required=False, help="name of the umbox image")
    parser.add_argument("-f", "--imagefile", dest="imagefile", required=False, help="the name of the image file")
    parser.add_argument("-bc", "--bridgecontrol", dest="controlbr", required=False, help="name of the control virtual bridge")
    parser.add_argument("-bd", "--bridgedata", dest="databr", required=False, help="name of the data ovs virtual bridge")
    args = parser.parse_args()
    return args


def main():
    global logger
    logger = setup_custom_logger("main")

    args = parse_arguments()
    logger.info("Command: " + args.command)
    logger.info("Data node to use: " + args.datanodeip)
    if args.command == "start":
        logger.info("Umbox ID: " + args.umboxid)
        logger.info("Image name: " + args.imagename)
        logger.info("Image file: " + args.imagefile)
        logger.info("Control bridge: " + args.controlbr)
        logger.info("Data bridge: " + args.databr)

        umbox = create_and_start_umbox(args.datanodeip, args.umboxid, args.imagename, args.imagefile, args.controlbr, args.databr)

        # Print the TAP device name so that it can be returned and used by ovs commands if needed.
        print(umbox.data_in_iface_name + " " + umbox.data_out_iface_name)
    else:
        logger.info("Umbox ID: " + args.umboxid)
        logger.info("Image name: " + str(args.imagename))
        logger.info("Instance: " + str(args.imagename) + "-" + args.umboxid)

        stop_umbox(args.datanodeip, args.umboxid, args.imagename)


if __name__ == '__main__':
    main()
