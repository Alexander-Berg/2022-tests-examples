# -*- coding: utf-8 -*-
import datetime
import decimal
import itertools

import pytest
import hamcrest as hm
import mock

from balance import mapper
# from balance import exc
from balance import core
from balance.actions import acts as a_a
from balance.utils.xml2json import xml2json_auto
from balance.corba_buffers import StateBuffer  # , RequestBuffer
from balance import xmlizer as xr
from balance import constants as cst

from muzzle.api import act as api_act

from tests import object_builder as ob
from tests.matchers import string_as_number_equals_to
# from balance import multilang_support

from muzzle.api.act import (DBService, parse_filter_params, fetch_tariffied_acts_from_yt,
                            get_acts_xls_data, ReportService)
import datetime as dt
from sqlalchemy import select, text, column
import balance.scheme as scheme
from balance.constants import ConstraintTypes
from butils import xls_export

D = decimal.Decimal


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def credit_contract(session):
    client = ob.ClientBuilder(is_agency=1).build(session).obj
    params = dict(
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=datetime.datetime.now()
    )

    contract = ob.ContractBuilder(
        client=client,
        person=ob.PersonBuilder(client=client, type='ur'),
        **params
    ).build(session).obj
    return contract


def create_invoice(session, qty, client=None, product_id=None, firm_id=cst.FirmId.YANDEX_OOO):
    client = client or ob.ClientBuilder()
    order = ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, product_id) if product_id else ob.ProductBuilder()
    ).build(session).obj
    return ob.InvoiceBuilder(
        person=ob.PersonBuilder(client=client, person_type='ur').build(session),
        request=ob.RequestBuilder(
            firm_id=firm_id,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
            )
        )
    ).build(session).obj


def create_act(session, client=None, firm_id=cst.FirmId.YANDEX_OOO):
    qty = D('50')
    invoice = create_invoice(session, qty=qty, client=client, firm_id=firm_id)
    invoice.turn_on_rows()
    invoice.invoice_orders[0].order.calculate_consumption(
        dt=datetime.datetime.today() - datetime.timedelta(days=1),
        stop=0,
        shipment_info={'Bucks': qty}
    )
    act, = invoice.generate_act(force=True)
    return act


@pytest.fixture
def act(session):
    return create_act(session)


@pytest.fixture()
def acts(session, client):
    return [create_act(session, client) for _ in xrange(3)]


@pytest.fixture
def db_service(session):
    return DBService(session)


class TestGetActs(object):
    def test_get_acts(self, session, muzzle_logic, client, acts):
        state_obj = StateBuffer(
            params={'req_client_id': str(client.id)}
        )
        acts_xml = muzzle_logic.get_acts(
            session=session,
            is_admin=True,
            state_obj=state_obj,
            request_obj=None,
        )
        acts_json = xml2json_auto(acts_xml)

        required_act_ids = sorted([str(act.id) for act in acts])
        got_act_ids = sorted([act['act_id'] for act in acts_json['entry']])
        assert got_act_ids == required_act_ids


