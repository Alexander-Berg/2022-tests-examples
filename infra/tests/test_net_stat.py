import socket
import random


def test_net_stat_dc(all_networks, run_net_stat, get_net_stats, setup_veth_peer):
    setup_veth_peer([n.net[1] for n in all_networks])

    prev_stats = None
    for n in all_networks:
        addr = str(n.net[1])  # ::1
        dest = (addr, 80)

        packets = random.randrange(10)
        for i in range(packets):
            # can't reuse socket due to ICMP port unreachable error
            # so just create new one in the loop
            sock = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
            sock.connect(dest)
            sock.sendto(b'\x00' * 1000, dest)
            sock.close()

        cur_stats = get_net_stats()

        dc_key = str(n.dc).replace('DC.', 'DC_')
        nettype_key = str(n.nettype).replace('NETTYPE.', 'NET_')
        cur_packets = cur_stats[dc_key][nettype_key]['packets']
        if prev_stats is not None:
            prev_packets = prev_stats[dc_key][nettype_key]['packets']
            assert cur_packets - prev_packets == packets, 'Incorrect packet accounting for {}'.format(n)
        else:
            assert cur_packets == packets, 'Incorrect packet accounting for {}'.format(n)

        prev_stats = cur_stats
