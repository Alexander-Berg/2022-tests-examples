# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal
from collections import defaultdict
import itertools
import json

import mock
import pytest
import sqlalchemy as sa

from balance import constants as cst, core, exc
from balance import contractpage, mapper, muzzle_util as ut, reverse_partners as rp
from balance.actions import consumption
import balance.muzzle_util as ut
from balance.processors import month_proc as mp
from balance.providers.personal_acc_manager import PersonalAccountManager
from butils import logger
from butils.application import getApplication
from cluster_tools import generate_partner_acts as gpa

from tests import object_builder as ob


log = logger.get_logger('test_act_destructor')


Q = 'PARTNER_ACTS'

# disable testing of all DESTRUCTABLE_PARTNER_SERVICES mentioned below
# only tested cases are multiservice taxi, singleservice tickets, adfox
LIGHT = True


@pytest.fixture(scope='module')
def modular_session(modular_session):
    modular_session.config.__dict__['DESTRUCTABLE_PARTNER_SERVICES'] = [
        128, 641, 130, 131, 645, 135, 650, 151, 141, 142, 143, 124, 643, 661, 662, 23, 153, 668, 671, 672, 170,
        683, 172, 685, 125, 690, 116, 202, 663, 126, 80, 270, 600, 604, 607, 610, 611, 612, 101, 616, 617, 171, 620,
        621, 111, 625, 114, 628, 629, 118, 120, 636, 618, 638, 699, 605, 102,
    ]
    return modular_session


def make_transient(session, objects):
    session.add_all(objects)
    session.flush()
    for obj in objects:
        sa.orm.session.make_transient(obj)


def create_contract(session, **kwargs):
    with_integration = False
    if kwargs.get('integration'):
        kwargs.pop('integration')
        with_integration = True
    contract = ob.ContractBuilder(**kwargs).build(session).obj
    if with_integration:
        generators = {
            'GENERAL': 'RevPartnerGenerator',
            'SPENDABLE': 'SpendablePartnerProductGenerator',
        }
        scheme = {
            'contracts': [
                {
                    'ctype': contract.ctype.type,
                    'tag': 'tag',
                }
            ],
            'close_month': [
                {
                    'contract_tag': 'tag',
                    'month_close_generator': generators[contract.ctype.type],
                }
            ],
        }
        uniq_suffix = session.now().isoformat()
        integration = mapper.Integration(
            cc='test integration ' + uniq_suffix,
            display_name='test integration ' + uniq_suffix,
        )
        configuration = mapper.IntegrationsConfiguration(
            cc='test configuration ' + uniq_suffix,
            display_name='test configuration ' + uniq_suffix,
            integration=integration,
            scheme=json.dumps(scheme)
        )
        clients_integration = mapper.ClientsIntegration(
            client=contract.client,
            integration=integration,
            configuration=configuration,
            start_dt=session.now().date(),
        )
        session.add(integration)
        session.add(configuration)
        session.add(clients_integration)
        session.flush()

    cp = contractpage.ContractPage(session, contract.id)
    cp.create_personal_accounts()
    # for test data debug, slow
    # processor = RulesProcessor(cp)
    # cp.process_pyrules(processor, loading=True)
    # assert processor.success, processor.error
    return contract


@pytest.fixture(scope='module')
def shadow(modular_session):
    session = modular_session
    prev_act_month = mapper.ActMonth(
        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
    )
    act_month = mapper.ActMonth()
    general_acts = []
    non_general_acts = []

    contract_params = {
        'ctype': 'GENERAL',
        'firm': cst.FirmId.YANDEX_OOO,
        'services': {cst.ServiceId.DIRECT: 1},
        'dt': prev_act_month.begin_dt,
    }
    contract = create_contract(session, **contract_params)
    order = (
        ob.OrderBuilder(service_id=cst.ServiceId.DIRECT, client=contract.client)
        .build(session)
        .obj
    )
    order.calculate_consumption(prev_act_month.document_dt, {order.shipment_type: 200})
    request = (
        ob.RequestBuilder(
            firm_id=contract.col0.firm,
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=100)],
            ),
        )
        .build(session)
        .obj
    )
    invoice = (
        ob.InvoiceBuilder(
            dt=prev_act_month.document_dt,
            contract=contract,
            type='prepayment',
            request=request,
            person=contract.person,
        )
        .build(session)
        .obj
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    acts = invoice.generate_act(force=True, backdate=prev_act_month.document_dt)
    assert len(acts) == 1
    general_acts.extend(acts)

    core.Core(session).pay_order_cc(order.id, cst.CERT_PAYSYS_CC, 100)
    assert order.consumes[-1].invoice.paysys.certificate
    acts = order.consumes[-1].invoice.generate_act(
        force=True, backdate=prev_act_month.document_dt
    )
    assert len(acts) == 1
    general_acts.extend(acts)

    order.calculate_consumption(act_month.document_dt, {order.shipment_type: 300})
    request = (
        ob.RequestBuilder(
            firm_id=contract.col0.firm,
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=100)],
            ),
        )
        .build(session)
        .obj
    )
    invoice = (
        ob.InvoiceBuilder(
            dt=act_month.document_dt,
            contract=contract,
            type='prepayment',
            request=request,
            person=contract.person,
        )
        .build(session)
        .obj
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice.dt)
    acts = invoice.generate_act(force=True, backdate=act_month.document_dt)
    assert len(acts) == 1
    general_acts.extend(acts)

    act_params = {
        'place_id': 0,
        'place_type': 6,
        'page_id': 1000,
        'currency': 'RUR',
        'description': 'test',
        'block_id': 0,
        'dsp_id': 0,
        'hits': 0,
        'shows': 0,
        'partner_reward_wo_nds': 100,
        'aggregator_reward_wo_nds': 0,
        'dsp_charge': 100,
        'domain': '',
        'nds': 0,
        'status': 0,
        'completion_type': 0,
    }

    contract_params = {
        'ctype': 'DISTRIBUTION',
        'firm': cst.FirmId.YANDEX_OOO,
        'service_start_dt': prev_act_month.begin_dt,
        'distribution_tag': 1,
        'distribution_places': {1},
        'products_revshare': {1000: {'value_str': None, 'value_num': 1}},
    }
    contract = create_contract(session, **contract_params)
    non_general_acts.extend(
        [
            mapper.PartnerActData(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                dt=prev_act_month.begin_dt,
                end_dt=prev_act_month.document_dt,
                **act_params
            ),
            mapper.PartnerActData(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                dt=act_month.begin_dt,
                end_dt=act_month.document_dt,
                **act_params
            ),
        ]
    )
    non_general_acts.extend(
        [
            mapper.PartnerActDataForecast(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                dt=prev_act_month.begin_dt,
                end_dt=prev_act_month.document_dt,
                **act_params
            ),
            mapper.PartnerActDataForecast(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                dt=act_month.begin_dt,
                end_dt=act_month.document_dt,
                **act_params
            ),
        ]
    )

    contract_params = {
        'ctype': 'PARTNERS',
        'firm': cst.FirmId.YANDEX_OOO,
        'services': {cst.ServiceId.RNY: 1},
        'dt': prev_act_month.begin_dt,
        'person': ob.PersonBuilder(type='ur'),
    }
    contract = create_contract(session, **contract_params)
    non_general_acts.extend(
        [
            mapper.PartnerActData(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                report_dt=prev_act_month.begin_dt,
                report_end_dt=prev_act_month.document_dt,
                **act_params
            ),
            mapper.PartnerActData(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                report_dt=act_month.begin_dt,
                report_end_dt=act_month.document_dt,
                **act_params
            ),
        ]
    )
    non_general_acts.extend(
        [
            mapper.PartnerActDataForecast(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                report_dt=prev_act_month.begin_dt,
                report_end_dt=prev_act_month.document_dt,
                **act_params
            ),
            mapper.PartnerActDataForecast(
                partner_contract_id=contract.id,
                owner_id=contract.client_id,
                report_dt=act_month.begin_dt,
                report_end_dt=act_month.document_dt,
                **act_params
            ),
        ]
    )

    contract_params = {
        'ctype': 'GENERAL',
        'firm': cst.FirmId.TAXI,
        'services': {cst.ServiceId.TAXI_CORP_CLIENTS: 1},
        'dt': prev_act_month.begin_dt,
        'personal_account': 1,
        'currency': cst.NUM_CODE_RUR,
    }
    contract = create_contract(session, **contract_params)
    order = rp.get_order(cst.ServiceId.TAXI_CORP_CLIENTS, contract, ua_root=True)
    paysys = rp.get_paysys(contract, cst.ServiceId.TAXI_CORP_CLIENTS)
    invoice = (
        PersonalAccountManager(session)
        .for_contract(contract)
        .for_paysys(paysys)
        .get(auto_create=True)
    )
    invoice.transfer(order, sum=200, mode=cst.TransferMode.dst, skip_check=True)

    order.calculate_consumption(prev_act_month.document_dt, {order.shipment_type: 100})
    acts = invoice.generate_act(force=True, backdate=prev_act_month.document_dt)
    assert len(acts) == 1
    general_acts.extend(acts)

    order.calculate_consumption(act_month.document_dt, {order.shipment_type: 200})
    acts = invoice.generate_act(force=True, backdate=act_month.document_dt)
    assert len(acts) == 1
    general_acts.extend(acts)

    make_transient(session, general_acts)
    make_transient(session, non_general_acts)

    assert shadow_correctness(session, general_acts, non_general_acts)

    return general_acts, non_general_acts


def shadow_correctness(session, general_acts, non_general_acts):
    assert general_acts and all(
        session.query(type(act)).filter(type(act).id == act.id).all()
        for act in general_acts
    )
    assert non_general_acts and all(
        session.query(type(act))
        .filter(type(act).partner_contract_id == act.partner_contract_id)
        .all()
        for act in non_general_acts
    )
    # no need for make_transient - session does not get objects inside assert

    session_general_acts = filter(lambda obj: isinstance(obj, mapper.Act), session)
    act_ids = [act.id for act in general_acts]
    session_non_general_acts = filter(
        lambda obj: isinstance(obj, mapper.PartnerActData), session
    )
    non_general_contract_ids = [act.partner_contract_id for act in non_general_acts]
    return not (
        any(act.id in act_ids for act in session_general_acts)
        or any(
            act.partner_contract_id in non_general_contract_ids
            for act in session_non_general_acts
        )
    )


