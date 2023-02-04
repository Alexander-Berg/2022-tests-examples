import pytest

from maps_adv.geosmb.telegraphist.proto.errors_pb2 import Error
from maps_adv.geosmb.telegraphist.proto.v3.common_pb2 import (
    NotificationResult,
    NotificationTransportResult,
    Transport,
    TransportType,
)

from maps_adv.geosmb.telegraphist.proto.v3.notifications_pb2 import (
    OrgInfo,
    CartOrderItem,
    OrderInfo,
    Contacts,
)

from maps_adv.geosmb.telegraphist.server.lib.enums import (
    Transport as TransportEnum,
    NotificationType,
)
from maps_adv.geosmb.telegraphist.server.lib.notification_router_v3 import (
    NoAddress,
    SendFailed,
)

from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    notification_proto,
    make_request_created,
    make_cart_order_created
)

pytestmark = [pytest.mark.asyncio]

url = "/api/v3/send-notification/"


@pytest.mark.parametrize(
    "notification_router_v3_return, expected_pb",
    [
        (
            [{TransportEnum.EMAIL: NoAddress()}],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=TransportType.EMAIL, error="No address to send to"
                    )
                ]
            ),
        ),
        (
            [
                {TransportEnum.EMAIL: {"email": "passed_email@yandex.ru"}},
                {TransportEnum.EMAIL: {"email": "another_email@yandex.ru"}},
            ],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=TransportType.EMAIL, email="passed_email@yandex.ru"
                    ),
                    NotificationTransportResult(
                        transport=TransportType.EMAIL, email="another_email@yandex.ru"
                    ),
                ]
            ),
        ),
        (
            [
                {TransportEnum.EMAIL: {"email": "passed_email@yandex.ru"}},
                {TransportEnum.EMAIL: SendFailed()},
            ],
            NotificationResult(
                results=[
                    NotificationTransportResult(
                        transport=TransportType.EMAIL, email="passed_email@yandex.ru"
                    ),
                    NotificationTransportResult(
                        transport=TransportType.EMAIL, error="Send failed"
                    ),
                ]
            ),
        ),
    ],
)
@pytest.mark.parametrize(
    "notification_type, get_args", [
        ('request_created', make_request_created),
        ('cart_order_created', make_cart_order_created)
    ]
)
async def test_returns_notification_result(
    api, notification_router_v3, notification_router_v3_return, expected_pb, notification_type, get_args
):
    notification_router_v3.send_notification.coro.return_value = (
        notification_router_v3_return
    )
    params = dict()
    params[notification_type] = get_args()

    got = await api.post(
        url,
        proto=notification_proto(**params),
        decode_as=NotificationResult,
        expected_status=200,
    )

    assert got == expected_pb