@pytest.mark.permissions
@mock.patch('butils.passport.passport_admsubscribe')
class TestGetActsPermissions(object):
    role_map = {
        'client': lambda s: ob.Getter(mapper.Role, cst.RoleName.CLIENT).build(s).obj,
        'view_invoices': lambda s: ob.create_role(
            s,
            (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}),
        ),
    }

    @pytest.mark.parametrize(
        'inv_firm_ids, role_firm_ids',
        (
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             set()),
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             (cst.FirmId.YANDEX_OOO,)),
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             (cst.FirmId.YANDEX_OOO,)),
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET, cst.FirmId.AUTORU),
             (cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET)),
        ),
    )
    def test_filter_act_by_constraints(self, _mock_pas_adm, session, muzzle_logic,
                                       client, inv_firm_ids, role_firm_ids):
        """Фильтр актов по фирме указанной в роли"""
        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        acts = []
        required_acts = []

        if role_firm_ids:
            ob.set_roles(session, session.passport, [
                (role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ])
        else:
            ob.set_roles(session, session.passport, role)
        for firm_id in inv_firm_ids:
            act = create_act(session, client=client, firm_id=firm_id)
            acts.append(act)
            if firm_id in role_firm_ids:
                required_acts.append(act)

        if not role_firm_ids:
            required_acts = acts  # нет ограничения по фирме

        state_obj = StateBuffer(
            params={'req_client_id': str(client.id)}
        )
        response = muzzle_logic.get_acts(session, is_admin=True, state_obj=state_obj, request_obj={})
        response_json = xml2json_auto(response, 'entries/entry')
        assert response_json['total_row_count'] == str(len(required_acts))
        hm.assert_that(
            response_json['entry'],
            hm.contains_inanyorder(*[
                hm.has_entries({'act_id': str(act.id), 'firm_id': str(act.invoice.firm_id)})
                for act in required_acts
            ]),
        )

    @pytest.mark.parametrize(
        'link_client, roles, act_count',
        [
            (False, ('client',), 0),  # без привязанного клиента - ничего
            (True, ('client',), 1),  # только свой счёт
            (True, ('client', 'view_invoices'), 2),  # под админом получаем все счета
            (False, ('client', 'view_invoices'), 2),  # под админом получаем все счета

        ],
    )
    def test_from_client_ui(self, _mock_pas_adm, session, muzzle_logic, roles, act_count, link_client):
        """Доступны только счета, которе принадлежат клиенту"""
        firm_id = cst.FirmId.YANDEX_OOO
        min_dt = session.now() - datetime.timedelta(days=1)

        client1 = create_client(session)
        client2 = create_client(session)

        if link_client:
            session.passport.link_to_client(client1)
        ob.set_roles(session, session.passport, [self.role_map[role_name](session) for role_name in roles])

        act1 = create_act(session, client1, firm_id)
        act2 = create_act(session, client2, firm_id)

        session.flush()
        max_dt = session.now()

        state_obj = StateBuffer(
            params={
                'req_act_dt_from': min_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'req_act_dt_to': max_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'req_firm_id': str(firm_id),
            }
        )
        response = muzzle_logic.get_acts(session, is_admin=False, state_obj=state_obj, request_obj={})
        response_json = xml2json_auto(response, 'entries/entry')
        assert response_json['total_row_count'] == str(act_count)
        hm.assert_that(
            response_json.get('entry', []),
            hm.contains_inanyorder(*[
                hm.has_entries({'act_id': str(a.id), 'client_id': str(c.id)})
                for a, c in [(act1, client1), (act2, client2)][:act_count]
            ]),
        )


@pytest.mark.parametrize(
    'legacy_act_tax',
    [True, False],
    ids=['legacy_act_tax', 'modern_act_tax']
)
class TestGetActsByInvoice(object):

    @staticmethod
    def _get_res(session, invoice):
        api_response = api_act.get_by_invoice_id(session, invoice.id, 10, 1, True, True, sort_key='act_id')
        xmlizer = xr.InvoiceXmlizer(api_response['invoice'])
        acts = [
            xml2json_auto(xmlizer.xmlize_act_to_node(act))
            for act in api_response['acts']
        ]
        return api_response, acts

    def test_prepay(self, session, client, legacy_act_tax):
        invoice = create_invoice(session, qty=666, client=client, product_id=cst.DIRECT_PRODUCT_ID)
        invoice.turn_on_rows()
        order, = (io.order for io in invoice.invoice_orders)

        for qty in [10, 30, 60]:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
            act, = invoice.generate_act(force=1, backdate=datetime.datetime.now())
            if legacy_act_tax:
                act.tax_policy_pct = None
            session.flush()

        api_response, acts = self._get_res(session, invoice)

        hm.assert_that(
            acts,
            hm.contains(*[
                hm.has_entries({
                    'id': str(act.id),
                    'invoice-id': str(invoice.id),
                    'amount': str(act.amount.as_decimal()),
                    'amount-nds': str(act.amount_nds.as_decimal()),
                    'paid-amount': string_as_number_equals_to(0),
                    'nds-pct': str(act.tax_policy_pct.nds_pct.as_decimal()),
                })
                for act in sorted(invoice.acts, key=lambda a: a.id)
            ])
        )
        hm.assert_that(
            api_response,
            hm.has_entries(
                acts_totals=hm.has_entries(
                    amount=60 * 30,
                    amount_nds=invoice.tax_policy_pct.nds_from(60 * 30),
                )
            )
        )

    @pytest.mark.parametrize(
        'is_sa_cached',
        [False, True],
        ids=['from_db', 'cached']
    )
    def test_fpa(self, session, credit_contract, legacy_act_tax, is_sa_cached):
        order = ob.OrderBuilder(
            agency=credit_contract.client,
            client=ob.ClientBuilder(agency=credit_contract.client),
            product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_ID)
        ).build(session).obj

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=credit_contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=6666)]
            )
        ).build(session).obj

        pa, = core.Core(session).pay_on_credit(request.id, 1003, credit_contract.person_id, credit_contract.id)
        for qty in [10, 30, 60]:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
            act, = a_a.ActAccounter(
                credit_contract.client, mapper.ActMonth(for_month=datetime.datetime.now()),
                dps=[], invoices=[pa.id], force=1
            ).do()
            if legacy_act_tax:
                act.tax_policy_pct = None
            session.flush()

        if not is_sa_cached:
            session.expire_all()

        api_response, acts = self._get_res(session, pa)

        hm.assert_that(
            acts,
            hm.contains(*[
                hm.has_entries({
                    'id': str(act.id),
                    'invoice-id': str(act.invoice_id),
                    'amount': str(act.amount.as_decimal()),
                    'amount-nds': str(act.amount_nds.as_decimal()),
                    'paid-amount': string_as_number_equals_to(0),
                    'nds-pct': str(act.tax_policy_pct.nds_pct.as_decimal()),
                })
                for act in sorted(itertools.chain.from_iterable(r.acts for r in pa.repayments), key=lambda a: a.id)
            ])
        )
        hm.assert_that(
            api_response,
            hm.has_entries(
                acts_totals=hm.has_entries(
                    amount=60 * 30,
                    amount_nds=pa.tax_policy_pct.nds_from(60 * 30),
                )
            )
        )


