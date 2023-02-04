import ipaddress
import tempfile
import ctypes
import pytest
import kern
import os


@pytest.mark.xfail(not kern.kernel_in('4.19.144', '5.4.64'), reason='not fixed')
def test_bpf_prog_test_run(find_bin, bpffs):
    # https://st.yandex-team.ru/RTCNETWORK-466

    def generate_data_in(net=ipaddress.IPv6Network('fe80::/10')):
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
                ("remote_ip4" , ctypes.c_uint32),     # Stored in network byte order
                ("local_ip4", ctypes.c_uint32),       # Stored in network byte order
                ("remote_ip6", ctypes.c_uint32 * 4),  # Stored in network byte order
                ("local_ip6", ctypes.c_uint32 * 4),   # Stored in network byte order
                ("remote_port", ctypes.c_uint32),     # Stored in network byte order
                ("local_port", ctypes.c_uint32),      # Stored in host byte order
            ]

        def struct_to_bytes(s):
            b = ctypes.create_string_buffer(ctypes.sizeof(s))
            ctypes.memmove(b, ctypes.addressof(s), ctypes.sizeof(s))
            return b.raw

        skbuff = BpfSkbuff(**kwargs)
        return struct_to_bytes(skbuff)

    bpftool_bin = find_bin('bpftool')
    pinned = bpffs.load_prog(bpftool_bin, 'infra/ebpf-agent/progs/obj/dummy_egress.o')
    with tempfile.NamedTemporaryFile() as data_in, tempfile.NamedTemporaryFile() as ctx_in:
        data_in.write(generate_data_in())
        data_in.seek(0)

        ctx_in.write(generate_ctx_in())
        ctx_in.seek(0)

        result = kern.run_bpftool(['prog', 'run', 'pinned', pinned, 'data_in', data_in.name, 'ctx_in', ctx_in.name, 'repeat', '100'], bpftool_bin=bpftool_bin)
        assert "duration" in result