@pytest.mark.parametrize(
    "notification_type, get_args, incorrect_params, expected_error",
    (
        ['request_created', make_request_created, dict(biz_id=0), "{'biz_id': ['Must be at least 1.']}"],
        ['request_created', make_request_created,
            dict(org=dict(cabinet_link="", company_link="http://company.link")),
            "{'org': {'cabinet_link': ['Shorter than minimum length 1.']}}"],
        ['request_created', make_request_created,
            dict(org=dict(company_link="", cabinet_link="http://cabinet.link")),
            "{'org': {'company_link': ['Shorter than minimum length 1.']}}"],

        ['cart_order_created', make_cart_order_created, dict(biz_id=0), "{'biz_id': ['Must be at least 1.']}"],
        ['cart_order_created', make_cart_order_created,
            dict(org=OrgInfo(
                cabinet_link="",
                company_link="http://company.link"
            )),
            "{'org': {'cabinet_link': ['Shorter than minimum length 1.']}}"],
        ['cart_order_created', make_cart_order_created,
            dict(org=OrgInfo(
                company_link="",
                cabinet_link="http://cabinet.link"
            )),
            "{'org': {'company_link': ['Shorter than minimum length 1.']}}"],
        ['cart_order_created', make_cart_order_created,
            dict(order=OrderInfo(
                id=1,
                final_price="0",
                final_count=0,
                created_at="2020-02-02",
                contacts=Contacts(
                name="Имя",
                phone="+78904093221"
                ),
                cart_items=[
                    CartOrderItem(
                        name="Товар",
                        price="1000",
                        min_price="900",
                        description="Описание",
                        count=1
                    )
                ]
            )),
            "{'order': {'final_count': ['Must be at least 1.']}}"],
        ['cart_order_created', make_cart_order_created,
         dict(order=OrderInfo(
             id=1,
             final_price="0",
             final_count=1,
             created_at="",
             contacts=Contacts(
                 name="Имя",
                 phone="+78904093221"
             ),
             cart_items=[
                 CartOrderItem(
                     name="Товар",
                     price="1000",
                     min_price="900",
                     description="Описание",
                     count=1
                 )
             ]
         )),
         "{'order': {'created_at': ['Shorter than minimum length 1.']}}"],
        ['cart_order_created', make_cart_order_created,
         dict(order=OrderInfo(
             id=1,
             final_price="0",
             final_count=1,
             created_at="2020-02-02",
             contacts=Contacts(
                 name="",
                 phone="+78904093221"
             ),
             cart_items=[
                 CartOrderItem(
                     name="Товар",
                     price="1000",
                     min_price="900",
                     description="Описание",
                     count=1
                 )
             ]
         )),
         "{'order': {'contacts': {'name': ['Shorter than minimum length 1.']}}}"],
        ['cart_order_created', make_cart_order_created,
         dict(order=OrderInfo(
             id=1,
             final_price="0",
             final_count=1,
             created_at="2020-02-02",
             contacts=Contacts(
                 name="Имя",
                 phone=""
             ),
             cart_items=[
                 CartOrderItem(
                     name="Товар",
                     price="1000",
                     min_price="900",
                     description="Описание",
                     count=1
                 )
             ]
         )),
         "{'order': {'contacts': {'phone': ['Shorter than minimum length 1.']}}}"],
        ['cart_order_created', make_cart_order_created,
         dict(order=OrderInfo(
             id=1,
             final_price="0",
             final_count=1,
             created_at="2020-02-02",
             contacts=Contacts(
                 name="Имя",
                 phone="+78904093221"
             ),
             cart_items=[
                 CartOrderItem(
                     name="",
                     price="1000",
                     min_price="900",
                     description="Описание",
                     count=1
                 )
             ]
         )),
         "{'order': {'cart_items': {0: {'name': ['Shorter than minimum length 1.']}}}}"],
        ['cart_order_created', make_cart_order_created,
         dict(order=OrderInfo(
             id=1,
             final_price="0",
             final_count=1,
             created_at="2020-02-02",
             contacts=Contacts(
                 name="Имя",
                 phone="+78904093221"
             ),
             cart_items=[
                 CartOrderItem(
                     name="Товар",
                     price="1000",
                     min_price="900",
                     description="Описание",
                     count=0
                 )
             ]
         )),
         "{'order': {'cart_items': {0: {'count': ['Must be at least 1.']}}}}"],
    ),
)
async def test_errored_on_wrong_recipient_input(incorrect_params, expected_error, api, notification_type, get_args):
    params = dict()
    params[notification_type] = get_args(**incorrect_params)

    got = await api.post(
        url,
        proto=notification_proto(**params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description=f"{notification_type}: {expected_error}",
    )


@pytest.mark.parametrize(
    "notification_type, get_args, mail_type", [
        ('request_created', make_request_created, 'request_created_for_business'),
        ('cart_order_created', make_cart_order_created, 'cart_order_created')
    ]
)
async def test_errored_on_unsupported_transport(api, notification_type, get_args, mail_type):
    params = dict()
    params[notification_type] = get_args()

    got = await api.post(
        url,
        proto=notification_proto(
            transport=Transport(
                type=TransportType.PUSH,
            ),
            **params
        ),
        decode_as=Error,
        expected_status=400,
    )

    print(got)

    assert got == Error(
        code=Error.UNSUPPORTED_TRANSPORT,
        description=f"notification_type={mail_type}, unsupported_transports=PUSH",
    )


@pytest.mark.parametrize(
    "pb_transport, expected_transport",
    [
        (
            Transport(type=TransportType.EMAIL, emails=['test@ya.ru']),
            dict(type=TransportEnum.EMAIL, emails=['test@ya.ru'], phones=[], telegram_uids=[])
        ),
        (
            Transport(type=TransportType.SMS, phones=[1234567890]),
            dict(type=TransportEnum.SMS, emails=[], phones=[1234567890], telegram_uids=[])
        ),
    ],
)
@pytest.mark.parametrize(
    "notification_type, get_args, mail_type, details", [
        ('request_created', make_request_created, NotificationType.REQUEST_CREATED_FOR_BUSINESS,
         dict(
             biz_id=15,
             org=dict(
                 cabinet_link="http://cabinet.link",
                 company_link="http://company.link",
             ),
             details_link="http://details.link",
         )),
        ('cart_order_created', make_cart_order_created, NotificationType.CART_ORDER_CREATED,
         dict(
             biz_id=15,
             org=dict(
                 cabinet_link="http://cabinet.link",
                 company_link="http://company.link",
             ),
             details_link="http://details.link",
             landing=dict(
                 domain="domain",
                 url="https://domain.ru"
             ),
             order=dict(
                 id=1,
                 final_price="0",
                 final_count=2,
                 created_at="2020-02-02",
                 contacts=dict(
                     name="Name",
                     phone="+78904093221"
                 ),
                 cart_items=[
                     dict(
                         name="Good",
                         price="1000",
                         min_price="900",
                         description="Description",
                         count=1
                     ),
                     dict(
                         name="Good2",
                         price="2000",
                         min_price="1900",
                         description="Description",
                         count=1
                     )
                 ]
             )
         ))
    ]
)
async def test_will_notify_about_request_creation(
    notification_router_v3,
    api,
    pb_transport,
    expected_transport,
    notification_type,
    get_args,
    mail_type,
    details
):
    params = dict()
    params[notification_type] = get_args()

    await api.post(
        url,
        proto=notification_proto(
            transport=pb_transport,
            **params
        ),
    )

    params2 = dict()
    params2[notification_type] = get_args()

    notification_router_v3.send_notification.assert_called_with(
        transport=expected_transport,
        notification_type=mail_type,
        notification_details=details
    )
