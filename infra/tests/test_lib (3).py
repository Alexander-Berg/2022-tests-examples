import socket
from pyroute2.netlink.rtnl.ifaddrmsg import ifaddrmsg
from pyroute2.netlink.rtnl.ifinfmsg import ifinfmsg
from pyroute2.netlink.rtnl.rtmsg import rtmsg


def form_pyroute_addr_answer(addr, family=socket.AF_INET6, scope=0):
    netlink_msg = ifaddrmsg()
    netlink_msg['index'] = 2  # index of the interface
    netlink_msg['family'] = family  # address family
    netlink_msg['prefixlen'] = 64  # the address mask
    netlink_msg['scope'] = scope  # see /etc/iproute2/rt_scopes
    # attach NLA -- it MUST be a list / mutable
    netlink_msg['attrs'] = [['IFA_ADDRESS', addr]]

    return netlink_msg


def form_pyroute_route_answer(dst, oif, family=socket.AF_INET6, table=254):
    netlink_msg = rtmsg()
    netlink_msg['family'] = family  # address family
    netlink_msg['table'] = table  # table_id
    # attach NLA -- it MUST be a list / mutable
    netlink_msg['attrs'] = [
        ('RTA_TABLE', table),
        ('RTA_DST', dst),
        ('RTA_OIF', oif),
    ]

    return netlink_msg


def form_pyroute_rule_answer(src, priority=10000):
    netlink_msg = rtmsg()
    # attach NLA -- it MUST be a list / mutable
    netlink_msg['attrs'] = [
        ('FRA_SRC', src),
        ('FRA_PRIORITY', priority),
    ]

    return netlink_msg


def form_pyroute_link_answer(index, name, attrs=None):
    if not attrs:
        attrs = [
            ('IFLA_IFNAME', name),
        ]
    else:
        attrs.append(('IFLA_IFNAME', name))

    netlink_msg = ifinfmsg()
    netlink_msg['index'] = index
    netlink_msg['attrs'] = attrs

    return netlink_msg


def setup_proper_ip6tnl_to_remote(remote_ip):
    info_data_description = {
        'attrs': [
            ('IFLA_IP6TNL_LOCAL', '2001:db8::100'),
            ('IFLA_IP6TNL_REMOTE', remote_ip),
            ('IFLA_IP6TNL_PROTO', 41)
        ]
    }
    info_data = ifinfmsg()
    info_data.load(info_data_description)

    link_info_description = {
        'attrs': [
            ('IFLA_INFO_KIND', 'ip6tnl'),
            ('IFLA_INFO_DATA', info_data),
        ]
    }
    link_info = ifinfmsg()
    link_info.load(link_info_description)

    link_obj_description = {
        'index': 7,
        'attrs': [
            ('IFLA_IFNAME', 'ok_TUN0'),
            ('IFLA_LINKINFO', link_info),
        ],
    }
    link_obj = ifinfmsg()
    link_obj.load(link_obj_description)
    return link_obj