@pytest.fixture(autouse=True)
def ensure_single_session(modular_session):
    default_begin = modular_session.begin
    with mock.patch.object(
        modular_session, 'clone', return_value=modular_session
    ), mock.patch.object(
        modular_session, 'begin', lambda *args, **kwargs: default_begin(nested=True)
    ):
        yield


class TestDestructorCommon(object):
    @pytest.mark.parametrize(
        'input_, contract_params, error',
        [
            ({}, {'ctype': 'GENERAL'}, 'KeyError'),
            (
                {'forecast': True},
                {'ctype': 'SPENDABLE', 'services': {cst.ServiceId.BUG_BOUNTY: 1}},
                'KeyError',
            ),
            (
                (mapper.ActMonth(),),
                {
                    'ctype': 'DISTRIBUTION',
                    'firm': cst.FirmId.YANDEX_OOO,
                    'service_start_dt': datetime.datetime.now(),
                    'distribution_tag': 1,
                    'distribution_places': {1},
                    'products_revshare': {1000: {'value_str': None, 'value_num': 1}},
                },
                'TypeError',
            ),
            (
                {'for_month': mapper.ActMonth().document_dt, 'mode': 'hide'},
                {
                    'ctype': 'PARTNERS',
                    'firm': cst.FirmId.YANDEX_OOO,
                    'person': ob.PersonBuilder(type='ur'),
                },
                'Invalid mode',
            ),
            (
                {
                    'for_month': ut.add_months_to_date(datetime.datetime.today(), -2),
                    'mode': cst.PartnerActsMode.HIDE_ONLY,
                },
                {
                    'ctype': 'PARTNERS',
                    'firm': cst.FirmId.YANDEX_OOO,
                    'person': ob.PersonBuilder(type='ur'),
                },
                'Only previous month',
            ),
            (
                {
                    'for_month': datetime.datetime.today(),
                    'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
                },
                {
                    'ctype': 'PARTNERS',
                    'firm': cst.FirmId.YANDEX_OOO,
                    'person': ob.PersonBuilder(type='ur'),
                },
                'Only previous month',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
                },
                {'ctype': 'AFISHA'},
                'No act destructor for contract',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
                },
                {'ctype': 'ACQUIRING', 'paysys_type': 'cardpay'},
                'No act destructor for contract',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
                },
                {
                    'ctype': 'GEOCONTEXT',
                    'firm': cst.FirmId.YANDEX_OOO,
                    'nds': 0,
                    'currency': 643,
                },
                'No act destructor for contract',
            ),
        ],
    )
    def test_main(self, app, modular_session, shadow, input_, contract_params, error):
        general_acts, non_general_acts = shadow

        contract = create_contract(modular_session, **contract_params)
        contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        with pytest.raises(Exception) as e:
            gpa.PartnerActsQueue(app.cfg).process(contract.exports[Q])
        assert error in str(e)

        assert not filter(lambda obj: isinstance(obj, mapper.Act), modular_session)
        assert not filter(
            lambda obj: isinstance(obj, mapper.PartnerActData), modular_session
        )
        assert not filter(
            lambda obj: isinstance(obj, mapper.PartnerActDataForecast), modular_session
        )

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


inputs = [
    {'for_month': mapper.ActMonth().document_dt, 'forecast': True},
    {'for_month': mapper.ActMonth().document_dt, 'forecast': False},
    {'for_month': mapper.ActMonth().document_dt},
    {
        'for_month': mapper.ActMonth().document_dt,
        'forecast': True,
        'mode': cst.PartnerActsMode.GENERATE_ONLY,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'forecast': False,
        'mode': cst.PartnerActsMode.GENERATE_ONLY,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'mode': cst.PartnerActsMode.GENERATE_ONLY,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'forecast': True,
        'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'forecast': False,
        'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'forecast': True,
        'mode': cst.PartnerActsMode.HIDE_ONLY,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'forecast': False,
        'mode': cst.PartnerActsMode.HIDE_ONLY,
    },
    {'for_month': mapper.ActMonth().document_dt, 'mode': cst.PartnerActsMode.HIDE_ONLY},
]


class TestDestructorDistributionParent(object):
    @pytest.mark.parametrize(
        'input_, contracts_params, with_prev_acts, with_acts',
        itertools.product(
            inputs,
            [
                [
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.GROUP_OFFER,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                    },
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.CHILD_OFFER,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                        'distribution_tag': 1,
                        'distribution_places': {1},
                        'products_revshare': {
                            1000: {'value_str': None, 'value_num': 1}
                        },
                    },
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.CHILD_OFFER,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth(
                            for_month=ut.add_months_to_date(
                                datetime.datetime.today(), -2
                            )
                        ).begin_dt,
                        'distribution_tag': 2,
                        'distribution_places': {1},
                        'products_revshare': {
                            1001: {'value_str': None, 'value_num': 10}
                        },
                    },
                ],
                [
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.GROUP,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                    },
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.UNIVERSAL,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                        'distribution_tag': 1,
                        'distribution_places': {1},
                        'products_revshare': {
                            1000: {'value_str': None, 'value_num': 0}
                        },
                    },
                ],
            ],
            [False, True],
            [False, True],
        ),
    )
    def test_main(
        self,
        app,
        modular_session,
        shadow,
        input_,
        contracts_params,
        with_prev_acts,
        with_acts,
    ):
        general_acts, non_general_acts = shadow
        prev_act_month = mapper.ActMonth(
            for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
        )
        act_month = mapper.ActMonth()

        parent_contract = None
        child_contracts = []
        for contract_params in contracts_params:
            if contract_params['contract_type'] in (
                cst.DistributionContractType.GROUP,
                cst.DistributionContractType.GROUP_OFFER,
            ):
                contract = create_contract(modular_session, **contract_params)
                modular_session.flush()
                parent_contract = contract
            else:
                if parent_contract:
                    contract_params['parent_contract_id'] = parent_contract.id
                    contract_params['person'] = parent_contract.person
                contract = create_contract(modular_session, **contract_params)
                modular_session.flush()
                child_contracts.append(contract)

        forecast = input_.get('forecast', False)
        old_prev_act_data = []
        old_act_data = []
        old_act_data_forecast = []
        for contract in child_contracts:
            act_params = {
                'partner_contract_id': getattr(
                    contract.col0, 'parent_contract_id', None
                )
                or contract.id,
                'real_contract_id': contract.id
                if getattr(contract.col0, 'parent_contract_id', None)
                else None,
                'owner_id': contract.client_id,
                'place_id': contract.id,
                'place_type': 6,
                'page_id': 1000,
                'currency': 'RUR',
                'description': 'test',
                'block_id': 0,
                'dsp_id': 0,
                'hits': 0,
                'shows': 0,
                'partner_reward_wo_nds': 100,
                'aggregator_reward_wo_nds': 0,
                'dsp_charge': 100,
                'domain': '',
                'nds': 0,
                'status': 0,
                'completion_type': 0,
            }
            if with_prev_acts:
                old_prev_act_data.append(
                    mapper.PartnerActData(
                        dt=prev_act_month.begin_dt,
                        end_dt=prev_act_month.document_dt,
                        **act_params
                    )
                )
                old_prev_act_data.append(
                    mapper.PartnerActDataForecast(
                        dt=prev_act_month.begin_dt,
                        end_dt=prev_act_month.document_dt,
                        **act_params
                    )
                )
            if with_acts:
                if not forecast:
                    old_act_data.append(
                        mapper.PartnerActData(
                            dt=act_month.begin_dt,
                            end_dt=act_month.document_dt,
                            **act_params
                        )
                    )
                else:
                    old_act_data_forecast.append(
                        mapper.PartnerActDataForecast(
                            dt=act_month.begin_dt,
                            end_dt=act_month.document_dt,
                            **act_params
                        )
                    )
        modular_session.add_all(
            old_prev_act_data + old_act_data + old_act_data_forecast
        )
        modular_session.flush()

        main_contract = parent_contract
        main_contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        gpa.PartnerActsQueue(app.cfg).process(main_contract.exports[Q])

        query = modular_session.query(mapper.PartnerActData).filter(
            mapper.PartnerActData.partner_contract_id == main_contract.id
        )
        query_forecast = modular_session.query(mapper.PartnerActDataForecast).filter(
            mapper.PartnerActDataForecast.partner_contract_id == main_contract.id
        )
        prev_act_data = query.filter(
            mapper.PartnerActData.dt == prev_act_month.begin_dt
        ).all()
        prev_act_data_forecast = query_forecast.filter(
            mapper.PartnerActDataForecast.dt == prev_act_month.begin_dt
        ).all()
        assert (
            with_prev_acts
            and prev_act_data
            and prev_act_data_forecast
            or (not with_prev_acts and not prev_act_data and not prev_act_data_forecast)
        )
        assert set(old_prev_act_data) == set(prev_act_data) | set(
            prev_act_data_forecast
        )

        act_data = query.filter(mapper.PartnerActData.dt == act_month.begin_dt).all()
        act_data_forecast = query_forecast.filter(
            mapper.PartnerActDataForecast.dt == act_month.begin_dt
        ).all()
        if not forecast:
            assert with_acts and not act_data_forecast and act_data or not with_acts
        else:
            assert with_acts and act_data_forecast and not act_data or not with_acts
        assert set(old_act_data) == set(act_data)
        assert set(old_act_data_forecast) == set(act_data_forecast)

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


