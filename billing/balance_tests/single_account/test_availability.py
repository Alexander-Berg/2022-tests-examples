# coding=utf-8

from datetime import datetime, timedelta

import mock
from mock import Mock
import pytest

from balance import mapper
from balance import muzzle_util as ut
from balance.actions.single_account.availability import (
    DenyReason,
    get_deny_reason_for_invoice_params,
    get_deny_reason_for_client,
    get_deny_reason_for_order,
    get_deny_reason_for_product,
    get_deny_reason_for_service,
    get_deny_reason_for_ref_invoice,
    check_person_params,
    get_denied_person_categories,
    check_person_attributes,
    ALLOWED_INDIVIDUAL_PERSON_CATEGORIES,
    normalize_inn,
)
from balance.constants import (
    PersonCategoryCodes,
    TAX_POLICY_RUSSIA_RESIDENT,
    FirmId,
    RegionId,
    PaysysGroupIDs,
    PaymentMethodIDs,
    ServiceId,
)
from balance import exc

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_invoice,
)

pytestmark = [pytest.mark.single_account]


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


ALLOWED_INDIVIDUAL_CATEGORY_CODE = next(iter(ALLOWED_INDIVIDUAL_PERSON_CATEGORIES))


@pytest.fixture(name='allowed_individual_category')
def get_allowed_individual_category(session):
    return session.query(mapper.PersonCategory).getone(ALLOWED_INDIVIDUAL_CATEGORY_CODE)


def get_allowed_paysys(session, category):
    paysys = (
        session.query(mapper.Paysys)
            .filter_by(firm_id=FirmId.YANDEX_OOO,
                       category=category,
                       group_id=PaysysGroupIDs.default,
                       payment_method_id=PaymentMethodIDs.bank)
            .order_by(mapper.Paysys.id)
            .first()
    )
    return paysys


class TestGetDeniedPersonCategories(object):
    def test_client_already_has_allowed_individual(self, session, client):
        category = ALLOWED_INDIVIDUAL_CATEGORY_CODE
        ob.PersonBuilder(
            type=category, client=client
        ).build(session)
        denied_categories = {category.category for category in get_denied_person_categories(client)}
        assert denied_categories == {category}

    def test_client_does_not_have_allowed_individual(self, session, client):
        assert get_denied_person_categories(client) == set()


class TestCheckPersonParams(object):
    def test_is_partner_ignored(self, client):
        check_person_params(client=client, params={'is_partner': True})

    def test_not_allowed_category_ignored(self, client):
        check_person_params(client=client, params={'category': PersonCategoryCodes.armenia_individual})

    def test_duplicated_legal_entity(self, session, client):
        category = PersonCategoryCodes.russia_resident_legal_entity
        inn = 1
        person = ob.PersonBuilder(type=category, client=client, inn=inn).build(session=session).obj
        with pytest.raises(exc.DUPLICATED_LEGAL_ENTITY) as e:
            check_person_params(client, {'category': category, 'inn': inn})
        assert e.value.conflicting_person_id == person.id

    def test_multiple_individuals(self, session, client):
        category = ALLOWED_INDIVIDUAL_CATEGORY_CODE
        person = ob.PersonBuilder(type=category, client=client).build(session=session).obj
        with pytest.raises(exc.MULTIPLE_INDIVIDUALS) as e:
            check_person_params(client, {'category': category})
        assert e.value.conflicting_person_id == person.id

    def test_can_create_legal_entity_with_existing_individual(self, session, client):
        ob.PersonBuilder(type=ALLOWED_INDIVIDUAL_CATEGORY_CODE, client=client).build(session)
        check_person_params(client, {
            'is_partner': 0,
            'category': PersonCategoryCodes.russia_resident_legal_entity,
            'inn': 1
        })

    def test_can_create_individual_with_existing_legal_entity(self, session, client):
        ob.PersonBuilder(
            type=PersonCategoryCodes.russia_resident_legal_entity,
            client=client, inn=1
        ).build(session=session)
        check_person_params(client, {
            'is_partner': 0,
            'category': ALLOWED_INDIVIDUAL_CATEGORY_CODE
        })

    def test_can_create_non_duplicate_legal_entity(self, session, client):
        ob.PersonBuilder(
            type=PersonCategoryCodes.russia_resident_legal_entity,
            client=client, inn=1
        ).build(session=session)
        check_person_params(client, {
            'is_partner': 0,
            'category': PersonCategoryCodes.russia_resident_legal_entity,
            'inn': 2
        })

    def test_with_ignore_person_id(self, session, client):
        category = ALLOWED_INDIVIDUAL_CATEGORY_CODE
        person = ob.PersonBuilder(type=category, client=client).build(session=session).obj
        check_person_params(client, {'category': category}, ignore_person_id=person.id)


