"""Tests Wall-E Agent."""

from __future__ import unicode_literals

import pytest
import mock

from agent.agent import *


def test_lldpctl_no_data():
    with pytest.raises(LldpDaemonMalfunction):
        assert parse_lldpctl_output("""
            <?xml version="1.0" encoding="UTF-8"?>
            <lldp label="LLDP neighbors"/>
        """, 0) == []


def test_lldpctl_with_switch():
    lldpctl_output = """
        <?xml version="1.0" encoding="UTF-8"?>
        <lldp label="LLDP neighbors">
         <interface label="Interface" name="eth1" via="LLDP" rid="1" age="0 day, 00:00:37">
          <chassis label="Chassis">
           <id label="ChassisID" type="mac">78:1d:ba:31:04:f2</id>
           <name label="SysName">fol1-s49</name>
           <descr label="SysDescr">S5352C-SI&#13;
        Huawei Versatile Routing Platform Software&#13;
        VRP (R) software, Version 5.150 (S5300 V200R005C00SPC300)&#13;
        LTD</descr>
           <mgmt-ip label="MgmtIP">178.154.142.139</mgmt-ip>
           <capability label="Capability" type="Bridge" enabled="on"/>
          </chassis>
          <port label="Port">
           <id label="PortID" type="ifname">GigabitEthernet0/0/30</id>
           <descr label="PortDescr">\</descr>
           <mfs label="MFS">10224</mfs>
           <auto-negotiation label="PMD autoneg" supported="yes" enabled="yes">
            <advertised label="Adv" type="10Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-X" hd="no" fd="yes"/>
            <advertised label="Adv" type="1000Base-T" hd="no" fd="yes"/>
            <current label="MAU oper type">1000BaseTFD - Four-pair Category 5 UTP, full duplex mode</current>
           </auto-negotiation>
           <power label="MDI Power" supported="no" enabled="no" paircontrol="no">
            <device-type label="Device type">PD</device-type>
            <pairs label="Power pairs">unknown</pairs>
            <class label="Class">unknown</class>
           </power>
          </port>
          <vlan label="VLAN" vlan-id="1">VLAN 0001</vlan>
          <vlan label="VLAN" vlan-id="604" pvid="yes"/>
          <ppvid label="PPVID" supported="no" enabled="no"/>
         </interface>
        </lldp>
    """
    expected_parsed_output = [{"switch": "fol1-s49", "port": "GigabitEthernet0/0/30", "time": 0}]
    assert parse_lldpctl_output(lldpctl_output, 0) == expected_parsed_output


def test_lldpctl_two_interfaces_one_switch():
    lldpctl_output = """
        <?xml version="1.0" encoding="UTF-8"?>
        <lldp label="LLDP neighbors">
         <interface label="Interface" name="eth1" via="LLDP" rid="1" age="0 day, 00:00:37">
          <chassis label="Chassis">
           <id label="ChassisID" type="mac">78:1d:ba:31:04:f2</id>
           <name label="SysName">fol1-s49</name>
           <descr label="SysDescr">S5352C-SI&#13;
        Huawei Versatile Routing Platform Software&#13;
        VRP (R) software, Version 5.150 (S5300 V200R005C00SPC300)&#13;
        LTD</descr>
           <mgmt-ip label="MgmtIP">178.154.142.139</mgmt-ip>
           <capability label="Capability" type="Bridge" enabled="on"/>
          </chassis>
          <port label="Port">
           <id label="PortID" type="ifname">GigabitEthernet0/0/30</id>
           <descr label="PortDescr">\</descr>
           <mfs label="MFS">10224</mfs>
           <auto-negotiation label="PMD autoneg" supported="yes" enabled="yes">
            <advertised label="Adv" type="10Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-X" hd="no" fd="yes"/>
            <advertised label="Adv" type="1000Base-T" hd="no" fd="yes"/>
            <current label="MAU oper type">1000BaseTFD - Four-pair Category 5 UTP, full duplex mode</current>
           </auto-negotiation>
           <power label="MDI Power" supported="no" enabled="no" paircontrol="no">
            <device-type label="Device type">PD</device-type>
            <pairs label="Power pairs">unknown</pairs>
            <class label="Class">unknown</class>
           </power>
          </port>
          <vlan label="VLAN" vlan-id="1">VLAN 0001</vlan>
          <vlan label="VLAN" vlan-id="604" pvid="yes"/>
          <ppvid label="PPVID" supported="no" enabled="no"/>
         </interface>
         <interface label="Interface" name="eth2" via="LLDP" rid="1" age="0 day, 00:00:37">
          <chassis label="Chassis">
           <id label="ChassisID" type="mac">78:1d:ba:31:04:f2</id>
           <name label="SysName">fol1-s49</name>
           <descr label="SysDescr">S5352C-SI&#13;
        Huawei Versatile Routing Platform Software&#13;
        VRP (R) software, Version 5.150 (S5300 V200R005C00SPC300)&#13;
        LTD</descr>
           <mgmt-ip label="MgmtIP">178.154.142.139</mgmt-ip>
           <capability label="Capability" type="Bridge" enabled="on"/>
          </chassis>
          <port label="Port">
           <id label="PortID" type="ifname">GigabitEthernet0/0/30</id>
           <descr label="PortDescr">\</descr>
           <mfs label="MFS">10224</mfs>
           <auto-negotiation label="PMD autoneg" supported="yes" enabled="yes">
            <advertised label="Adv" type="10Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-X" hd="no" fd="yes"/>
            <advertised label="Adv" type="1000Base-T" hd="no" fd="yes"/>
            <current label="MAU oper type">1000BaseTFD - Four-pair Category 5 UTP, full duplex mode</current>
           </auto-negotiation>
           <power label="MDI Power" supported="no" enabled="no" paircontrol="no">
            <device-type label="Device type">PD</device-type>
            <pairs label="Power pairs">unknown</pairs>
            <class label="Class">unknown</class>
           </power>
          </port>
          <vlan label="VLAN" vlan-id="1">VLAN 0001</vlan>
          <vlan label="VLAN" vlan-id="604" pvid="yes"/>
          <ppvid label="PPVID" supported="no" enabled="no"/>
         </interface>
        </lldp>
    """
    expected_parsed_output = [{"switch": "fol1-s49", "port": "GigabitEthernet0/0/30", "time": 0}]
    assert parse_lldpctl_output(lldpctl_output, 0) == expected_parsed_output


