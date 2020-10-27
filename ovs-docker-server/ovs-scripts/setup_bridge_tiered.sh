#!/bin/bash

MODE="$1"

OF_BRIDGE=ovs-br
OF_EXT_BRIDGE=ovs-ext-bridge
OF_BRIDGE_PORT=6653
OVS_DB_PORT=6654

RESULT_NIC_IP=""
RESULT_NIC_BDCAST=""

source ../config.sh

clear_bridge() {
    local bridge_name="$1"
    echo "Removing bridge in case it existed already."
    sudo ovs-vsctl del-br $bridge_name
}

restart_nic() {
    local nic_name="$1"
    echo "Restarting NIC ${nic_name}"
    sudo ip link set $nic_name down
    sudo ip link set $nic_name up
}

clear_all() {
    clear_bridge $OF_BRIDGE
    clear_bridge $OF_EXT_BRIDGE
    restart_nic $IOT_NIC
    restart_nic $EXT_NIC

    # Stop here if we just want to clear the bridge.
    if [ "$MODE" == "clear" ]; then
      exit 0
    fi
}

# Gets the IP and broadacast IP of the given NIC, waits if not ready, returns in temp variables.
get_nic_ip() {
    local nic_name="$1"

    RESULT_NIC_IP=""
    RESULT_NIC_BDCAST=""
    IP_FOUND="false"
    while [ "${IP_FOUND}" == "false" ]; do
      echo "Getting IP and brodcast IP for the NIC ${nic_name}."
      RESULT_NIC_IP=$(ip addr show ${nic_name} | grep -Po 'inet \K[\d.]+')
      RESULT_NIC_BDCAST=$(ip addr show ${nic_name} | grep -Po 'brd \K[\d.]+')
      echo "NIC (${nic_name}) IP: ${RESULT_NIC_IP}, NIC BROADCAST: ${RESULT_NIC_BDCAST}"

      if [ -z "${RESULT_NIC_IP}" ]; then
        echo "Could not get IP address from NIC ${nic_name}. Will wait and retry."
        sleep 2s
      else
        IP_FOUND="true"
      fi
    done
}

# Connects a NIC to a virtual bridge, stripping it of its IP address as well.
connect_interface_to_bridge() {
    local bridge_name="$1"
    local interface="$2"
    local port_num="$3"

    echo "Connecting NIC $interface to bridge ${bridge_name}"
    sudo ethtool -K $interface gro off

    # Add the given port_name as a port to the bridge, and assign it the given OpenFlow port number.
    sudo ovs-vsctl add-port $bridge_name $interface -- set interface $interface ofport_request=$port_num
    sudo ovs-ofctl mod-port $bridge_name $interface up

    # Remove the IP address from the NIC since it no longer makes sense.
    sudo ip addr flush dev $interface

    echo "NIC $interface connected to bridge ${bridge_name}"
}

# Adds the given IP address from a NIC into a virtual switch, assuming it has been flushed from NIC before.
set_bridge_ip() {
    local bridge_name="$1"
    local nic_name="$2"
    local nic_ip="$3"
    local bcast_ip="$4"

    echo "Attaching IP of network ${nic_name} (IP: ${nic_ip}) to bridge ${bridge_name}"
    sudo ip addr add ${nic_ip}/24 broadcast ${bcast_ip} dev $bridge_name
}

# Connects a "patch" type connection between a bridge and a peer (used to connect bridges).
# Has to be done on each bridge to properly connect them to each other.
connect_patch_port() {
    local bridge_name="$1"
    local patch_port_name="$2"
    local port_num="$3"
    local peer_port_name="$4"

    echo "Connecting patch port to bridge ${bridge_name}..."
    sudo ovs-vsctl \
                -- add-port $bridge_name $patch_port_name \
                -- set interface $patch_port_name ofport_request=$port_num \
                -- set interface $patch_port_name type=patch options:peer=$peer_port_name
    echo "Connected."
}

# Creates and sets up a bridge connected to a bridge on one port, and to a NIC on the other.
setup_nic_bridge() {
    local bridge_name="$1"
    local nic_name="$2"
    local patch_port_name="$3"
    local patch_peer_name="$4"
    local of_bridge_name="$5"
    local of_patch_port_num="$6"
    local nic_ip="$7"
    local bcast_ip="$8"

    # Local bridge ports to use.
    local to_nic_port=1
    local to_of_bridge_port=2

    echo "Setting up NIC bridge $bridge_name"
    sudo ovs-vsctl add-br $bridge_name

    # Connect to NIC to the OVS switch in port to_nic_port, and transfer its IP to the bridge.
    connect_interface_to_bridge $bridge_name $nic_name $to_nic_port
    set_bridge_ip ${bridge_name} ${nic_name} ${nic_ip} ${bcast_ip}

    echo "Starting bridge and setting up OF version"
    sudo ip link set $bridge_name up
    sudo ovs-vsctl set bridge $bridge_name protocols=OpenFlow13

    # Set up patch port to other bridge in port to_of_bridge_port.
    echo "Setting up patch ports between both bridges"
    connect_patch_port $bridge_name $patch_port_name $to_of_bridge_port $patch_peer_name
    connect_patch_port $of_bridge_name $patch_peer_name $of_patch_port_num $patch_port_name

    # Setting default bridge rules.
    setup_passthrough_bridge_rules $bridge_name $nic_ip $to_nic_port $to_nic_bridge_port $IOT_NIC_IP

    echo "NIC bridge $bridge_name setup complete"
}

