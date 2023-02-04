# -*- coding: utf-8 -*-

import datetime
import pytest

from balance import mapper
from balance import exc
from balance.constants import (
    POSTPAY_PAYMENT_TYPE,
    PREPAY_PAYMENT_TYPE,
    ServiceId,
    FirmId,
    ContractComsnId,
)
from billing.contract_iface import ContractTypeId
from balance.actions.invoice_create import InvoiceFactory

from tests import object_builder as ob

from tests.balance_tests.invoices.invoice_factory.invoice_factory_common import (
    create_contract, create_person, create_firm, create_subclient_non_resident,
    create_currency, create_request, create_order, create_product,
    create_person_category, create_client, create_service, create_agency,
    create_service2discount_type_map,
    create_contract_commission2discount_type_map, create_contract_commission_type)

NOW = datetime.datetime.now()
WO_COMMISSION = 0

SKIPPED_SERVICES = [
    ServiceId.MUSIC,
    ServiceId.MUSIC_MEDIASERVICES,
    ServiceId.ONE_TIME_SALE,
    ServiceId.TAXI_CASH,
    ServiceId.TICKETS,
    ServiceId.MUSIC_PROMO,
    ServiceId.TAXI_PAYMENT,
    ServiceId.TICKETS_TO_EVENTS,
    ServiceId.TAXI_CARD,
    ServiceId.APIKEYS,
    ServiceId.TICKETS2,
    ServiceId.BUSES2_WEEKLY,
    ServiceId.AFISHA_MOVIEPASS,
    ServiceId.CLOUD,
    ServiceId.ZAXI,
    ServiceId.FOOD_COURIERS_PAYMENT,
    ServiceId.FOOD_SRV,
    ServiceId.LAVKA_COURIERS_PAYMENT,
    ServiceId.FOOD_SHOPS_SRV,
    ServiceId.FOOD_PICKERS_PAYMENT,
    ServiceId.FOOD_PICKERS_BUILD_ORDER,
]


