import mock
import pytest

from django.utils.encoding import force_text
from django.utils import timezone

from intranet.crt.constants import HOST_VALIDATION_CODE_STATUS, CERT_STATUS
from intranet.crt.core.models import HostToApprove, Certificate
from __tests__.utils import factories as f


pytestmark = pytest.mark.django_db


expire_at = timezone.now() + timezone.timedelta(days=1)


@pytest.yield_fixture(autouse=True)
def patch_get_name_servers():
    with mock.patch('intranet.crt.core.models.get_name_servers', return_value=set()):
        yield


def test_hosts_to_approve(crt_client, users):

    host = f.HostToApprove()
    f.HostValidationCode(
        host=host,
        status=HOST_VALIDATION_CODE_STATUS.validation,
        code='deadbeef',
        expire_at=expire_at,
    )
    f.Certificate().hosts_to_approve.add(host)

    HostToApprove.objects.filter(pk=host.pk).update(
        name_servers='ns1.yandex.ru,ns2.yandex.ru',
        managed_dns=True,
    )
    Certificate.objects.update(status=CERT_STATUS.VALIDATION)

    helpdesk_user = users['helpdesk_user']
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.get('/api/hosts/to-approve/')
    assert 'deadbeef' in force_text(response.content)

    response = crt_client.json.get('/api/hosts-to-approve.xml')
    assert 'deadbeef' in force_text(response.content)


def test_many_hosts_to_approve(crt_client, users):
    host = f.HostToApprove(host='test.yandex.ru')
    f.HostValidationCode(
        host=host,
        status=HOST_VALIDATION_CODE_STATUS.validation,
        code='deadbeef',
    )
    f.Certificate().hosts_to_approve.add(host)
    f.HostValidationCode(
        host=host,
        status=HOST_VALIDATION_CODE_STATUS.validation,
        code='bad1dea',
    )
    f.Certificate().hosts_to_approve.add(host)
    f.HostValidationCode(
        host=host,
        status=HOST_VALIDATION_CODE_STATUS.validated,
        code='decaf',
    )
    f.Certificate().hosts_to_approve.add(host)

    HostToApprove.objects.filter(pk=host.pk).update(
        name_servers='ns1.yandex.ru,ns2.yandex.ru',
        managed_dns=True,
    )
    Certificate.objects.update(status=CERT_STATUS.VALIDATION)

    helpdesk_user = users['helpdesk_user']
    crt_client.login(helpdesk_user.username)

    expected = [
        {'host': 'test.yandex.ru', 'code': 'deadbeef', 'is_waiting_for_validation': True},
        {'host': 'test.yandex.ru', 'code': 'bad1dea', 'is_waiting_for_validation': True},
    ]

    response = crt_client.json.get('/api/hosts/to-approve/')
    assert expected == response.json()

    response = crt_client.json.get('/api/hosts-to-approve.xml')
    assert 'deadbeef' in force_text(response.content)
    assert 'bad1dea' in force_text(response.content)
    assert 'decaf' not in force_text(response.content)


def test_hosts_auto_managed(crt_client):
    def _test_auto_managed(request, expected_hosts):
        hosts_list = crt_client.json.get('/api/hosts/auto-managed/', request).json()
        assert hosts_list == [
            {'host': host, 'last_validation': None}
            for host in expected_hosts
        ]

    HostToApprove.objects.bulk_create([
        HostToApprove(host='not.dns.not.auto.yandex.ru', auto_managed=False, managed_dns=False),
        HostToApprove(host='dns.not.auto.yandex.ru', auto_managed=False, managed_dns=True),
        HostToApprove(host='not.dns.auto.yandex.ru', auto_managed=True, managed_dns=False),
        HostToApprove(host='dns.auto.yandex.ru', auto_managed=True, managed_dns=True),
    ])

    # auto managed hosts
    not_dns_host = 'not.dns.auto.yandex.ru'
    dns_host = 'dns.auto.yandex.ru'
    # without managed_dns return all auto_managed hosts
    _test_auto_managed({}, [not_dns_host, dns_host])
    # with managed_dns=true return all auto_managed dns_managed hosts
    _test_auto_managed({'managed_dns': True}, [dns_host])
    _test_auto_managed({'managed_dns': 'true'}, [dns_host])
    _test_auto_managed({'managed_dns': 1}, [dns_host])
    # with managed_dns=false return all auto_managed & not dns_managed hosts
    _test_auto_managed({'managed_dns': False}, [not_dns_host])
    _test_auto_managed({'managed_dns': 'false'}, [not_dns_host])
    _test_auto_managed({'managed_dns': 0}, [not_dns_host])
    # if managed_dns has multiple values, use last one
    _test_auto_managed({'managed_dns': [False, True]}, [dns_host])
    _test_auto_managed({'managed_dns': [True, False]}, [not_dns_host])
