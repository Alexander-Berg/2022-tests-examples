#!/usr/bin/env python

import mock

from infra.netconfig.lib import fixutil
from infra.netconfig.proto import fixutil_pb2

from infra.netconfig.lib.master import PBR_PRIORITY


def create_interface(tmpdir, content=None):
    if not content:
        content = """
auto lo
iface lo inet loopback

auto eth0
iface eth0 inet6 auto
  privext 0
  ya-netconfig-networks-url https://noc-export.yandex.net/rt/l3-segments2.json
  mtu 9000
  debug yes
  ya-netconfig-project-id-host-method mac
  ya-netconfig-set-group yes
  project-id 604
  pre-up /sbin/ethtool -K $IFACE lro off tso off
"""

    network_interfaces = str(tmpdir.join('interfaces'))
    with open(network_interfaces, 'w') as f:
        f.write(content)
    interfaces = fixutil.get_interfaces(network_interfaces)
    interfaces = [iface for iface in interfaces if iface.name != 'lo']
    assert len(interfaces) == 1
    iface = interfaces[0]
    return iface


def test_prepare_ipv6_eui64_address():
    result_struct = [
        ('fe80::/64', '00:14:22:01:23:45', 'fe80::214:22ff:fe01:2345'),
        ('2a02:6b8:b000:657::/64', '90:2b:34:c1:d0:6a', '2a02:6b8:b000:657:922b:34ff:fec1:d06a'),
        ('2a02:6b8:c0e:32::/64', '1c:1b:0d:b7:9f:0f', '2a02:6b8:c0e:32:1e1b:dff:feb7:9f0f'),
    ]
    for prefix, mock_mac_address, result in result_struct:
        with mock.patch('infra.netconfig.lib.master.get_mac_address', return_value=mock_mac_address):
            assert fixutil.prepare_ipv6_eui64_address('dummy', prefix) == result


def test_prepare_ipv6_backbone_global(tmpdir):
    iface = create_interface(tmpdir)
    result_struct = [
        # TODO: Add more project ids
        ('2a02:6b8:c0e:32::/64', '1c:1b:0d:b7:9f:0f', '2a02:6b8:c0e:32:0:604:db7:9f0f/64'),
        ('2a02:6b8:c0e:78::/64', '1c:1b:0d:b7:a7:5c', '2a02:6b8:c0e:78:0:604:db7:a75c/64'),
        ('2a02:6b8:b000:657::/64', '90:2b:34:c1:d0:6a', '2a02:6b8:b000:657:922b:34ff:fec1:d06a/64'),
        ('2a02:6b8:b000:63f::/64', '90:2b:34:cf:37:e6', '2a02:6b8:b000:63f:922b:34ff:fecf:37e6/64'),
    ]
    for prefix, mock_mac_address, cidr in result_struct:
        with mock.patch('infra.netconfig.lib.master.get_mac_address', return_value=mock_mac_address):
            res_ip, res_cidr = fixutil.prepare_ipv6_backbone_global(iface, prefix)
            assert res_cidr == cidr
            assert res_ip == cidr.split('/')[0]


def form_proto(iface_name, prefix, ipv6_address, cidr, mtu, group):
    proto = fixutil_pb2.InterfaceState()
    proto.name = iface_name
    proto.prefix = prefix
    proto.ipv6_address = ipv6_address
    proto.cidr = cidr
    proto.mtu = mtu
    proto.group = group
    return proto