class TestPrepayWithPostpayLsContract(object):
    @pytest.mark.parametrize('crossfirm', [True, False])
    def test_crossfirm(self, session, firm, client, service, currency, crossfirm):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES
        по договору с ЛС и постоплатой.
        Но если счет межфилиальный - можно"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=True,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=crossfirm)
        if crossfirm:
            i_f.do_checks()
        else:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()

    @pytest.mark.parametrize('w_contract', [True, False])
    def test_contract(self, session, firm, client, service, currency, w_contract):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES  по договору с ЛС и постоплатой.
        Но если не указывать такой договор, счет получится выставить"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=True,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract if w_contract else None, credit=0, postpay=0,
                             temporary=False, crossfirm=False)
        if w_contract:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()

        else:
            i_f.do_checks()

    @pytest.mark.parametrize('service_id', SKIPPED_SERVICES + [ServiceId.DIRECT, ServiceId.MARKET])
    def test_skipped_services(self, session, firm, client, currency, service_id, product):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES  по договору с ЛС и постоплатой.
        С сервисом из SKIPPED_SERVICES можно"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=True,
                                   services={service_id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service_id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        if service_id in SKIPPED_SERVICES:
            i_f.do_checks()
        else:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()

    @pytest.mark.parametrize('postpay', [0, 1])
    @pytest.mark.parametrize('credit', [0, 1])
    @pytest.mark.parametrize('temporary', [False, True])
    def test_credit_and_postpay(self, session, firm, client, service, currency, postpay, credit, temporary):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES по договору с ЛС
         и постоплатой.
        Но постоплатный или временный предоплатный можно"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=True,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client)
        i_f = InvoiceFactory(request=request, contract=contract, credit=credit, postpay=postpay, temporary=temporary,
                             crossfirm=False)
        if credit == 0 and postpay == 0 and not temporary:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('personal_account', [False, True])
    def test_personal_account(self, session, firm, client, service, currency, personal_account):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES по договору с ЛС
         и постоплатой.
        Выставить предоплатный счет по постоплатному договору без ЛС можно"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=personal_account,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        if personal_account:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('payment_type', [POSTPAY_PAYMENT_TYPE, PREPAY_PAYMENT_TYPE])
    def test_payment_type(self, session, firm, client, service, currency, payment_type):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES  по договору с ЛС и постоплатой.
        С сервисом из SKIPPED_SERVICES можно"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=payment_type, personal_account=True,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        if payment_type == POSTPAY_PAYMENT_TYPE:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('requested_type', ['y_invoice', None])
    def test_requested_type(self, session, firm, client, service, currency, requested_type):
        """Нельзя выставить предоплатный счет по реквесту с сервисом не из SKIPPED_SERVICES  по договору с ЛС и постоплатой.
        С сервисом из SKIPPED_SERVICES можно"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=True,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False, requested_type=requested_type)
        if requested_type != 'y_invoice':
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('firm_id', [FirmId.YANDEX_OOO, FirmId.TAXI])
    @pytest.mark.parametrize('personal_account_fictive', [True, False])
    def test_personal_account_fictive(self, session, client, service, currency, firm_id, personal_account_fictive):
        firm = ob.Getter(mapper.Firm, firm_id).build(session).obj
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   personal_account_fictive=personal_account_fictive, client=client,
                                   payment_type=POSTPAY_PAYMENT_TYPE,
                                   personal_account=True, services={service.id}, is_signed=NOW, person=person,
                                   currency=currency.num_code)
        request = create_request(session, client, orders=[create_order(session, client, service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        if personal_account_fictive is True and firm_id != FirmId.TAXI:
            i_f.do_checks()
        else:
            with pytest.raises(exc.INCOMPATIBLE_INVOICE_PARAMS):
                i_f.do_checks()


class TestNonResidentSubclients(object):

    @pytest.mark.parametrize('w_subclient_non_resident', [True, False])
    def test_w_non_res(self, session, agency, service, currency, firm, w_subclient_non_resident):
        """если в заказах реквеста указан субклиент-нерезидент, не даем создавать счет без договора с признаком
        'работа с нерезидентами'"""
        if w_subclient_non_resident:
            client = create_subclient_non_resident(session, currency)
        else:
            client = create_client(session)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=agency)
        request = create_request(session, agency, orders=[create_order(session, client, service_id=service.id,
                                                                       agency=agency)])
        i_f = InvoiceFactory(request=request, contract=None, credit=0, postpay=0, temporary=False,
                             crossfirm=False, person=person)
        if w_subclient_non_resident:
            with pytest.raises(exc.INVOICE_HASNOT_NON_RESIDENT_CONTRACT):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('w_mixed_subclients', [True, False])
    def test_w_non_res_mixed_subclients(self, session, agency, service, currency, firm, w_mixed_subclients):
        """если в заказах реквеста указан субклиент-нерезидент и обычный субклиент, не даем создавать счет"""
        subclient_non_resident = create_subclient_non_resident(session, currency)
        if w_mixed_subclients:
            second_subclient = create_client(session)
        else:
            second_subclient = create_subclient_non_resident(session, currency)

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=agency)
        request = create_request(session, agency,
                                 orders=[create_order(session, subclient_non_resident, service_id=service.id,
                                                      agency=agency),
                                         create_order(session, second_subclient, service_id=service.id,
                                                      agency=agency)])
        contract = create_contract(session, commission=ContractTypeId.WHOLESALE_AGENCY, firm=firm.id,
                                   client=agency, payment_type=PREPAY_PAYMENT_TYPE, services={service.id},
                                   is_signed=NOW, person=person, currency=currency.num_code,
                                   non_resident_clients=1)
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False, person=person)
        if w_mixed_subclients:
            with pytest.raises(exc.INVOICE_HAS_MIXED_NON_RESIDENT):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('w_mixed_currencies', [True, False])
    def test_w_non_res_mixed_currencies(self, session, agency, service, currency, firm, w_mixed_currencies):
        """если в заказах реквеста указан субклиенты-нерезиденты с разными валютами, не даем создавать счет"""
        subclient_non_resident = create_subclient_non_resident(session, currency)
        if w_mixed_currencies:
            second_subclient = create_subclient_non_resident(session, create_currency(session))
        else:
            second_subclient = create_subclient_non_resident(session, currency)

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=agency)
        request = create_request(session, agency,
                                 orders=[create_order(session, subclient_non_resident, service_id=service.id,
                                                      agency=agency),
                                         create_order(session, second_subclient, service_id=service.id,
                                                      agency=agency)])
        contract = create_contract(session, commission=ContractTypeId.WHOLESALE_AGENCY, firm=firm.id,
                                   client=agency, payment_type=PREPAY_PAYMENT_TYPE, services={service.id},
                                   is_signed=NOW, person=person, currency=currency.num_code,
                                   non_resident_clients=1)
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False, person=person)
        if w_mixed_currencies:
            with pytest.raises(exc.INVOICE_HAS_MIXED_NON_RESIDENT):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('contract_non_resident_clients', [True, False])
    def test_w_non_res_contract(self, session, agency, service, currency, firm, contract_non_resident_clients):
        """если в заказах реквесета указан субклиент-нерезидент, не даем создавать счет без договора с признаком
        'работа с нерезидентами'"""
        subclient_non_resident = create_subclient_non_resident(session, currency)

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=agency)
        request = create_request(session, agency,
                                 orders=[create_order(session, subclient_non_resident, service_id=service.id,
                                                      agency=agency),
                                         create_order(session, subclient_non_resident, service_id=service.id,
                                                      agency=agency)])
        contract = create_contract(session, commission=ContractTypeId.WHOLESALE_AGENCY, firm=firm.id,
                                   client=agency, payment_type=PREPAY_PAYMENT_TYPE, services={service.id},
                                   is_signed=NOW, person=person, currency=currency.num_code,
                                   non_resident_clients=contract_non_resident_clients)
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False, person=person)
        if not contract_non_resident_clients:
            with pytest.raises(exc.INVOICE_HASNOT_NON_RESIDENT_CONTRACT):
                i_f.do_checks()
        else:
            i_f.do_checks()


class TestCommissionType(object):

    def test_base(self, session, client, firm, currency, service):
        """Если ни для сервисa, ни для типа коммисии договора не настроен маппинг скидочных политик,
        не делаем проверку скидочной политики в продуктах
        """
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract_commission_type = create_contract_commission_type(session)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=contract_commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        request = create_request(session, client,
                                 orders=[create_order(session, client,
                                                      product=create_product(session,
                                                                             commission_type=ob.get_big_number()),
                                                      service_id=service.id)])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        i_f.do_checks()

    @pytest.mark.parametrize('product_commission_type, contract_discount_type_ids',
                             [
                                 ({1, 3}, {1}),
                                 ({1}, {1, 3}),
                                 ({1, 3}, {1, 3}),
                             ])
    def test_contract_commission_has_discount_types(self, session, client, firm, currency, service,
                                                    product_commission_type, contract_discount_type_ids):
        """если для типа комиссии из коммиссионного договора настроен маппинг скидочных политик,
        проверяем, что скидочные политики из продуктов - подмножество скидочных политик из договора
        """
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract_commission_type = create_contract_commission_type(session)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=contract_commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client,
                               product=create_product(session, commission_type=commission_type),
                               service_id=service.id)
                  for commission_type in product_commission_type]
        request = create_request(session, client, orders=orders)
        create_contract_commission2discount_type_map(session, contract_commission_type,
                                                     discount_type_ids=contract_discount_type_ids)
        session.flush()
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        if product_commission_type <= contract_discount_type_ids:
            i_f.do_checks()
        else:
            with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
                i_f.do_checks()

    @pytest.mark.parametrize('product_commission_type, service_discount_type_ids',
                             [
                                 ({1, 3}, {1}),
                                 ({1}, {1, 3}),
                                 ({1, 3}, {1, 3}),
                             ])
    def test_service_has_discount_types(self, session, client, firm, currency, service, product_commission_type,
                                        service_discount_type_ids):
        """если для сервиса настроен маппинг скидочных политик, проверяем, что скидочные политики
        из продуктов - подмножество скидочных политик из сервиса
        """
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract_commission_type = create_contract_commission_type(session)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=contract_commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client, product=create_product(session, commission_type=commission_type),
                               service_id=service.id)
                  for commission_type in product_commission_type]
        request = create_request(session, client, orders=orders)
        create_service2discount_type_map(session, service.id, service_discount_type_ids)
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=False)
        if product_commission_type <= service_discount_type_ids:
            i_f.do_checks()
        else:
            with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
                i_f.do_checks()

    def test_crossfirm(self, session, client, firm, currency, service):
        """Пропускаем проверку для межфилиальных счетов"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract_commission_type = create_contract_commission_type(session)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=contract_commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client, product=create_product(session, commission_type=commission_type),
                               service_id=service.id)
                  for commission_type in [1, 3]]
        request = create_request(session, client, orders=orders)
        create_service2discount_type_map(session, service.id, [1])
        create_contract_commission2discount_type_map(session, contract_commission_type,
                                                     discount_type_ids=[1])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=True)

        with pytest.raises(exc.CROSSFIRM_NOT_ALLOWED_WITH_COMMISSION_CONTRACT):
            i_f.do_checks()

    @pytest.mark.parametrize('commission_type', [WO_COMMISSION, ContractComsnId.AUTO])
    def test_commission(self, session, client, firm, currency, service, commission_type):
        """Пропускаем проверку для договоров без типа коммиссий"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract = create_contract(session, commission=ContractTypeId.COMMISSION, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client,
                               product=create_product(session, commission_type=product_commission_type),
                               service_id=service.id)
                  for product_commission_type in [1, 3]]
        request = create_request(session, client, orders=orders)
        create_service2discount_type_map(session, service.id, [1])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=False,
                             crossfirm=None)
        if commission_type != WO_COMMISSION:
            with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
                i_f.do_checks()
        else:
            i_f.do_checks()

    @pytest.mark.parametrize('temporary', [True, False])
    def test_temporary(self, session, client, firm, currency, service, temporary):
        """Пропускаем проверку для временных счетов"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract_commission_type = create_contract_commission_type(session)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=contract_commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client, product=create_product(session, commission_type=commission_type),
                               service_id=service.id)
                  for commission_type in [1, 3]]
        request = create_request(session, client, orders=orders)
        create_service2discount_type_map(session, service.id, [1])
        create_contract_commission2discount_type_map(session, contract_commission_type, discount_type_ids=[1])
        i_f = InvoiceFactory(request=request, contract=contract, credit=0, postpay=0, temporary=temporary,
                             crossfirm=False)
        if temporary:
            i_f.do_checks()
        else:
            with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
                i_f.do_checks()

    @pytest.mark.parametrize('w_contract', [True, False])
    def test_contract(self, session, client, firm, currency, service, w_contract):
        """Пропускаем проверку для временных счетов"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        if w_contract:
            contract_commission_type = create_contract_commission_type(session)
            contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                       client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                       commission_type=contract_commission_type,
                                       services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client, product=create_product(session, commission_type=commission_type),
                               service_id=service.id)
                  for commission_type in [1, 3]]
        request = create_request(session, client, orders=orders)
        create_service2discount_type_map(session, service.id, [1])

        i_f = InvoiceFactory(request=request, contract=contract if w_contract else None, credit=0, postpay=0,
                             temporary=False, crossfirm=False)
        if not w_contract:
            i_f.do_checks()
        else:
            with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
                i_f.do_checks()

    @pytest.mark.parametrize('postpay', [1, 0])
    @pytest.mark.parametrize('credit', [1, 0])
    def test_credit(self, session, client, firm, currency, service, postpay, credit):
        """Пропускаем проверку для постоплатных счетов"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        person = create_person(session, type=person_category.category, client=client)
        contract_commission_type = create_contract_commission_type(session)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                                   commission_type=contract_commission_type,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        orders = [create_order(session, client, product=create_product(session, commission_type=commission_type),
                               service_id=service.id)
                  for commission_type in [1, 3]]
        request = create_request(session, client, orders=orders)
        create_service2discount_type_map(session, service.id, [1])
        create_contract_commission2discount_type_map(session, contract_commission_type, discount_type_ids=[1])
        i_f = InvoiceFactory(request=request, contract=contract, credit=credit, postpay=postpay, temporary=False,
                             crossfirm=False)
        if credit:
            i_f.do_checks()
        else:
            with pytest.raises(exc.COMMISSION_TYPE_NOT_ALLOWED_BY_CONTRACT):
                i_f.do_checks()
