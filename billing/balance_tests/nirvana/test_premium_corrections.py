# -*- coding: utf-8 -*-

"test_processor"

from __future__ import unicode_literals

import mock
import pytest
import json
import datetime as dt
from collections import OrderedDict
from copy import deepcopy

from balance.actions.nirvana.operations import premium_corrections as pc
from balance import mapper
from tests.tutils import mock_transactions
from tests import object_builder as ob


OPTIONS = {
    "reward_type_period": '{"1": [1, 10, 301, 310, 311, 312, 313, 314], "3": [20, 320], "6": [2, 302]}'
}

INPUT_EXAMPLE_JSON = '''[
  {
    "contract_eid": "22194801/1922",
    "contract_id": "648393",
    "currency": "RUR",
    "delkredere_to_pay": null,
    "discount_type": null,
    "dsc": "PAYSUP-738296",
    "from_dt": "2020-12-01 00:00:00",
    "nds": "1",
    "reward_to_charge": null,
    "reward_to_pay": "-250,30",
    "reward_type": "301.70",
    "till_dt": "2020-12-31 00:00:00",
    "turnover_to_charge": null,
    "turnover_to_pay": null,
    "type": "spec_inline",
    "ignore_duplication": null,
    "reward_period": "1"
  }
]'''

INPUT_EXAMPLE = json.loads(INPUT_EXAMPLE_JSON)[0]
for k in pc.MONEY_PARAMS:
    if INPUT_EXAMPLE.get(k):
        INPUT_EXAMPLE[k] = INPUT_EXAMPLE[k].replace(',', '.')

OUTPUTS = {}
INPUTS = {}


@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {
        'input_rows': []
    }
    OUTPUTS = {
        'success_report': [],
        'fail_report': []
    }


def check_outputs(req_output):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        result = set(tuple(sorted(d.iteritems())) for d in list_of_dicts)
        assert len(result) == len(list_of_dicts)
        return result

    assert to_set(OUTPUTS['success_report']) == to_set(req_output['success_report'])
    assert to_set(OUTPUTS['fail_report']) == to_set(req_output['fail_report'])


def patcher(f):
    def output_patch(self, name, data):
        OUTPUTS[name] = json.loads(data)

    patch_funcs = [
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.download',
            lambda nb, name: json.dumps(INPUTS[name], cls=mapper.BalanceJSONEncoder)
        ),
        ('balance.mapper.nirvana_processor.NirvanaBlock.upload', output_patch)
    ]

    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)
    return f


