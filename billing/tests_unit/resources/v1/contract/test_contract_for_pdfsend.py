# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

standard_library.install_aliases()

import json
import pytest
import hamcrest as hm
import http.client as http
import datetime

from balance import constants as cst
from billing.contract_iface.contract_meta import collateral_types
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
)
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract, create_distribution_contract
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_manager
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.firm import create_firm
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.service import create_service


@pytest.fixture(name='alter_print_template_role')
def create_alter_print_template_role():
    return create_role(
        (
            cst.PermissionCode.ALTER_PRINT_TEMPLATE,
            {cst.ConstraintTypes.firm_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseContractForPdfsend(TestCaseApiAppBase):
    BASE_API = u'/v1/contract/for-pdfsend'

    @staticmethod
    def _create_contract_with_collaterals_with_func(func, col_type, firm_id, dt=None, collateral_count=2):
        # all collaterals must have dt
        if dt is None:
            dt = datetime.datetime.now()
        contract = func(firm_id=firm_id, w_ui=False, dt=dt)
        contract.col0.print_template = ob.generate_character_string(10)
        collaterals = []
        for _ in range(collateral_count):
            col = contract.append_collateral(
                dt=dt,
                collateral_type=col_type,
            )
            col.print_template = ob.generate_character_string(10)
            collaterals.append(col)
        return contract, collaterals

    def _create_general_contract_with_collaterals(self, *args, **kwargs):
        col_type = collateral_types['GENERAL'][1021]  # универсальный договор
        return self._create_contract_with_collaterals_with_func(create_general_contract, col_type, *args, **kwargs)

    def _create_distribution_contract_with_collaterals(self, *args, **kwargs):
        col_type = collateral_types['DISTRIBUTION'][3010]  # изменение налогообложения
        return self._create_contract_with_collaterals_with_func(create_distribution_contract, col_type, *args, **kwargs)

    @staticmethod
    def _check_result_for_having_objects(data, contracts, collaterials):
        contract_matchers = [
            hm.has_entries(
                object_info=hm.has_entries(
                    id=contract.id,
                    type='contract',
                ),
            )
            for contract in contracts
        ]
        contract_count = len(contracts)

        collateral_matchers = [
            hm.has_entries(
                object_info=hm.has_entries(
                    id=col.id,
                    type='collateral',
                ),
            )
            for col in collaterials
        ]
        collateral_count = len(collaterials)

        hm.assert_that(data, hm.has_entries(
            total_count=contract_count + collateral_count,
            items=hm.contains_inanyorder(*(contract_matchers + collateral_matchers)),
        ))

    def _check_simple_attr(
        self,
        firm,
        param_name,
        param_value,
        match_first,
        match_second,
        first_attr_name=None,
        first_attr_value=None,
        second_attr_name=None,
        second_attr_value=None,
    ):
        contract1, cols1 = self._create_general_contract_with_collaterals(firm.id)
        contract2, cols2 = self._create_general_contract_with_collaterals(firm.id)

        if first_attr_name:
            for col in [contract1.col0] + cols1:
                setattr(col, first_attr_name, first_attr_value)
        if second_attr_name:
            for col in [contract2.col0] + cols2:
                setattr(col, second_attr_name, second_attr_value)
        self.test_session.flush()
        params = {'firm_id': firm.id}
        if param_name:
            params[param_name] = param_value
        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})

        contracts = []
        cols = []
        if match_first:
            contracts.append(contract1)
            cols.extend(cols1)
        if match_second:
            contracts.append(contract2)
            cols.extend(cols2)
        self._check_result_for_having_objects(data, contracts, cols)

    @pytest.mark.parametrize(
        'ps',
        [1, 2, 10, 100],
    )
    def test_pagination(self, ps, firm, manager):
        contracts = []
        cols = []
        for _ in range(3):
            contract, cols_f = self._create_general_contract_with_collaterals(firm.id)
            for col in [contract.col0] + cols_f:
                col.manager_code = manager.manager_code
            contracts.append(contract)
            cols.extend(cols_f)
        self.test_session.flush()

        objects_count = len(contracts) + len(cols)
        got_objects = []
        # Getting all objects page to page
        pn = 0
        while pn * ps <= objects_count:
            pn += 1
            res = self.test_client.get(
                self.BASE_API,
                {
                    'manager_code': manager.manager_code,
                    'pagination_pn': pn,
                    'pagination_ps': ps,
                },
            )
            hm.assert_that(res.status_code, hm.equal_to(http.OK))
            data = res.get_json().get('data', {})
            hm.assert_that(
                data,
                hm.has_entries(
                    total_count=objects_count,
                    items=hm.has_length(hm.less_than_or_equal_to(ps)),
                ),
            )
            got_objects.extend([row['object_info'] for row in data['items']])
        hm.assert_that(got_objects, hm.has_length(hm.equal_to(objects_count)))
        objects_matchers = [hm.has_entries(id=ctr.id, type='contract') for ctr in contracts]
        objects_matchers += [hm.has_entries(id=col.id, type='collateral') for col in cols]
        hm.assert_that(got_objects, hm.contains_inanyorder(*objects_matchers))

    @pytest.mark.parametrize('param', [None, False, True])
    def test_is_email_enqueued(self, param, firm):
        self._check_simple_attr(
            first_attr_name='print_tpl_email_log',
            first_attr_value=[(datetime.datetime.now().isoformat(), 1337)],  # Fake log
            param_name=(param is not None) and 'is_email_enqueued',
            param_value=param,
            match_first=param is not False,
            match_second=param is not True,
            firm=firm,
        )

    @pytest.mark.parametrize('param', [False, True])
    def test_contract_type(self, param, firm):
        contract1, cols1 = self._create_general_contract_with_collaterals(firm.id)
        contract2, cols2 = self._create_distribution_contract_with_collaterals(firm.id)
        self.test_session.flush()
        params = {'firm_id': firm.id}
        if param:
            params['contract_type'] = contract1.type
        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})
        if param:
            self._check_result_for_having_objects(data, [contract1], cols1)
        else:
            self._check_result_for_having_objects(data, [contract1, contract2], cols1 + cols2)

    @pytest.mark.parametrize('param', [False, True])
    def test_contract_eid(self, param, firm):
        contract1, cols1 = self._create_general_contract_with_collaterals(firm.id)
        contract2, cols2 = self._create_general_contract_with_collaterals(firm.id)
        contract3, cols3 = self._create_general_contract_with_collaterals(firm.id)
        contract1.external_id = '421'
        contract2.external_id = '999'
        contract2.external_id = '888'
        self.test_session.flush()
        params = [('firm_id', firm.id)]
        if param:
            params += [('contract_eid', contract1.external_id), ('contract_eid', contract2.external_id)]
        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})
        if param:
            self._check_result_for_having_objects(data, [contract1, contract2], cols1 + cols2)
        else:
            self._check_result_for_having_objects(data, [contract1, contract2, contract3], cols1 + cols2 + cols3)

    @pytest.mark.parametrize('object_type', ['collateral', 'contract', None])
    def test_object_type(self, object_type, firm):
        contract1, cols1 = self._create_general_contract_with_collaterals(firm.id)
        contract2, cols2 = self._create_general_contract_with_collaterals(firm.id)
        self.test_session.flush()
        params = {'firm_id': firm.id}
        if object_type:
            params['object_type'] = object_type
        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})

        if object_type == 'contract':
            self._check_result_for_having_objects(data, [contract1, contract2], [])
        elif object_type == 'collateral':
            self._check_result_for_having_objects(data, [], cols1 + cols2)
        else:
            self._check_result_for_having_objects(data, [contract1, contract2], cols1 + cols2)

    @pytest.mark.parametrize('manager_param', [False, True])
    @pytest.mark.parametrize('manager_field_name', ['manager_code', 'manager_bo_code'])
    def test_manager_code(self, manager_param, manager_field_name, firm, manager):
        self._check_simple_attr(
            first_attr_name=manager_field_name,
            first_attr_value=manager.manager_code,
            param_name=manager_param and manager_field_name,
            param_value=manager.manager_code,
            match_first=True,
            match_second=not manager_param,
            firm=firm,
        )

    @pytest.mark.parametrize('service_id_param', [False, True])
    def test_service_id(self, service_id_param, firm, service):
        self._check_simple_attr(
            first_attr_name='services',
            first_attr_value={service.id: 1},
            param_name=service_id_param and 'service_id',
            param_value=service.id,
            match_first=True,
            match_second=not service_id_param,
            firm=firm,
        )

    @pytest.mark.parametrize('firm_id_param', [False, True])
    def test_firm_id(self, firm_id_param, manager, firm):
        firm2 = create_firm()
        contract1, cols1 = self._create_general_contract_with_collaterals(firm.id)
        contract2, cols2 = self._create_general_contract_with_collaterals(firm2.id)

        for col in [contract1.col0, contract2.col0] + cols1 + cols2:
            col.manager_code = manager.manager_code
        self.test_session.flush()
        params = {'manager_code': manager.manager_code}
        if firm_id_param:
            params['firm_id'] = firm.id
        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})

        if firm_id_param:
            self._check_result_for_having_objects(data, [contract1], cols1)
        else:
            self._check_result_for_having_objects(data, [contract1, contract2], cols1 + cols2)

    @pytest.mark.parametrize('param', [None, False, True])
    def test_is_faxed(self, param, firm):
        self._check_simple_attr(
            first_attr_name='is_faxed',
            first_attr_value=datetime.datetime.now(),
            second_attr_name='is_faxed',
            second_attr_value=None,
            param_name=(param is not None) and 'is_faxed',
            param_value=param,
            match_first=param is not False,
            match_second=param is not True,
            firm=firm,
        )

    @pytest.mark.parametrize('param', [None, False, True])
    def test_is_signed(self, param, firm):
        self._check_simple_attr(
            first_attr_name='is_signed',
            first_attr_value=datetime.datetime.now(),
            second_attr_name='is_signed',
            second_attr_value=None,
            param_name=(param is not None) and 'is_signed',
            param_value=param,
            match_first=param is not False,
            match_second=param is not True,
            firm=firm,
        )

    @pytest.mark.parametrize('param', [None, False, True])
    def test_is_sent_original(self, param, firm):
        self._check_simple_attr(
            first_attr_name='sent_dt',
            first_attr_value=datetime.datetime.now(),
            second_attr_name='sent_dt',
            second_attr_value=None,
            param_name=(param is not None) and 'is_sent_original',
            param_value=param,
            match_first=param is not False,
            match_second=param is not True,
            firm=firm,
        )

    @pytest.mark.parametrize('param', [None, False, True])
    def test_is_atypical_conditions(self, param, firm):
        self._check_simple_attr(
            first_attr_name='atypical_conditions',
            first_attr_value=1,
            second_attr_name='atypical_conditions',
            second_attr_value=0,
            param_name=(param is not None) and 'is_atypical_conditions',
            param_value=param,
            match_first=param is not False,
            match_second=param is not True,
            firm=firm,
        )

    @pytest.mark.parametrize('param', [None, False, True])
    def test_is_booked(self, param, firm):
        self._check_simple_attr(
            first_attr_name='is_booked',
            first_attr_value=1,
            second_attr_name='is_booked',
            second_attr_value=0,
            param_name=(param is not None) and 'is_booked',
            param_value=param,
            match_first=param is not False,
            match_second=param is not True,
            firm=firm,
        )

    @pytest.mark.parametrize('param', [False, True])
    def test_payment_type(self, param, firm):
        self._check_simple_attr(
            first_attr_name='payment_type',
            first_attr_value=1,
            param_name=param and 'payment_type',
            param_value=1,
            match_first=True,
            match_second=not param,
            firm=firm,
        )

    @pytest.mark.parametrize('bound_from', [False, True])
    @pytest.mark.parametrize('bound_to', [False, True])
    def test_date_boundary(self, firm, bound_from, bound_to):
        anchor_datetime = datetime.datetime.now()
        contract_before, cols_before = self._create_general_contract_with_collaterals(
            firm.id,
            dt=anchor_datetime - datetime.timedelta(days=2),
        )
        first_datetime = anchor_datetime - datetime.timedelta(days=1)
        contract_middle, cols_middle = self._create_general_contract_with_collaterals(firm.id, dt=anchor_datetime)
        second_datetime = anchor_datetime
        contract_after, cols_after = self._create_general_contract_with_collaterals(
            firm.id,
            dt=anchor_datetime + datetime.timedelta(days=1),
        )
        self.test_session.flush()

        contracts = []
        cols = []
        params = {'firm_id': firm.id}
        if bound_from:
            params['dt_from'] = first_datetime.isoformat()
        else:
            contracts.append(contract_before)
            cols.extend(cols_before)
        contracts.append(contract_middle)
        cols.extend(cols_middle)
        if bound_to:
            params['dt_to'] = second_datetime.isoformat()
        else:
            contracts.append(contract_after)
            cols.extend(cols_after)

        res = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})

        self._check_result_for_having_objects(data, contracts, cols)

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_constraint',
        [None, True, False],
    )
    def test_permission(self, match_constraint, admin_role, alter_print_template_role, firm):
        roles = [admin_role]
        if match_constraint is not None:
            firm_id = firm.id if match_constraint else create_firm().id
            roles.append(
                (alter_print_template_role, {cst.ConstraintTypes.firm_id: firm_id}),
            )
        security.set_roles(roles)
        self._create_general_contract_with_collaterals(firm.id)
        self.test_session.flush()
        res = self.test_client.get(
            self.BASE_API,
            {'firm_id': firm.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})
        if match_constraint:
            hm.assert_that(
                data,
                hm.has_entries(
                    items=hm.has_length(hm.greater_than(0)),
                    total_count=hm.greater_than(0),
                ),
            )
        else:
            hm.assert_that(
                data,
                hm.has_entries(
                    items=hm.has_length(hm.equal_to(0)),
                    total_count=hm.equal_to(0),
                ),
            )
