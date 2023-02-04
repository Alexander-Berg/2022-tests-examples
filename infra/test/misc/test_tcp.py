import kern
import pytest
import subprocess
import socket
import threading


def serve(ln):
    try:
        conn, _ = ln.accept()
        conn.close()
    except OSError as e:
        if e.errno != 9:
            raise


@pytest.mark.parametrize('proto, addr, iptables_cmd', [
    (socket.AF_INET, "127.0.0.1", "iptables"),
    (socket.AF_INET6, "::1", "ip6tables"),

])
@pytest.mark.skipif(not kern.kernel_in('5.4.131-18'), reason="not fixed")
def test_tcp_accept_fwmark(sysctl, proto, addr, iptables_cmd):
    tcp_fwmark_accept = sysctl['net.ipv4.tcp_fwmark_accept']
    with socket.socket(proto) as conn, socket.socket(proto) as ln:
        ln.bind((addr, 0))
        port = ln.getsockname()[1]
        conn.setsockopt(socket.SOL_SOCKET, socket.SO_MARK, port)
        conn.settimeout(5)
        ln.settimeout(5)
        rules = [
            ['OUTPUT', '-m', 'mark', '--mark', str(port), '-p', 'tcp', '--sport', str(port), '-j', 'ACCEPT'],
            ['OUTPUT', '-p', 'tcp', '--sport', str(port), '-j', 'REJECT'],
        ]
        applied_rules = []
        try:
            sysctl['net.ipv4.tcp_fwmark_accept'] = 1
            for r in rules:
                subprocess.check_call([iptables_cmd, '-A'] + r)
                applied_rules.append(r)

            ln.listen()
            t = threading.Thread(target=lambda: serve(ln))
            t.start()

            conn.connect((addr, port))
        finally:
            sysctl['net.ipv4.tcp_fwmark_accept'] = tcp_fwmark_accept
            ln.close()
            for r in reversed(applied_rules):
                subprocess.check_call([iptables_cmd, '-D'] + r)
