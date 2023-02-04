# -*- coding: utf-8 -*-

"test_processor"

from __future__ import unicode_literals

import mock
import pytest
import json
import random
import datetime as dt

from billing.contract_iface.cmeta import general, distribution, partners, spendable
from balance.actions.nirvana.operations import multiple_changes as mc
from balance.actions.nirvana.operations.multiple_changes.exceptions import *
from balance import mapper
from tests.test_application import ApplicationForTests
from tests.tutils import mock_transactions
from tests import object_builder as ob


OUTPUTS = {}
INPUTS = {}

mc.batch_size = 2

CONTRACT_ERROR_TEMPLATE = "\nAttribute '%(attr)s' not allowed for contract_type " \
                  "'%(c_type)s'"

COLLATERAL_ERROR_TEMPLATE = "\nAttribute '%(attr)s' not allowed for collateral_type " \
                   "'%(cc_type)s'"


@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {
        'input_rows': []
    }
    OUTPUTS = {
        'success_report': [],
        'fail_report': [],
        'exported_objects': []
    }


@pytest.fixture()
def checks():
    return {
        'contractruleschecker': True,
    }


@pytest.fixture()
def collateral_required_attrs():
    return {
        'processor': 'create_collaterals',
        'dt': dt.datetime.now().strftime('%Y-%m-%d'),
        'memo': 'создано по задаче PAYSUP-666',
    }


def check_outputs(req_output):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        result = set(tuple(sorted(d.iteritems())) for d in list_of_dicts)
        assert len(result) == len(list_of_dicts)
        return result

    def rm_from_report(dict_list, name):
        return [{k: v for k, v in d.iteritems() if k != name}
                for d in dict_list]

    assert to_set(rm_from_report(OUTPUTS['success_report'], 'collateral_id')) == \
           to_set(req_output['success_report'])
    assert to_set(rm_from_report(OUTPUTS['fail_report'], 'traceback')) == \
           to_set(req_output['fail_report'])
    assert to_set(OUTPUTS['exported_objects']) == \
           to_set(req_output['exported_objects'])


def patcher(f):
    def output_patch(self, name, data):
        OUTPUTS[name] = json.loads(data)

    patch_funcs = [
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.download',
            lambda nb, name: json.dumps(INPUTS[name], cls=mapper.BalanceJSONEncoder)
        ),
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.upload',
            output_patch
        ),
    ]

    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)
    return f


def create_nirvana_block(session, options):
    nb = ob.NirvanaBlockBuilder(
        operation='create_collaterals',
        request={
            'data': {
                'options': options,
                'inputs': {}
            },
            'context': {
                'workflow': {
                    'owner': 'autodasha'
                }
            }
        }
    ).build(session).obj

    return nb


def get_contract(session, ctype, **params):
    default_params = {k: v for k, v in params.iteritems()}
    external_id = default_params.pop('external_id', None)

    if ctype == 'PARTNERS':
        default_params['client'] = ob.ClientBuilder()
        default_params['firm'] = 1
        default_params['person'] = ob.PersonBuilder(client=default_params['client'],
                                                    type='ur')
    elif ctype == 'DISTRIBUTION':
        default_params['client'] = ob.ClientBuilder()
        default_params['person'] = ob.PersonBuilder(client=default_params['client'],
                                                    type='sw_yt')
        default_params['distribution_tag'] = 7073
        default_params['distribution_places'] = 1
        default_params['products_revshare'] = {
            10000: {
                'value_num': 0,
                'value_str': None
            }
        }
        default_params['service_start_dt'] = dt.datetime.now()
    elif ctype == 'SPENDABLE':
        default_params['services'] = {42: 1}

    return ob.ContractBuilder.construct(session, ctype=ctype,
                                        external_id=external_id if external_id
                                        else '%s/test' % ctype, **default_params)


def get_default_options(c_type, pf_type, add_sequence_number):
    pf_type_map = {
        0: '',
        1: u'У',
        2: u'П',
        3: u'Ф'
    }
    options = {}

    if c_type != 'PARTNERS':
        options['print_template'] = '/path/to/print/template'

    if c_type in ('GENERAL', 'DISTRIBUTION'):
        col_name = pf_type_map.get(pf_type, None) if add_sequence_number else pf_type_map.get(
            pf_type, '') + '-б/н'
        options['print_form_type'] = pf_type
        options['col_name'] = col_name
        options['add_sequence_number'] = add_sequence_number
        if c_type == 'GENERAL':
            options['tickets'] = 'PAYSUP-666'

        if pf_type in (1, 3):
            options['is_signed'] = True
        else:
            options['is_booked'] = True

    return options