def create_nirvana_block(session, options):
    nb = ob.NirvanaBlockBuilder(
        operation='premium_correction',
        request={
            'data': {
                'options': options,
                'inputs': ['input_rows']
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

def update_options(options, **kwargs):
    for k, v in kwargs.iteritems():
        options[k] = v


@pytest.mark.parametrize('param', pc.NOT_EMPTY_PARAMS)
@patcher
def test_missing_mandatory_params(session, param):
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input[param] = None

    if param == 'contract_eid':
        fail_report = [OrderedDict({'id': 1, 'message': 'Поля %s не должны быть пустыми' % param})]
    else:
        fail_report = [OrderedDict({'id': copy_input['contract_eid'], 'message': 'Поля %s не должны быть пустыми' % param})]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_wrong_nds_value(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['nds'] = '20'

    fail_report = [OrderedDict(
        {
            'id': copy_input['contract_eid'],
            'message': 'Поле nds может иметь значение 0 или 1, получено %s' % copy_input['nds']
        }
    )]


    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)

@patcher
def test_wrong_date_format(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['from_dt'] = '22-11-33'

    fail_report = [OrderedDict({'id': copy_input['contract_eid'], 'message': 'Проверь формат даты в столбце from_dt'})]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)

@patcher
def test_contract_id_not_found_in_db(session):
    fail_report = [
        OrderedDict(
            {
                'id': INPUT_EXAMPLE['contract_eid'],
                'message': 'Не удалось найти договор'
            }
        )
    ]
    INPUTS['input_rows'] = [INPUT_EXAMPLE]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report,
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)

@patcher
def test_external_id_not_found_in_db(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['contract_id'] = None

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': 'Не удалось найти договор'
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report,
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_no_active_contract_by_external_id(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['contract_id'] = None

    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])

    contract2 = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': 'Не удалось найти акивный договор на дату 2020-12-01 00:00:00'
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_several_contracts_by_external_id(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['contract_id'] = None

    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])

    contract2 = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])

    for c in [contract, contract2]:
        c.col0.dt = dt.datetime(2020, 12, 1)
        c.col0.is_faxed = dt.datetime(2020, 12, 1)

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': 'Найдено несколько договоров'
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_missing_all_money_params(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    for k in pc.MONEY_PARAMS:
        copy_input[k] = None
    copy_input['contract_id'] = '%s' % contract.id
    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': u'Не заполнено ни одно поле из: %s' % ', '.join(pc.MONEY_PARAMS)
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_wrong_input_currency(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id

    contract.col0.currency = 840

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': u'Валюта договора USD и валюта строки в файле RUR не совпадают'
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_no_current_signed(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    contract.col0.is_signed = None
    contract.col0.is_faxed = None
    copy_input['contract_id'] = None

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': u'Не удалось найти подписанный договор'
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_expected_field_missing(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input.pop('turnover_to_charge')
    copy_input['contract_id'] = '%s' % contract.id

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': u"(sqlalchemy.exc.InvalidRequestError) A value is required for bind parameter u'turnover_to_charge'"
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_too_long_number(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['turnover_to_charge'] = '100000000100000000100000000100000000100000000100000000100000000100000000'
    copy_input['contract_id'] = '%s' % contract.id

    fail_report = [
        OrderedDict(
            {
                'id': copy_input['contract_eid'],
                'message': u'(cx_Oracle.DatabaseError) DPI-1044: value cannot be represented as an Oracle number'
            }
        )
    ]

    INPUTS['input_rows'] = [copy_input]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_duplicate_commission_correction(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id
    for k, v in copy_input.items():
        if v == None:
            copy_input[k] = None
    copy_input['delkredere_to_pay'] = 0
    copy_input2 = deepcopy(INPUT_EXAMPLE)
    copy_input2['contract_id'] = '%s' % contract.id
    INPUTS['input_rows'] = [copy_input2]

    required_outputs = {
        'success_report': [],
        'fail_report': [
            OrderedDict(
                {
                    'id': copy_input['contract_eid'],
                    'message': u'Указанная коррректировка для данного договора уже выполнена'
                }
            )]
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        session.execute(pc.INSERT_QUERY, copy_input)
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_ignore_duplication(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id
    for k, v in copy_input.items():
        if v == None:
            copy_input[k] = None
    copy_input['delkredere_to_pay'] = 0
    copy_input2 = deepcopy(INPUT_EXAMPLE)
    copy_input2['contract_id'] = '%s' % contract.id
    copy_input2['ignore_duplication'] = '1'
    INPUTS['input_rows'] = [copy_input2]

    required_outputs = {
        'success_report': [{'Contract_eid': copy_input2['contract_eid']}],
        'fail_report': []
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        session.execute(pc.INSERT_QUERY, copy_input)
        res = pc.process(nb)

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_duplicate_rows_in_input_json(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id
    INPUTS['input_rows'] = [copy_input]*2

    success_report = [{'Contract_eid': contract.external_id}]
    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)
        res_row = session.execute(
            'select 1 from bo.t_commission_correction where contract_id = %s' % contract.id).fetchone()
        assert res_row

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_delkredere_to_pay(session):
    copy_input = deepcopy(INPUT_EXAMPLE)
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id
    copy_input['delkredere_to_pay'] = '10'

    INPUTS['input_rows'] = [copy_input]

    success_report = [{'Contract_eid': contract.external_id}]
    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)
        res_row = session.execute(
            '''
            select
                1
            from bo.t_commission_correction
            where contract_id = {contract_id}
                and reward_to_pay = {reward_to_pay}
                and delkredere_to_pay = {delkredere_to_pay}'''.format(
                contract_id=copy_input['contract_id'],
                reward_to_pay=copy_input['reward_to_pay'],
                delkredere_to_pay=copy_input['delkredere_to_pay']
            )
        ).fetchone()
        assert res_row

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
def test_several_success_several_not_success(session):
    def get_data(field):
        copy_input = deepcopy(INPUT_EXAMPLE)
        for p in pc.MONEY_PARAMS:
            copy_input[p] = None
        copy_input['contract_eid'] = field + '/test'
        contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                                external_id=copy_input['contract_eid'])
        copy_input['contract_id'] = '%s' % contract.id
        copy_input[field] = '100'
        return copy_input

    valid_rows = [{'reward_to_charge': get_data('reward_to_charge')}, {'turnover_to_pay': get_data('turnover_to_pay')}]

    wo_from_dt = deepcopy(INPUT_EXAMPLE)
    wo_from_dt['from_dt'] = None
    wo_from_dt['contract_eid'] = 'wo/from_dt'

    wo_eid = deepcopy(INPUT_EXAMPLE)
    for p in ['contract_id', 'contract_eid']:
        wo_eid[p] = None

    INPUTS['input_rows'] = [
        wo_from_dt,
        valid_rows[0].values()[0],
        wo_eid,
        valid_rows[1].values()[0]
                            ]
    required_outputs = {
        'success_report': [{'Contract_eid': row.values()[0]['contract_eid']} for row in valid_rows],
        'fail_report': [
            OrderedDict(
                {'id': wo_from_dt['contract_eid'], 'message': 'Поля from_dt не должны быть пустыми'}
            ),
            OrderedDict(
                {'id': 3, 'message': 'Поля contract_eid не должны быть пустыми'}
                        )
        ]
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)
        for row in valid_rows:
            res_row = session.execute(
                '''
                select
                    1
                from bo.t_commission_correction
                where contract_id = {contract_id}
                    and {field_} = 100
                    and delkredere_to_pay = {delkredere_to_pay}'''.format(
                    contract_id=row.values()[0]['contract_id'],
                    field_=row.keys()[0],
                    delkredere_to_pay=0
                )
                ).fetchone()
            assert res_row

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
@pytest.mark.parametrize('reward_type', ['10', '20', '2'])
def test_different_reward_period(session, reward_type):
    mapping = {
        '10': '1',
        '20': '3',
        '2': '6'
    }
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['reward_type'] = reward_type
    copy_input['current_reward_period'] = mapping[reward_type]
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id

    INPUTS['input_rows'] = [copy_input]

    success_report = [{'Contract_eid': contract.external_id}]
    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    nb = create_nirvana_block(session, OPTIONS)

    with mock_transactions():
        res = pc.process(nb)
        res_row = session.execute(
            '''
            select
                1
            from bo.t_commission_correction
            where contract_id = {contract_id}
                and reward_to_pay = {reward_to_pay}
                and reward_type = {reward_type}
                and till_dt = add_months(trunc(to_date('{from_dt}', 'YYYY-MM-DD HH24:MI:SS'), 'mm'), {current_reward_period}) - 1/24/60/60
            '''.format(
                contract_id=copy_input['contract_id'],
                reward_to_pay=copy_input['reward_to_pay'],
                reward_type=copy_input['reward_type'],
                from_dt=copy_input['from_dt'],
                current_reward_period=copy_input['current_reward_period']
            )
        ).fetchone()
        assert res_row

    assert res.is_finished()

    check_outputs(required_outputs)


@patcher
@pytest.mark.parametrize('reward_type', ['10', '20', '2'])
def test_different_reward_period_wo_option(session, reward_type):
    mapping = {
        '10': '1',
        '20': '3',
        '2': '6'
    }
    copy_input = deepcopy(INPUT_EXAMPLE)
    copy_input['reward_type'] = reward_type
    copy_input['current_reward_period'] = mapping[reward_type]
    contract = ob.ContractBuilder.construct(session, ctype='GENERAL',
                                            external_id=copy_input['contract_eid'])
    copy_input['contract_id'] = '%s' % contract.id

    INPUTS['input_rows'] = [copy_input]

    success_report = [{'Contract_eid': contract.external_id}]
    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    nb = create_nirvana_block(session, {})

    with mock_transactions():
        res = pc.process(nb)
        res_row = session.execute(
            '''
            select
                1
            from bo.t_commission_correction
            where contract_id = {contract_id}
                and reward_to_pay = {reward_to_pay}
                and reward_type = {reward_type}
                and till_dt = add_months(trunc(to_date('{from_dt}', 'YYYY-MM-DD HH24:MI:SS'), 'mm'), {current_reward_period}) - 1/24/60/60
            '''.format(
                contract_id=copy_input['contract_id'],
                reward_to_pay=copy_input['reward_to_pay'],
                reward_type=copy_input['reward_type'],
                from_dt=copy_input['from_dt'],
                current_reward_period=copy_input['current_reward_period']
            )
        ).fetchone()
        assert res_row

    assert res.is_finished()

    check_outputs(required_outputs)