class TestDestructorDistribution(object):
    @pytest.mark.parametrize(
        'input_, contracts_params, with_prev_acts, with_acts, with_new_acts',
        itertools.product(
            inputs,
            [
                [
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.UNIVERSAL,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                        'is_signed': mapper.ActMonth().begin_dt,
                        'dt': mapper.ActMonth().begin_dt,
                        'distribution_tag': 1,
                        'distribution_places': {1},
                        'products_revshare': {
                            1000: {'value_str': None, 'value_num': 0}
                        },
                    }
                ],
                [
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.GROUP,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                        'is_signed': mapper.ActMonth().begin_dt,
                        'dt': mapper.ActMonth().begin_dt,
                    },
                    {
                        'ctype': 'DISTRIBUTION',
                        'firm': cst.FirmId.YANDEX_OOO,
                        'contract_type': cst.DistributionContractType.UNIVERSAL,
                        'currency_calculation': 1,
                        'service_start_dt': mapper.ActMonth().begin_dt,
                        'is_signed': mapper.ActMonth().begin_dt,
                        'dt': mapper.ActMonth().begin_dt,
                        'distribution_tag': 1,
                        'distribution_places': {1},
                        'products_revshare': {
                            1000: {'value_str': None, 'value_num': 0}
                        },
                    },
                ],
            ],
            [False, True],
            [False, True],
            [False, True],
        ),
    )
    def test_main(
        self,
        app,
        modular_session,
        shadow,
        input_,
        contracts_params,
        with_prev_acts,
        with_acts,
        with_new_acts,
    ):
        general_acts, non_general_acts = shadow

        prev_act_month = mapper.ActMonth(
            for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
        )
        act_month = mapper.ActMonth()

        parent_contract = None
        child_contracts = []
        for contract_params in contracts_params:
            if contract_params['contract_type'] in (
                cst.DistributionContractType.GROUP,
                cst.DistributionContractType.GROUP_OFFER,
            ):
                contract = create_contract(modular_session, **contract_params)
                modular_session.flush()
                parent_contract = contract
            else:
                if parent_contract:
                    contract_params['parent_contract_id'] = parent_contract.id
                    contract_params['person'] = parent_contract.person
                contract = create_contract(modular_session, **contract_params)
                modular_session.flush()
                child_contracts.append(contract)

        forecast = input_.get('forecast', False)
        old_prev_act_data = []
        old_act_data = []
        old_act_data_forecast = []
        new_act_data = []
        new_act_data_forecast = []
        for contract in child_contracts:
            act_params = {
                'partner_contract_id': getattr(
                    contract.col0, 'parent_contract_id', None
                )
                or contract.id,
                'real_contract_id': contract.id
                if getattr(contract.col0, 'parent_contract_id', None)
                else None,
                'owner_id': contract.client_id,
                'place_type': 6,
                'page_id': 1000,
                'currency': 'RUR',
                'description': 'test',
                'block_id': 0,
                'dsp_id': 0,
                'hits': 0,
                'shows': 0,
                'partner_reward_wo_nds': 100,
                'aggregator_reward_wo_nds': 0,
                'dsp_charge': 100,
                'domain': '',
                'nds': 0,
                'status': 0,
                'completion_type': 0,
            }
            if with_prev_acts:
                old_prev_act_data.append(
                    mapper.PartnerActData(
                        dt=prev_act_month.begin_dt,
                        end_dt=prev_act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
                old_prev_act_data.append(
                    mapper.PartnerActDataForecast(
                        dt=prev_act_month.begin_dt,
                        end_dt=prev_act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
            if with_acts:
                old_act_data.append(
                    mapper.PartnerActData(
                        dt=act_month.begin_dt,
                        end_dt=act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
                old_act_data_forecast.append(
                    mapper.PartnerActDataForecast(
                        dt=act_month.begin_dt,
                        end_dt=act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
            if with_new_acts:
                if not forecast:
                    new_act_data.append(
                        mapper.PartnerActData(
                            dt=act_month.begin_dt,
                            end_dt=act_month.document_dt,
                            place_id=contract.id * 10,
                            **act_params
                        )
                    )
                else:
                    new_act_data_forecast.append(
                        mapper.PartnerActDataForecast(
                            dt=act_month.begin_dt,
                            end_dt=act_month.document_dt,
                            place_id=contract.id * 10,
                            **act_params
                        )
                    )
        modular_session.add_all(
            old_prev_act_data + old_act_data + old_act_data_forecast
        )
        modular_session.flush()

        main_contract = child_contracts[0]
        main_contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        with mock.patch(
            'cluster_tools.generate_partner_acts.DistrEntityGenerator.get_rows',
            return_value=new_act_data or new_act_data_forecast,
        ):
            gpa.PartnerActsQueue(app.cfg).process(main_contract.exports[Q])

        query = modular_session.query(mapper.PartnerActData).filter(
            sa.func.nvl(
                mapper.PartnerActData.real_contract_id,
                mapper.PartnerActData.partner_contract_id,
            )
            == main_contract.id
        )
        query_forecast = modular_session.query(mapper.PartnerActDataForecast).filter(
            sa.func.nvl(
                mapper.PartnerActDataForecast.real_contract_id,
                mapper.PartnerActDataForecast.partner_contract_id,
            )
            == main_contract.id
        )
        prev_act_data = query.filter(
            mapper.PartnerActData.dt == prev_act_month.begin_dt
        ).all()
        prev_act_data_forecast = query_forecast.filter(
            mapper.PartnerActDataForecast.dt == prev_act_month.begin_dt
        ).all()
        assert (
            with_prev_acts
            and prev_act_data
            and prev_act_data_forecast
            or (not with_prev_acts and not prev_act_data and not prev_act_data_forecast)
        )
        assert set(old_prev_act_data) == set(prev_act_data) | set(
            prev_act_data_forecast
        )

        act_data = query.filter(mapper.PartnerActData.dt == act_month.begin_dt).all()
        act_data_forecast = query_forecast.filter(
            mapper.PartnerActDataForecast.dt == act_month.begin_dt
        ).all()
        mode = input_.get('mode', cst.PartnerActsMode.GENERATE_ONLY)
        if not forecast:
            assert with_acts and act_data_forecast or not with_acts
            if mode == cst.PartnerActsMode.GENERATE_ONLY:
                assert set(act_data) == set(old_act_data) | set(new_act_data)
            elif mode == cst.PartnerActsMode.HIDE_AND_GENERATE:
                assert set(act_data) == set(new_act_data)
            elif mode == cst.PartnerActsMode.HIDE_ONLY:
                assert not act_data
            else:
                assert False
        else:
            assert with_acts and act_data or not with_acts
            if mode == cst.PartnerActsMode.GENERATE_ONLY:
                assert set(act_data_forecast) == set(old_act_data_forecast) | set(
                    new_act_data_forecast
                )
            elif mode == cst.PartnerActsMode.HIDE_AND_GENERATE:
                assert set(act_data_forecast) == set(new_act_data_forecast)
            elif mode == cst.PartnerActsMode.HIDE_ONLY:
                assert not act_data_forecast
            else:
                assert False

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


class TestDestructorSpendable(object):
    @pytest.mark.parametrize(
        'input_, contract_params, with_prev_acts, with_acts, with_new_acts',
        itertools.product(
            inputs,
            [
                {
                    'ctype': 'SPENDABLE',
                    'firm': cst.FirmId.YANDEX_OOO,
                    'is_signed': mapper.ActMonth().begin_dt,
                    'dt': mapper.ActMonth().begin_dt,
                    'services': {cst.ServiceId.INGAME_PURCHASES: 1},
                },
                {
                    'ctype': 'SPENDABLE',
                    'firm': cst.FirmId.GAS_STATIONS,
                    'is_signed': mapper.ActMonth().begin_dt,
                    'dt': mapper.ActMonth().begin_dt,
                    'services': {cst.ServiceId.ZAXI_SELFEMPLOYED_SPENDABLE: 1},
                    'integration': True,
                },
            ],
            [False, True],
            [False, True],
            [False, True],
        ),
    )
    def test_main(
        self,
        app,
        modular_session,
        shadow,
        input_,
        contract_params,
        with_prev_acts,
        with_acts,
        with_new_acts,
    ):
        general_acts, non_general_acts = shadow

        prev_act_month = mapper.ActMonth(
            for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
        )
        act_month = mapper.ActMonth()

        child_contracts = []
        contract = create_contract(modular_session, **contract_params)
        child_contracts.append(contract)

        forecast = input_.get('forecast', False)
        old_prev_act_data = []
        old_act_data = []
        old_act_data_forecast = []
        new_act_data = []
        new_act_data_forecast = []
        for contract in child_contracts:
            act_params = {
                'partner_contract_id': contract.id,
                'owner_id': contract.client_id,
                'place_type': 6,
                'page_id': 1000,
                'currency': 'RUR',
                'description': 'test',
                'block_id': 0,
                'dsp_id': 0,
                'hits': 0,
                'shows': 0,
                'partner_reward_wo_nds': 100,
                'aggregator_reward_wo_nds': 0,
                'dsp_charge': 100,
                'domain': '',
                'nds': 0,
                'status': 0,
                'completion_type': 0,
            }
            if with_prev_acts:
                old_prev_act_data.append(
                    mapper.PartnerActData(
                        dt=prev_act_month.begin_dt,
                        end_dt=prev_act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
                old_prev_act_data.append(
                    mapper.PartnerActDataForecast(
                        dt=prev_act_month.begin_dt,
                        end_dt=prev_act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
            if with_acts:
                old_act_data.append(
                    mapper.PartnerActData(
                        dt=act_month.begin_dt,
                        end_dt=act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
                old_act_data_forecast.append(
                    mapper.PartnerActDataForecast(
                        dt=act_month.begin_dt,
                        end_dt=act_month.document_dt,
                        place_id=contract.id,
                        **act_params
                    )
                )
            if with_new_acts:
                if not forecast:
                    new_act_data.append(
                        mapper.PartnerActData(
                            dt=act_month.begin_dt,
                            end_dt=act_month.document_dt,
                            place_id=contract.id * 10,
                            **act_params
                        )
                    )
                else:
                    new_act_data_forecast.append(
                        mapper.PartnerActDataForecast(
                            dt=act_month.begin_dt,
                            end_dt=act_month.document_dt,
                            place_id=contract.id * 10,
                            **act_params
                        )
                    )
        modular_session.add_all(
            old_prev_act_data + old_act_data + old_act_data_forecast
        )
        modular_session.flush()

        main_contract = child_contracts[0]
        main_contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        with mock.patch(
            'cluster_tools.generate_partner_acts.SpendablePartnerProductGenerator.generate',
            lambda *args, **kwargs: modular_session.add_all(
                new_act_data + new_act_data_forecast
            ),
        ):
            gpa.PartnerActsQueue(app.cfg).process(contract.exports[Q])

        query = modular_session.query(mapper.PartnerActData).filter(
            sa.func.nvl(
                mapper.PartnerActData.real_contract_id,
                mapper.PartnerActData.partner_contract_id,
            )
            == main_contract.id
        )
        query_forecast = modular_session.query(mapper.PartnerActDataForecast).filter(
            sa.func.nvl(
                mapper.PartnerActDataForecast.real_contract_id,
                mapper.PartnerActDataForecast.partner_contract_id,
            )
            == main_contract.id
        )
        prev_act_data = query.filter(
            mapper.PartnerActData.dt == prev_act_month.begin_dt
        ).all()
        prev_act_data_forecast = query_forecast.filter(
            mapper.PartnerActDataForecast.dt == prev_act_month.begin_dt
        ).all()
        assert (
            with_prev_acts
            and prev_act_data
            and prev_act_data_forecast
            or (not with_prev_acts and not prev_act_data and not prev_act_data_forecast)
        )
        assert set(old_prev_act_data) == set(prev_act_data) | set(
            prev_act_data_forecast
        )

        act_data = query.filter(mapper.PartnerActData.dt == act_month.begin_dt).all()
        act_data_forecast = query_forecast.filter(
            mapper.PartnerActDataForecast.dt == act_month.begin_dt
        ).all()
        mode = input_.get('mode', cst.PartnerActsMode.GENERATE_ONLY)
        if not forecast:
            assert with_acts and act_data_forecast or not with_acts
            if mode == cst.PartnerActsMode.GENERATE_ONLY:
                assert set(act_data) == set(old_act_data) | set(new_act_data)
            elif mode == cst.PartnerActsMode.HIDE_AND_GENERATE:
                assert set(act_data) == set(new_act_data)
            elif mode == cst.PartnerActsMode.HIDE_ONLY:
                assert not act_data
            else:
                assert False
        else:
            assert with_acts and act_data or not with_acts
            if mode == cst.PartnerActsMode.GENERATE_ONLY:
                assert set(act_data_forecast) == set(old_act_data_forecast) | set(
                    new_act_data_forecast
                )
            elif mode == cst.PartnerActsMode.HIDE_AND_GENERATE:
                assert set(act_data_forecast) == set(new_act_data_forecast)
            elif mode == cst.PartnerActsMode.HIDE_ONLY:
                assert not act_data_forecast
            else:
                assert False

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


class TestDestructorGeneralCommon(object):
    @pytest.mark.parametrize(
        'input_, contract_params, error',
        [
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'reason': None,
                    'mode': cst.PartnerActsMode.HIDE_ONLY,
                },
                {
                    'ctype': 'GENERAL',
                    'services': {cst.ServiceId.MUSIC: 1},
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_cancelled': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.YANDEX_OOO,
                },
                'No reason',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'mode': cst.PartnerActsMode.HIDE_ONLY,
                },
                {
                    'ctype': 'GENERAL',
                    'services': {cst.ServiceId.MUSIC: 1},
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_cancelled': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.YANDEX_OOO,
                },
                'No reason',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'reason': 'PAYSUP',
                    'mode': cst.PartnerActsMode.HIDE_ONLY,
                },
                {
                    'ctype': 'GENERAL',
                    'services': {cst.ServiceId.MUSIC: 1},
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_cancelled': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.YANDEX_OOO,
                },
                'Closed month',
            ),
        ],
    )
    def test_main(self, app, modular_session, shadow, input_, contract_params, error):
        general_acts, non_general_acts = shadow

        contract = create_contract(modular_session, **contract_params)
        contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        with mock.patch(
            'cluster_tools.generate_partner_acts.mncloselib.can_close_invoice',
            return_value=False,
        ):
            with pytest.raises(Exception) as e:
                gpa.PartnerActsQueue(app.cfg).process(contract.exports[Q])
        assert error in str(e)

        assert not filter(lambda obj: isinstance(obj, mapper.Act), modular_session)
        assert not filter(
            lambda obj: isinstance(obj, mapper.PartnerActData), modular_session
        )
        assert not filter(
            lambda obj: isinstance(obj, mapper.PartnerActDataForecast), modular_session
        )

        assert shadow_correctness(modular_session, general_acts, non_general_acts)

    @pytest.mark.parametrize(
        'input_, contract_params, error',
        [
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'reason': 'PAYSUP',
                    'mode': cst.PartnerActsMode.HIDE_ONLY,
                    'service_ids': [],
                },
                {
                    'ctype': 'GENERAL',
                    'services': {
                        cst.ServiceId.TAXI_CARD: 1,
                        cst.ServiceId.TAXI_PAYMENT: 1,
                        cst.ServiceId.UBER_PAYMENT: 1,
                    },
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_signed': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.TAXI,
                    'country': cst.RegionId.RUSSIA,
                    'person': ob.PersonBuilder(type='ur'),
                    'nds_for_receipt': 0,
                },
                'services without destructor',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'reason': 'PAYSUP',
                    'mode': cst.PartnerActsMode.HIDE_ONLY,
                    'service_ids': [cst.ServiceId.TAXI_CORP_CLIENTS],
                },
                {
                    'ctype': 'GENERAL',
                    'services': {
                        cst.ServiceId.TAXI_CARD: 1,
                        cst.ServiceId.TAXI_PAYMENT: 1,
                        cst.ServiceId.TAXI_CASH: 1,
                    },
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_signed': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.TAXI,
                    'country': cst.RegionId.RUSSIA,
                    'person': ob.PersonBuilder(type='ur'),
                    'nds_for_receipt': 0,
                },
                'do not match contract destructable services',
            ),
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'reason': 'PAYSUP',
                    'mode': cst.PartnerActsMode.GENERATE_ONLY,
                    'service_ids': [cst.ServiceId.TAXI_CORP_CLIENTS],
                },
                {
                    'ctype': 'GENERAL',
                    'services': {
                        cst.ServiceId.TAXI_CARD: 1,
                        cst.ServiceId.TAXI_PAYMENT: 1,
                        cst.ServiceId.TAXI_CASH: 1,
                    },
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_signed': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.TAXI,
                    'country': cst.RegionId.RUSSIA,
                    'person': ob.PersonBuilder(type='ur'),
                    'nds_for_receipt': 0,
                },
                'do not match contract services',
            ),
        ],
    )
    def test_config(self, app, modular_session, shadow, input_, contract_params, error):
        general_acts, non_general_acts = shadow

        contract = create_contract(modular_session, **contract_params)
        contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        modular_session.config.__dict__['DESTRUCTABLE_PARTNER_SERVICES'] = [
            cst.ServiceId.MUSIC,
            cst.ServiceId.TAXI_CARD,
            cst.ServiceId.TAXI_PAYMENT,
            cst.ServiceId.TAXI_CASH,
        ]
        with mock.patch(
            'cluster_tools.generate_partner_acts.mncloselib.can_close_invoice',
            return_value=True,
        ):
            with pytest.raises(Exception) as e:
                gpa.PartnerActsQueue(app.cfg).process(contract.exports[Q])
        assert error in str(e)

        assert not filter(lambda obj: isinstance(obj, mapper.Act), modular_session)
        assert not filter(
            lambda obj: isinstance(obj, mapper.PartnerActData), modular_session
        )
        assert not filter(
            lambda obj: isinstance(obj, mapper.PartnerActDataForecast), modular_session
        )

        assert shadow_correctness(modular_session, general_acts, non_general_acts)

    @pytest.mark.parametrize(
        'input_, contract_params, completions, error',
        [
            (
                {
                    'for_month': mapper.ActMonth().document_dt,
                    'reason': 'PAYSUP',
                    'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
                    'service_ids': [cst.ServiceId.TAXI_CASH, cst.ServiceId.TAXI_PAYMENT],
                },
                {
                    'ctype': 'GENERAL',
                    'services': {
                        cst.ServiceId.TAXI_CARD: 1,
                        cst.ServiceId.TAXI_PAYMENT: 1,
                        cst.ServiceId.TAXI_CASH: 1,
                    },
                    'dt': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'is_signed': mapper.ActMonth(
                        for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                    ).begin_dt,
                    'currency': cst.NUM_CODE_RUR,
                    'firm': cst.FirmId.TAXI,
                    'country': cst.RegionId.RUSSIA,
                    'person': ob.PersonBuilder(type='ur'),
                    'nds_for_receipt': 0,
                },
                {cst.ServiceId.TAXI_CARD: [500], cst.ServiceId.TAXI_PAYMENT: [1000], cst.ServiceId.TAXI_CASH: [2000]},
                'do not match passed service_ids',
            ),
        ],
    )
    def test_inconsistent_input(self, app, modular_session, shadow, input_, contract_params, completions, error):
        general_acts, non_general_acts = shadow

        contract = create_contract(modular_session, **contract_params)
        contract.enqueue(Q, input_=input_, force=True)
        modular_session.flush()
        modular_session.config.__dict__['DESTRUCTABLE_PARTNER_SERVICES'] = [
            cst.ServiceId.TAXI_CARD,
            cst.ServiceId.TAXI_PAYMENT,
            cst.ServiceId.TAXI_CASH,
        ]

        act_month = mapper.ActMonth()
        invoices = {}
        for service_id, service_completions in completions.items():
            paysyses = rp.get_paysyses_for_service(contract, service_id)
            for service_code, paysys in paysyses.items():
                invoices[service_code] = (PersonalAccountManager(modular_session).for_contract(contract)
                                          .for_paysys(paysys).for_service_code(service_code)
                                          .get(auto_create=True))
            for completion in service_completions:
                product = (
                    ob.ProductBuilder(
                        engine_id=service_id,
                        price=1,
                        service_code='AGENT_REWARD'
                        if service_id in (cst.ServiceId.TAXI_PAYMENT,)
                        else 'YANDEX_SERVICE',
                    )
                        .build(modular_session)
                        .obj
                )
                order = rp.get_order(service_id, contract, product=product)
                order.calculate_consumption(act_month.document_dt, {order.shipment_type: completion})
                invoices[product.service_code].transfer(order, mode=2, sum=completion, skip_check=1)

        for personal_account in invoices.values():
            personal_account.generate_act(force=1, backdate=act_month.document_dt)

        with mock.patch(
            'cluster_tools.generate_partner_acts.mncloselib.can_close_invoice',
            return_value=True,
        ):
            with pytest.raises(Exception) as e:
                gpa.PartnerActsQueue(app.cfg).process(contract.exports[Q])
        assert error in str(e)

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


inputs = [
    {'for_month': mapper.ActMonth().document_dt},
    {
        'for_month': mapper.ActMonth().document_dt,
        'mode': cst.PartnerActsMode.GENERATE_ONLY,
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
        'reason': 'PAYSUP',
    },
    {
        'for_month': mapper.ActMonth().document_dt,
        'mode': cst.PartnerActsMode.HIDE_ONLY,
        'reason': 'PAYSUP',
    },
]


invoice_schemes = [
    # {
    #     'payment_type': cst.PREPAY_PAYMENT_TYPE,
    #     'personal_account': 0
    # },
    {'payment_type': cst.PREPAY_PAYMENT_TYPE, 'personal_account': 1},
    {
        'payment_type': cst.POSTPAY_PAYMENT_TYPE,
        'personal_account': 0,
        'partner_credit': 1,
    },
    {
        'payment_type': cst.POSTPAY_PAYMENT_TYPE,
        'personal_account': 1,
        'partner_credit': 1,
    },
    {
        'payment_type': cst.POSTPAY_PAYMENT_TYPE,
        'personal_account': 1,
        'personal_account_fictive': 1,
        'partner_credit': 1,
    },
]


contracts = [
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MUSIC: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.MUSIC: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.DSP: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.DSP: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MULTISHIP_DELIVERY: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MARKET,
        },
        'completions': {cst.ServiceId.MULTISHIP_DELIVERY: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.AVIA: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.AVIA: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.DISK: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.DISK: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TICKETS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MEDIASERVICES,
        },
        'completions': {cst.ServiceId.TICKETS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MULTISHIP_PAYMENT: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MARKET,
            'minimal_payment_commission': 0,
        },
        'completions': {cst.ServiceId.MULTISHIP_PAYMENT: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TICKETS_TO_EVENTS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MEDIASERVICES,
        },
        'completions': {cst.ServiceId.TICKETS_TO_EVENTS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.REALTY_PAYMENTS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.VERTIKALI,
        },
        'completions': {cst.ServiceId.REALTY_PAYMENTS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TICKETS2: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MEDIASERVICES,
            'country': cst.RegionId.RUSSIA,
        },
        'completions': {cst.ServiceId.TICKETS2: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.ADDAPTER_DEV: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.ADDAPTER_DEV: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.ADDAPTER_RET: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.ADDAPTER_RET: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.CLOUD: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.CLOUD,
        },
        'completions': {cst.ServiceId.CLOUD: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.BUSES: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.BUSES: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.HEALTH: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.HEALTH_CLINIC,
        },
        'completions': {cst.ServiceId.HEALTH: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TELEMED_AG: 1, cst.ServiceId.TELEMED_SRV: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.HEALTH_CLINIC,
            'medicine_pay_commission2': 0,
            'medicine_pay_commission': 0,
        },
        'completions': {
            cst.ServiceId.TELEMED_AG: [1000],
            cst.ServiceId.TELEMED_SRV: [5000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.UFS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.UFS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MARKETPLACE_NEW: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MARKET,
            'partner_commission_pct': 1,
        },
        'completions': {cst.ServiceId.MARKETPLACE_NEW: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.KINOPOISK_PLUS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.KINOPOISK,
        },
        'completions': {cst.ServiceId.KINOPOISK_PLUS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.DRIVE: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.DRIVE,
        },
        'completions': {cst.ServiceId.DRIVE: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.AERO: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.AERO: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.BLUE_PAYMENTS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MARKET,
        },
        'completions': {cst.ServiceId.BLUE_PAYMENTS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.ALISA_BOX_AG: 1, cst.ServiceId.ALISA_BOX_SRV: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
            'partner_commission_pct2': 1,
            'commissions': json.dumps({"Default": "1"}),
        },
        'completions': {
            cst.ServiceId.ALISA_BOX_AG: [1000],
            cst.ServiceId.ALISA_BOX_SRV: [2000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.BLUE_SRV: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MARKET,
        },
        'completions': {cst.ServiceId.BLUE_SRV: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.AFISHA_MOVIEPASS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MEDIASERVICES,
        },
        'completions': {cst.ServiceId.AFISHA_MOVIEPASS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.RED_SRV: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': 840,
            'firm': cst.FirmId.HK_ECOMMERCE,
            'person': ob.PersonBuilder(type='hk_ur'),
        },
        'completions': {cst.ServiceId.RED_SRV: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.RED_PAYMENTS: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': 840,
            'firm': cst.FirmId.HK_ECOMMERCE,
            'person': ob.PersonBuilder(type='hk_ur'),
        },
        'completions': {cst.ServiceId.RED_PAYMENTS: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.GAS_STATION: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.GAS_STATIONS,
        },
        'completions': {cst.ServiceId.GAS_STATION: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MESSENGER: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
            'partner_commission_pct2': 0,
        },
        'completions': {cst.ServiceId.MESSENGER: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.FOOD_PAYMENT: 1, cst.ServiceId.FOOD_SRV: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.FOOD,
            'partner_commission_pct2': 0,
        },
        'completions': {
            cst.ServiceId.FOOD_PAYMENT: [1000],
            cst.ServiceId.FOOD_SRV: [2000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.ZAXI: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.GAS_STATIONS,
        },
        'completions': {cst.ServiceId.ZAXI: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TICKETS3: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MEDIASERVICES,
        },
        'completions': {cst.ServiceId.TICKETS3: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TRAVEL: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.TRAVEL: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.DRIVE_REFUELLER_PENALTY: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.DRIVE,
        },
        'completions': {cst.ServiceId.DRIVE_REFUELLER_PENALTY: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.FOOD_COURIERS_PAYMENT: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.FOOD,
        },
        'completions': {cst.ServiceId.FOOD_COURIERS_PAYMENT: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {
                cst.ServiceId.FOOD_SHOPS_SRV: 1,
                cst.ServiceId.FOOD_SHOPS_PAYMENT: 1,
            },
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.FOOD,
            'partner_commission_pct2': 0,
        },
        'completions': {
            cst.ServiceId.FOOD_SHOPS_SRV: [1000],
            cst.ServiceId.FOOD_SHOPS_PAYMENT: [500],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.LAVKA_COURIERS_PAYMENT: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': 376,
            'firm': cst.FirmId.ISRAEL_GO,
            'person': ob.PersonBuilder(type='il_ur'),
            'israel_tax_pct': 0,
            'country': cst.RegionId.ISRAEL,
        },
        'completions': {cst.ServiceId.LAVKA_COURIERS_PAYMENT: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.DISK_B2B: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.DISK_B2B: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.HEALTH_HELP: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.HEALTH_CLINIC,
        },
        'completions': {cst.ServiceId.HEALTH_HELP: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.HEALTH_HELP: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.HEALTH_CLINIC,
        },
        'completions': {cst.ServiceId.HEALTH_HELP: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MUSIC_MEDIASERVICES: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.MEDIASERVICES,
        },
        'completions': {cst.ServiceId.MUSIC_MEDIASERVICES: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.MAIL_PRO: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.MAIL_PRO: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {
                cst.ServiceId.TAXI_CASH: 1,
                cst.ServiceId.TAXI_CARD: 1,
                cst.ServiceId.TAXI_PAYMENT: 1,
                cst.ServiceId.UBER_PAYMENT: 1,
            },  # , cst.ServiceId.TAXI_RUTAXI_PAYMENT: 1, cst.ServiceId.TAXI_VEZET_PAYMENT: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
            'person': ob.PersonBuilder(type='ur'),
            'nds_for_receipt': 0,
        },
        'completions': {
            cst.ServiceId.TAXI_CARD: [5000],
            cst.ServiceId.TAXI_CASH: [10000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {
                cst.ServiceId.TAXI_CARD: 1,
                cst.ServiceId.TAXI_PAYMENT: 1,
                cst.ServiceId.UBER_PAYMENT: 1,
            },
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
            'person': ob.PersonBuilder(type='ur'),
            'nds_for_receipt': 0,
        },
        'completions': {
            cst.ServiceId.TAXI_CARD: [5000],
            cst.ServiceId.TAXI_PAYMENT: [10000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.TAXI_CASH: 1,},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
            'person': ob.PersonBuilder(type='ur'),
            'nds_for_receipt': 0,
        },
        'completions': {cst.ServiceId.TAXI_CASH: [5000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {
                cst.ServiceId.TAXI_CORP: 1,
                cst.ServiceId.TAXI_CORP_CLIENTS: 1,
            },
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
        },
        'completions': {
            cst.ServiceId.TAXI_CORP: [1000],
            cst.ServiceId.TAXI_CORP_CLIENTS: [2000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.FOOD_CORP: 1,},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
        },
        'completions': {cst.ServiceId.FOOD_CORP: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.DRIVE_B2B: 1,},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
        },
        'completions': {cst.ServiceId.DRIVE_B2B: [1000],},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.FOOD_PICKERS_PAYMENT: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.FOOD,
        },
        'completions': {cst.ServiceId.FOOD_PICKERS_PAYMENT: [1000]},
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {cst.ServiceId.ADFOX: 1},
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.YANDEX_OOO,
        },
        'completions': {cst.ServiceId.ADFOX: [1000]},
    },
]


if LIGHT:
    contracts = [
        {
            'contract_params': {
                'ctype': 'GENERAL',
                'services': {cst.ServiceId.TICKETS3: 1},
                'dt': mapper.ActMonth(
                    for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                ).begin_dt,
                'is_signed': mapper.ActMonth(
                    for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                ).begin_dt,
                'currency': cst.NUM_CODE_RUR,
                'firm': cst.FirmId.MEDIASERVICES,
            },
            'completions': {cst.ServiceId.TICKETS3: [1000],},
        },
        {
            'contract_params': {
                'ctype': 'GENERAL',
                'services': {
                    cst.ServiceId.TAXI_CASH: 1,
                    cst.ServiceId.TAXI_CARD: 1,
                    cst.ServiceId.TAXI_PAYMENT: 1,
                    cst.ServiceId.UBER_PAYMENT: 1,
                },
                'dt': mapper.ActMonth(
                    for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                ).begin_dt,
                'is_signed': mapper.ActMonth(
                    for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                ).begin_dt,
                'currency': cst.NUM_CODE_RUR,
                'firm': cst.FirmId.TAXI,
                'country': cst.RegionId.RUSSIA,
                'person': ob.PersonBuilder(type='ur'),
                'nds_for_receipt': 0,
            },
            'completions': {
                cst.ServiceId.TAXI_CARD: [5000],
                cst.ServiceId.TAXI_CASH: [10000],
            },
        },
        {
            'contract_params': {
                'ctype': 'GENERAL',
                'services': {cst.ServiceId.ADFOX: 1},
                'dt': mapper.ActMonth(
                    for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                ).begin_dt,
                'is_signed': mapper.ActMonth(
                    for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
                ).begin_dt,
                'currency': cst.NUM_CODE_RUR,
                'firm': cst.FirmId.YANDEX_OOO,
            },
            'completions': {cst.ServiceId.ADFOX: [1000]},
        },
    ]


def rp_process(contract, input_, completions, strict=True):
    app = getApplication()
    session = contract.session
    if not completions:
        completions = {
            service_id: lambda *args, **kwargs: rp.AttributedList([])
            for service_id in contract.col0.services
        }

    with mock.patch(
        'cluster_tools.generate_partner_acts.mncloselib.can_close_invoice',
        return_value=True,
    ), mock.patch.dict('cluster_tools.generate_partner_acts.rp.compl_map', completions),\
        mock.patch('balance.actions.acts.account.group_rows', return_value=True):
        contract.enqueue(Q, input_=input_, force=True)
        session.flush()
        gpa.PartnerActsQueue(app.cfg).process(contract.exports[Q])
        contract.session.expire_all()
        export_obj = contract.client.exports.get('MONTH_PROC', None)
        if strict:
            mp.handle_client(export_obj.object, ut.Struct(export_obj.input))
            export_obj.state = 1
            session.flush()
        else:
            if export_obj and export_obj.state == cst.ExportState.enqueued:
                mp.handle_client(export_obj.object, ut.Struct(export_obj.input))
                export_obj.state = 1
                session.flush()


def get_actual_completions(contract, completions, act_month):
    session = contract.session
    actual_completions = defaultdict(rp.AttributedList)
    rpc = rp.ReversePartnerCalc(contract, contract.col0.services, act_month)
    main_service_id = rp.get_taxi_eff_sid(contract)
    for service_id, service_completions in completions.items():
        try:
            processor = rpc.get_processor(contract.current_state(), service_id)
        except rp.NoErrorReturn:
            continue
        if any(
            isinstance(processor, processor_type)
            for processor_type in (
                rp.ReversePartnersCorpTaxiProcessor,
                rp.ReversePartnersTaxiProcessor,
            )
        ):
            if service_id != main_service_id:
                continue
            for completion in service_completions:
                product = rp.get_product(main_service_id, contract, ua_root=True)
                order = rp.get_order(main_service_id, contract, product=product)
                actual_completions[service_id].append((product.id, completion, order))
        else:
            for completion in service_completions:
                product = (
                    ob.ProductBuilder(
                        engine_id=service_id,
                        price=1,
                        service_code='AGENT_REWARD'
                        if service_id in (cst.ServiceId.TAXI_PAYMENT,)
                        else None,
                    )
                    .build(session)
                    .obj
                )
                actual_completions[service_id].append(
                    (
                        product.id,
                        completion,
                        rp.get_order(service_id, contract, product=product),
                    )
                )
    return actual_completions


class TestDestructorGeneralTypical(object):
    @pytest.mark.parametrize(
        'input_, invoice_scheme_data, contract_data, with_acts, with_new_acts',
        itertools.product(
            inputs, invoice_schemes, contracts, [False, True], [False, True]
        ),
    )
    def test_main(
        self,
        app,
        modular_session,
        shadow,
        input_,
        invoice_scheme_data,
        contract_data,
        with_acts,
        with_new_acts,
    ):
        general_acts, non_general_acts = shadow

        prev_act_month = mapper.ActMonth(
            for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
        )
        act_month = mapper.ActMonth()

        contract_params = contract_data['contract_params']
        contract_params.update(invoice_scheme_data)
        if contract_params['payment_type'] == cst.PREPAY_PAYMENT_TYPE:
            if contract_params['personal_account'] == 1:
                if any(
                    sid
                    in (
                        cst.ServiceId.MUSIC,  # unknown
                        cst.ServiceId.DSP,  # non thirdparty
                        cst.ServiceId.MULTISHIP_DELIVERY,  # non thirdparty
                        cst.ServiceId.ADFOX,  # non thirdparty
                        cst.ServiceId.AVIA,  # non thirdparty
                        cst.ServiceId.DISK,  # unknown
                        cst.ServiceId.TICKETS,  # unknown
                        cst.ServiceId.MULTISHIP_PAYMENT,  # unknown
                        cst.ServiceId.TICKETS_TO_EVENTS,  # unknown
                        cst.ServiceId.APIKEYS,  # non thirdparty
                        cst.ServiceId.REALTY_PAYMENTS,  # unknown
                        cst.ServiceId.TICKETS2,  # unknown
                        cst.ServiceId.ADDAPTER_DEV,  # only postpay, contract rules
                        cst.ServiceId.ADDAPTER_RET,  # only postpay, contract rules
                        cst.ServiceId.CLOUD,  # non thirdparty
                        cst.ServiceId.BUSES,  # unknown
                        cst.ServiceId.HEALTH,  # non thirdparty
                        cst.ServiceId.UFS,  # unknown
                        cst.ServiceId.MARKETPLACE_NEW,  # unknown
                        # cst.ServiceId.MEDIA_ADVANCE_PAYMENT,  # non thirdparty
                        cst.ServiceId.KINOPOISK_PLUS,  # unknown
                        cst.ServiceId.DRIVE,  # unknown
                        cst.ServiceId.AERO,  # unknown
                        cst.ServiceId.BLUE_PAYMENTS,  # unknown
                        cst.ServiceId.ALISA_BOX_AG,
                        cst.ServiceId.ALISA_BOX_SRV,  # only postpay, contract rules, non thirdparty
                        cst.ServiceId.TELEMED_AG,
                        cst.ServiceId.TELEMED_SRV,  # unknown
                        cst.ServiceId.BLUE_SRV,  # non thirdparty
                        cst.ServiceId.AFISHA_MOVIEPASS,  # unknown
                        cst.ServiceId.RED_SRV,  # non thirdparty
                        cst.ServiceId.RED_PAYMENTS,  # unknown
                        cst.ServiceId.GAS_STATION,  # unknown
                        cst.ServiceId.MESSENGER,  # only postpay, contract rules
                        cst.ServiceId.FOOD_PAYMENT,
                        cst.ServiceId.FOOD_SRV,  # unknown
                        cst.ServiceId.ZAXI,  # unknown
                        cst.ServiceId.TRAVEL,  # unknown
                        cst.ServiceId.DRIVE_REFUELLER_PENALTY,  # unknown
                        cst.ServiceId.DISK_B2B,  # non thirdparty
                        cst.ServiceId.HEALTH_HELP,  # unknown
                        cst.ServiceId.MUSIC_MEDIASERVICES,  # unknown
                        cst.ServiceId.MAIL_PRO,  # unknown
                    )
                    for sid in contract_params['services']
                ):
                    return
        if contract_params['payment_type'] == cst.POSTPAY_PAYMENT_TYPE:
            if contract_params['personal_account'] == 0:
                if any(
                    sid
                    in (
                        cst.ServiceId.MUSIC,  # unknown
                        cst.ServiceId.DISK,  # unknown
                        # cst.ServiceId.APIKEYS,  # direct-like
                        cst.ServiceId.BLUE_SRV,  # unknown
                        cst.ServiceId.MUSIC_MEDIASERVICES,  # unknown
                        cst.ServiceId.MAIL_PRO,  # unknown
                        cst.ServiceId.TAXI_CASH,
                        cst.ServiceId.TAXI_CARD,
                        cst.ServiceId.TAXI_PAYMENT,
                        cst.ServiceId.UBER_PAYMENT,
                        cst.ServiceId.TAXI_RUTAXI_PAYMENT,
                        cst.ServiceId.TAXI_VEZET_PAYMENT,
                        cst.ServiceId.TAXI_CORP,
                        cst.ServiceId.TAXI_CORP_CLIENTS,
                        cst.ServiceId.FOOD_CORP,
                        cst.ServiceId.DRIVE_B2B,
                        # cst.ServiceId.TAXI_CORP_DISPATCHING,
                    )
                    for sid in contract_params['services']
                ):
                    return

        completions = contract_data['completions']
        contract = create_contract(modular_session, **contract_params)

        prev_month_acts = []
        month_acts = []
        actual_completions = defaultdict(rp.AttributedList)
        if with_acts:
            actual_completions = get_actual_completions(
                contract, completions, prev_act_month
            )
            completions_patch = {
                service_id: (lambda compl=compl: (lambda *args, **kwargs: compl))()
                for service_id, compl in actual_completions.items()
            }

            rp_process(
                contract, {'for_month': prev_act_month.document_dt}, completions_patch
            )

            invoices = (
                modular_session.query(mapper.Invoice)
                .filter_by(contract_id=contract.id)
                .all()
            )
            for invoice in invoices:
                prev_month_acts.extend(
                    filter(
                        lambda act: act.dt == prev_act_month.document_dt, invoice.acts
                    )
                )
            product_completions = defaultdict(Decimal)
            for service_id, service_completions in actual_completions.items():
                for product_id, completion, order in service_completions:
                    product_completions[product_id] += completion
            acted_completions = defaultdict(Decimal)
            for act in prev_month_acts:
                for row in act.rows:
                    acted_completions[row.consume.order.product.id] += row.act_qty
            assert acted_completions == product_completions
        prev_actual_completions = actual_completions.copy()
        if with_new_acts:
            if with_acts:
                for service_id in actual_completions:
                    actual_completions[service_id] = rp.AttributedList(
                        [
                            (product_id, completion * 2, order)
                            for product_id, completion, order in actual_completions[
                                service_id
                            ]
                        ]
                    )
            else:
                actual_completions = get_actual_completions(
                    contract, completions, act_month
                )
            completions_patch = {
                service_id: (lambda compl=compl: (lambda *args, **kwargs: compl))()
                for service_id, compl in actual_completions.items()
            }

            rp_process(
                contract, {'for_month': act_month.document_dt}, completions_patch
            )

            acts = []
            invoices = (
                modular_session.query(mapper.Invoice)
                .filter_by(contract_id=contract.id)
                .all()
            )
            for invoice in invoices:
                acts.extend(invoice.acts)
                month_acts.extend(
                    filter(lambda act: act.dt == act_month.document_dt, invoice.acts)
                )
            product_completions = defaultdict(Decimal)
            for service_id, service_completions in actual_completions.items():
                for product_id, completion, order in service_completions:
                    product_completions[product_id] += completion
            if cst.ServiceId.ADFOX in actual_completions:
                for service_id, service_completions in prev_actual_completions.items():
                    for product_id, completion, order in service_completions:
                        product_completions[product_id] += completion
            acted_completions = defaultdict(Decimal)
            for act in acts:
                for row in act.rows:
                    acted_completions[row.consume.order.product.id] += row.act_qty
            assert acted_completions == product_completions
        else:
            if cst.ServiceId.ADFOX in actual_completions:
                actual_completions = {}

        completions_patch = {
            service_id: (lambda compl=compl: (lambda *args, **kwargs: compl))()
            for service_id, compl in actual_completions.items()
        }

        checkpoint = datetime.datetime.now()
        checkpoint = checkpoint.replace(microsecond=0) - datetime.timedelta(microseconds=1)

        mode = input_.get('mode', cst.PartnerActsMode.GENERATE_ONLY)
        rp_process(
            contract, input_, completions_patch, strict=False,
        )

        acts = []
        new_acts = []
        invoices = (
            modular_session.query(mapper.Invoice)
            .filter_by(contract_id=contract.id)
            .all()
        )
        for invoice in invoices:
            acts.extend(invoice.acts)
            new_acts.extend(
                filter(
                    lambda act: act.dt == act_month.document_dt
                    and act.id not in [act.id for act in month_acts],
                    invoice.acts,
                )
            )
        for act in acts:
            invoice = act.invoice
            if act.hidden:
                if not isinstance(invoice, mapper.PersonalAccount):
                    assert invoice.hidden
                    assert not invoice.receipt_sum
                    if isinstance(invoice, mapper.RepaymentInvoice):
                        for fictive in invoice.fictives:
                            assert fictive.hidden
                            assert not fictive.receipt_sum
                else:
                    assert not invoice.hidden
            else:
                assert any(
                    isinstance(invoice, type_)
                    for type_ in (
                        mapper.PersonalAccount,
                        mapper.YInvoice,
                        mapper.RepaymentInvoice,
                    )
                )
        prev_product_completions = defaultdict(Decimal)
        for service_id, service_completions in prev_actual_completions.items():
            for product_id, completion, order in service_completions:
                prev_product_completions[product_id] += completion
        product_completions = defaultdict(Decimal)
        for service_id, service_completions in actual_completions.items():
            for product_id, completion, order in service_completions:
                product_completions[product_id] += completion
        if cst.ServiceId.ADFOX in actual_completions:
            for service_id, service_completions in prev_actual_completions.items():
                for product_id, completion, order in service_completions:
                    product_completions[product_id] += completion
        actual_prev_month_acts = list(
            filter(
                lambda act: not act.hidden and act.dt == prev_act_month.document_dt,
                acts,
            )
        )
        assert all(not act.hidden for act in actual_prev_month_acts)
        actual_month_acts = list(
            filter(lambda act: not act.hidden and act.dt == act_month.document_dt, acts)
        )
        if mode == cst.PartnerActsMode.GENERATE_ONLY:
            assert not new_acts
            assert set((act.id, act.hidden) for act in prev_month_acts) | set(
                (act.id, act.hidden) for act in month_acts
            ) == set((act.id, act.hidden) for act in acts)
            if not with_acts and not with_new_acts:
                assert not acts
            elif not with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        assert (
                            act_month.document_dt
                            <= order.shipment.dt
                            < act_month.end_dt
                        )
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and not with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert order.shipment.dt == prev_act_month.document_dt
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        assert (
                            act_month.document_dt
                            <= order.shipment.dt
                            < act_month.end_dt
                        )
                        assert order.shipment.update_dt < checkpoint
            else:
                assert False
        elif mode == cst.PartnerActsMode.HIDE_AND_GENERATE:
            if with_new_acts:
                assert new_acts
            assert all(act.jira_id == input_['reason'] for act in month_acts)
            assert set((act.id, act.hidden) for act in prev_month_acts) | set(
                (act.id, 4) for act in month_acts
            ) | set((act.id, 0) for act in new_acts) == set(
                (act.id, act.hidden) for act in acts
            )
            assert set((act.id, act.hidden) for act in new_acts).issubset(
                set((act.id, act.hidden) for act in acts)
            )
            if not with_acts and not with_new_acts:
                assert not acts
            elif not with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        assert (
                            act_month.document_dt
                            <= order.shipment.dt
                            < act_month.end_dt
                        )
                        assert order.shipment.update_dt > checkpoint
            elif with_acts and not with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert order.shipment.dt == prev_act_month.document_dt
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        assert (
                            act_month.document_dt
                            <= order.shipment.dt
                            < act_month.end_dt
                        )
                        assert order.shipment.update_dt > checkpoint
            else:
                assert False
        elif mode == cst.PartnerActsMode.HIDE_ONLY:
            assert not new_acts
            assert all(act.jira_id == input_['reason'] for act in month_acts)
            assert set((act.id, act.hidden) for act in prev_month_acts) | set(
                (act.id, 4) for act in month_acts
            ) == set((act.id, act.hidden) for act in acts)
            if not with_acts and not with_new_acts:
                assert not acts
            elif not with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert not order.shipment.consumption
                        assert (
                            act_month.document_dt
                            <= order.shipment.dt
                            < act_month.end_dt
                        )
                        assert order.shipment.update_dt > checkpoint
            elif with_acts and not with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert order.shipment.dt == prev_act_month.document_dt
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert (
                            act_month.document_dt
                            <= order.shipment.dt
                            < act_month.end_dt
                        )
                        assert order.shipment.update_dt > checkpoint
            else:
                assert False

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


contracts = [
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {
                cst.ServiceId.TAXI_CORP: 1,
                cst.ServiceId.TAXI_CORP_CLIENTS: 1,
            },
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
        },
        'completions': {
            cst.ServiceId.TAXI_CORP: [1000],
            cst.ServiceId.TAXI_CORP_CLIENTS: [2000],
        },
    },
    {
        'contract_params': {
            'ctype': 'GENERAL',
            'services': {
                cst.ServiceId.TAXI_CASH: 1,
                cst.ServiceId.TAXI_CARD: 1,
                cst.ServiceId.TAXI_PAYMENT: 1,
                cst.ServiceId.UBER_PAYMENT: 1,
            },
            'dt': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'is_signed': mapper.ActMonth(
                for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
            ).begin_dt,
            'currency': cst.NUM_CODE_RUR,
            'firm': cst.FirmId.TAXI,
            'country': cst.RegionId.RUSSIA,
            'person': ob.PersonBuilder(type='ur'),
            'nds_for_receipt': 0,
        },
        'completions': {
            cst.ServiceId.TAXI_CARD: [5000],
            cst.ServiceId.TAXI_CASH: [10000],
        },
    },
]


class TestDestructorGeneralAtypical(object):
    @pytest.mark.parametrize(
        'input_, invoice_scheme_data, contract_data, with_acts, with_new_acts',
        itertools.product(
            inputs, invoice_schemes, contracts, [True], [True]
        ),
    )
    def test_main(
        self,
        app,
        modular_session,
        shadow,
        input_,
        invoice_scheme_data,
        contract_data,
        with_acts,
        with_new_acts,
    ):
        general_acts, non_general_acts = shadow

        prev_act_month = mapper.ActMonth(
            for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
        )
        act_month = mapper.ActMonth()

        contract_params = contract_data['contract_params']
        contract_params.update(invoice_scheme_data)
        if contract_params['payment_type'] == cst.POSTPAY_PAYMENT_TYPE:
            if contract_params['personal_account'] == 0:
                if any(
                    sid
                    in (
                        cst.ServiceId.TAXI_CASH,
                        cst.ServiceId.TAXI_CORP_CLIENTS,
                    )
                    for sid in contract_params['services']
                ):
                    return

        completions = contract_data['completions']
        contract = create_contract(modular_session, **contract_params)

        prev_month_acts = []
        month_acts = []
        actual_completions = defaultdict(rp.AttributedList)
        if with_acts:
            for service_id, service_completions in completions.items():
                if service_id != rp.get_taxi_eff_sid(contract):
                    paysyses = rp.get_paysyses_for_service(contract, service_id)
                    invoices = {}
                    for service_code, paysys in paysyses.items():
                        invoices[paysys] = (PersonalAccountManager(modular_session).for_contract(contract)
                                            .for_paysys(paysys).for_service_code(service_code)
                                            .get(auto_create=True))
                    assert len(invoices) == 1
                    personal_account, = invoices.values()
                    for completion in service_completions:
                        product = (
                            ob.ProductBuilder(
                                engine_id=service_id,
                                price=1,
                                service_code='AGENT_REWARD'
                                if service_id in (cst.ServiceId.TAXI_PAYMENT,)
                                else None,
                            )
                            .build(modular_session)
                            .obj
                        )
                        order = rp.get_order(service_id, contract, product=product)
                        order.calculate_consumption(prev_act_month.document_dt, {order.shipment_type: completion})
                        personal_account.transfer(order, mode=2, sum=completion, skip_check=1)

            personal_account.generate_act(force=1, backdate=prev_act_month.document_dt)

            invoices = (
                modular_session.query(mapper.Invoice)
                .filter_by(contract_id=contract.id)
                .all()
            )
            for invoice in invoices:
                for consume in invoice.consumes:
                    consume.completion_qty = 0
                    consume.completion_sum = 0
                    consumption.reverse_consume(consume, None, consume.current_qty)
                    consume.order.calculate_consumption(prev_act_month.document_dt, {order.shipment_type: 0})
                prev_month_acts.extend(
                    filter(
                        lambda act: act.dt == prev_act_month.document_dt, invoice.acts
                    )
                )
            modular_session.flush()

        prev_actual_completions = actual_completions.copy()
        if with_new_acts:
            actual_completions = get_actual_completions(
                contract, completions, act_month
            )
            completions_patch = {
                service_id: (lambda compl=compl: (lambda *args, **kwargs: compl))()
                for service_id, compl in actual_completions.items() if service_id == rp.get_taxi_eff_sid(contract)
            }

            rp_process(
                contract, {'for_month': act_month.document_dt}, completions_patch
            )

            acts = []
            invoices = (
                modular_session.query(mapper.Invoice)
                .filter_by(contract_id=contract.id)
                .all()
            )
            for invoice in invoices:
                acts.extend(invoice.acts)
                month_acts.extend(
                    filter(lambda act: act.dt == act_month.document_dt, invoice.acts)
                )
            assert all(row.netting is None for act in prev_month_acts for row in act.rows)
            assert any(row.netting and row.act_qty > 0 for act in month_acts for row in act.rows)
            assert any(row.netting and row.act_qty < 0 for act in month_acts for row in act.rows)

        completions_patch = {
            service_id: (lambda compl=compl: (lambda *args, **kwargs: compl))()
            for service_id, compl in actual_completions.items()
        }

        checkpoint = datetime.datetime.now()
        checkpoint = checkpoint.replace(microsecond=0) - datetime.timedelta(microseconds=1)

        mode = input_.get('mode', cst.PartnerActsMode.GENERATE_ONLY)
        rp_process(
            contract, input_, completions_patch, strict=False,
        )

        acts = []
        new_acts = []
        invoices = (
            modular_session.query(mapper.Invoice)
            .filter_by(contract_id=contract.id)
            .all()
        )
        for invoice in invoices:
            acts.extend(invoice.acts)
            new_acts.extend(
                filter(
                    lambda act: act.dt == act_month.document_dt
                    and act.id not in [act.id for act in month_acts],
                    invoice.acts,
                )
            )
        for act in acts:
            invoice = act.invoice
            if act.hidden:
                if not isinstance(invoice, mapper.PersonalAccount):
                    assert invoice.hidden
                    assert not invoice.receipt_sum
                    if isinstance(invoice, mapper.RepaymentInvoice):
                        for fictive in invoice.fictives:
                            assert fictive.hidden
                            assert not fictive.receipt_sum
                else:
                    assert not invoice.hidden
            else:
                assert any(
                    isinstance(invoice, type_)
                    for type_ in (
                        mapper.PersonalAccount,
                        mapper.YInvoice,
                        mapper.RepaymentInvoice,
                    )
                )
        prev_product_completions = defaultdict(Decimal)
        for service_id, service_completions in prev_actual_completions.items():
            for product_id, completion, order in service_completions:
                prev_product_completions[product_id] += completion
        product_completions = defaultdict(Decimal)
        for service_id, service_completions in actual_completions.items():
            for product_id, completion, order in service_completions:
                product_completions[product_id] += completion
        actual_prev_month_acts = list(
            filter(
                lambda act: not act.hidden and act.dt == prev_act_month.document_dt,
                acts,
            )
        )
        assert all(not act.hidden for act in actual_prev_month_acts)
        actual_month_acts = list(
            filter(lambda act: not act.hidden and act.dt == act_month.document_dt, acts)
        )
        if mode == cst.PartnerActsMode.GENERATE_ONLY:
            assert not new_acts
            assert set((act.id, act.hidden) for act in prev_month_acts) | set(
                (act.id, act.hidden) for act in month_acts
            ) == set((act.id, act.hidden) for act in acts)
            if not with_acts and not with_new_acts:
                assert not acts
            elif not with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        if order.service_id == rp.get_taxi_eff_sid(contract):
                            assert (
                                act_month.document_dt
                                <= order.shipment.dt
                                < act_month.end_dt
                            )
                        else:
                            assert (
                                prev_act_month.document_dt
                                <= order.shipment.dt
                                < prev_act_month.end_dt
                            )
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and not with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert order.shipment.dt == prev_act_month.document_dt
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        if order.service_id == rp.get_taxi_eff_sid(contract):
                            assert (
                                act_month.document_dt
                                <= order.shipment.dt
                                < act_month.end_dt
                            )
                        else:
                            assert (
                                prev_act_month.document_dt
                                <= order.shipment.dt
                                < prev_act_month.end_dt
                            )
                        assert order.shipment.update_dt < checkpoint
            else:
                assert False
        elif mode == cst.PartnerActsMode.HIDE_AND_GENERATE:
            if with_new_acts:
                assert new_acts
            assert all(act.jira_id == input_['reason'] for act in month_acts)
            assert set((act.id, act.hidden) for act in prev_month_acts) | set(
                (act.id, 4) for act in month_acts
            ) | set((act.id, 0) for act in new_acts) == set(
                (act.id, act.hidden) for act in acts
            )
            assert set((act.id, act.hidden) for act in new_acts).issubset(
                set((act.id, act.hidden) for act in acts)
            )
            if not with_acts and not with_new_acts:
                assert not acts
            elif not with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        if order.service_id == rp.get_taxi_eff_sid(contract):
                            assert (
                                act_month.document_dt
                                <= order.shipment.dt
                                < act_month.end_dt
                            )
                            assert order.shipment.update_dt > checkpoint
                        else:
                            assert (
                                prev_act_month.document_dt
                                <= order.shipment.dt
                                < prev_act_month.end_dt
                            )
                            assert order.shipment.update_dt < checkpoint
            elif with_acts and not with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert order.shipment.dt == prev_act_month.document_dt
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == product_completions[order.product.id]
                        )
                        if order.service_id == rp.get_taxi_eff_sid(contract):
                            assert (
                                act_month.document_dt
                                <= order.shipment.dt
                                < act_month.end_dt
                            )
                            assert order.shipment.update_dt > checkpoint
                        else:
                            assert (
                                prev_act_month.document_dt
                                <= order.shipment.dt
                                < prev_act_month.end_dt
                            )
                            assert order.shipment.update_dt < checkpoint
            else:
                assert False
        elif mode == cst.PartnerActsMode.HIDE_ONLY:
            assert not new_acts
            assert all(act.jira_id == input_['reason'] for act in month_acts)
            assert set((act.id, act.hidden) for act in prev_month_acts) | set(
                (act.id, 4) for act in month_acts
            ) == set((act.id, act.hidden) for act in acts)
            if not with_acts and not with_new_acts:
                assert not acts
            elif not with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert not order.shipment.consumption
                        if order.service_id == rp.get_taxi_eff_sid(contract):
                            assert (
                                act_month.document_dt
                                <= order.shipment.dt
                                < act_month.end_dt
                            )
                        else:
                            assert (
                                prev_act_month.document_dt
                                <= order.shipment.dt
                                < prev_act_month.end_dt
                            )
                        assert order.shipment.update_dt > checkpoint
            elif with_acts and not with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        assert order.shipment.dt == prev_act_month.document_dt
                        assert order.shipment.update_dt < checkpoint
            elif with_acts and with_new_acts:
                for act in actual_month_acts:
                    for row in act.rows:
                        order = row.consume.order
                        assert (
                            order.shipment.consumption
                            == prev_product_completions[order.product.id]
                        )
                        if order.service_id == rp.get_taxi_eff_sid(contract):
                            assert (
                                act_month.document_dt
                                <= order.shipment.dt
                                < act_month.end_dt
                            )
                        else:
                            assert (
                                prev_act_month.document_dt
                                <= order.shipment.dt
                                < prev_act_month.end_dt
                            )
                        assert order.shipment.update_dt < checkpoint
            else:
                assert False

        assert shadow_correctness(modular_session, general_acts, non_general_acts)


inputs_debug = [
    # {'for_month': mapper.ActMonth().document_dt},
    # {
    #     'for_month': mapper.ActMonth().document_dt,
    #     'mode': cst.PartnerActsMode.GENERATE_ONLY,
    # },
    # {
    #     'for_month': mapper.ActMonth().document_dt,
    #     'mode': cst.PartnerActsMode.HIDE_AND_GENERATE,
    #     'reason': 'PAYSUP',
    # },
    # {
    #     'for_month': mapper.ActMonth().document_dt,
    #     'mode': cst.PartnerActsMode.HIDE_ONLY,
    #     'reason': 'PAYSUP',
    # },
]


invoice_schemes_debug = [
    # {
    #     'payment_type': cst.PREPAY_PAYMENT_TYPE,
    #     'personal_account': 0
    # },
    # {'payment_type': cst.PREPAY_PAYMENT_TYPE, 'personal_account': 1},
    # {
    #     'payment_type': cst.POSTPAY_PAYMENT_TYPE,
    #     'personal_account': 0,
    #     'partner_credit': 1,
    # },
    # {
    #     'payment_type': cst.POSTPAY_PAYMENT_TYPE,
    #     'personal_account': 1,
    #     'partner_credit': 1,
    # },
    # {
    #     'payment_type': cst.POSTPAY_PAYMENT_TYPE,
    #     'personal_account': 1,
    #     'personal_account_fictive': 1,
    #     'partner_credit': 1,
    # },
]


contracts_debug = [
    # {
    #     'contract_params': {
    #         'ctype': 'GENERAL',
    #         'services': {cst.ServiceId.MEDIA_ADVANCE_PAYMENT: 1},
    #         'dt': mapper.ActMonth(for_month=ut.add_months_to_date(datetime.datetime.today(), -2)).begin_dt,
    #         'is_signed': mapper.ActMonth(for_month=ut.add_months_to_date(datetime.datetime.today(), -2)).begin_dt,
    #         'currency': cst.NUM_CODE_RUR,
    #         'firm': cst.FirmId.MEDIASERVICES,
    #     },
    #     'completions': {
    #         cst.ServiceId.MEDIA_ADVANCE_PAYMENT: [1000]
    #     }
    # },
    # {
    #     'contract_params': {
    #         'ctype': 'GENERAL',
    #         'services': {
    #             cst.ServiceId.TAXI_CORP_DISPATCHING: 1,
    #         },
    #         'dt': mapper.ActMonth(
    #             for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
    #         ).begin_dt,
    #         'is_signed': mapper.ActMonth(
    #             for_month=ut.add_months_to_date(datetime.datetime.today(), -2)
    #         ).begin_dt,
    #         'currency': cst.BYN_NUM_CODE,
    #         'firm': cst.FirmId.BELGO_CORP,
    #         'person': ob.PersonBuilder(type='byu'),
    #     },
    #     'completions': {
    #         cst.ServiceId.TAXI_CORP_DISPATCHING: [1000],
    #     },
    # },
]
