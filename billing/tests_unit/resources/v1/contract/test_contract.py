# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import hamcrest as hm
import http.client as http
import pytest
from decimal import Decimal as D

from billing.contract_iface.cmeta import general
from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client, create_agency
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
from yb_snout_api.tests_unit.fixtures.invoice import create_custom_invoice
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract, create_collateral, create_partner_contract
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.fixture(name='view_contract_role')
def create_view_contract_role():
    return create_role((
        cst.PermissionCode.VIEW_CONTRACTS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


class BaseTestCaseMixin(object):
    BASE_API = '/v1/contract'

    @staticmethod
    def check_response(contract, res, allowed=True):
        if allowed:
            hm.assert_that(res.status_code, hm.equal_to(http.OK))
            data = res.get_json()
            hm.assert_that(
                data['data'],
                hm.has_entries({
                    'id': contract.id,
                    'external_id': contract.external_id,
                    'currency': contract.current_signed_currency.char_code,
                }),
            )
        else:
            hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))


class TestContract(BaseTestCaseMixin, TestCaseApiAppBase):
    @pytest.mark.smoke
    @pytest.mark.parametrize('filter_field, contract_field', [
        pytest.param('contract_id', 'id', id='By id'),
        pytest.param('contract_eid', 'external_id', id='By external_id'),
    ])
    def test_get_contract_shot(self, filter_field, contract_field, admin_role, view_contract_role, client):
        security.set_roles([admin_role, view_contract_role])

        now = datetime.datetime.now()
        contract = create_general_contract(client=client, dt=now, is_signed=now)

        res = self.test_client.get(
            self.BASE_API,
            params={filter_field: getattr(contract, contract_field)},
        )
        self.check_response(contract, res, allowed=True)

    @pytest.mark.parametrize('filter_field', [
        pytest.param('contract_id', id='By id'),
        pytest.param('contract_eid', id='By external_id'),
    ])
    def test_contract_not_found(self, filter_field, admin_role, view_contract_role, client):
        val = not_existing_id(ob.ContractBuilder) if filter_field == 'contract_id' else '-100'
        security.set_roles([admin_role, view_contract_role])
        res = self.test_client.get(
            self.BASE_API,
            params={filter_field: val},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        data = res.get_json()
        hm.assert_that(
            data,
            hm.has_entries({'error': 'CONTRACT_NOT_FOUND'}),
        )

    def test_id_missed(self):
        res = self.test_client.get(self.BASE_API, {})
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({
                        'error': 'CONTRACT_REQUIRED_FIELD_VALIDATION_ERROR',
                        'description': 'Required "id" or "external_id"',
                    })),
                }),
            }),
        )

    def test_multiple_contracts(self):
        eid = 'snout-test-%s' % ob.get_big_number()
        [create_general_contract(external_id=eid) for _i in range(2)]
        res = self.test_client.get(self.BASE_API, {'contract_eid': eid})
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'MULTIPLE_CONTRACTS_FOUND',
                'description': 'Multiple contracts %s were found' % eid,
            }),
        )

    @pytest.mark.slow
    @pytest.mark.smoke
    def test_contract_detailed(self, client, person):
        session = self.test_session
        dt_fmt = '%Y-%m-%dT%H:%M:%S'
        now = session.now()
        t1 = now - datetime.timedelta(minutes=5)
        t2 = now - datetime.timedelta(minutes=1)
        finish_dt = now + datetime.timedelta(days=7)

        contract = create_general_contract(
            client=client,
            person=person,
            postpay=0,
            comission=0,  # должно отображаться BALANCE-36356
            firm_id=cst.FirmId.YANDEX_EU_AG,  # доступны electronic_reports
            currency=978,  # 'EUR'
            services={cst.ServiceId.DIRECT: 1},
            dt=t1,
            is_signed=t1,
        )
        create_collateral(contract, num='02', collateral_type=general.collateral_types[1003], dt=t1, is_signed=t1, memo=u'Test memооо')
        create_collateral(contract, num='03', collateral_type=general.collateral_types[80], dt=t2, finish_dt=finish_dt, is_signed=t2)
        periods = [
            ob.CRPaymentReportBuilder.construct(session, contract=contract, avans_amount=D('3.33')),
            ob.CRPaymentReportBuilder.construct(session, contract=contract, avans_amount=D('3.66')),
        ]
        invoice = create_custom_invoice(
            client=client,
            person=person,
            firm_id=cst.FirmId.YANDEX_EU_AG,
            contract=contract,
            postpay=0,
        )
        invoice.manual_turn_on(D('1000'))
        order = invoice.consumes[0].order
        order.calculate_consumption(
            dt=datetime.datetime.today() - datetime.timedelta(days=1),
            stop=0,
            shipment_info={'Bucks': D('10')},
        )
        invoice.generate_act(force=True)
        session.flush()

        res = self.test_client.get(
            self.BASE_API,
            {
                'contract_id': contract.id,
                'show_details': True,
                'show_zero_attrs': True,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'contract': hm.has_entries({
                    'id': contract.id,
                    'external_id': contract.external_id,
                    'type': 'GENERAL',
                    'oebs_exportable': True,
                    'is_new_comm': False,
                    # 'currency': 'EUR',
                    'client_id': client.id,
                    'person_id': person.id,
                    'periods': hm.contains(*[
                        hm.has_entries({
                            'id': p.id,
                            'avans_amount': str(p.avans_amount),
                            'pretty_dt': hm.not_none(),
                            'nach_amount': '1.23',
                            'db_status': 0,
                            'perech_amount': '6.66',
                            'factura_id': None,
                            'start_dt': hm.not_none(),
                            'end_dt': hm.not_none(),
                        })
                        for p in periods
                    ]),
                    'electronic_reports': hm.contains(
                        hm.has_entries({
                            'dt': datetime.datetime(now.year, now.month, 1, 0, 0, 0).strftime(dt_fmt),
                            'period': hm.not_none(),
                        }),
                    ),
                }),
                'request_params': hm.has_entries({
                    'show_details': True,
                }),
                'contract_attributes': hm.contains(
                    hm.has_entries({'group': 1, 'name': 'commission', 'value': u'Не агентский', 'highlighted': True, 'caption': u'Тип договора', 'type': 'refselect'}),
                    hm.has_entries({'group': 1, 'name': 'firm', 'value': 'Yandex Europe AG', 'highlighted': False, 'caption': u'Фирма', 'type': 'refselect'}),
                    hm.has_entries({'group': 1, 'name': 'currency', 'value': 'EUR', 'highlighted': False, 'caption': u'Валюта расчетов', 'type': 'refselect'}),
                    hm.has_entries({'group': 1, 'name': 'manager_code', 'value': contract.col0.manager_code, 'highlighted': False, 'caption': u'Менеджер', 'type': 'autocomplete'}),
                    hm.has_entries({'group': 1, 'name': 'dt', 'value': contract.col0.dt.strftime(dt_fmt), 'highlighted': False, 'caption': u'Дата начала', 'type': 'date'}),
                    hm.has_entries({'group': 1, 'name': 'finish_dt', 'value': finish_dt.strftime(dt_fmt), 'highlighted': False, 'caption': u'Дата окончания', 'type': 'date'}),
                    hm.has_entries({'group': 1, 'name': 'payment_type', 'value': u'постоплата', 'highlighted': True, 'caption': u'Оплата', 'type': 'refselect'}),
                    hm.has_entries({'group': 1, 'name': 'services', 'value': u'Директ: Рекламные кампании', 'highlighted': False, 'caption': u'Сервисы', 'type': 'checkboxes'}),
                    hm.has_entries({'group': 3, 'name': 'is_signed', 'value': t1.strftime(dt_fmt), 'highlighted': False, 'caption': u'Подписан', 'type': 'datecheckbox'}),
                ),
            }),
        )

    def test_client_limits(self, agency):
        # как пример сложного аттрибута
        person = create_person(client=agency)

        clients = [create_client(agency=agency) for _i in range(3)]
        limits = [666, 777, 999]

        contract = create_general_contract(
            client=agency,
            person=person,
            firm_id=cst.FirmId.YANDEX_OOO,
            credit_limit_single=200,
            client_limits={
                c.id: {'currency': 'RUR', 'client_limit': limit}
                for c, limit in zip(clients, limits)
            },
        )
        res = self.test_client.get(self.BASE_API, {'contract_id': contract.id, 'show_details': True})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'contract': hm.has_entries({
                    'id': contract.id,
                    'external_id': contract.external_id,
                }),
                'contract_attributes': hm.has_items(
                    hm.has_entries({
                        'group': 2,
                        'name': 'credit_limit_single',
                        'highlighted': False,
                        'caption': u'Кредитный лимит, RUB',
                        'type': 'pctinput',
                        'value': 200,
                    }),
                    hm.has_entries({
                        'group': 2,
                        'name': 'client_limits',
                        'highlighted': False,
                        'caption': u'Лимиты по клиентам',
                        'type': 'client_limits_grid',
                        'value': hm.contains_inanyorder(*[
                            hm.has_entries({
                                'attrs': hm.has_entries({
                                    'client_limit_currency': 'RUR',
                                    'currency': 'RUR',
                                    'client_limit': str(limit),
                                    'label': c.name,
                                }),
                                'val': c.id,
                            })
                            for c, limit in zip(clients, limits)
                        ]),
                    }),
                ),
            }),
        )

    @pytest.mark.parametrize(
        'detailed',
        [True, False],
    )
    def test_invalid_currency(self, client, detailed):
        # todo[natabers]: Поисследовать https://st.yandex-team.ru/BALANCE-35671#5fa96d85fb246d59fb2a5394
        # пока что костыль, т.к. неправильно записывается валюта в базу
        contract = create_partner_contract(
            client=client,
            currency=810,  # присваиваем неправильный код валюты
        )
        res = self.test_client.get(self.BASE_API, {'contract_id': contract.id, 'show_details': detailed})

        if not detailed:
            hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'NoResultFound',
                    'description': 'Unknown error happened',
                }),
            )

        else:
            hm.assert_that(res.status_code, hm.equal_to(http.OK))
            hm.assert_that(
                res.get_json()['data'],
                hm.has_entries({
                    'contract': hm.has_entries({
                        'id': contract.id,
                        'external_id': contract.external_id,
                    }),
                }),
            )


