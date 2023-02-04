#!/usr/bin/env python3

import subprocess
import ipaddress
import resource
import tempfile
import argparse
import ctypes
import errno
import json
import sys
import os

from defs import DC, dc_name_to_enum

bpftool = None
def run_bpftool(args):
    global bpftool
    if bpftool is None:
        bpftool = subprocess.check_output(['ya', 'tool', 'bpftool', '--print-path']).decode('utf-8')[:-1]
    try:
        return json.loads(subprocess.check_output([bpftool, '-j'] + list(args)).decode('utf-8'))
    except subprocess.CalledProcessError as e:
        print(json.dumps(json.loads(e.stdout.decode('utf-8')), indent=4))
        raise

def setup():
    subprocess.check_call(['mkdir', '-p', '/tmp/bpffs'])
    subprocess.check_call(['mount', '-t', 'bpf', 'bpf', '/tmp/bpffs'])

def cleanup():
    if os.path.isdir('/tmp/bpffs'):
        subprocess.check_call(['umount', '-f', '/tmp/bpffs'])
        subprocess.check_call(['rmdir', '/tmp/bpffs'])

def generate_data_in(net):
    def generate_mac():
        return b'\x52\x54\00' + os.urandom(3)

    eth = generate_mac() + generate_mac() + b'\x86\xdd'
    ip6 = b'\x60\x00\x00\x00\x00\x00\x3b\x40'
    src = net[1]
    dst = net[2]

    data = b''
    data += eth
    data += ip6
    data += src.packed
    data += dst.packed

    return data

def struct_to_bytes(s):
    b = ctypes.create_string_buffer(ctypes.sizeof(s))
    ctypes.memmove(b, ctypes.addressof(s), ctypes.sizeof(s))
    return b.raw

def struct_from_bytes(s, b):
    ctypes.memmove(ctypes.addressof(s), b, ctypes.sizeof(s))

def generate_ctx_in(**kwargs):
    class BpfSkbuff(ctypes.Structure):
        _fields_ = [
            ("len", ctypes.c_uint32),
            ("pkt_type", ctypes.c_uint32),
            ("mark", ctypes.c_uint32),
            ("queue_mapping", ctypes.c_uint32),
            ("protocol", ctypes.c_uint32),
            ("vlan_present", ctypes.c_uint32),
            ("vlan_tci", ctypes.c_uint32),
            ("vlan_proto", ctypes.c_uint32),
            ("priority", ctypes.c_uint32),
            ("ingress_ifindex", ctypes.c_uint32),
            ("ifindex", ctypes.c_uint32),
            ("tc_index", ctypes.c_uint32),
            ("cb", ctypes.c_uint32 * 5),
            ("hash", ctypes.c_uint32),
            ("tc_classid", ctypes.c_uint32),
            ("data", ctypes.c_uint32),
            ("data_end", ctypes.c_uint32),
            ("napi_id", ctypes.c_uint32),
            ("family", ctypes.c_uint32),
            ("remote_ip4" , ctypes.c_uint32),    # Stored in network byte order
            ("local_ip4", ctypes.c_uint32),      # Stored in network byte order
            ("remote_ip6", ctypes.c_uint32 * 4), # Stored in network byte order
            ("local_ip6", ctypes.c_uint32 * 4),  # Stored in network byte order
            ("remote_port", ctypes.c_uint32),    # Stored in network byte order
            ("local_port", ctypes.c_uint32),     # Stored in host byte order
        ]

    skbuff = BpfSkbuff(**kwargs)
    return struct_to_bytes(skbuff)

def hex_list_to_bytes(l):
     return bytes.fromhex(''.join(h[2:] if h.startswith('0x') else h for h in l))

class NetStat(ctypes.Structure):
    _fields_ = [
        ("packets", ctypes.c_uint64),
        ("bytes", ctypes.c_uint64),
    ]

    def __add__(self, other):
        return NetStat(self.packets + other.packets, self.bytes + other.bytes)

    def __repr__(self):
        return 'packets={} bytes={}'.format(self.packets, self.bytes)

class CgNetStat(ctypes.Structure):
    _fields_ = [
        ("localhost", NetStat),
        ("uplink", NetStat),
    ]

    def __add__(self, other):
        return CgNetStat(self.localhost + other.localhost, self.uplink + other.uplink)

    def __repr__(self):
        return 'localhost: {}, uplink: {}'.format(self.localhost, self.uplink)