def test_lldpctl_two_interfaces_two_switches():
    lldpctl_output = """
        <?xml version="1.0" encoding="UTF-8"?>
        <lldp label="LLDP neighbors">
         <interface label="Interface" name="eth1" via="LLDP" rid="1" age="0 day, 00:00:37">
          <chassis label="Chassis">
           <id label="ChassisID" type="mac">78:1d:ba:31:04:f2</id>
           <name label="SysName">fol1-s49</name>
           <descr label="SysDescr">S5352C-SI&#13;
        Huawei Versatile Routing Platform Software&#13;
        VRP (R) software, Version 5.150 (S5300 V200R005C00SPC300)&#13;
        LTD</descr>
           <mgmt-ip label="MgmtIP">178.154.142.139</mgmt-ip>
           <capability label="Capability" type="Bridge" enabled="on"/>
          </chassis>
          <port label="Port">
           <id label="PortID" type="ifname">GigabitEthernet0/0/30</id>
           <descr label="PortDescr">\</descr>
           <mfs label="MFS">10224</mfs>
           <auto-negotiation label="PMD autoneg" supported="yes" enabled="yes">
            <advertised label="Adv" type="10Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-X" hd="no" fd="yes"/>
            <advertised label="Adv" type="1000Base-T" hd="no" fd="yes"/>
            <current label="MAU oper type">1000BaseTFD - Four-pair Category 5 UTP, full duplex mode</current>
           </auto-negotiation>
           <power label="MDI Power" supported="no" enabled="no" paircontrol="no">
            <device-type label="Device type">PD</device-type>
            <pairs label="Power pairs">unknown</pairs>
            <class label="Class">unknown</class>
           </power>
          </port>
          <vlan label="VLAN" vlan-id="1">VLAN 0001</vlan>
          <vlan label="VLAN" vlan-id="604" pvid="yes"/>
          <ppvid label="PPVID" supported="no" enabled="no"/>
         </interface>
         <interface label="Interface" name="eth2" via="LLDP" rid="1" age="0 day, 00:00:37">
          <chassis label="Chassis">
           <id label="ChassisID" type="mac">78:1d:ba:31:04:f2</id>
           <name label="SysName">fol1-s50</name>
           <descr label="SysDescr">S5352C-SI&#13;
        Huawei Versatile Routing Platform Software&#13;
        VRP (R) software, Version 5.150 (S5300 V200R005C00SPC300)&#13;
        LTD</descr>
           <mgmt-ip label="MgmtIP">178.154.142.139</mgmt-ip>
           <capability label="Capability" type="Bridge" enabled="on"/>
          </chassis>
          <port label="Port">
           <id label="PortID" type="ifname">GigabitEthernet0/0/31</id>
           <descr label="PortDescr">\</descr>
           <mfs label="MFS">10224</mfs>
           <auto-negotiation label="PMD autoneg" supported="yes" enabled="yes">
            <advertised label="Adv" type="10Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-T" hd="yes" fd="yes"/>
            <advertised label="Adv" type="100Base-X" hd="no" fd="yes"/>
            <advertised label="Adv" type="1000Base-T" hd="no" fd="yes"/>
            <current label="MAU oper type">1000BaseTFD - Four-pair Category 5 UTP, full duplex mode</current>
           </auto-negotiation>
           <power label="MDI Power" supported="no" enabled="no" paircontrol="no">
            <device-type label="Device type">PD</device-type>
            <pairs label="Power pairs">unknown</pairs>
            <class label="Class">unknown</class>
           </power>
          </port>
          <vlan label="VLAN" vlan-id="1">VLAN 0001</vlan>
          <vlan label="VLAN" vlan-id="604" pvid="yes"/>
          <ppvid label="PPVID" supported="no" enabled="no"/>
         </interface>
        </lldp>
    """
    expected_parsed_output = [
        {"switch": "fol1-s49", "port": "GigabitEthernet0/0/30", "time": 0},
        {"switch": "fol1-s50", "port": "GigabitEthernet0/0/31", "time": 0},
    ]
    assert sorted(parse_lldpctl_output(lldpctl_output, 0),
                  key=lambda info: info["switch"]) == expected_parsed_output


