import pytest
from mock import patch, Mock

from django.conf import settings

from staff.racktables.objects import YaIP
from staff.racktables.models import AllowedNet, DeniedNet
from staff.racktables.tasks import SyncAllowedNets
from staff.racktables.utils import is_ip_internal


RESPONSE_ALLOWED_NET = (
    '["someallowednet.yandex.net", '  # hostname
    '"2a02:6b8:0:3706::/120", '  # /c, c > 64
    '"2a02:6b8:b010:bf::/64", '  # /c, c == 64
    '"2a02:6b8:c05::/48", '  # /c, c < 64
    '"1340@2a02:6b8:c00::/40", '  # ip with @ (yandex-style)
    '"95.108.222.0/23"]'  # ipv4
)

RESPONSE_DENIED_NET = (
    '["somedeniednet.yandex.net", '
    '"2a02:6b8:0:1629::1:0/120", '
    '"2a02:6b8:b010:7041::/64", '
    '"2a02:6b8:c08::/48", '
    '"10df3e4@2a02:6b8:c00::/40", '
    '"37.140.165.208/29"]'
)

ALLOWED_NETS = ['88.43.81.0/29', '88.108.255.65', '2a02:6b8:0:280a:aa20:66ff:fe12:1c40']
DENIED_NETS = ['88.43.85.0/29', '88.108.255.67', '2a02:6b8:b011:4c00:215:b2ff:feaa:6be']

ASSERT_IS_DENIED = [
    '88.43.85.7',
    '88.108.255.67',
    '2a02:6b8:b011:4c00:215:b2ff:feaa:6be',
    '2a02:6b8:0:1629::1:a',
    '2a02:6b8:b010:7041::a:b:c',
    '2a02:6b8:c08:ab:cd:ef:1:9',
    '2a02:6b8:c00:0:10df:3e4:1:2',
]

ASSERT_IS_ALLOWED = [
    '88.43.81.5',
    '88.108.255.65',
    '2a02:6b8:0:280a:aa20:66ff:fe12:1c40',
    '2a02:6b8:0:3706:0:0:0:ff',
    '2a02:6b8:b010:bf:1:2:3:a',
    '2a02:6b8:c00:777:0:1340:deda:baba',
    '95.108.222.0',
]


def source(url, timeout=None):
    assert settings.HBF_URL in url
    assert 'trypo' in url

    if '/_ALLOWED_MACRO_?' in url:
        data = RESPONSE_ALLOWED_NET
    else:
        data = RESPONSE_DENIED_NET

    return Mock(
        status_code=200,
        content=data,
    )


def fake_getaddrinfo(name, port):
    if 'denied' in name:
        return [
            (10, 1, 6, '', ('2a02:6b8:0:1a4f::bc', 80, 0, 0)),
            (10, 2, 17, '', ('2a02:6b8:0:1a4f::bc', 80, 0, 0)),
        ]
    else:
        return [
            (1, 2, 3, '', ('1740:16:09:2019::af', 80, 0, 0)),
            (4, 5, 6, '', ('1740:16:09:2019::af', 80, 0, 0)),
        ]


@pytest.fixture
def init_nets():
    AllowedNet.objects.create(macro_or_net='_ALLOWED_MACRO_', comment='super puper macro (A)')
    DeniedNet.objects.create(macro_or_net='_DENIED_MACRO_', comment='super puper macro (D)')
    for net in ALLOWED_NETS:
        AllowedNet.objects.create(macro_or_net=net, comment='some')
    for net in DENIED_NETS:
        DeniedNet.objects.create(macro_or_net=net, comment='some')


@pytest.mark.django_db
def test_is_ip_internal(init_nets):
    with patch('staff.racktables.tasks.requests', get=source):
        with patch('socket.getaddrinfo', fake_getaddrinfo):
            SyncAllowedNets()

    for ip in ASSERT_IS_ALLOWED:
        assert is_ip_internal(ip)

    for ip in ASSERT_IS_DENIED:
        assert not is_ip_internal(ip)


def test_rule_is_inside_ipv6():
    # given
    first_rule = YaIP('2a02:6b8:0:801::/64').as_rule()
    second_rule = YaIP('2a02:6b8:0:801::1').as_rule()

    # when
    result = second_rule.is_inside(first_rule)

    # then
    assert result


def test_rule_is_outside_ipv6():
    # given
    first_rule = YaIP('2a02:6b8:0:801::/64').as_rule()
    second_rule = YaIP('2a03:6b8:0:801::1').as_rule()

    # when
    result = second_rule.is_inside(first_rule)

    # then
    assert not result


def test_rule_is_inside_ipv4():
    # given
    first_rule = YaIP('77.88.28.0/24').as_rule()
    second_rule = YaIP('77.88.28.1').as_rule()

    # when
    result = second_rule.is_inside(first_rule)

    # then
    assert result


def test_rule_is_outside_ipv4():
    # given
    first_rule = YaIP('77.88.28.0/24').as_rule()
    second_rule = YaIP('78.88.28.1').as_rule()

    # when
    result = second_rule.is_inside(first_rule)

    # then
    assert not result
