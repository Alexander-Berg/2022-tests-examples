import pytest

from maps_adv.common.helpers import dt
from maps_adv.geosmb.telegraphist.server.lib.exceptions import RecipientDenied

pytestmark = [pytest.mark.asyncio]


async def test_will_send_email(email_client, domain):
    await domain.send_notification(
        recipient="peka@yandex.ru",
        purchased_certificate_details=dict(
            org_name="Peka house",
            certificate_code="peka_code_123",
            active_from=dt("2020-01-01 16:00:00"),
            active_to=dt("2020-07-20 19:00:00"),
            link_to_org="https://peka-house.com",
        ),
    )

    email_client.send_message.assert_called_with(
        args={
            "org_name": "Peka house",
            "certificate_code": "peka_code_123",
            "active_from": "1 января",
            "active_to": "20 июля 2020",
            "link_to_org": "https://peka-house.com",
        },
        asynchronous=True,
        template_code="purchased_certificate_template_code",
        to_email="peka@yandex.ru",
    )


async def test_returns_nothing(email_client, domain):
    got = await domain.send_notification(
        recipient="peka@yandex.ru",
        purchased_certificate_details=dict(
            org_name="Peka house",
            certificate_code="peka_code_123",
            active_from=dt("2020-01-01 16:00:00"),
            active_to=dt("2020-07-20 19:00:00"),
            link_to_org="https://peka-house.com",
        ),
    )

    assert got is None


async def test_raises_on_api_error(email_client, domain):
    email_client.send_message.side_effect = KeyError("Really unexpected error")

    with pytest.raises(KeyError, match="Really unexpected error"):
        await domain.send_notification(
            recipient="peka@yandex.ru",
            purchased_certificate_details=dict(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00"),
                active_to=dt("2020-07-20 19:00:00"),
                link_to_org="https://peka-house.com",
            ),
        )


@pytest.mark.config(LIMIT_RECIPIENTS=True)
async def test_raises_when_recipients_limited(email_client, domain):
    with pytest.raises(RecipientDenied):
        await domain.send_notification(
            recipient="peka@yandex.ru",
            purchased_certificate_details=dict(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00"),
                active_to=dt("2020-07-20 19:00:00"),
                link_to_org="https://peka-house.com",
            ),
        )

    assert email_client.send_message.called is False


@pytest.mark.config(LIMIT_RECIPIENTS=True)
async def test_sends_ok_to_yandex_team_if_recipients_limited(email_client, domain):
    await domain.send_notification(
        recipient="peka@yandex-team.ru",
        purchased_certificate_details=dict(
            org_name="Peka house",
            certificate_code="peka_code_123",
            active_from=dt("2020-01-01 16:00:00"),
            active_to=dt("2020-07-20 19:00:00"),
            link_to_org="https://peka-house.com",
        ),
    )

    assert email_client.send_message.called is True
