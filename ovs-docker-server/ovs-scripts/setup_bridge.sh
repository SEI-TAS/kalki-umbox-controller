#!/bin/bash

MODE="$1"

IOT_NIC=enp2s0f1
EXT_NIC=enp2s0f0

OF_BRIDGE=ovs-br
OF_BRIDGE_PORT=6653
OVS_DB_PORT=6654

clear_bridge() {
    echo "Removing bridge in case it existed already."
    sudo ovs-vsctl del-br $OF_BRIDGE

    echo "Restarting bridged ifaces."
    sudo ip link set $EXT_NIC down
    sudo ip link set $EXT_NIC up
    sudo ip link set $IOT_NIC down
    sudo ip link set $IOT_NIC up
}

connect_interface() {
    local bridge_name="$1"
    local interface="$2"
    local port_num="$3"

    echo "Connecting NIC $interface"
    sudo ethtool -K $interface gro off

    # Add the given port_name as a port to the bridge, and assign it the given OpenFlow port number.
    sudo ovs-vsctl add-port $bridge_name $interface -- set interface $interface ofport_request=$port_num
    sudo ovs-ofctl mod-port $bridge_name $interface up

    # Remove the IP address from the NIC since it no longer makes sense.
    sudo ip addr flush dev $interface
}

setup_nic_bridge() {
    local bridge_name="$1"
    local nic1_name="$2"
    local nic2_name="$3"
    local local_ip="$4"
    local bcast_ip="$5"

    echo "Setting up NIC OVS bridge $bridge_name"
    sudo ovs-vsctl add-br $bridge_name

    # Connect to NIC to the OVS switch in port 1.
    connect_interface $bridge_name $nic1_name 1

    # Connect to NIC to the OVS switch in port 1.
    connect_interface $bridge_name $nic2_name 2

    echo "Setting up OF params"
    sudo ip link set $bridge_name up
    sudo ovs-vsctl set bridge $bridge_name protocols=OpenFlow13
    sudo ovs-vsctl set-controller $bridge_name ptcp:$OF_BRIDGE_PORT
    sudo ovs-vsctl set controller $bridge_name connection-mode=out-of-band

    # Attach the IP set up of the IOT network to the bridge.
    sudo ip addr add ${local_ip}/24 broadcast ${bcast_ip} dev $bridge_name

    echo "Bridge setup complete"
 }

setup_passthrough_bridge_rules() {
    local bridge_name="$1"
    local local_ip="$2"

    # Rule to drop mDNS requests and IPv6 traffic.
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=160,udp,tp_dst=5353,actions=drop"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=160,ipv6,actions=drop"

    # Set rules to be able to process requests and responses to our own IP.
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=150,arp,nw_src=${local_ip},actions=output:1"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=150,arp,in_port=1,nw_dst=${local_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=150,ip,ip_src=${local_ip},actions=normal"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=150,ip,in_port=1,ip_dst=${local_ip},actions=normal"

    # Set up default rules to connect bridges together.
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=50,in_port=1,actions=output:2"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=50,in_port=2,actions=output:1"
    sudo ovs-ofctl -O OpenFlow13 add-flow $bridge_name "priority=0,actions=drop"
}

# Setup
echo "Beginning switches setup..."

clear_bridge

# Stop here if we just want to clear the bridge.
if [ "$MODE" == "clear" ]
then
  exit 0
fi

# Getting IP and brodcast IP for the IoT NIC.
IOT_NIC_IP=$(ip addr show ${IOT_NIC} | grep -Po 'inet \K[\d.]+')
IOT_NIC_BROADCAST=$(ip addr show ${IOT_NIC} | grep -Po 'brd \K[\d.]+')

setup_nic_bridge $OF_BRIDGE $IOT_NIC $EXT_NIC $IOT_NIC_IP $IOT_NIC_BROADCAST
setup_passthrough_bridge_rules $OF_BRIDGE $IOT_NIC_IP

# Configure OVS DB to listen to remote commands on given TCP port.
sudo ovs-appctl -t ovsdb-server ovsdb-server/add-remote ptcp:$OVS_DB_PORT

echo "OVS switch ready"

