import ipaddress
import six


class RacktablesMockClient(object):
    # output of https://racktables.yandex-team.ru/export/expand-fw-macro.php?macro=_SLBPUBLICSUPERNETS_
    ip_nets = u"""5.45.196.0/24
5.45.205.0/24
5.45.213.0/24
5.45.217.0/24
5.255.255.0/24
37.9.64.0/24
37.9.112.0/24
77.88.8.0/24
77.88.21.0/24
77.88.44.0/24
77.88.55.0/24
87.250.247.0/24
87.250.250.0/24
87.250.251.0/24
87.250.254.0/24
87.250.255.0/24
93.158.134.0/24
100.43.87.0/24
141.8.154.16/28
178.154.131.0/24
178.154.170.0/24
213.180.193.0/24
213.180.199.0/24
213.180.204.0/24
2620:10f:d001::/48
2a02:6b8::/62
2a02:6b8::/64
2a02:6b8:0:1::/64
2a02:6b8:4::/48
2a02:6b8:6::/48
2a02:6b8:8::/48
2a02:6b8:9::/48
2a02:6b8:a::/48
2a02:6b8:b::/48
2a02:6b8:c::/48
2a02:6b8:d::/48
2a02:6b8:e::/48
2a02:6b8:f::/48
2a02:6b8:20::/48
2a02:6b8:21::/48
2a02:6b8:22::/48"""

    def ip_belongs_to_macro(self, ip, macro):
        ip_address = ipaddress.ip_address(six.text_type(ip))
        for ip_net in self.ip_nets.splitlines():
            if ip_address in ipaddress.ip_network(ip_net):
                return True
        return False

    def is_public_ip(self, ip):
        return self.ip_belongs_to_macro(ip, None)