class CgNetStatDc(ctypes.Structure):
    _fields_ = [
        ("localhost", NetStat),
        ("dc", NetStat * len(DC)),
    ]

    def __add__(self, other):
        stat = CgNetStatDc()
        stat.localhost = self.localhost + other.localhost
        for dc in DC:
            stat.dc[dc] = self.dc[dc] + other.dc[dc]
        return stat

    def __repr__(self):
        ret = 'localhost: {}'.format(self.localhost)
        for dc in DC:
            ret.append(', {}: {}'.format(dc.name.lower(), self.dc[dc]))
        return ret

def parse_map_value(v):
    stat = CgNetStatDc()
    struct_from_bytes(stat, v)
    return stat

def aggregate_cpu_values(values):
    stat = CgNetStatDc()
    for v in values:
        stat += v
    return stat

def prog_test_run(obj, repeat, net, dc):
    basename=os.path.splitext(os.path.basename(obj))[0]
    pinned='/tmp/bpffs/' + basename
    cgroup = '/sys/fs/cgroup/unified/test'

    try:
        run_bpftool(['prog', 'load', obj, pinned])
        #map_id = str(run_bpftool(['prog', 'show', 'pinned', pinned])['map_ids'][0])
        #run_bpftool(['map', 'pin', 'id', map_id, pinned + '_map'])

        if not os.path.exists(cgroup):
            os.makedirs(cgroup)
        cgroup_id = os.stat(cgroup).st_ino
        with open('/sys/fs/cgroup/unified/test/cgroup.procs', 'w') as f:
            f.write(str(os.getpid()))

        run_bpftool(['cgroup', 'attach', cgroup, 'egress', 'pinned', pinned])

        with tempfile.NamedTemporaryFile() as data_in, tempfile.NamedTemporaryFile() as ctx_in:
            data_in.write(generate_data_in(net))
            data_in.seek(0)

            ctx_in.write(generate_ctx_in(ifindex=2))
            ctx_in.seek(0)

            result = run_bpftool(['prog', 'run', 'pinned', pinned, 'data_in', data_in.name, 'ctx_in', ctx_in.name, 'repeat', str(repeat)])
            duration = result["duration"]
            print('{} average duration in net {}: {} ns'.format(basename, net, duration))
            return duration

            #dump = run_bpftool(['map', 'dump', 'id', map_id])
            #for e in dump:
            #    if cgroup_id == int.from_bytes(hex_list_to_bytes(e['key'][:8]), byteorder='little'):
            #        values = []
            #        for v in e['values']:
            #            values.append(parse_map_value(hex_list_to_bytes(v['value'])))
            #        stat = aggregate_cpu_values(values)
            #        assert stat.dc[dc].packets >= repeat
    except:
        print(sys.exc_info()[1], file=sys.stderr)
        raise
    finally:
        run_bpftool(['cgroup', 'detach', cgroup, 'egress', 'pinned', pinned])
        subprocess.check_call(['rm', '-rf', pinned + '_map'])
        subprocess.check_call(['rm', '-rf', pinned])

def file_path(path):
    if os.path.isfile(path):
        return path
    else:
        raise FileNotFoundError(errno.ENOENT, os.strerror(errno.ENOENT), path)

def main():
    try:
        parser=argparse.ArgumentParser()
        parser.add_argument('-t', '--tool', type=file_path)
        parser.add_argument('-r', '--repeat', type=int, default=1000000)
        parser.add_argument('-o', '--object', dest='objects', nargs='+', type=file_path, default=[])
        parser.add_argument('-n', '--nets', type=file_path)
        args = parser.parse_args()

        setup()

        global bpftool
        bpftool = args.tool

        MLOCK = 300 * 1024 * 1024
        resource.setrlimit(resource.RLIMIT_MEMLOCK, (MLOCK, MLOCK))

        nets = []
        if args.nets is not None:
            with open(args.nets) as f:
                for line in f:
                    line.strip()
                    net, dc, _ = line.split(' ')
                    dc = dc_name_to_enum(dc)
                    nets.append((ipaddress.IPv6Network(net), dc))
        if not nets:
            nets.append((ipaddress.IPv6Network('2a02:6b8:c02::/48'), DC.SAS1))

        for obj in args.objects:
            durations = []
            for net, dc in nets:
                duration = prog_test_run(obj, args.repeat, net=net, dc=dc)
                durations.append(duration)
            print("Max duration {} ns, min duration {} ns\n".format(max(durations), min(durations)))
    except:
        print(sys.exc_info()[1], file=sys.stderr)
    finally:
        cleanup()

if __name__ == '__main__':
    main()
