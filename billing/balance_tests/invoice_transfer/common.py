# -*- coding: utf-8 -*-

from balance import mapper
from balance.actions import promocodes
from balance.constants import *

from tests import object_builder as ob


def create_order(client, product=DIRECT_PRODUCT_RUB_ID, service_id=None):
    product = ob.Getter(mapper.Product, product).build(client.session).obj
    if service_id is None:
        service_id = product.engine_id
    return ob.OrderBuilder(
        client=client,
        product=product,
        service_id=service_id
    ).build(client.session).obj


def create_contract(client, **kwargs):
    if kwargs.get('services') is None:
        kwargs['services'] = {ServiceId.DIRECT}
    return ob.ContractBuilder(
        client=client,
        firm=FirmId.YANDEX_OOO,
        **kwargs
    ).build(client.session).obj


def create_person(client, **kwargs):
    return ob.PersonBuilder(client=client, **kwargs).build(client.session).obj


def create_invoice(
    client,
    paysys_id,
    quantity=100,
    overdraft=0,
    orders=None,
    contract=None,
    person=None,
    cashback_bonus=None,
    promocode_discount_pct=None
):
    if not orders:
        orders = [create_order(client), ]

    if cashback_bonus:
        ob.ClientCashbackBuilder.construct(
            client.session,
            client=client,
            bonus=cashback_bonus
        )

    if promocode_discount_pct:
        promo_code, = ob.PromoCodeGroupBuilder.construct(
            client.session,
            calc_class_name='FixedDiscountPromoCodeGroup',
            calc_params={
                # adjust_quantity и apply_on_create общие для всех типов промокодов
                'adjust_quantity': 0,  # увеличиваем количество (иначе уменьшаем сумму)
                'apply_on_create': 1,  # применяем при создании счёта иначе при включении (оплате)
                # остальные зависят от типа
                'discount_pct': promocode_discount_pct,
            }
        ).promocodes
        promocodes.reserve_promo_code(client, promo_code)

    return ob.InvoiceBuilder(
        overdraft=overdraft,
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        contract=contract,
        person=person,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(quantity=quantity, order=order)
                    for order in orders
                ]
            )
        )
    ).build(client.session).obj