class TestParseFilterParams(object):
    def test_empty_fields(self):
        fields = []
        req = {}

        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

    def test_like_field(self):
        fields = [{'type': 'like', 'name': 'test'}]
        req = {}

        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

        req = {'test': '12'}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': 'like', 'name': 'test', 'value': '%12%'}

        fields[0]['type'] = 'llike'
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': 'llike', 'name': 'test', 'value': '%12'}

        fields[0]['type'] = 'rlike'
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': 'rlike', 'name': 'test', 'value': '12%'}

        req['test_like'] = '0'
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': 'rlike', 'name': 'test', 'value': '12%'}

        req['test_like'] = ''
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': 'rlike', 'name': 'test', 'value': '12%'}

        req['test_like'] = '1'
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': 'rlike', 'name': 'test', 'value': '%12%'}

    @pytest.mark.parametrize('type_name', ['range', 'range_rhs_shift'])
    def test_range_field(self, type_name):
        fields = [{'type': type_name, 'name': 'test', 'format': 'string'}]
        req = {}

        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

        req = {'test': 'ex_val'}
        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

        req = {'test_from': 'ex_val'}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'value': ('ex_val', None), 'format': 'string'}

        req['test_to'] = 'ex_val1'
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'value': ('ex_val', 'ex_val1'), 'format': 'string'}

        fields[0]['format'] = 'datetime'
        req['test_from'] = '2021-05-26'
        req['test_to'] = '2022-05-26'
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'value': (
            dt.datetime(2021, 5, 26), dt.datetime(2022, 5, 26)), 'format': 'datetime'}

    @pytest.mark.parametrize('type_name', ['point', 'not_eq'])
    def test_point_field(self, type_name):
        # string format
        fields = [{'type': type_name, 'name': 'test', 'format': 'string'}]
        req = {}

        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

        req = {'test': 'one'}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'format': 'string', 'value': 'one'}

        req = {'test': ''}
        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

        req = {'test': 0}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'format': 'string', 'value': '0'}

        # integer format
        fields[0]['format'] = 'integer'
        req = {'test': '0'}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'format': 'integer', 'value': 0}

        req = {'test': ''}
        res = parse_filter_params(fields, req)
        assert type(res) is list and len(res) == 0

        # decimal format
        fields[0]['format'] = 'decimal'
        req = {'test': '0'}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'format': 'decimal', 'value': decimal.Decimal(0)}

        # datetime format
        fields[0]['format'] = 'datetime'
        req = {'test': '2021-05-26'}
        res = parse_filter_params(fields, req)
        assert res[0] == {'type': type_name, 'name': 'test', 'format': 'datetime', 'value': dt.datetime(2021, 5, 26)}


