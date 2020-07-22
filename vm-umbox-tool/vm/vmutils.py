# 
#  Kalki - A Software-Defined IoT Security Platform
#  Copyright 2020 Carnegie Mellon University.
#  NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
#  Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.
#  [DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see Copyright notice for non-US Government use and distribution.
#  This Software includes and/or makes use of the following Third-Party Software subject to its own license:
#  1. Google Guava (https://github.com/google/guava) Copyright 2007 The Guava Authors.
#  2. JSON.simple (https://code.google.com/archive/p/json-simple/) Copyright 2006-2009 Yidong Fang, Chris Nokleberg.
#  3. JUnit (https://junit.org/junit5/docs/5.0.1/api/overview-summary.html) Copyright 2020 The JUnit Team.
#  4. Play Framework (https://www.playframework.com/) Copyright 2020 Lightbend Inc..
#  5. PostgreSQL (https://opensource.org/licenses/postgresql) Copyright 1996-2020 The PostgreSQL Global Development Group.
#  6. Jackson (https://github.com/FasterXML/jackson-core) Copyright 2013 FasterXML.
#  7. JSON (https://www.json.org/license.html) Copyright 2002 JSON.org.
#  8. Apache Commons (https://commons.apache.org/) Copyright 2004 The Apache Software Foundation.
#  9. RuleBook (https://github.com/deliveredtechnologies/rulebook/blob/develop/LICENSE.txt) Copyright 2020 Delivered Technologies.
#  10. SLF4J (http://www.slf4j.org/license.html) Copyright 2004-2017 QOS.ch.
#  11. Eclipse Jetty (https://www.eclipse.org/jetty/licenses.html) Copyright 1995-2020 Mort Bay Consulting Pty Ltd and others..
#  12. Mockito (https://github.com/mockito/mockito/wiki/License) Copyright 2007 Mockito contributors.
#  13. SubEtha SMTP (https://github.com/voodoodyne/subethasmtp) Copyright 2006-2007 SubEthaMail.org.
#  14. JSch - Java Secure Channel (http://www.jcraft.com/jsch/) Copyright 2002-2015 Atsuhiko Yamanaka, JCraft,Inc. .
#  15. ouimeaux (https://github.com/iancmcc/ouimeaux) Copyright 2014 Ian McCracken.
#  16. Flask (https://github.com/pallets/flask) Copyright 2010 Pallets.
#  17. Flask-RESTful (https://github.com/flask-restful/flask-restful) Copyright 2013 Twilio, Inc..
#  18. libvirt-python (https://github.com/libvirt/libvirt-python) Copyright 2016 RedHat, Fedora project.
#  19. Requests: HTTP for Humans (https://github.com/psf/requests) Copyright 2019 Kenneth Reitz.
#  20. netifaces (https://github.com/al45tair/netifaces) Copyright 2007-2018 Alastair Houghton.
#  21. ipaddress (https://github.com/phihag/ipaddress) Copyright 2001-2014 Python Software Foundation.
#  DM20-0543
#
#
import libvirt

QEMU_URI_PREFIX = "qemu://"
SYSTEM_LIBVIRT_DAEMON_SUFFIX = "/system"
SESSION_LIBVIRT_DAEMON_SUFFIX = "/session"

# Global connection object, only open connection to hypervisor used by app.
_hypervisor = None


################################################################################################################
# Exception type used in our system.
################################################################################################################
class VirtualMachineException(Exception):
    def __init__(self, message):
        super(VirtualMachineException, self).__init__(message)
        self.message = message