@pytest.fixture
def enabled_service(session):
    """ Single account enabled service """
    service = ob.ServiceBuilder().build(session).obj
    session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = {service.id}
    return service


class TestGetDenyReasonForService(object):
    def test_is_not_enabled_for_service(self, session):
        service = ob.ServiceBuilder().build(session).obj
        pay_policy_part_id = ob.create_pay_policy_service(session, service.id, FirmId.YANDEX_OOO)
        ob.create_pay_policy_region(
            session, pay_policy_part_id, RegionId.RUSSIA, is_agency=False
        )
        assert get_deny_reason_for_service(session, service.id) == (
            'Single account is not enabled for service with id = {service_id}'
        ).format(service_id=service.id)

    def test_wrong_service_firm(self, session, enabled_service):
        pay_policy_part_id = ob.create_pay_policy_service(session, enabled_service.id, FirmId.UBER_KZ)
        ob.create_pay_policy_region(
            session, pay_policy_part_id, RegionId.RUSSIA, is_agency=False
        )
        assert get_deny_reason_for_service(session, enabled_service.id) == (
            'Service (id = {service_id}) has wrong matching firm by pay policies for single account'
        ).format(service_id=enabled_service.id)

    def test_positive(self, session, enabled_service):
        pay_policy_part_id = ob.create_pay_policy_service(session, enabled_service.id, FirmId.YANDEX_OOO)
        ob.create_pay_policy_region(
            session, pay_policy_part_id, RegionId.RUSSIA, is_agency=False
        )
        assert get_deny_reason_for_service(session, enabled_service.id) is None

    def test_boolean_config_is_true(self, session):
        service = ob.ServiceBuilder().build(session).obj
        pay_policy_part_id = ob.create_pay_policy_service(session, service.id, FirmId.YANDEX_OOO)
        ob.create_pay_policy_region(
            session, pay_policy_part_id, RegionId.RUSSIA, is_agency=False
        )
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = 1
        assert get_deny_reason_for_service(session, service.id) is None

    def test_boolean_config_is_false(self, session):
        service = ob.ServiceBuilder().build(session).obj
        pay_policy_part_id = ob.create_pay_policy_service(session, service.id, FirmId.YANDEX_OOO)
        ob.create_pay_policy_region(
            session, pay_policy_part_id, RegionId.RUSSIA, is_agency=False
        )
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = 0
        assert get_deny_reason_for_service(session, service.id) == (
            'Single account is not enabled for service with id = {service_id}'
        ).format(service_id=service.id)


@pytest.fixture
def product(session, enabled_service):
    return ob.ProductBuilder(create_taxes=False, service=enabled_service).build(session).obj


# noinspection PyProtectedMember
class TestGetDenyReasonForProduct(object):
    DT = datetime(2019, 1, 1)

    def test_positive(self, session, product):
        tax_policy = session.query(mapper.TaxPolicy).getone(TAX_POLICY_RUSSIA_RESIDENT)
        ob.TaxBuilder(tax_policy=tax_policy, dt=self.DT, product=product).build(session)
        assert get_deny_reason_for_product(product, self.DT) is None

    def test_missing_required_product_tax_policy(self, product):
        assert get_deny_reason_for_product(product, self.DT) == (
            "Product (id = {product_id}) doesn't have required tax policy for single account"
        ).format(product_id=product.id)


