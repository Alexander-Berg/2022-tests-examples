import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.telegraphist.proto.errors_pb2 import Error
from maps_adv.geosmb.telegraphist.proto.notifications_v1_pb2 import (
    EmailNotification,
    PurchasedCertificateNotificationDetails,
)

pytestmark = [pytest.mark.asyncio]

url = "/api/v1/send-notification/"


async def test_will_sent_email(email_client, api):
    await api.post(
        url,
        proto=EmailNotification(
            recipient="peka@yandex.ru",
            purchased_certificate_details=PurchasedCertificateNotificationDetails(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00", as_proto=True),
                active_to=dt("2020-07-20 19:00:00", as_proto=True),
                link_to_org="https://peka-house.com",
            ),
        ),
        expected_status=204,
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


async def test_errored_on_api_error(email_client, api):
    email_client.send_message.side_effect = Exception("Realy unexpected error")

    await api.post(
        url,
        proto=EmailNotification(
            recipient="peka@yandex.ru",
            purchased_certificate_details=PurchasedCertificateNotificationDetails(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00", as_proto=True),
                active_to=dt("2020-07-20 19:00:00", as_proto=True),
                link_to_org="https://peka-house.com",
            ),
        ),
        expected_status=500,
    )


@pytest.mark.parametrize(
    "recipient", ("", "peka", "@yandex.ru", "yandex.ru", "peka@", "peka@yandex-team")
)
async def test_errored_on_wrong_recipient(recipient, api):
    got = await api.post(
        url,
        proto=EmailNotification(
            recipient=recipient,
            purchased_certificate_details=PurchasedCertificateNotificationDetails(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00", as_proto=True),
                active_to=dt("2020-07-20 19:00:00", as_proto=True),
                link_to_org="https://peka-house.com",
            ),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="recipient: ['Not a valid email address.']",
    )


@pytest.mark.parametrize(
    "replaces, expected",
    (
        [dict(org_name=""), "{'org_name': ['Shorter than minimum length 1.']}"],
        [
            dict(certificate_code=""),
            "{'certificate_code': ['Shorter than minimum length 1.']}",
        ],
        [dict(link_to_org=""), "{'link_to_org': ['Shorter than minimum length 1.']}"],
    ),
)
async def test_errored_on_wrong_details(replaces, expected, api):
    kw = dict(
        org_name="Peka house",
        certificate_code="peka_code_123",
        link_to_org="https://peka-house.com",
        active_from=dt("2020-01-01 16:00:00", as_proto=True),
        active_to=dt("2020-07-20 19:00:00", as_proto=True),
    )
    kw.update(replaces)

    got = await api.post(
        url,
        proto=EmailNotification(
            recipient="peka@yandex.ru",
            purchased_certificate_details=PurchasedCertificateNotificationDetails(**kw),
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=f"purchased_certificate_details: {expected}",
    )


@pytest.mark.config(LIMIT_RECIPIENTS=True)
async def test_errored_if_recipients_limited(email_client, api):
    got = await api.post(
        url,
        proto=EmailNotification(
            recipient="peka@yandex.ru",
            purchased_certificate_details=PurchasedCertificateNotificationDetails(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00", as_proto=True),
                active_to=dt("2020-07-20 19:00:00", as_proto=True),
                link_to_org="https://peka-house.com",
            ),
        ),
        decode_as=Error,
        expected_status=406,
    )

    assert got == Error(code=Error.RECIPIENT_DENIED)
    assert email_client.send_message.called is False


@pytest.mark.config(LIMIT_RECIPIENTS=True)
async def test_sends_ok_to_yandex_team_if_recipients_limited(email_client, api):
    await api.post(
        url,
        proto=EmailNotification(
            recipient="peka@yandex-team.ru",
            purchased_certificate_details=PurchasedCertificateNotificationDetails(
                org_name="Peka house",
                certificate_code="peka_code_123",
                active_from=dt("2020-01-01 16:00:00", as_proto=True),
                active_to=dt("2020-07-20 19:00:00", as_proto=True),
                link_to_org="https://peka-house.com",
            ),
        ),
        decode_as=Error,
        expected_status=204,
    )

    assert email_client.send_message.called is True