class TestDBService(object):
    def test_get_client_acts(self, acts, client, db_service):
        rows = db_service.get_acts(
            columns=['external_id'],
            filters=[{'type': 'point', 'name': 'client_id', 'value': client.id}]
        )

        assert len(rows) == len(acts)

        act_ids = list(act.external_id for act in acts)
        for row in rows:
            assert row.external_id in act_ids

    def test_acts_not_equal_filter(self, acts, client, db_service):
        rows = db_service.get_acts(
            columns=['external_id'],
            filters=[
                {'type': 'point', 'name': 'client_id', 'value': client.id},
                {'type': 'not_eq', 'name': 'act_id', 'value': acts[0].id}
            ]
        )

        assert len(rows) + 1 == len(acts)
        assert acts[0].external_id not in list(row.external_id for row in rows)

    def test_acts_like_filter(self, acts, client, db_service):
        rows = db_service.get_acts(
            columns=['external_id'],
            filters=[
                {'type': 'point', 'name': 'client_id', 'value': client.id},
                {'type': 'rlike', 'name': 'act_id', 'value': '%' + str(acts[0].id % 100)}
            ]
        )

        assert len(rows) == 1
        assert rows[0].external_id == acts[0].external_id

    def test_acts_range_filter(self, acts, client, db_service):
        rows = db_service.get_acts(
            columns=['external_id'],
            filters=[
                {'type': 'point', 'name': 'client_id', 'value': client.id},
                {'type': 'range', 'name': 'act_id', 'value': (acts[0].id, acts[0].id)}
            ]
        )

        assert len(rows) == 1
        assert rows[0].external_id == acts[0].external_id

    def test_acts_exists_filter(self, acts, client, db_service):
        sel = select(
            columns=[text('0')],
            from_obj=scheme.invoices,
            whereclause=column('invoice_id') == scheme.invoices.c.id
        ).where(scheme.invoices.c.id == acts[0].invoice_id)

        rows = db_service.get_acts(
            columns=['external_id', 'invoice_id'],
            filters=[
                {'type': 'point', 'name': 'client_id', 'value': client.id},
                {'type': 'exists', 'value': sel}
            ]
        )

        assert len(rows) == 1
        assert rows[0].external_id == acts[0].external_id
        assert rows[0].invoice_id == acts[0].invoice_id

    def test_acts_text_filter(self, acts, db_service):
        rows = db_service.get_acts(
            columns=['external_id'],
            filters=[{
                'type': 'text',
                'value': column('act_id') == acts[0].id
            }]
        )

        assert len(rows) == 1
        assert rows[0].external_id == acts[0].external_id

    def test_filter_access_to_invoices_by_client(self, session, db_service):
        role = session.query(mapper.Role).get(0)
        perm, = session.query(mapper.Permission).filter(
            mapper.Permission.code == cst.PermissionCode.VIEW_INVOICES).all()

        role_perm = mapper.RolePermission(
            role=role,
            permission=perm,
            constraints={ConstraintTypes.firm_id: [666, 10]}
        )
        session.add(role_perm)
        session.flush()

        res = db_service.filter_access_to_invoices_by_client(None)

        assert type(res) is list and len(res) == 1

        statement = res[0].compile()
        assert ConstraintTypes.firm_id in str(statement) and set(statement.params.values()) == set([666, 10])