################################################################################################################
# A slightly clearer interface for managing VMs that wraps calls to libvirt in a VM object.
################################################################################################################
class VirtualMachine(object):

    ################################################################################################################
    #
    ################################################################################################################
    def __init__(self):
        self.vm = None

    ################################################################################################################
    # Returns the hypervisor connection and will auto connect if the connection is null.
    ################################################################################################################
    @staticmethod
    def get_hypervisor_instance(is_system_level=True, host_name='', transport=None):
        global _hypervisor
        if _hypervisor is None:
            _hypervisor = VirtualMachine.connect_to_hypervisor(is_system_level, host_name, transport)
        return _hypervisor

    ################################################################################################################
    # Returns the hypervisor connection.
    ################################################################################################################
    @staticmethod
    def connect_to_hypervisor(is_system_level=True, host_name='', transport=None):
        try:
            uri = VirtualMachine._get_qemu_libvirt_connection_uri(is_system_level, host_name=host_name, transport=transport)
            #print uri
            hypervisor = libvirt.open(uri)
            return hypervisor
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    # Builds a libvir URI for a QEMU connection.
    ################################################################################################################
    @staticmethod
    def _get_qemu_libvirt_connection_uri(is_system_level=False, host_name='', transport=None):
        uri = QEMU_URI_PREFIX
        if transport is not None:
            uri = uri.replace(":", "+" + transport + ":")
        uri += host_name
        if is_system_level:
            uri += SYSTEM_LIBVIRT_DAEMON_SUFFIX
        else:
            uri += SESSION_LIBVIRT_DAEMON_SUFFIX
        return uri

    ################################################################################################################
    # Lookup a specific instance by its uuid
    ################################################################################################################
    def connect_to_virtual_machine(self, uuid):
        try:
            self.vm = VirtualMachine.get_hypervisor_instance().lookupByUUIDString(uuid)
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    # Lookup a specific instance by its name
    ################################################################################################################
    def connect_to_virtual_machine_by_name(self, name):
        try:
            self.vm = VirtualMachine.get_hypervisor_instance().lookupByName(name)
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    # Get the XML description of a running VM.
    ################################################################################################################
    def get_running_vm_xml_string(self):
        try:
            return self.vm.XMLDesc(libvirt.VIR_DOMAIN_XML_SECURE)
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    # Get the XML description of a stored VM.
    ################################################################################################################
    @staticmethod
    def get_stored_vm_xml_string(saved_state_filename):
        try:
            return VirtualMachine.get_hypervisor_instance().saveImageGetXMLDesc(saved_state_filename, 0)
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    # Creates and starts a new VM from an XML description.
    ################################################################################################################
    def create_and_start_vm(self, xml_descriptor):
        try:
            self.vm = VirtualMachine.get_hypervisor_instance().createXML(xml_descriptor, 0)
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    # Save the state of the give VM to the indicated file.
    ################################################################################################################
    def save_state(self, vm_state_image_file):
        try:
            # We indicate that we want want to use as much bandwidth as possible to store the VM's memory when suspending.
            unlimited_bandwidth = 1000000
            self.vm.migrateSetMaxSpeed(unlimited_bandwidth, 0)

            result = self.vm.save(vm_state_image_file)
            if result != 0:
                raise VirtualMachineException("Cannot save memory state to file {}".format(vm_state_image_file))
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    #
    ################################################################################################################
    @staticmethod
    def restore_saved_vm(saved_state_filename, updated_xml_descriptor):
        try:
            VirtualMachine.get_hypervisor_instance().restoreFlags(saved_state_filename, updated_xml_descriptor,
                                                                  libvirt.VIR_DOMAIN_SAVE_RUNNING)
        except libvirt.libvirtError as e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    #
    ################################################################################################################
    def pause(self):
        try:
            result = self.vm.suspend()
            was_suspend_successful = result == 0
            return was_suspend_successful
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    #
    ################################################################################################################
    def unpause(self):
        try:
            result = self.vm.resume()
            was_resume_successful = result == 0
            return was_resume_successful
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    #
    ################################################################################################################
    def destroy(self):
        try:
            self.vm.destroy()
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))

    ################################################################################################################
    #
    ################################################################################################################
    def perform_memory_migration(self, remote_host, p2p=False):
        # Prepare basic flags. Bandwidth 0 lets libvirt choose the best value
        # (and some hypervisors do not support it anyway).
        flags = 0
        new_id = None
        bandwidth = 0

        if p2p:
            flags = flags | libvirt.VIR_MIGRATE_PEER2PEER | libvirt.VIR_MIGRATE_TUNNELLED
            uri = None
        else:
            uri = VirtualMachine._get_qemu_libvirt_tcp_connection_uri(host_name=remote_host)

        try:
            # Migrate the state and memory (note that have to connect to the system-level libvirtd on the remote host).
            remote_hypervisor = VirtualMachine.connect_to_hypervisor(is_system_level=True, host_name=remote_host)
            self.vm.migrate(remote_hypervisor, flags, new_id, uri, bandwidth)
        except libvirt.libvirtError, e:
            raise VirtualMachineException(str(e))


################################################################################################################
# Helper to convert normal uuid to string
################################################################################################################
def uuid_to_str(raw_uuid):
    hx = ['0', '1', '2', '3', '4', '5', '6', '7',
          '8', '9', 'a', 'b', 'c', 'd', 'e', 'f']
    uuid = []
    for i in range(16):
        uuid.append(hx[((ord(raw_uuid[i]) >> 4) & 0xf)])
        uuid.append(hx[(ord(raw_uuid[i]) & 0xf)])
        if i == 3 or i == 5 or i == 7 or i == 9:
            uuid.append('-')
    return "".join(uuid)
