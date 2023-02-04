import pytest
from datetime import date, datetime, timedelta

from intranet.domenator.src.db.domain.models import MailUserDomain, MailUserDomainStatus

pytestmark = pytest.mark.asyncio


@pytest.fixture()
async def domains(client):
    status = MailUserDomainStatus.registered.value

    await MailUserDomain.create(
        domain='already-ok.ru', uid='1', status=status, login='i', service_id='1', expired_at=datetime.now() + timedelta(days=265),
    )
    await MailUserDomain.create(
        domain='without-expired-at.ru', uid='2', status=status, login='i', service_id='2', expired_at=None,
    )
    await MailUserDomain.create(
        domain='with-min-date.ru', uid='2', status=status, login='i', service_id='2', expired_at=date.min,
    )
    await MailUserDomain.create(
        domain='need-renew.ru', uid='3', status=status, login='i', service_id='3', expired_at=datetime.now() + timedelta(days=35),
    )
    await MailUserDomain.create(
        domain='error-on-renew.ru', uid='3', status=status, login='i', service_id='4', expired_at=datetime.now() - timedelta(days=35),
    )
    await MailUserDomain.create(
        domain='error-on-get-info.ru', uid='3', status=status, login='i', service_id='5', expired_at=datetime.now() - timedelta(days=35),
    )


async def test_renew_empty_domains(client):
    from intranet.domenator.src.worker.periodic_tasks import renew_domains

    await renew_domains()


async def test_renew_ok(test_vcr, client, domains):
    from intranet.domenator.src.worker.periodic_tasks import renew_domains

    with test_vcr.use_cassette('test_renew_domains_task.yaml'):
        await renew_domains()

        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'already-ok.ru').gino.first()).expired_at == datetime.now().date() + timedelta(days=265)
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'without-expired-at.ru').gino.first()).expired_at is None
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'with-min-date.ru').gino.first()).expired_at == date.min
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'need-renew.ru').gino.first()).expired_at is None
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'error-on-renew.ru').gino.first()).expired_at is None
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'error-on-get-info.ru').gino.first()).expired_at is None
