import pytest

from maps_adv.common.aiosup import SupClient
from maps_adv.common.aioyav import YavClient
from maps_adv.common.email_sender import Client
from maps_adv.common.helpers import coro_mock
from maps_adv.common.yasms import YasmsClient
from maps_adv.geosmb.clients.notify_me import NotifyMeClient

from maps_adv.geosmb.telegraphist.server.lib import Application
from maps_adv.geosmb.telegraphist.server.lib.domain import Domain
from maps_adv.geosmb.telegraphist.server.lib.enums import Transport
from maps_adv.geosmb.telegraphist.server.lib.notification_router import NotificationRouter
from maps_adv.geosmb.telegraphist.server.lib.notification_router_v3 import NotificationRouterV3
from maps_adv.geosmb.tuner.client import (
    BusinessEmailNotificationSettings,
    BusinessSmsNotificationSettings,
    TunerClient,
)

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "smb.common.aiotvm.pytest.plugin",
    "maps_adv.geosmb.clients.bvm.pytest.plugin",
    "maps_adv.geosmb.doorman.client.pytest.plugin",
    "maps_adv.geosmb.clients.geosearch.pytest.plugin",
]

_config = dict(
    EMAIL_CLIENT_ACCOUNT_SLUG="email_slug",
    EMAIL_CLIENT_ACCOUNT_TOKEN="email_token",
    EMAIL_CLIENT_API_URL="http://localhost/email",
    LIMIT_RECIPIENTS=False,
    PURCHASED_CERTIFICATE_EMAIL_TEMPLATE_CODE="purchased_certificate_template_code",
    SOURCE_TVM_ID="source-tvm-id",
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    YASMS_SENDER="yasms-sender-code",
    YASMS_URL="http://yasms.server",
    DOORMAN_URL="http://doorman.server",
    DOORMAN_TVM_ID="doorman",
    BVM_URL="http://bvm.server",
    GEOSEARCH_URL="http://geosearch.server",
    GEOSEARCH_TVM_ID="geoserach",
    TVM_WHITELIST=None,
    EMAIL_TEMPLATE_CODE_CART_ORDER_CREATED="cart_order_created",
    EMAIL_TEMPLATE_CODE_ORDER_CREATED="order_created_email",
    EMAIL_TEMPLATE_CODE_ORDER_REMINDER="order_reminder_email",
    EMAIL_TEMPLATE_CODE_ORDER_CHANGED="order_changed_email",
    EMAIL_TEMPLATE_CODE_ORDER_CANCELLED="order_cancelled_email",
    EMAIL_TEMPLATE_CODE_ORDER_CREATED_FOR_BUSINESS="order_created_email_for_business",
    EMAIL_TEMPLATE_CODE_ORDER_CHANGED_FOR_BUSINESS="order_changed_email_for_business",
    EMAIL_TEMPLATE_CODE_ORDER_CANCELLED_FOR_BUSINESS="order_cancelled_email_for_business",  # noqa
    EMAIL_TEMPLATE_CODE_CERTIFICATE_EXPIRING="certificate_expiring",
    EMAIL_TEMPLATE_CODE_CERTIFICATE_EXPIRED="certificate_expired",
    EMAIL_TEMPLATE_CODE_CERTIFICATE_CONNECT_PAYMENT="certificate_connect_payment",
    EMAIL_TEMPLATE_CODE_CERTIFICATE_REJECTED="certificate_rejected",
    EMAIL_TEMPLATE_CODE_FIRST_CERTIFICATE_APPROVED="first_certificate_approved",
    EMAIL_TEMPLATE_CODE_SUBSEQUENT_CERTIFICATE_APPROVED="subsequent_certificate_approved",  # noqa
    EMAIL_TEMPLATE_CODE_CERTIFICATE_PURCHASED="certificate_purchased",
    EMAIL_TEMPLATE_CODE_CERTIFICATE_CREATED="certificate_created",
    EMAIL_TEMPLATE_CODE_REQUEST_CREATED_FOR_BUSINESS="request_created_email_for_business",  # noqa
    YAV_TOKEN="yav-token",
    YAV_SECRET_ID="kek-id",
    SUP_OAUTH_TOKEN="sup-oauth-token",
    SUP_URL="http://sup.server",
    SUP_PROJECT="sup-project",
    SUP_THROTTLE_POLICY_NAME="throttle-policy-1",
    TUNER_URL="http://tuner.server",
    NOTIFY_ME_URL="http://notify_me.server",
)


@pytest.fixture
def config(request):
    __config = _config.copy()

    config_mark = request.node.get_closest_marker("config")
    if config_mark:
        __config.update(config_mark.kwargs)

    return __config


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture(autouse=True)
async def email_client(mocker, config):
    mocker.patch("maps_adv.common.email_sender.Client.send_message", coro_mock())

    async with Client(
        account_slug=config["EMAIL_CLIENT_ACCOUNT_SLUG"],
        account_token=config["EMAIL_CLIENT_ACCOUNT_TOKEN"],
        url=config["EMAIL_CLIENT_API_URL"],
    ) as client:
        yield client


@pytest.fixture(autouse=True)
async def yasms(mocker, aiotvm, config):
    mocker.patch("maps_adv.common.yasms.YasmsClient.send_sms", coro_mock())

    async with YasmsClient(
        url=config["YASMS_URL"],
        sender=config["YASMS_SENDER"],
        tvm=aiotvm,
        tvm_destination="yasms",
    ) as client:
        yield client


@pytest.fixture(autouse=True)
async def notify_me(mocker, config):
    mocker.patch("maps_adv.geosmb.clients.notify_me.NotifyMeClient.send_message", coro_mock())

    async with NotifyMeClient(
        url=config["NOTIFY_ME_URL"],
    ) as client:
        yield client


