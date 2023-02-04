# -*- coding: utf-8 -*-

import collections

from balance import mapper
from balance.constants import (
    ServiceId,
    FirmId,
    DIRECT_PRODUCT_RUB_ID,
)

from tests import object_builder as ob

SERVICE_ID = ServiceId.DIRECT
FIRM_ID = FirmId.YANDEX_OOO
ServiceFirmOverdraftParams = collections.namedtuple(
    "ServiceFirmOverdraftParams",
    [
        "service_id",
        "firm_id",
        "start_dt",
        "end_dt",
        "payment_term_id",
        "use_working_cal",
        "fixed_currency",
        "thresholds",
        "turnover_firms",
        "is_agency_allowed",
        "only_external"
    ],
)


def set_limit(
    client, currency, limit, service_id=ServiceId.DIRECT, firm_id=FirmId.YANDEX_OOO
):
    client.set_overdraft_limit(service_id, firm_id, limit, currency)
    client.session.flush()


def create_invoice(client, overdraft=1, quantity=100):
    if not isinstance(quantity, collections.Iterable):
        quantity = [quantity]

    invoice = (
        ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client,
                    rows=[
                        ob.BasketItemBuilder(
                            order=ob.OrderBuilder(
                                client=client,
                                product=ob.Getter(
                                    mapper.Product, DIRECT_PRODUCT_RUB_ID
                                ),
                            ),
                            quantity=qty,
                        )
                        for qty in quantity
                    ],
                )
            ),
            overdraft=overdraft,
        )
        .build(client.session)
        .obj
    )
    if overdraft:
        invoice.turn_on_rows()
    return invoice


def generate_invoices(
    client,
    dt_qtys,
    firm_id=FirmId.YANDEX_OOO,
    service_id=ServiceId.DIRECT,
    paysys_id=1003,
    product_id=DIRECT_PRODUCT_RUB_ID,
):
    invoices = [
        ob.InvoiceBuilder(
            paysys=ob.Getter(mapper.Paysys, paysys_id),
            request=ob.RequestBuilder(
                firm_id=firm_id,
                basket=ob.BasketBuilder(
                    dt=dt_,
                    client=client,
                    rows=[
                        ob.BasketItemBuilder(
                            order=ob.OrderBuilder(
                                client=client,
                                service_id=service_id,
                                product=ob.Getter(mapper.Product, product_id),
                            ),
                            quantity=qty,
                        )
                    ],
                ),
            ),
        )
        .build(client.session)
        .obj
        for dt_, qty in dt_qtys
    ]

    for invoice in invoices:
        invoice.turn_on_rows()
        invoice.close_invoice(invoice.dt)

    return invoices


def patch_sfop(**kwargs):
    service_id = kwargs.get("service_id", SERVICE_ID) or SERVICE_ID
    firm_id = kwargs.get("firm_id", FIRM_ID) or FIRM_ID
    thresholds = kwargs.get("thresholds", {"RUB": 1}) or {"RUB": 1}
    is_agency_allowed = kwargs.get("is_agency_allowed", 1)
    only_external = kwargs.get("only_external", 0)

    return ServiceFirmOverdraftParams(
        **{
            "service_id": service_id,
            "firm_id": firm_id,
            "start_dt": None,
            "end_dt": None,
            "payment_term_id": 15,
            "use_working_cal": 0,
            "fixed_currency": 0,
            "thresholds": thresholds,
            "turnover_firms": [firm_id],
            "is_agency_allowed": is_agency_allowed,
            "only_external": only_external
        }
    )