@pytest.mark.permissions
@pytest.mark.slow
class TestContractPermissions(BaseTestCaseMixin, TestCaseApiAppBase):
    @pytest.mark.parametrize(
        'contract_firm_id, role_firm_id',
        (
            pytest.param(None, None, id='wo firm - w role/ OK'),
            pytest.param(None, cst.FirmId.YANDEX_OOO, id='wo firm - w role w constraints / OK'),
            pytest.param(cst.FirmId.YANDEX_OOO, None, id='contracts w firm - w role wo constraints / OK'),
            pytest.param(cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE,
                         id='contracts w firms - w role w constraints / FORBIDDEN'),
            pytest.param(cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO,
                         id='contracts w firms - w role w constraints / OK'),
        ),
    )
    def test_firm_constraint(self, admin_role, view_contract_role,
                             contract_firm_id, role_firm_id):
        roles = [admin_role, (view_contract_role, {cst.ConstraintTypes.firm_id: role_firm_id})]
        security.set_roles(roles)

        contract = create_general_contract(firm_id=contract_firm_id)
        res = self.test_client.get(
            self.BASE_API,
            params={'contract_id': contract.id},
        )

        allowed = not role_firm_id or not contract_firm_id or contract_firm_id == role_firm_id
        self.check_response(contract, res, allowed=allowed)

    @pytest.mark.parametrize(
        'same_client_in_role_and_contract, firm_id, allowed',
        (
            pytest.param(True, cst.FirmId.YANDEX_OOO, True, id='w client w firm / OK'),
            pytest.param(True, None, True, id='w client wo firm / OK'),
            pytest.param(False, cst.FirmId.YANDEX_OOO, False, id='wo client w firm / FORBIDDEN'),
            pytest.param(True, cst.FirmId.DRIVE, False, id='w client w wrong firm / OK'),
        ),
    )
    def test_client_constraint(self, client, admin_role, view_contract_role,
                               same_client_in_role_and_contract, firm_id, allowed):
        role_client = create_role_client(client=client if same_client_in_role_and_contract else create_client())

        roles = [
            admin_role,
            (view_contract_role, {
                cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
            }),
        ]
        security.set_roles(roles)

        contract = create_general_contract(client=client, firm_id=firm_id)
        res = self.test_client.get(
            self.BASE_API,
            params={'contract_eid': contract.external_id},
        )
        self.check_response(contract, res, allowed=allowed)

    @pytest.mark.parametrize(
        'owner',
        [True, False],
    )
    @mock_client_resource('yb_snout_api.resources.v1.contract.routes.contract.Contract')
    def test_client_owner(self, client, owner):
        security.set_roles([])
        if owner:
            security.set_passport_client(client)

        contract = create_general_contract(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {'contract_id': contract.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if owner else http.FORBIDDEN))