class TestGetDenyReasonForClient(object):
    def test_old_client(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT - timedelta(days=1)
        assert get_deny_reason_for_client(client) is DenyReason.old_client

    def test_filter_client_id(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT + timedelta(days=1)
        session.config.__dict__['SINGLE_ACCOUNT_ID_MODULUS'] = client.id + 1

        assert get_deny_reason_for_client(client) is DenyReason.client_not_in_experiment

    def test_filter_client_id_positive(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT + timedelta(days=1)
        client.id = client.id * 10  # подгоняем чтобы остаток от деления был 0
        session.config.__dict__['SINGLE_ACCOUNT_ID_MODULUS'] = 10

        assert get_deny_reason_for_client(client) is None

    def test_forbidden_client_category(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
        client.assign_agency_status(is_agency=True)
        assert get_deny_reason_for_client(client) is DenyReason.forbidden_client_category

    def test_multiple_individuals(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
        for _ in range(2):
            ob.PersonBuilder(client=client, type=ALLOWED_INDIVIDUAL_CATEGORY_CODE).build(session)
        assert get_deny_reason_for_client(client) is DenyReason.multiple_individuals

    def test_duplicate_legal_entities(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
        for _ in range(2):
            ob.PersonBuilder(
                client=client, type=PersonCategoryCodes.russia_resident_legal_entity, inn=1
            ).build(session)
        assert get_deny_reason_for_client(client) is DenyReason.duplicate_legal_entities

    def test_positive(self, session, client):
        client.creation_dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT
        assert get_deny_reason_for_client(client) is None


class TestGetDenyReasonForRequest(object):
    # "request" is reserved pytest fixture name
    @pytest.fixture(name='request_obj')
    def create_request(self, session, client):
        invoice = create_invoice(session)
        return ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=invoice)]
            )
        ).build(session).obj

    @pytest.fixture
    def russia(self, session):
        return session.query(mapper.Firm).getone(FirmId.YANDEX_OOO)

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_client',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_order',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_ref_invoice',
                new=Mock(return_value=None))
    def test_not_allowed_firm(self, session, request_obj, allowed_individual_category):
        firm = session.query(mapper.Firm).getone(FirmId.UBER_KZ)
        assert get_deny_reason_for_invoice_params(request_obj, allowed_individual_category, firm) == (
            'Single account is not available for firm (id = {firm_id})'.format(firm_id=firm.id)
        )

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_client',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_order',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_ref_invoice',
                new=Mock(return_value=None))
    def test_forbidden_person_category(self, session, request_obj, russia):
        category = session.query(mapper.PersonCategory).getone(PersonCategoryCodes.armenia_individual)
        assert get_deny_reason_for_invoice_params(request_obj, category, russia) == (
            'Forbidden person category for single account: {category}.'
        ).format(category=category.category)

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_order',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_ref_invoice',
                new=Mock(return_value=None))
    def test_denied_for_client(self, session, request_obj, russia, allowed_individual_category):
        deny_reason = 'Bad client'
        with mock.patch('balance.actions.single_account.availability.get_deny_reason_for_client',
                        new=Mock(return_value=deny_reason)):
            assert get_deny_reason_for_invoice_params(request_obj, allowed_individual_category, russia) == deny_reason

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_client',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_ref_invoice',
                new=Mock(return_value=None))
    def test_denied_for_row(self, session, request_obj, russia, allowed_individual_category):
        deny_reason = 'Bad row'
        with mock.patch('balance.actions.single_account.availability.get_deny_reason_for_order',
                        new=Mock(return_value=deny_reason)):
            assert get_deny_reason_for_invoice_params(request_obj, allowed_individual_category, russia) == deny_reason

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_client',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_order',
                new=Mock(return_value=None))
    def test_denied_for_ref_invoice(self, session, request_obj, russia, allowed_individual_category):
        deny_reason = 'Bad row'
        with mock.patch('balance.actions.single_account.availability.get_deny_reason_for_ref_invoice',
                        new=Mock(return_value=deny_reason)):
            assert get_deny_reason_for_invoice_params(request_obj, allowed_individual_category, russia) == deny_reason

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_order',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_ref_invoice',
                new=Mock(return_value=None))
    def test_get_deny_reason_for_client_not_called_if_client_has_single_account(
            self, session, russia, allowed_individual_category
    ):
        client = ob.ClientBuilder(with_single_account=True).build(session).obj
        request = self.create_request(session, client)
        with mock.patch('balance.actions.single_account.availability.get_deny_reason_for_client') \
                as get_deny_reason_for_client_mock:
            get_deny_reason_for_client_mock.return_value = 'Denied'
            assert get_deny_reason_for_invoice_params(request, allowed_individual_category, russia) is None
        get_deny_reason_for_client_mock.assert_not_called()

    def test_positive_fair(self, session, enabled_service, client, product, russia, allowed_individual_category):
        """ Честно выполняем весь stack проверок """
        dt = session.config.SINGLE_ACCOUNT_MIN_CLIENT_DT

        client.creation_dt = dt

        tax_policy = session.query(mapper.TaxPolicy).getone(TAX_POLICY_RUSSIA_RESIDENT)
        ob.TaxBuilder(tax_policy=tax_policy, dt=dt, product=product).build(session)

        pay_policy_part_id = ob.create_pay_policy_service(session, enabled_service.id, FirmId.YANDEX_OOO)
        ob.create_pay_policy_region(session, pay_policy_part_id, RegionId.RUSSIA, is_agency=False)

        ref_invoice = create_invoice(
            session,
            paysys_id=get_allowed_paysys(session, allowed_individual_category.category).id,
            service_id=enabled_service.id
        )

        order = ob.OrderBuilder(client=client, product=product, service_id=enabled_service.id)
        basket = ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(quantity=10, order=order)],
            register_rows=[ob.BasketRegisterRowBuilder(ref_invoice=ref_invoice)]
        )
        request = ob.RequestBuilder(basket=basket).build(session).obj
        request.dt = dt

        assert get_deny_reason_for_invoice_params(request, allowed_individual_category, russia) is None