def test_lldpctl_filter_by_chassis_descr():
    lldpctl_output = """<?xml version="1.0" encoding="UTF-8"?>
        <lldp label="LLDP neighbors">
         <interface label="Interface" name="eth0" via="LLDP" rid="1" age="0 day, 04:18:45">
          <chassis label="Chassis">
           <name label="SysName">cloud-myt-6s15</name>
           <descr label="SysDescr">Huawei Versatile Routing Platform Software&#13;
        VRP (R) software, Version 8.160 (CE8850EI V200R003C00SPC810)&#13;
        Ltd.&#13;
        HUAWEI CE8850-32CQ-EI&#13;
        </descr>
          </chassis>
          <port label="Port">
           <id label="PortID" type="ifname">100GE1/0/2:4</id>
          </port>
         </interface>
         <interface label="Interface" name="eth0" via="LLDP" rid="5" age="0 day, 03:42:53">
          <chassis label="Chassis">
           <name label="SysName">oct-myt5.svc.cloud-preprod.yandex.net</name>
           <descr label="SysDescr">CloudSvm</descr>
          </chassis>
          <port label="Port">
           <id label="PortID" type="mac">52:9a:06:46:c2:77</id>
          </port>
         </interface>
        </lldp>"""
    expected_parsed_output = [{'port': '100GE1/0/2:4', 'switch': 'cloud-myt-6s15', 'time': 0}]
    assert sorted(parse_lldpctl_output(lldpctl_output, 0)) == expected_parsed_output


def test_get_lldp_from_server_info():
    open_mock = mock.mock_open(read_data='''\
{
    "cpu_model": "Intel(R) Xeon(R) Gold 6230 CPU @ 2.10GHz",
    "cpuarch": "x86_64",
    "disks_info": {
        "nonssd": [
            "/dev/sda"
        ],
        "nvme": [
            "/dev/nvme0n1"
        ],
        "ssd": []
    },
    "gencfg": [
        "ALL_QLOUD",
        "ALL_QLOUD_HOSTS",
        "ALL_RTC",
        "ALL_WALLE_TAG_QLOUD",
        "SAS_JUGGLER_CLIENT_STABLE",
        "SAS_YASM_YASMAGENT_STABLE"
    ],
    "id": "sas2-7940.search.yandex.net",
    "init": "systemd",
    "kernel": "Linux",
    "kernelrelease": "4.19.162-40",
    "lldp": [
        {
            "port": "swp4",
            "switch": "sas2-8s95.yndx.net"
        }
    ],
    "location": "sas",
    "lsb_distrib_codename": "xenial",
    "lsb_distrib_description": "Ubuntu 16.04.5 LTS",
    "lsb_distrib_id": "Ubuntu",
    "lsb_distrib_release": "16.04",
    "lui": {
        "name": "web",
        "timestamp": 1608047095
    },
    "mem_total": 515635,
    "nodename": "sas2-7940.search.yandex.net",
    "num_cpus": 80,
    "os": "Ubuntu",
    "os_family": "Debian",
    "osarch": "amd64",
    "oscodename": "xenial",
    "osfinger": "Ubuntu-16.04",
    "osfullname": "Ubuntu",
    "osmajorrelease": "16",
    "osrelease": "16.04",
    "osrelease_info": [
        16,
        4
    ],
    "points_info": {
        "/place": {
            "device": "/dev/sda4",
            "disks_info": [
                {
                    "sda": {
                        "disk_type": "hdd"
                    }
                }
            ],
            "mount_point": "/place",
            "raid": null
        },
        "/ssd": {
            "device": "/dev/nvme0n1p2",
            "disks_info": [
                {
                    "nvme0n1": {
                        "disk_type": "nvme"
                    }
                }
            ],
            "mount_point": "/ssd",
            "raid": null
        }
    },
    "qloud": {
        "installation": "ext",
        "loc": "ext.common-ci",
        "segment": "common-ci"
    },
    "walle_country": "ru",
    "walle_dc": "sas",
    "walle_location": "sas",
    "walle_project": "qloud-common-ci",
    "walle_queue": "sas-08",
    "walle_rack": "08.04.23",
    "walle_switch": "sas2-8s95",
    "walle_tags": [
        "qloud",
        "qloud-external",
        "rtc",
        "rtc.automation-enabled",
        "rtc.gpu-none",
        "rtc.reboot_segment-qloud-pre",
        "rtc.scheduler-qloud",
        "rtc.stage-prestable",
        "rtc_network",
        "skynet_installed",
        "yasm_qloud_monitored"
    ]
}
''')
    atime = int(time.time()) - 900
    sw = get_lldp_from_server_info(atime, open_mock)
    assert sw == [{"switch": "sas2-8s95.yndx.net", "port": "swp4", "time": atime}]