def get_default_failed_options(c_type):
    options = {}
    if c_type == 'PARTNERS':
        options['print_template'] = '/path/to/print/template'
    options['print_form_type'] = 0
    options['col_name'] = 'ХЗ'
    options['tickets'] = 'PAYSUP-666\nIWILLBREAKYOU-1985'

    return options


def update_options(options, **kwargs):
    for k, v in kwargs.iteritems():
        options[k] = v


@patcher
def test_contract_type_parse_failed(session):
    options = {'processor': 'create_collaterals'}

    input_rows = [{'external_id': '666666/13'}]
    INPUTS['input_rows'] = input_rows

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(AttributeIsRequired):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@patcher
def test_contract_select_failed(session, collateral_required_attrs, c_type):
    input_rows = []
    fail_report = []
    options = get_default_options(c_type, 0, random.choice([True, False]))
    update_options(options, contract_type=c_type, collateral_type='прочее',
                   **collateral_required_attrs)

    input_rows.append({'id': 666})
    fail_report.append({'client_id': None, 'person_id': None, 'contract_id': None,
                        'contract': None, 'error':
                            "Couldn't find entity of type: "
                            "'contract' with identifiers: 666"})

    input_rows.append({'external_id': '%s/NOT_FOUND' % c_type})
    input_rows.append({'external_id': '%s/DOUBLE' % c_type})

    get_contract(session, c_type, external_id='%s/DOUBLE' % c_type)
    get_contract(session, c_type, external_id='%s/DOUBLE' % c_type)

    fail_report.append({'client_id': None, 'person_id': None, 'contract_id': None,
                        'contract': None, 'error':
                            "Couldn't find entity of type: "
                            "'contract' with identifiers: %s/NOT_FOUND" % c_type})
    fail_report.append({'client_id': None, 'person_id': None, 'contract_id': None,
                        'contract': None, 'error':
                            "Found multiple entities of type: "
                            "'contract' with identifiers: %s/DOUBLE" % c_type})

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report,
        'exported_objects': []
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@patcher
def test_required_attrs_failed(session, c_type, checks):
    options = {}
    update_options(options, contract_type=c_type, processor='create_collaterals',
                   **checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@patcher
def test_contract_type_attrs_check(session, c_type, collateral_required_attrs):
    options = get_default_failed_options(c_type)
    update_options(options, contract_type=c_type, collateral_type='прочее',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   add_services=[35], remove_services=[7], withdraw_on_demand=True,
                   wholesale_agent_premium_awards_scale_type=28, nds=0,
                   **collateral_required_attrs)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_other_check_failed(session, c_type, collateral_required_attrs,
                                     pf_type):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type, collateral_type='прочее',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)
    if c_type == 'GENERAL':
        update_options(
            options, add_services=[35], remove_services=[7], withdraw_on_demand=True,
            wholesale_agent_premium_awards_scale_type=28
        )
    else:
        update_options(options, nds=0)
        if c_type == 'SPENDABLE':
            update_options(options, withdraw_on_demand=True)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_other_success(session, c_type, pf_type, add_sequence_number,
                                collateral_required_attrs, checks):
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type, collateral_type='прочее',
                   **collateral_required_attrs)
    update_options(options, **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    passport = session.query(mapper.Passport).getone(login='autodasha')
    cc = contract.collaterals[-1]
    assert cc.passport_id == passport.passport_id
    for attr in cc.attributes:
        assert attr.passport_id == passport.passport_id

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_cancellation_options_failed(session, c_type, pf_type,
                                              collateral_required_attrs):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type,
                   collateral_type='расторжение договора', **collateral_required_attrs)
    if c_type == 'GENERAL':
        update_options(options, add_services=[35], remove_services=[7],
                       withdraw_on_demand=True,
                       wholesale_agent_premium_awards_scale_type=28)
    else:
        update_options(options, nds=0)
        if c_type == 'SPENDABLE':
            update_options(options, withdraw_on_demand=True)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_cancellation_success(session, c_type, pf_type, add_sequence_number,
                                       collateral_required_attrs, checks):
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type,
                   collateral_type='расторжение договора',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)
    update_options(options, **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_prolongation_check_failed(session, c_type, collateral_required_attrs,
                                            pf_type):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type, collateral_type='продление договора',
                   **collateral_required_attrs)

    if c_type == 'GENERAL':
        update_options(options, add_services=[35], remove_services=[7],
                       withdraw_on_demand=True,
                       wholesale_agent_premium_awards_scale_type=0)
    else:
        update_options(options, nds=0)
        if c_type == 'SPENDABLE':
            update_options(options, withdraw_on_demand=True)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_prolongation_success(session, c_type, pf_type, add_sequence_number,
                                       collateral_required_attrs, checks):
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type, collateral_type='продление договора',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)
    update_options(options, **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_withdraw_on_demand_check_failed(session, c_type, pf_type,
                                                  collateral_required_attrs):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type,
                   collateral_type='изменение способа вывода денег',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)

    if c_type == 'GENERAL':
        update_options(
            options, add_services=[35], remove_services=[7],
            wholesale_agent_premium_awards_scale_type=28
        )
    else:
        update_options(options, nds=0)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_withdraw_on_demand_success(session, c_type, pf_type, add_sequence_number,
                                             collateral_required_attrs, checks):
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение способа вывода денег',
                   withdraw_on_demand=1, **collateral_required_attrs)
    update_options(options, **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_premium_awards_scale_type_check_failed(session, c_type, pf_type,
                                                         collateral_required_attrs):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type,
                   collateral_type='изменение шкалы премий',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   add_services=[35], remove_services=[7], withdraw_on_demand=True,
                   **collateral_required_attrs)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['GENERAL'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_premium_awards_scale_type_success(session, c_type, pf_type,
                                                    add_sequence_number, checks,
                                                    collateral_required_attrs):
    contract = get_contract(session, c_type, commission=61)
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение шкалы премий',
                   wholesale_agent_premium_awards_scale_type=28,
                   **collateral_required_attrs)
    update_options(options, **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_partners_end_announcement(session, collateral_required_attrs):
    c_type = 'PARTNERS'
    contract = get_contract(session, c_type)
    options = {}
    update_options(options, contract_type=c_type,
                   collateral_type='о закрытии договора',
                   end_reason=1, col_name='У-01',
                   **collateral_required_attrs)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref,
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_netting(session, collateral_required_attrs):
    c_type = 'GENERAL'
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, 1, True)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение взаимозачета',
                   netting=1, netting_pct=100,
                   **collateral_required_attrs)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref + '-01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_commission_pct(session, collateral_required_attrs):
    c_type = 'GENERAL'
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, 1, True)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение процента комиссии с карт',
                   partner_commission_pct2=0.1,
                   **collateral_required_attrs)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref + '-01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_revshare(session, collateral_required_attrs):
    c_type = 'DISTRIBUTION'
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, 1, True)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение условий УДД/ГД',
                   add_supplements=[1, 2],
                   add_products_revshare=[
                     '10003 NET-711_market_RS_3rd',
                     '10004 100',
                   ],
                   remove_products_revshare=[10000],
                   partner_resources='What even is billing?',
                   distribution_products='Dunno',
                   tail_time=1,
                   fixed_scale=0,
                   products_currency=643,
                   **collateral_required_attrs)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref + '-01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_revshare_key_fail(session, collateral_required_attrs):
    c_type = 'DISTRIBUTION'
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, 1, True)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение условий УДД/ГД',
                   add_supplements=[1, 2],
                   add_products_revshare=[
                     'kek NET-711_market_RS_3rd',
                     '10004 100',
                   ],
                   remove_products_revshare=[10000],
                   partner_resources='What even is billing?',
                   distribution_products='Dunno',
                   tail_time=1,
                   fixed_scale=0,
                   products_currency=643,
                   **collateral_required_attrs)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    error = 'Options errors: Invalid value for key: kek'

    required_outputs = {
        'success_report': [],
        'fail_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'error': error
            }
        ],
        'exported_objects': [],
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_revshare_remove_fail(session, collateral_required_attrs):
    c_type = 'DISTRIBUTION'
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, 1, True)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение условий УДД/ГД',
                   add_supplements=[1, 2],
                   add_products_revshare=[
                     'kek NET-711_market_RS_3rd',
                     '10004 100',
                   ],
                   remove_products_revshare=[666666],
                   partner_resources='What even is billing?',
                   distribution_products='Dunno',
                   tail_time=1,
                   fixed_scale=0,
                   products_currency=643,
                   **collateral_required_attrs)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    error = 'Options errors: Key 666666 is not present in products_revshare'

    required_outputs = {
        'success_report': [],
        'fail_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'error': error
            }
        ],
        'exported_objects': [],
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_services_check_failed(session, c_type, collateral_required_attrs,
                                        pf_type):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type, collateral_type='изменение сервисов',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)
    if c_type == 'GENERAL':
        update_options(options, withdraw_on_demand=True,
                       wholesale_agent_premium_awards_scale_type=28)
    else:
        update_options(options, nds=0)
        if c_type == 'SPENDABLE':
            update_options(options, withdraw_on_demand=True)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize(
    'add_services, remove_services',
    [
        ([35], None),
        (None, [7]),
        ([35, 37], [7, 77])
    ]
)
@pytest.mark.parametrize('c_type', ['GENERAL'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_services_success(session, c_type, pf_type, add_sequence_number,
                                   collateral_required_attrs, checks,
                                   add_services, remove_services):
    contract = get_contract(session, c_type, services={7: 1, 70: 1, 77: 1})
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type, collateral_type='изменение сервисов',
                   add_services=add_services, remove_services=remove_services,
                   **collateral_required_attrs)
    update_options(options, **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt': dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['DISTRIBUTION', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@patcher
def test_contract_nds_check_failed(session, c_type, collateral_required_attrs,
                                   pf_type):
    options = get_default_options(c_type, pf_type, random.choice([True, False]))
    update_options(options, contract_type=c_type,
                   collateral_type='изменение налогообложения',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)

    if c_type == 'SPENDABLE':
        update_options(options, withdraw_on_demand=True)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        with pytest.raises(OptionsErrors):
            mc.process(nb)


@pytest.mark.parametrize('c_type', ['DISTRIBUTION', 'SPENDABLE'])
@pytest.mark.parametrize('pf_type', [0, 1, 2, 3])
@pytest.mark.parametrize('add_sequence_number', [True, False])
@patcher
def test_contract_nds_success(session, c_type, pf_type, add_sequence_number,
                              collateral_required_attrs, checks):
    contract = get_contract(session, c_type, nds=0,
                            dt=dt.datetime.today().replace(day=1) - dt.timedelta(days=1)
                            )
    options = get_default_options(c_type, pf_type, add_sequence_number)
    update_options(options, contract_type=c_type,
                   collateral_type='изменение налогообложения', nds=18,
                   **collateral_required_attrs)
    update_options(options, dt=dt.datetime.today().replace(day=1).strftime('%Y-%m-%d'),
                   **checks)
    col_pref = options.get('col_name', None)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': col_pref
                if col_pref and not add_sequence_number
                else '%s-01' % col_pref if col_pref and add_sequence_number
                else '01',
                'collateral_dt':
                    dt.datetime.now().replace(day=1).strftime('%Y-%m-%dT00:00:00')
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@patcher
def test_before_checks_wrong_contract_type(session, c_type, collateral_required_attrs):
    contract = get_contract(session, c_type)

    if c_type == 'GENERAL':
        options = get_default_options('SPENDABLE', 0, random.choice([True, False]))
        update_options(options, contract_type='SPENDABLE')
    else:
        options = get_default_options('GENERAL', 0, random.choice([True, False]))
        update_options(options, contract_type='GENERAL')

    update_options(options, collateral_type='прочее', **collateral_required_attrs)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    error = 'Check errors: Wrong contract_type'

    required_outputs = {
        'success_report': [],
        'fail_report': [{'client_id': contract.client_id,
                         'person_id': contract.person_id,
                         'contract_id': contract.id,
                         'contract': contract.external_id,
                         'error': error}],
        'exported_objects': []
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('c_type', ['GENERAL', 'PARTNERS', 'DISTRIBUTION', 'SPENDABLE'])
@patcher
def test_before_checks_same_col_in_future(session, c_type, collateral_required_attrs):
    contract = get_contract(session, c_type)
    options = get_default_options(c_type, 0, random.choice([True, False]))
    options.update(collateral_required_attrs)
    update_options(options, contract_type=c_type,
                   collateral_type='расторжение договора',
                   end_dt=dt.datetime.now().strftime('%Y-%m-%d'),
                   **collateral_required_attrs)

    col_type_map = {
        'GENERAL': general.collateral_types[90],
        'PARTNERS': partners.collateral_types[2050],
        'DISTRIBUTION': distribution.collateral_types[3060],
        'SPENDABLE': spendable.collateral_types[7050]
    }

    col = ob.CollateralBuilder.construct(
        session,
        contract=contract,
        collateral_type=col_type_map[c_type],
        num='01',
        dt=dt.datetime.now() + dt.timedelta(days=10),
        finish_dt=dt.datetime.now() + dt.timedelta(days=10)
    )

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    error = 'Check errors: Got collateral with same type in the future'

    required_outputs = {
        'success_report': [],
        'fail_report': [{'client_id': contract.client_id,
                         'person_id': contract.person_id,
                         'contract_id': contract.id,
                         'contract': contract.external_id,
                         'error': error}],
        'exported_objects': []
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_after_checks_rules_failed(session, checks, collateral_required_attrs):
    contract = get_contract(session, 'GENERAL')
    options = {}
    update_options(options, contract_type='GENERAL', collateral_type='прочее',
                   add_sequence_number=False, print_form_type=1,
                   col_name='Я-сломаю-рулзы', tickets='PAYSUP-666',
                   **collateral_required_attrs)
    update_options(options, **checks)

    error = u'Check errors: Номер допсоглашения должен начинаться с У'

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [],
        'fail_report': [{'client_id': contract.client_id,
                         'person_id': contract.person_id,
                         'contract_id': contract.id,
                         'contract': contract.external_id,
                         'error': error}],
        'exported_objects': []
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_not_active_on_col_dt(session, collateral_required_attrs):
    contract = get_contract(session, 'GENERAL')  # Created with finish_dt = now + 10

    options = {}
    update_options(options, contract_type='GENERAL', collateral_type='расторжение договора',
                   add_sequence_number=False, print_form_type=1,
                   col_name='Сегодня я никого не расторгну', tickets='PAYSUP-666',
                   **collateral_required_attrs)
    finish_dt = (dt.datetime.now() + dt.timedelta(20)).strftime('%Y-%m-%d')
    update_options(options, dt=finish_dt, finish_dt=finish_dt)

    error = u'Contract %(contract)s is not active on collateral date %(col_dt)s' \
            % dict(contract=contract.external_id, col_dt=dt.datetime.strptime(finish_dt, '%Y-%m-%d'))

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [],
        'fail_report': [{'client_id': None,
                         'person_id': None,
                         'contract_id': None,
                         'contract': None,
                         'error': error}],
        'exported_objects': []
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_contract_not_active_on_col_dt_allowed(session, collateral_required_attrs):
    contract = get_contract(session, 'GENERAL')  # Created with finish_dt = now + 10

    options = {}
    update_options(options, contract_type='GENERAL', collateral_type='расторжение договора',
                   add_sequence_number=False, print_form_type=1,
                   col_name='Сегодня я никого не расторгну', tickets='PAYSUP-666',
                   **collateral_required_attrs)
    finish_dt = (dt.datetime.now() + dt.timedelta(20)).strftime('%Y-%m-%d')
    update_options(options, dt=finish_dt,
                   finish_dt=finish_dt, check_active_contract=False)

    input_rows = [{'external_id': contract.external_id}]
    INPUTS['input_rows'] = input_rows
    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': options['col_name'],
                'collateral_dt': dt.datetime.strptime(finish_dt, '%Y-%m-%d').isoformat()
            }
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            }
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_option_override(session, collateral_required_attrs):
    contract = get_contract(session, 'GENERAL')
    contract2 = get_contract(session, 'GENERAL')
    contract3 = get_contract(session, 'GENERAL')
    options = {}
    update_options(options, contract_type='GENERAL', collateral_type='прочее',
                   add_sequence_number=False, print_form_type=1,
                   col_name='Никто не узнает, что я гей', tickets='PAYSUP-666',
                   **collateral_required_attrs)

    input_rows = [
        {
            'id': contract.id,
            'col_name': 'Я важнее'
        },
        {
            'id': contract2.id,
            'col_name': '01'
        },
        {
            'id': contract3.id,
        },
    ]
    INPUTS['input_rows'] = input_rows
    required_outputs = {
        'success_report': [
            {
                'client_id': contract.client_id,
                'person_id': contract.person_id,
                'contract_id': contract.id,
                'contract': contract.external_id,
                'collateral': input_rows[0]['col_name'],
                'collateral_dt':
                    dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            },
            {
                'client_id': contract2.client_id,
                'person_id': contract2.person_id,
                'contract_id': contract2.id,
                'contract': contract2.external_id,
                'collateral': input_rows[1]['col_name'],
                'collateral_dt':
                    dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            },
            {
                'client_id': contract3.client_id,
                'person_id': contract3.person_id,
                'contract_id': contract3.id,
                'contract': contract3.external_id,
                'collateral': options['col_name'],
                'collateral_dt':
                    dt.datetime.now().strftime('%Y-%m-%dT00:00:00')
            },
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': contract.id,
                'queue': 'OEBS_API'
            },
            {
                'classname': 'Contract',
                'object_id': contract2.id,
                'queue': 'OEBS_API'
            },
            {
                'classname': 'Contract',
                'object_id': contract3.id,
                'queue': 'OEBS_API'
            },
        ]
    }

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)
