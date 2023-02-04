import time
import dns
import mock
import pytest
from django.test import override_settings
from dns.rdtypes.ANY.NS import NS
from dns.rdtypes.ANY.PTR import PTR

from intranet.crt.core.models import HostToApprove

pytestmark = pytest.mark.django_db


class FakeAnwer(object):
    def __init__(self, answers=None):
        self.rrset = answers
        if self.rrset is None:
            self.rrset = []
        self.expiration = time.time() + 60

    def __iter__(self):
        return iter(self.rrset)


def test_dns_is_managed():
    with mock.patch('socket.getaddrinfo') as getaddrinfo:
        getaddrinfo.return_value = [(0, 0, 0, 0, ('1.1.1.1', 0))]  # используется здесь только ip
        resolver = dns.resolver.get_default_resolver()
        resolver.cache = dns.resolver.LRUCache(4)
        resolver.cache.put(
            (dns.name.from_text('example.com.'), dns.rdatatype.NS, dns.rdataclass.IN),
            FakeAnwer([
                NS(
                    rdclass=dns.rdataclass.IN,
                    rdtype=dns.rdatatype.NS,
                    target=dns.name.from_text('ns300.example.com.')
                )
            ])
        )
        resolver.cache.put(
            (dns.name.from_text('1.1.1.1.in-addr.arpa.'), dns.rdatatype.PTR, dns.rdataclass.IN),
            FakeAnwer([
                PTR(
                    rdclass=dns.rdataclass.IN,
                    rdtype=dns.rdatatype.PTR,
                    target=dns.name.from_text('ns300.example.com.')
                )
            ])
        )
        hta = HostToApprove(host='example.com')
        hta.save()
    assert hta.name_servers == 'ns300.example.com'
    assert hta.managed_dns is False


def test_aliased_name_server():
    with override_settings(CRT_MANAGED_NAME_SERVERS={'ns600.example.com'}), \
         mock.patch('socket.getaddrinfo') as getaddrinfo:
        getaddrinfo.return_value = [(0, 0, 0, 0, ('1.1.1.1', 0))]  # используется здесь только ip
        resolver = dns.resolver.get_default_resolver()
        resolver.cache = dns.resolver.LRUCache(4)
        resolver.cache.put(
            (dns.name.from_text('example.com.'), dns.rdatatype.NS, dns.rdataclass.IN),
            FakeAnwer([
                NS(
                    rdclass=dns.rdataclass.IN,
                    rdtype=dns.rdatatype.NS,
                    target=dns.name.from_text('ns300.example.com.')
                )
            ])
        )
        resolver.cache.put(
            (dns.name.from_text('1.1.1.1.in-addr.arpa.'), dns.rdatatype.PTR, dns.rdataclass.IN),
            FakeAnwer([
                PTR(
                    rdclass=dns.rdataclass.IN,
                    rdtype=dns.rdatatype.PTR,
                    target=dns.name.from_text('ns600.example.com.')
                )
            ])
        )

        hta = HostToApprove(host='example.com')
        hta.save()
    assert hta.name_servers == 'ns300.example.com'
    assert hta.managed_dns is True
