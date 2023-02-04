from decimal import Decimal
from unittest.mock import Mock

from smb.common.testing_utils import dt

from maps_adv.geosmb.telegraphist.server.lib.enums import Transport

from maps_adv.geosmb.telegraphist.proto.v2 import (
    notifications_for_business_pb2,
    notifications_pb2,
)
from maps_adv.geosmb.telegraphist.proto.v2.common_pb2 import Cost, OrderItem

from maps_adv.geosmb.telegraphist.proto.v3.common_pb2 import (
    Transport as ApiTransport,
    TransportType
)

from maps_adv.geosmb.telegraphist.proto.v3.notifications_pb2 import (
    Notification,
    RequestCreated,
    OrderCreatedV3,
    OrgInfo,
    CartOrderItem,
    LandingInfo,
    OrderInfo,
    Contacts,
)

__all__ = [
    "make_order_item",
    "make_order",
    "make_base_certificate",
    "make_full_certificate",
    "make_certificate_for_expiration_notification",
    "make_order_for_business_notification",
    "UncopiedMock",
    "notification_proto",
    "make_input_params",
    "make_request_created",
    "make_cart_order_created"
]


def order_mock():
    return dict(
        biz_id=15,
        org=OrgInfo(
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
        ),
        details_link="http://details.link",
        landing=LandingInfo(
            domain="domain",
            url="https://domain.ru"
        ),
        order=OrderInfo(
            id=1,
            final_price="0",
            final_count=2,
            created_at="2020-02-02",
            contacts=Contacts(
                name="Name",
                phone="+78904093221"
            ),
            cart_items=[
                CartOrderItem(
                    name="Good",
                    price="1000",
                    min_price="900",
                    description="Description",
                    count=1
                ),
                CartOrderItem(
                    name="Good2",
                    price="2000",
                    min_price="1900",
                    description="Description",
                    count=1
                )
            ]
        )
    )


class UncopiedMock(Mock):
    def __deepcopy__(self, _):
        return self


def make_input_params(**updates):
    input_params = dict(
        transport=dict(
            type=Transport.EMAIL,
            emails=["passed_email@yandex.ru"],
        ),
    )
    input_params.update(updates)

    return input_params


def notification_proto(**updates) -> Notification:
    kwargs = dict(
        transport=ApiTransport(
            type=TransportType.EMAIL,
            emails=["kek@yandex.ru", "cheburekkek@yandex.ru"]
        ),
    )
    kwargs.update(updates)

    return Notification(**kwargs)


def make_request_created(**updates) -> RequestCreated:
    request = dict(
        biz_id=15,
        org=dict(
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
        ),
        details_link="http://details.link",
    )

    request.update(updates)

    return RequestCreated(**request)


def make_cart_order_created(**updates) -> OrderCreatedV3:
    cart_order_created = order_mock()
    cart_order_created.update(updates)

    return OrderCreatedV3(**cart_order_created)


def make_order_item(*, as_proto: bool = False, **updates) -> dict:
    kw = dict(
        name="Охлаждение жепы",
        booking_timestamp=dt("2020-10-01 13:00:00", as_proto=as_proto),
        employee_name="Веселый Эдик",
        cost=(as_proto and Cost or dict)(
            final_cost=(as_proto and str or Decimal)("50.50")
        ),
    )
    kw.update(updates)
    return (as_proto and OrderItem or dict)(**kw)


def make_order(*, as_proto: bool = False, **updates) -> dict:
    kw = dict(
        booking_code="booking_code_1",
        items=[
            make_order_item(
                name="Стрижка бровей",
                booking_timestamp=dt("2020-10-01 16:00:00", as_proto=as_proto),
                employee_name="Грустный Саня",
                cost=(as_proto and Cost or dict)(
                    final_cost=(as_proto and str or Decimal)("50")
                ),
                as_proto=as_proto,
            ),
            make_order_item(as_proto=as_proto),
        ],
        total_cost=(as_proto and Cost or dict)(
            final_cost=(as_proto and str or Decimal)("100.50")
        ),
    )
    kw.update(updates)
    return (as_proto and notifications_pb2.Order or dict)(**kw)


def make_order_for_business_notification(*, as_proto: bool = False, **updates) -> dict:
    kw = dict(
        booking_code="booking_code_1",
        items=[
            make_order_item(
                name="Стрижка бровей",
                booking_timestamp=dt("2020-09-01 16:00:00", as_proto=as_proto),
                employee_name="Грустный Саня",
                cost=(as_proto and Cost or dict)(
                    final_cost=(as_proto and str or Decimal)("50")
                ),
                as_proto=as_proto,
            ),
            make_order_item(as_proto=as_proto),
        ],
        total_cost=(as_proto and Cost or dict)(
            final_cost=(as_proto and str or Decimal)("100.50")
        ),
        comment="Вики прямые, не косые",
        source="some_source",
    )
    kw.update(updates)
    return (as_proto and notifications_for_business_pb2.Order or dict)(**kw)


def make_base_certificate(as_proto: bool = False, **updates) -> dict:
    kw = dict(name="Скидочка на стрижечку", link="http://certificate.link")
    kw.update(updates)

    return (as_proto and notifications_for_business_pb2.CertificateBaseDetails or dict)(
        **kw
    )


def make_certificate_for_expiration_notification(
    as_proto: bool = False, **updates
) -> dict:
    kw = dict(
        name="Скидочка на стрижечку",
        link="http://certificate.link",
        sales=(as_proto and str or Decimal)("200.50"),
    )
    kw.update(updates)

    if as_proto:
        return notifications_for_business_pb2.CertificateForExpirationNotification(
            base_details=make_base_certificate(
                as_proto=True, name=kw["name"], link=kw["link"]
            ),
            sales=kw["sales"],
        )

    if kw["sales"] is None:
        kw.pop("sales")

    return kw


def make_full_certificate(as_proto: bool = False, **updates) -> dict:
    kw = dict(
        name="Скидочка на стрижечку",
        link="http://certificate.link",
        price=(as_proto and str or Decimal)("12.88"),
        discount=(as_proto and str or Decimal)("20"),
        validity_period=dict(
            valid_from=dt("2020-11-20 18:00:00", as_proto=as_proto),
            valid_to=dt("2020-11-25 18:00:00", as_proto=as_proto),
        ),
    )
    kw.update(updates)

    if as_proto:
        return notifications_for_business_pb2.CertificateFullDetails(
            base_details=make_base_certificate(
                as_proto=True, name=kw.pop("name"), link=kw.pop("link")
            ),
            **kw,
        )

    return kw