@mock.patch('balance.utils.yt_helpers.get_avialable_client')
class TestYTTariffiedActs(object):
    class YTClient(object):
        def __init__(self, path_data={}):
            self.path = path_data

        def exists(self, path):
            return path in self.path.keys()

        def get(self, path):
            return self.path[path]

        def read_table(self, table_path):
            root, file = str(table_path).split('/')[-2:]
            return self.path['//' + root][file]

    class Act(object):
        external_id = None
        service_id = None
        service_order_id = None
        client_id = None
        client_name = None
        agency_name = None
        product_id = None
        at_amount = None
        # fictive_invoice_eid = None
        # endbuyer_name = None
        # endbuyer_inn = None
        consume_id = None

        def __init__(self, **kwargs):
            for k, v in kwargs.items():
                setattr(self, k, v)

        def yt_data(self):
            return {
                'act_external_id': self.external_id,
                'service_id': self.service_id,
                'service_order_id': self.service_order_id,
                'client_id': self.client_id,
                'client_name': self.client_name,
                'agency_name': self.agency_name,
                'product_id': self.product_id,
                'acted_sum': self.at_amount,
                # 'fictive_invoice_eid': self.fictive_invoice_eid,
                # 'endbuyer_name': self.endbuyer_name,
                # 'endbuyer_inn': self.endbuyer_inn,
                'consume_id': self.consume_id
            }

        def __getitem__(self, name):
            return getattr(self, name)

    def test_empty_acts(self, _mock_yt_helper):
        acts_data, not_found_acts = fetch_tariffied_acts_from_yt([], '', ['external_id'])

        assert type(acts_data) is list and len(acts_data) == 0
        assert type(not_found_acts) is list and len(not_found_acts) == 0

        _mock_yt_helper.assert_not_called()

    def test_path_not_exists(self, _mock_yt_helper):
        client = self.YTClient({})
        _mock_yt_helper.return_value = client

        act = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11))
        acts_data, not_found_acts = fetch_tariffied_acts_from_yt([act], '//test_path_not_exists', ['external_id'])

        assert type(acts_data) is list and len(acts_data) == 0
        assert len(not_found_acts) == 1 and not_found_acts[0] == act

    def test_table_not_exists(self, _mock_yt_helper):
        _mock_yt_helper.return_value = self.YTClient({'//test_path': {}})

        act = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11))
        acts_data, not_found_acts = fetch_tariffied_acts_from_yt([act], '//test_path', ['external_id'])

        assert type(acts_data) is list and len(acts_data) == 0
        assert len(not_found_acts) == 1 and not_found_acts[0] == act

    def test_found_act(self, _mock_yt_helper):
        act = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11))

        yt_data = {'//test_path': {'2021-04': [act.yt_data()]}}
        _mock_yt_helper.return_value = self.YTClient(yt_data)

        acts_data, not_found_acts = fetch_tariffied_acts_from_yt([act], '//test_path', ['external_id'])

        assert len(acts_data) == 1 and acts_data[0][0] == act.external_id
        assert type(not_found_acts) is list and len(not_found_acts) == 0

    def test_not_found_act(self, _mock_yt_helper):
        act = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11))

        yt_data = {'//test_path': {'2021-04': []}}
        _mock_yt_helper.return_value = self.YTClient(yt_data)

        acts_data, not_found_acts = fetch_tariffied_acts_from_yt([act], '//test_path', ['external_id'])

        assert len(acts_data) == 0
        assert len(not_found_acts) == 1 and not_found_acts[0] is act

    def test_return_one_act_with_two_row(self, _mock_yt_helper):
        act = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11), at_amount=12)
        act1 = self.Act(id=13, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11), at_amount=20)

        yt_data = {'//test_path': {'2021-04': [act.yt_data(), act1.yt_data()]}}
        _mock_yt_helper.return_value = self.YTClient(yt_data)

        acts_data, not_found_acts = fetch_tariffied_acts_from_yt([act], '//test_path', ['external_id', 'at_amount'])

        assert len(acts_data) == 2
        assert type(not_found_acts) is list and len(not_found_acts) == 0

        first = acts_data[0]
        assert len(first) == 2 and first[0] == act.external_id and first[1] == act.at_amount

        second = acts_data[1]
        assert len(second) == 2 and second[0] == act1.external_id and second[1] == act1.at_amount

    def test_several_acts_not_found(self, _mock_yt_helper):
        act1 = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11), at_amount=12)
        act2 = self.Act(id=13, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11), at_amount=20)

        yt_data = {'//test_path': {'2021-04': []}}
        _mock_yt_helper.return_value = self.YTClient(yt_data)

        acts_data, not_found_acts = fetch_tariffied_acts_from_yt(
            acts=[act1, act2],
            path='//test_path',
            col_names=['external_id', 'at_amount']
        )

        assert len(acts_data) == 0
        assert type(not_found_acts) is list and len(not_found_acts) == 2

        assert not_found_acts[0] == act1
        assert not_found_acts[1] == act2

    def test_find_act_with_diff_date(self, _mock_yt_helper):
        act1 = self.Act(id=12, external_id='YB-9998', act_dt=dt.datetime(2021, 4, 11), at_amount=12)
        act2 = self.Act(id=13, external_id='YB-9999', act_dt=dt.datetime(2021, 5, 11), at_amount=20)
        act3 = self.Act(id=14, external_id='YB-6666', act_dt=dt.datetime(2021, 5, 12), at_amount=10)

        yt_data = {'//test_path': {
            '2021-04': [act1.yt_data()],
            '2021-05': [act2.yt_data()]
        }}
        _mock_yt_helper.return_value = self.YTClient(yt_data)

        acts_data, not_found_acts = fetch_tariffied_acts_from_yt(
            acts=[act1, act2, act3],
            path='//test_path',
            col_names=['external_id', 'at_amount']
        )

        assert len(acts_data) == 2

        for act_data in acts_data:
            if act_data[0] == act1.external_id:
                assert act_data[1] == act1.at_amount

            else:
                assert act_data[0] == act2.external_id
                assert act_data[1] == act2.at_amount

        assert len(not_found_acts) == 1
        assert not_found_acts[0] == act3