def test_prepare_base_phys_interface_proto(tmpdir):
    iface = create_interface(tmpdir)

    result_struct = [
        ('eth0', '2a02:6b8:b000:657::/64', '2a02:6b8:b000:657:922b:34ff:fec1:d06a',
            '2a02:6b8:b000:657:922b:34ff:fec1:d06a/64', 9000, 'backbone', '90:2b:34:c1:d0:6a'),

        ('eth0', '2a02:6b8:c0e:32::/64',  '2a02:6b8:c0e:32:0:604:db7:9f0f',
            '2a02:6b8:c0e:32:0:604:db7:9f0f/64', 9000, 'backbone', '1c:1b:0d:b7:9f:0f'),

        ('eth0', '2a02:6b8:b000:6039::/64', '2a02:6b8:b000:6039:f652:14ff:fe8b:f550',
            '2a02:6b8:b000:6039:f652:14ff:fe8b:f550/64', 9000, 'backbone', 'f4:52:14:8b:f5:50'),
    ]
    for struct in result_struct:
        target_state = form_proto(*struct[:-1])
        mock_prefix = struct[1]
        mock_mac_address = struct[-1]
        with mock.patch('infra.netconfig.lib.master.check_is_configurable', return_value=None):
            with mock.patch('infra.netconfig.lib.master.get_ra_prefix', return_value=mock_prefix):
                with mock.patch('infra.netconfig.lib.master.get_mac_address', return_value=mock_mac_address):
                    with mock.patch('infra.netconfig.lib.master.set_mtu') as m:
                        state = fixutil.prepare_base_phys_interface_proto(iface)
                        assert state.name == target_state.name
                        assert state.prefix == target_state.prefix
                        assert state.ipv6_address == target_state.ipv6_address
                        assert state.cidr == target_state.cidr
                        assert state.mtu == target_state.mtu
                        assert state.group == target_state.group
                        assert m.call_count == 1


def prepare_ip_rule_show_retval(bunch_of_rules):
    ip_rule_show_stdout = '0:  from all lookup local\n' + \
        '\n'.join(['{}\n'.format(rule) for rule in bunch_of_rules]) + \
        '\n32766:  from all lookup main\n'

    ip_rule_show_retval = (ip_rule_show_stdout, '', 0)
    return ip_rule_show_retval