@pytest.fixture(autouse=True)
async def tuner_client(mocker, aiotvm, config):
    fetch_settings = mocker.patch(
        "maps_adv.geosmb.tuner.client.TunerClient.fetch_settings", coro_mock()
    )
    fetch_settings.coro.return_value = {
        "emails": ["kek@yandex.ru", "foo@yandex-team.ru"],
        "phone": 900,
        "contacts": {
            "phones": [900],
            "emails": ["kek@yandex.ru", "foo@yandex-team.ru"],
        },
        "notifications": BusinessEmailNotificationSettings(
            order_created=True,
            order_cancelled=True,
            order_changed=True,
            certificate_notifications=True,
            request_created=True,
        ),
        "sms_notifications": BusinessSmsNotificationSettings(
            request_created=True,
        ),
    }

    async with TunerClient(
        url=config["TUNER_URL"],
        tvm=aiotvm,
        tvm_destination="tuner",
    ) as client:
        yield client


@pytest.fixture(autouse=True)
async def yav_client(mocker, config):
    mock = mocker.patch(
        "maps_adv.common.aioyav.YavClient.retrieve_secret_head", coro_mock()
    )
    mock.coro.return_value = {
        "value": {
            "PHONE_RECIPIENTS_WHITELIST": "71111111111,79999999999",
            "TELEGRAM_UIDS_RECIPIENTS_WHITELIST": "496329590,496329591",
            "PASSPORT_UID_RECIPIENTS_WHITELIST": "555,666",
            "DEVICE_ID_RECIPIENTS_WHITELIST": "777,888",
        }
    }

    async with YavClient(config["YAV_TOKEN"]) as client:
        yield client


@pytest.fixture(autouse=True)
async def sup_client(mocker, config):
    mock = mocker.patch(
        "maps_adv.common.aiosup.SupClient.send_push_notifications", coro_mock()
    )
    mock.coro.return_value = {
        "id": "1538131142619000-4568210906199431056",
        "receiver": ["login:861cae2a060f47c88e7dea0422cc9699", "uid:15678"],
        "strace": {
            "chunks": 1,
            "receivers": 1,
            "processingTime": [],
            "resolveTime": 0,
            "downloadTime": 18,
        },
    }

    async with SupClient(
        url=config["SUP_URL"],
        auth_token=config["SUP_OAUTH_TOKEN"],
        project=config["SUP_PROJECT"],
    ) as client:
        yield client


@pytest.fixture
def domain(
    email_client,
    yasms,
    notify_me,
    bvm,
    geosearch,
    notification_router,
    notification_router_v3,
    doorman,
    config,
):
    return Domain(
        email_client=email_client,
        yasms=yasms,
        notify_me=notify_me,
        bvm_client=bvm,
        doorman_client=doorman,
        geosearch_client=geosearch,
        notification_router=notification_router,
        notification_router_v3=notification_router_v3,
        purchased_certificate_template_code=config[
            "PURCHASED_CERTIFICATE_EMAIL_TEMPLATE_CODE"
        ],
        limit_recipients=config["LIMIT_RECIPIENTS"],
    )


@pytest.fixture
def notification_router(
    mocker, doorman, email_client, yasms, yav_client, sup_client, tuner_client
):
    send_client_notification = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router."
        "NotificationRouter.send_client_notification",
        coro_mock(),
    )
    send_client_notification.coro.return_value = [{Transport.SMS: {"phone": 322223}}]
    send_business_notification = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router."
        "NotificationRouter.send_business_notification",
        coro_mock(),
    )
    send_business_notification.coro.return_value = [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"email": "cheburekkek@yandex.ru"}},
    ]

    router = NotificationRouter(
        doorman_client=doorman,
        email_client=email_client,
        yasms=yasms,
        yav_client=yav_client,
        tuner_client=tuner_client,
        sup_client=sup_client,
        email_template_codes={
            "client": {
                "order_created": "order_created_email",
                "order_reminder": "order_reminder_email",
                "order_changed": "order_changed_email",
                "order_cancelled": "order_cancelled_email",
            },
            "business": {
                "order_created_for_business": "order_created_email_for_business",
                "order_changed_for_business": "order_changed_email_for_business",
                "order_cancelled_for_business": "order_cancelled_email_for_business",
                "certificate_expiring": "certificate_expiring",
                "certificate_expired": "certificate_expired",
                "certificate_connect_payment": "certificate_connect_payment",
                "certificate_rejected": "certificate_rejected",
                "first_certificate_approved": "first_certificate_approved",
                "subsequent_certificate_approved": "subsequent_certificate_approved",
                "request_created_for_business": "request_created_email_for_business",
            },
        },
        yav_secret_id="kek-id",
    )

    return router


@pytest.fixture
def notification_router_v3(mocker, email_client, notify_me, yasms, yav_client):
    send_notification = mocker.patch(
        "maps_adv.geosmb.telegraphist.server.lib.notification_router_v3."
        "NotificationRouterV3.send_notification",
        coro_mock(),
    )
    send_notification.coro.return_value = [
        {Transport.EMAIL: {"email": "kek@yandex.ru"}},
        {Transport.EMAIL: {"email": "cheburekkek@yandex.ru"}},
    ]

    router = NotificationRouterV3(
        email_client=email_client,
        telegram_client=notify_me,
        yasms=yasms,
        email_template_codes={
            "request_created_for_business": "request_created_email_for_business",
            "cart_order_created": "cart_order_created"
        },
        yav_client=yav_client,
        yav_secret_id="kek-id",
    )

    return router
