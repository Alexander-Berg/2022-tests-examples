# -*- coding: utf-8 -*-

from tests import object_builder as ob
from balance.actions.invoice_turnon import InvoiceTurnOn
from tests.balance_tests.pay_policy.pay_policy_common import create_pay_policy


def create_client(session, is_agency=0, **attrs):
    return ob.ClientBuilder(is_agency=is_agency, **attrs).build(session).obj


def create_person(session, **attrs):
    return ob.PersonBuilder(**attrs).build(session).obj


def create_invoice(session, order_client, order_agency=None, service_id=7, orders=None):
    if not orders:
        orders = [ob.OrderBuilder(client=order_client, agency=order_agency, service_id=service_id)]
    return ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=order_agency or order_client,
                rows=[ob.BasketItemBuilder(order=order,
                                           quantity=1) for order in orders]))).build(session).obj


def create_service(session):
    return ob.ServiceBuilder().build(session).obj


def create_firm(session):
    country = ob.CountryBuilder().build(session).obj
    return ob.FirmBuilder(country=country).build(session).obj


def test_get_pay_query_client_wo_person(session):
    """
    Пустой ответ метода для клиентов без плательщиков
    """
    client = create_client(session)
    result = client.get_used_person_categories(by_service=True)
    assert dict(result) == {}


def test_get_pay_query_person_wo_invoice(session):
    """
    Пустой ответ метода для клиентов без счетов
    """
    person = create_person(session)
    result = person.client.get_used_person_categories(by_service=True)
    assert dict(result) == {None: [(None, person.person_category)]}


def test_get_pay_query_several_firms(session):
    """
    По умолчанию метод возвращает список пар фирма-категория плательщика
    из включенных счетов, принадлежащих клиенту, и/или в которых клиент указан как субклиент
    без фильтрации по сервисам
    """
    agency = create_client(session, is_agency=1)
    client = create_client(session)
    invoice_1 = create_invoice(session, order_client=agency)
    InvoiceTurnOn(invoice_1, manual=True).do()

    invoice_2 = create_invoice(session, order_client=client, order_agency=agency)
    invoice_2.firm = create_firm(session)
    InvoiceTurnOn(invoice_2, manual=True).do()

    result = agency.get_used_person_categories()
    assert len(result) == 2
    assert set(result) == {(invoice_1.firm, invoice_1.person.person_category),
                           (invoice_2.firm, invoice_2.person.person_category)}


def test_get_pay_query_by_service(session):
    """
    С параметром by_service метод возвращает словарь, где ключ - сервис из заказа в счете,
    значение - список пар фирма-категория плательщика из счета
    """
    client = create_client(session)
    service_1 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_1.id,
        paymethods_params=[('USD', 1001)]
    )
    service_2 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_2.id,
        paymethods_params=[('USD', 1001)]
    )
    order_1 = ob.OrderBuilder(client=client, service_id=service_1.id)
    order_2 = ob.OrderBuilder(client=client, service_id=service_2.id)

    invoice_1 = create_invoice(session, order_client=client, orders=[order_1, order_2])
    InvoiceTurnOn(invoice_1, manual=True).do()

    result = client.get_used_person_categories(by_service=True)
    assert dict(result) == {service_1.id: [(invoice_1.firm, invoice_1.person.person_category)],
                            service_2.id: [(invoice_1.firm, invoice_1.person.person_category)]}


def test_get_pay_query_by_service_filter_service(session):
    """
    С параметром service_id метод возвращает пары фирма-категория плательщика только в разрезе указанного сервиса
    """
    client = create_client(session)
    service_1 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_1.id,
        paymethods_params=[('USD', 1001)]
    )
    invoice_1 = create_invoice(session, order_client=client, service_id=service_1.id)
    InvoiceTurnOn(invoice_1, manual=True).do()

    service_2 = create_service(session)
    create_pay_policy(
        session, firm_id=1, region_id=225, service_id=service_2.id,
        paymethods_params=[('USD', 1001)]
    )
    invoice_2 = create_invoice(session, order_client=client, service_id=service_2.id)
    InvoiceTurnOn(invoice_2, manual=True).do()

    result = client.get_used_person_categories(by_service=True, service_id=service_1.id)
    assert dict(result) == {service_1.id: [(invoice_1.firm, invoice_1.person.person_category)]}


def test_get_pay_query_unpaid_invoice(session):
    """
    Невключенный счет не учитывается в списке использованных категорий плательщиков
    """
    client = create_client(session)
    create_invoice(session, order_client=client)
    result = client.get_used_person_categories()
    assert not result


def test_get_pay_query_certificate_query(session):
    """
    Сертификатный счет не учитывается в списке использованных категорий плательщиков
    """
    client = create_client(session)
    invoice_ = create_invoice(session, order_client=client)
    invoice_.paysys.certificate = 1
    InvoiceTurnOn(invoice_, manual=True).do()

    result = client.get_used_person_categories()
    assert not result


def test_get_pay_query_client_only_service(session):
    """
    Счет с заказом непрофессиональных сервисов не учитывается в списке использованных категорий плательщиков
    """
    client = create_client(session)
    invoice_ = create_invoice(session, order_client=client)
    invoice_.invoice_orders[0].order.service.balance_service.client_only = 1
    InvoiceTurnOn(invoice_, manual=True).do()

    result = client.get_used_person_categories()
    assert not result


def test_get_pay_query_hidden_person(session):
    """
    Счет со скрытыми плательщиками учитывается в списке использованных категорий плательщиков
    """
    client = create_client(session)
    invoice_ = create_invoice(session, order_client=client)
    invoice_.person.hidden = 1
    InvoiceTurnOn(invoice_, manual=True).do()

    result = client.get_used_person_categories()
    assert result == [(invoice_.firm, invoice_.person.person_category)]