class TestGetActsXLS(object):
    class PM(object):
        lang = 'ru'

        def get(self, name):
            return name

        def get_many(self, names):
            return names

    def test_create_excel_document(self, session, db_service, client, acts):
        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            args = call_args.args[1]

            assert len(args) == len(acts)

            report = ReportService({}, xml_name='_act_orders_xreport')
            fields = map(lambda f: f.name, report.get_fields())

            id_i = fields.index('external_id')
            factura_i = fields.index('factura')
            dt_i = fields.index('act_dt')
            amount_i = fields.index('amount')
            amount_nds_i = fields.index('amount_nds')
            client_name_i = fields.index('client_name')

            for row in args:
                assert len(row) == len(fields)

                ext_id = row[id_i]

                act = next((act for act in acts if act.external_id == ext_id), None)
                assert act is not None

                assert factura_i is None or act.factura == row[factura_i]
                assert amount_i is None or act.amount == row[amount_i]
                assert amount_nds_i is None or act.amount_nds == row[amount_nds_i]
                assert dt_i is None or act.dt == row[dt_i]
                assert client_name_i is None or client.name in row[client_name_i]

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    @pytest.mark.parametrize('is_found', [True, False])
    def test_create_doc_by_service_id(self, session, db_service, client, acts, is_found):
        sel = select(columns=[scheme.orders.c.service_id],
                     whereclause=scheme.acttranses.c.act_id == acts[0].id)
        sources = scheme.acttranses.join(
            scheme.consumes, scheme.consumes.c.id == scheme.acttranses.c.consume_id)
        sources = sources.join(scheme.orders, scheme.consumes.c.parent_order_id == scheme.orders.c.id)

        res = session.execute(sel.select_from(sources))
        service_id = list(res)[0][0]

        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id, 'service_id': service_id if is_found else service_id + 1},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == (3 if is_found else 0)

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    @pytest.mark.parametrize('is_found', [True, False])
    def test_create_doc_by_service_group_id(self, session, db_service, client, acts, is_found):
        sel = select(columns=[scheme.services.c.service_group_id],
                     whereclause=scheme.acttranses.c.act_id == acts[0].id)
        sources = scheme.acttranses.join(
            scheme.consumes, scheme.consumes.c.id == scheme.acttranses.c.consume_id)
        sources = sources.join(scheme.orders, scheme.consumes.c.parent_order_id == scheme.orders.c.id)
        sources = sources.join(scheme.services, scheme.orders.c.service_id == scheme.services.c.id)

        res = session.execute(sel.select_from(sources))
        service_group_id = list(res)[0][0]

        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={
                    'client_id': client.id,
                    'service_group_id': service_group_id if is_found else service_group_id + 1
                },
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == (3 if is_found else 0)

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    def test_create_doc_by_subclient_login(self, db_service, client, acts):
        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id, 'subclient_login': 0},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == 3

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    @pytest.mark.parametrize('is_found', [True, False])
    def test_create_doc_by_contract_id(self, db_service, client, acts, is_found):
        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id, 'contract_id': 0 if is_found else 10},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == (3 if is_found else 0)

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    @pytest.mark.parametrize('is_found', [True, False])
    def test_create_doc_by_payment_type(self, db_service, client, acts, is_found):
        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id, 'payment_type': 3 if is_found else 1},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == (3 if is_found else 0)

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    @pytest.mark.parametrize('is_found', [True, False])
    def test_create_doc_by_service_cc(self, db_service, client, acts, is_found):
        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id, 'service_cc': 'PPC' if is_found else 'NotFound'},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == (3 if is_found else 0)

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0

    @pytest.mark.parametrize('is_found', [True, False])
    def test_create_doc_by_manager_code(self, session, db_service, client, acts, is_found):
        sel = select(columns=[scheme.acttranses.c.manager_code], from_obj=scheme.acttranses,
                     whereclause=scheme.acttranses.c.act_id == acts[0].id)
        res = session.execute(sel)
        res = [row for row in res]

        with mock.patch('butils.xls_export.get_excel_document', autospec=True) as xls_mock:
            res = get_acts_xls_data(
                is_admin=False,
                request={'client_id': client.id, 'manager_code': res[0].manager_code if is_found else '666'},
                phrase_manager=self.PM(),
                db_service=db_service
            )

            xls_mock.assert_called_once()
            call_args = xls_mock.call_args
            assert len(call_args.args[1]) == (1 if is_found else 0)

        res = xls_export.get_excel_document(*call_args.args, **call_args.kwargs)
        assert type(res) is str and len(res) > 0