setup_ovs_bridge() {
    local bridge_name="$1"
    local iot_nic="$2"
    local ext_nic="$3"

    local to_nic_port=1
    local to_nic_bridge_port=2

    # Get IOT NIC IP
    get_nic_ip ${IOT_NIC}
    IOT_NIC_IP=$RESULT_NIC_IP
    IOT_NIC_BROADCAST=$RESULT_NIC_BDCAST

    # Get EXT NIC IP
    get_nic_ip ${EXT_NIC}
    EXT_NIC_IP=$RESULT_NIC_IP
    EXT_NIC_BROADCAST=$RESULT_NIC_BDCAST

    set -e
    echo "Setting up OVS bridge $bridge_name"
    sudo ovs-vsctl add-br $bridge_name

    # Connect IOT NIC to the OVS switch in port to_nic_port.
    connect_interface_to_bridge $bridge_name $iot_nic $to_nic_port
    set_bridge_ip ${bridge_name} ${iot_nic} ${IOT_NIC_IP} ${IOT_NIC_BROADCAST}

    # Sets up intermediate bridge to EXT NIC and connects it also to OVS bridge
    ext_to_of_patch="ext-to-of"
    of_to_ext_patch="of-to-ext"
    setup_nic_bridge $OF_EXT_BRIDGE $ext_nic $ext_to_of_patch $of_to_ext_patch $OF_BRIDGE $to_nic_bridge_port $EXT_NIC_IP $EXT_NIC_BROADCAST

    echo "Setting up OF params"
    sudo ip link set $bridge_name up
    sudo ovs-vsctl set bridge $bridge_name protocols=OpenFlow13
    sudo ovs-vsctl set-controller $bridge_name ptcp:$OF_BRIDGE_PORT
    sudo ovs-vsctl set controller $bridge_name connection-mode=out-of-band

    setup_passthrough_bridge_rules $bridge_name $IOT_NIC_IP $to_nic_port $to_nic_bridge_port $EXT_NIC_IP

    echo "Bridge setup complete"
 }

# Sets up basic rules for OVS bridge.
setup_passthrough_bridge_rules() {
    local bridge_name="$1"
    local local_net_ip="$2"
    local net_port="$3"
    local bridge_port="$4"
    local other_net_ip="$5"

    # Rule to drop mDNS requests and IPv6 traffic.
    echo "Setting up drop rules for mDNS traffic and IPv6"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=160,udp,tp_dst=5353,actions=drop"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=160,ipv6,actions=drop"

    # ARP rules: process ARP requests/replies for us, send our ARP requests/replies, drop all others from this subnet
    # Process IP requests/replies as well.
    echo "Setting up OF rules for local IP: ${local_net_ip}"
    #sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=155,arp,arp_tpa=${local_net_ip},actions=normal"
    #sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=155,arp,arp_tpa=${other_net_ip},actions=normal"
    #sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=155,arp,arp_spa=${local_net_ip},actions=normal"
    #sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=155,arp,arp_spa=${other_net_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=154,ip,ip_src=${local_net_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=154,ip,ip_dst=${local_net_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=154,ip,ip_src=${other_net_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=154,ip,ip_dst=${other_net_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=153,arp,in_port=${net_port},nw_dst=${local_net_ip}/24,actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=153,arp,in_port=${net_port},nw_dst=${other_net_ip}/24,actions=drop"

    # Set up default rules to connect bridges together.
    echo "Setting up pass-through rules for non-device traffic"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=60,arp,actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=50,ip,in_port=1,actions=output:2"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=50,ip,in_port=2,actions=output:1"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=0,actions=normal"

    echo "Finished setting up flow rules."
}

# Setup
echo "Beginning switches setup..."
clear_all

# Sets up OVS bridge and subbridges, plus rules
setup_ovs_bridge $OF_BRIDGE $IOT_NIC $EXT_NIC

# Configure OVS DB to listen to remote commands on given TCP port.
sudo ovs-appctl -t ovsdb-server ovsdb-server/add-remote ptcp:$OVS_DB_PORT

echo "OVS switch ready"
