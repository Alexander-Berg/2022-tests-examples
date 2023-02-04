#!/usr/bin/env python3

import ipaddress
import sys

if __name__ == '__main__':
    from lib import yandex_networks
    from lib import defs
else:
    from .lib import yandex_networks
    from .lib import defs


def get_ebpf_mask(net):
    return '0x' + ''.join(str(net.exploded).split(':')[2:4])


def get_dc_and_nettype(line):
    if line.find("HTONL") == -1:
        return None
    line = line.split()
    dc_name = line[3][3:-1]
    net_name = line[4][4:]
    return defs.DC[dc_name], defs.NETTYPE[net_name]


def get_ebpf_networks(path):
    nets = []
    dc = None
    nettype = None
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line.startswith('//'):
                continue
            if line.find('ProjectIds') >= 0:
                break
            parsed = get_dc_and_nettype(line)
            if parsed is not None:
                dc = parsed[0]
                nettype = parsed[1]
            found = line.find('// 2a02')
            if found >= 0:
                net = ipaddress.IPv6Network(line[found + 3:])
                # check correct mask on the same line
                mask = get_ebpf_mask(net)
                assert line.find(mask) >= 0, "Missed network {} mask {} on line '{}'".format(net, mask, line)
                nets.append(yandex_networks.YandexNetwork(dc, nettype, net))
    return sorted(nets)


def get_ebpf_l3_networks(path):
    nets = []
    with open(path) as f:
        for line in f:
            line = line.strip()
            found = line.find('// L3 ')
            if found >= 0:
                net = ipaddress.IPv6Network(line[found + 6:])
                # check correct mask on the same line
                mask = get_ebpf_mask(net)
                assert line.find(mask) >= 0, "Missed L3 network {} mask {} on line '{}'".format(net, mask, line)
                nets.append(net)
    return sorted(nets)


def _test_ebpf_networks(path):
    nets = yandex_networks.get_networks()
    ebpf_nets = get_ebpf_networks(path)

    for n in nets:
        assert n in ebpf_nets, 'Missed network {} in {}'.format(n, path)

    for n in ebpf_nets:
        assert n in nets, 'Extra network {} in {}'.format(n, path)


def _test_ebpf_l3_networks(path):
    nets = yandex_networks.get_l3_networks()
    ebpf_nets = get_ebpf_l3_networks(path)

    for n in nets:
        assert n in ebpf_nets, 'Missed L3 network {} in {}'.format(n, path)

    for n in ebpf_nets:
        assert n in nets, 'Extra L3 network {} in {}'.format(n, path)


if __name__ != '__main__':
    import yatest.common

    def test_ebpf_networks():
        ebpf_path = yatest.common.source_path('infra/ebpf-agent/lib/nets.cpp')
        _test_ebpf_networks(ebpf_path)

    def test_ebpf_l3_networks():
        ebpf_l3_path = yatest.common.source_path('infra/ebpf-agent/progs/include/utils.h')
        _test_ebpf_l3_networks(ebpf_l3_path)


def main():
    if len(sys.argv) < 3:
        print("Usage: %s nets_source l3_nets_source" % sys.argv[0])
        exit(-1)

    ebpf_path = sys.argv[1]
    ebpf_l3_path = sys.argv[2]
    _test_ebpf_networks(ebpf_path)
    _test_ebpf_l3_networks(ebpf_l3_path)
    print("Networks are OK!")


if __name__ == '__main__':
    main()
