import pytest
from datetime import date

from intranet.domenator.src.db.domain.models import MailUserDomain, MailUserDomainStatus

pytestmark = pytest.mark.asyncio


@pytest.fixture()
async def domains(client):
    status = MailUserDomainStatus.registered.value

    await MailUserDomain.create(
        domain='already-ok.ru', uid='1', status=status, login='i', service_id='1', expired_at=date(2023, 1, 12),
    )
    await MailUserDomain.create(
        domain='not-found.ru', uid='2', status=status, login='i', service_id='2', expired_at=None,
    )
    await MailUserDomain.create(
        domain='domain-ok.ru', uid='3', status=status, login='i', service_id='3', expired_at=None,
    )
    await MailUserDomain.create(
        domain='domain-not-active.ru', uid='4', status=status, login='i', service_id='4', expired_at=None,
    )


async def test_sync_expired_at_empty_domains(client):
    from intranet.domenator.src.worker.periodic_tasks import sync_expired_at

    await sync_expired_at()


async def test_sync_expired_at_ok(test_vcr, client, domains):
    from intranet.domenator.src.worker.periodic_tasks import sync_expired_at

    with test_vcr.use_cassette('test_sync_expired_at_task.yaml'):
        await sync_expired_at()

        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'already-ok.ru').gino.first()).expired_at == date(2023, 1, 12)
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'not-found.ru').gino.first()).expired_at is None
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'domain-ok.ru').gino.first()).expired_at == date(2023, 1, 14)
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'domain-not-active.ru').gino.first()).expired_at is None


async def test_sync_expired_at_fail(test_vcr, client, domains):
    from intranet.domenator.src.worker.periodic_tasks import sync_expired_at

    with test_vcr.use_cassette('test_sync_expired_at_task_fail.yaml'):
        try:
            await sync_expired_at()
            assert False, 'No raise exception'
        except AssertionError:
            pass

        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'already-ok.ru').gino.first()).expired_at == date(2023, 1, 12)
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'not-found.ru').gino.first()).expired_at is None
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'domain-ok.ru').gino.first()).expired_at is None
        assert (await MailUserDomain.query.where(MailUserDomain.domain == 'domain-not-active.ru').gino.first()).expired_at is None