class TestGetDenyReasonForRow(object):
    @pytest.fixture
    def order(self, session):
        return ob.OrderBuilder().build(session).obj

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_service',
                new=Mock(return_value=None))
    def test_denied_for_product(self, session, order):
        deny_reason = 'Bad product'
        with mock.patch('balance.actions.single_account.availability.get_deny_reason_for_product',
                        new=Mock(return_value=deny_reason)):
            assert get_deny_reason_for_order(order) == deny_reason

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_product',
                new=Mock(return_value=None))
    def test_denied_for_service(self, session, order):
        deny_reason = 'Bad service'
        with mock.patch('balance.actions.single_account.availability.get_deny_reason_for_service',
                        new=Mock(return_value=deny_reason)):
            assert get_deny_reason_for_order(order) == deny_reason

    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_service',
                new=Mock(return_value=None))
    @mock.patch('balance.actions.single_account.availability.get_deny_reason_for_product',
                new=Mock(return_value=None))
    def test_positive(self, session, order):
        assert get_deny_reason_for_order(order) is None


class TestGetDenyReasonForRefInvoice(object):
    @pytest.fixture
    def paysys(self, session):
        return get_allowed_paysys(session, ALLOWED_INDIVIDUAL_CATEGORY_CODE)

    def test_positive(self, session, paysys):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True
        invoice = create_invoice(session, paysys_id=paysys.id)
        assert get_deny_reason_for_ref_invoice(invoice) is None

    def test_deny_for_service(self, session, paysys):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = False
        invoice = create_invoice(session, paysys_id=paysys.id, service_id=ServiceId.DIRECT)
        assert get_deny_reason_for_ref_invoice(invoice) == 'Single account is not enabled for service with id = 7'

    def test_deny_for_tax(self, session, paysys):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True
        invoice = create_invoice(session, paysys_id=paysys.id)

        msg = "Invoice %s doesn't have required tax policy for single account" % invoice.id
        with mock.patch('balance.mapper.invoices.Invoice.tax_policy_pct', ut.Struct(tax_policy_id=2)):
            assert get_deny_reason_for_ref_invoice(invoice) == msg

    def test_deny_for_invalid_tax(self, session, paysys):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = True
        invoice = create_invoice(session, paysys_id=paysys.id)

        def _mock_tax(s):
            raise exc.INVALID_PARAM

        with mock.patch('balance.mapper.invoices.Invoice.tax_policy_pct', property(_mock_tax)):
            assert get_deny_reason_for_ref_invoice(invoice) == "Tax is not defined for ref_invoice %s" % invoice.id


@mock.patch('balance.actions.single_account.availability.check_person_params')
def test_check_person_attributes(check_params_mock, session):
    """ Простая проверка передачи атрибутов в аргументах. """
    inn = 1
    is_partner = 0
    category = PersonCategoryCodes.russia_resident_legal_entity

    person = ob.PersonBuilder(
        type=PersonCategoryCodes.russia_resident_legal_entity,
        inn=inn,
        is_partner=is_partner
    ).build(session).obj

    check_person_attributes(person)

    check_params_mock.assert_called_once_with(person.client, {
        'category': category,
        'inn': inn,
        'is_partner': is_partner
    }, ignore_person_id=person.id)


def test_normalize_inn():
    assert normalize_inn(None) == ''
    assert normalize_inn('') == ''
    assert normalize_inn('test') == 'test'
    assert normalize_inn('тест') == 'тест'
    assert normalize_inn(u'тест') == u'тест'
    assert normalize_inn(1) == '1'