def test_fix_rules():

    # No mtn vlan ifaces
    target_states = fixutil_pb2.TargetState()
    target_iface = fixutil_pb2.InterfaceState()
    target_states.interfaces.extend([target_iface])

    assert fixutil.fix_rules(target_states.interfaces, dry_run=True) is False
    with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
        with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
            fixutil.fix_rules(target_states.interfaces, dry_run=False)
            assert add_r.call_count == 0
            assert del_r.call_count == 0

    # backbone and fastbone mtn with tun-rules
    rules_with_6010 = (
        '16000: from 2a02:6b8:0:3400::3:126 lookup 6010',
        '16000: from 2a02:6b8:0:3400::335 lookup 6010',
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:1f00::/57 to 2a02:6b8:fc00:1f16::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:1f00::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:1f80::/57 to 2a02:6b8:c08:1f96::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:1f80::/57 lookup 688',
    )

    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_with_6010)

    # create bb
    target_states = fixutil_pb2.TargetState()
    target_iface.name = 'vlan688'
    target_iface.prefix = '2a02:6b8:c08:5e00::/57'
    target_iface.network = '2a02:6b8:c08:5e09::/64'
    target_states.interfaces.extend([target_iface])

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert 'Found rules with prefixes not from MTN' in fixutil.fix_rules(target_states.interfaces, dry_run=True)
        assert 'Found rules with prefixes not from MTN' in fixutil.fix_rules(target_states.interfaces, dry_run=False)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                assert add_r.call_count == 0
                assert del_r.call_count == 0

    # check bb - diff exists
    rules_bb = (
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:1f80::/57 to 2a02:6b8:c08:1f96::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:1f80::/57 lookup 688',
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_bb)

    del_r_calls = [mock.call(rule) for rule in rules_bb]
    add_r_calls = []
    for iface in target_states.interfaces:
        vlan_id = iface.name.replace('vlan', '')
        add_r_calls.append(mock.call(str(PBR_PRIORITY + 5) + ': from {} lookup {}'.format(iface.prefix, vlan_id)))
        add_r_calls.append(mock.call(str(PBR_PRIORITY) + ': from {} to {} lookup main'.format(iface.prefix, iface.network)))

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert 'Found diff in rules' in fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                add_r.assert_has_calls(add_r_calls, any_order=True)
                assert add_r.call_count == 2
                assert del_r.call_count == 2

    # check bb vs fb - diff exists
    rules_fb = (
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:5e00::/57 to 2a02:6b8:fc00:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788',
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_fb)
    del_r_calls = [mock.call(rule) for rule in rules_fb]

    add_r_calls = []
    for iface in target_states.interfaces:
        vlan_id = iface.name.replace('vlan', '')
        add_r_calls.append(mock.call(str(PBR_PRIORITY + 5) + ': from {} lookup {}'.format(iface.prefix, vlan_id)))
        add_r_calls.append(mock.call(str(PBR_PRIORITY) + ': from {} to {} lookup main'.format(iface.prefix, iface.network)))

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert 'Found diff in rules' in fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                add_r.assert_has_calls(add_r_calls, any_order=True)
                assert add_r.call_count == 2
                assert del_r.call_count == 2

    # create fb iface
    # check bb and fb - there are old rules and new rule in target state
    target_iface_fb = fixutil_pb2.InterfaceState()
    target_iface_fb.name = 'vlan788'
    target_iface_fb.prefix = '2a02:6b8:fc00:5e00::/57'
    target_iface_fb.network = '2a02:6b8:fc00:5e09::/64'
    target_states.interfaces.extend([target_iface_fb])
    rules_with_old = (
        # 'from 2a02:6b8:fc00:5e00::/57 to 2a02:6b8:fc00:5e09::/64 lookup main'  # new rule
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e00::/57 to 2a02:6b8:c08:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e00::/57 lookup 688',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e01::/57 to 2a02:6b8:c08:5e10::/64 lookup main',  # old rule
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e01::/57 lookup main',  # old rule
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_with_old)

    del_r_calls = [
        mock.call(str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e01::/57 to 2a02:6b8:c08:5e10::/64 lookup main'),
        mock.call(str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e01::/57 lookup main'),
    ]

    add_r_calls = []
    for iface in target_states.interfaces:
        if iface.name == 'vlan788':
            vlan_id = iface.name.replace('vlan', '')
            add_r_calls.append(mock.call(str(PBR_PRIORITY) + ': from {} to {} lookup main'.format(iface.prefix, iface.network)))

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert 'Found diff in rules' in fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                add_r.assert_has_calls(add_r_calls, any_order=True)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                assert add_r.call_count == 1
                assert del_r.call_count == 2

    # Test completly new state for both bb and fb
    current_old_rules = (
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:a100::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:a100::/57 to 2a02:6b8:c08:a175::/64 lookup main',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:a100::/57 to 2a02:6b8:fc08:a175::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:a100::/57 lookup 688',
    )

    ip_rule_show_retval = prepare_ip_rule_show_retval(current_old_rules)

    del_r_calls = [mock.call(rule) for rule in current_old_rules]

    add_r_calls = []
    for iface in target_states.interfaces:
        vlan_id = iface.name.replace('vlan', '')
        add_r_calls.append(mock.call(str(PBR_PRIORITY + 5) + ': from {} lookup {}'.format(iface.prefix, vlan_id)))
        add_r_calls.append(mock.call(str(PBR_PRIORITY) + ': from {} to {} lookup main'.format(iface.prefix, iface.network)))

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                add_r.assert_has_calls(add_r_calls, any_order=True)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                assert add_r.call_count == 4
                assert del_r.call_count == 4

    # Bad rules and ok rules in current rules
    rules_double = (
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:a100::/57 lookup 788',  # old rule
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:a100::/57 to 2a02:6b8:c08:a175::/64 lookup main',  # old rule
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:a100::/57 to 2a02:6b8:fc08:a175::/64 lookup main',  # old rule
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:a100::/57 lookup 688',  # old rule
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:5e00::/57 to 2a02:6b8:fc00:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e00::/57 to 2a02:6b8:c08:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e00::/57 lookup 688',
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_double)

    del_r_calls = [mock.call(rule) for rule in current_old_rules if 'a100' in rule]

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                assert add_r.call_count == 0
                assert del_r.call_count == 4

    # Fix priority
    rules_fix_priority = (
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:5e00::/57 to 2a02:6b8:fc00:5e09::/64 lookup main',
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e00::/57 to 2a02:6b8:c08:5e09::/64 lookup main',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e00::/57 lookup 688',
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_fix_priority)

    del_r_calls = [mock.call(rule) for rule in rules_fix_priority if 'main' not in rule]
    add_r_calls = [
        mock.call(str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788'),
        mock.call(str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e00::/57 lookup 688'),
    ]

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                add_r.assert_has_calls(add_r_calls, any_order=True)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                assert add_r.call_count == 2
                assert del_r.call_count == 2

    # Fix priority. Have dual rules with different priorities
    rules_fix_priority = (
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:5e00::/57 to 2a02:6b8:fc00:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e00::/57 to 2a02:6b8:c08:5e09::/64 lookup main',
        '18000: from 2a02:6b8:c08:5e00::/57 to 2a02:6b8:c08:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e00::/57 lookup 688',
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_fix_priority)

    del_r_calls = [mock.call(rule) for rule in rules_fix_priority if '18000' in rule]

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert fixutil.fix_rules(target_states.interfaces, dry_run=True)
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                del_r.assert_has_calls(del_r_calls, any_order=True)
                assert del_r.call_count == 1
                assert add_r.call_count == 0

    # OK
    rules_ok = (
        str(PBR_PRIORITY) + ': from 2a02:6b8:fc00:5e00::/57 to 2a02:6b8:fc00:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:fc00:5e00::/57 lookup 788',
        str(PBR_PRIORITY) + ': from 2a02:6b8:c08:5e00::/57 to 2a02:6b8:c08:5e09::/64 lookup main',
        str(PBR_PRIORITY + 5) + ': from 2a02:6b8:c08:5e00::/57 lookup 688',
    )
    ip_rule_show_retval = prepare_ip_rule_show_retval(rules_ok)

    with mock.patch('infra.netconfig.lib.master.run_cmd', return_value=ip_rule_show_retval):
        assert fixutil.fix_rules(target_states.interfaces, dry_run=True) is None
        with mock.patch('infra.netconfig.lib.fixutil.add_rule') as add_r:
            with mock.patch('infra.netconfig.lib.fixutil.del_rule') as del_r:
                fixutil.fix_rules(target_states.interfaces, dry_run=False)
                assert add_r.call_count == 0
                assert del_r.call_count == 0


def test_fix_mtu():
    target_iface = fixutil_pb2.InterfaceState()
    # NOT OK
    current_mtu = 1450
    target_iface.mtu = 9000
    with mock.patch('infra.netconfig.lib.master.get_mtu', return_value=current_mtu):
        with mock.patch('infra.netconfig.lib.master.set_interface_group') as sig:
            assert fixutil.fix_link_and_mtu(target_iface, dry_run=True)
            with mock.patch('infra.netconfig.lib.master.set_mtu') as m:
                fixutil.fix_link_and_mtu(target_iface, dry_run=False)
                assert m.called
            assert sig.called
    # OK
    current_mtu = target_iface.mtu = 1450
    with mock.patch('infra.netconfig.lib.master.get_mtu', return_value=current_mtu):
        with mock.patch('infra.netconfig.lib.master.set_interface_group') as sig:
            assert fixutil.fix_link_and_mtu(target_iface, dry_run=True) is False
            with mock.patch('infra.netconfig.lib.master.set_mtu') as m:
                fixutil.fix_link_and_mtu(target_iface, dry_run=False)
                assert m.called is False
            assert sig.called


def test_fix_def_route():
    # set((dev, gw, mtu, prefix, table))
    ok_def_routes = {('eth0', 'fe80::1', 1450, 'default', 'main'), ('vlan688', 'fe80::1', 1450, 'default', '688')}
    fix_eth_route = {('eth0', 'fe80::1', 9000, 'default', 'main'), ('vlan688', 'fe80::1', 1450, 'default', '688')}
    fix_vlan688_route = {('eth0', 'fe80::1', 1450, 'default', 'main'), ('vlan688', 'fe80::1', 9000, 'default', '688')}

    ignore_6010_table_with_ok_others = {
        ('eth0', 'fe80::1', 1450, 'default', 'main'),
        ('vlan688', 'fe80::1', 1450, 'default', '688'),
        ('eth0', 'fe80::1', 9000, 'default', '6010'),
    }

    ignore_6010_table_but_fix_eth = {
        ('eth0', 'fe80::1', 9000, 'default', 'main'),
        ('vlan688', 'fe80::1', 1450, 'default', '688'),
        ('eth0', 'fe80::1', 9000, 'default', '6010'),
    }

    ignore_6010_table_but_fix_vlan = {
        ('eth0', 'fe80::1', 1450, 'default', 'main'),
        ('vlan688', 'fe80::1', 9000, 'default', '688'),
        ('eth0', 'fe80::1', 9000, 'default', '6010'),
    }
    ignore_6010_table_but_fix_others = {
        ('eth0', 'fe80::1', 9000, 'default', 'main'),
        ('vlan688', 'fe80::1', 9000, 'default', '688'),
        ('eth0', 'fe80::1', 9000, 'default', '6010'),
    }

    # OK
    with mock.patch('infra.netconfig.lib.master.add_route') as m:
        assert fixutil.fix_def_routes(ok_def_routes, dry_run=True) is False
        assert fixutil.fix_def_routes(ok_def_routes, dry_run=False) is False
        assert m.call_count == 0

    # Need to fix eth0 and then vlan688 def route
    sum_call_count = 0
    for route_set in (fix_eth_route, fix_vlan688_route):
        with mock.patch('infra.netconfig.lib.master.add_route') as m:
            assert fixutil.fix_def_routes(route_set, dry_run=True) is True
            assert fixutil.fix_def_routes(route_set, dry_run=False) is False
            assert m.call_count == 1
            sum_call_count += m.call_count
    assert sum_call_count == 2

    # Ignoring 6010 with OK others
    with mock.patch('infra.netconfig.lib.master.add_route') as m:
        assert fixutil.fix_def_routes(ignore_6010_table_with_ok_others, dry_run=True) is False
        assert fixutil.fix_def_routes(ignore_6010_table_with_ok_others, dry_run=False) is False
        assert m.call_count == 0

    # Ignoring 6010 but fix eth0
    with mock.patch('infra.netconfig.lib.master.add_route') as m:
        assert fixutil.fix_def_routes(ignore_6010_table_but_fix_eth, dry_run=True) is True
        assert fixutil.fix_def_routes(ignore_6010_table_but_fix_eth, dry_run=False) is False
        assert m.call_count == 1
        args, _ = m.call_args
        assert 'main' in args

    # Ignoring 6010 but fix 688
    with mock.patch('infra.netconfig.lib.master.add_route') as m:
        assert fixutil.fix_def_routes(ignore_6010_table_but_fix_vlan, dry_run=True) is True
        assert fixutil.fix_def_routes(ignore_6010_table_but_fix_vlan, dry_run=False) is False
        assert m.call_count == 1
        args, _ = m.call_args
        assert '688' in args

    # Ignoring 6010 but fix both
    sum_call_count = 0
    with mock.patch('infra.netconfig.lib.master.add_route') as m:
        assert fixutil.fix_def_routes(ignore_6010_table_but_fix_others, dry_run=True) is True
        assert fixutil.fix_def_routes(ignore_6010_table_but_fix_others, dry_run=False) is False
        assert m.call_count == 2
        for c in m.call_list:
            args, _ = c.call_args
            assert '6010' not in args
            assert 'main' in args or '688' in args


def test_need_to_fix():
    results = []
    assert fixutil.need_to_fix(results) is False

    results = [fixutil.NOT_MTN_RULES_MSG]
    assert fixutil.need_to_fix(results) is False

    results = [fixutil.NOT_MTN_RULES_MSG] * 2
    assert fixutil.need_to_fix(results) is False

    results = ['eth0:ip']
    assert fixutil.need_to_fix(results) is True

    results = ['eth0:ip', 'vlan688:routes']
    assert fixutil.need_to_fix(results) is True

    results = ['eth0:ip', 'vlan688:routes', fixutil.NOT_MTN_RULES_MSG]
    assert fixutil.need_to_fix(results) is True

    results = ['eth0:ip', 'vlan688:routes', fixutil.NOT_MTN_RULES_MSG, fixutil.NOT_MTN_RULES_MSG]
    assert fixutil.need_to_fix(results) is True
