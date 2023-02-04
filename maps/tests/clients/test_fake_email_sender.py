import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.common.email_sender import MailingListSource
from maps_adv.geosmb.scenarist.server.lib.clients.fake_email_sender import (
    FakeEmailSenderClient,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def client():
    return await FakeEmailSenderClient(
        url="http://localhost/",
        account_slug="account_slug",
        account_token="account_token",
    )


async def test_not_sends_api_requests(mocker, client):
    mock = mocker.patch("aiohttp.ClientSession.post")

    await client.schedule_promo_campaign(
        title="Название",
        from_email="from@yandex.ru",
        from_name="Предложения от партнёров",
        subject="Тема письма",
        body="Содержание",
        mailing_list_source=MailingListSource.IN_PLACE,
        mailing_list_params=[{"email": "example@yandex.ru", "params": {}}],
        tags=("tag1", "tag2"),
        schedule_dt=dt("2000-02-03 15:00:00"),
        unsubscribe_list_slug="yandex_clients",
    )

    mock.assert_not_called()


async def test_return_data(client):
    result = await client.schedule_promo_campaign(
        title="Название",
        from_email="from@yandex.ru",
        from_name="Предложения от партнёров",
        subject="Тема письма",
        body="Содержание",
        mailing_list_source=MailingListSource.IN_PLACE,
        mailing_list_params=[{"email": "example@yandex.ru", "params": {}}],
        tags=("tag1", "tag2"),
        schedule_dt=dt("2000-02-03 15:00:00"),
        unsubscribe_list_slug="yandex_clients",
    )

    assert result == {"fake_email_sender": Any(str)}
