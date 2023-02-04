# -*- coding: utf-8 -*-

import mock
import pytest
import json
import datetime as dt
import os

from balance.actions.nirvana.operations import multiple_changes as mc
from balance import mapper
from billing.contract_iface.cmeta import general, distribution
from butils.datetools import trunc_date
from tests.test_application import ApplicationForTests
from tests.tutils import mock_transactions
from tests import object_builder as ob
from tests.balance_tests.nirvana.test_multiple_changes import change_attributes

path_to_config = os.path.dirname(change_attributes.__file__) + '/test_config.json'
with open(path_to_config, 'r') as f:
    CONFIG = json.load(f)
OUTPUTS = {}
INPUTS = {}

mc.batch_size = 2


class State(object):
    def __init__(self, obj, **kwargs):
        self.obj = obj
        self.params = {}

        for attr, val in kwargs.iteritems():
            self.set(attr, val)

    def __getattr__(self, item):
        return self.params[item]

    def __setattr__(self, key, value):
        if key not in {'obj', 'params'}:
            self.set(key, value)
        else:
            super(State, self).__setattr__(key, value)

    def set(self, key, value):
        try:
            self.params[key] = (getattr(self.obj, key), value)
        except AttributeError:
            self.params[key] = (getattr(self.obj.col0, key), value)


def create_nirvana_block(session, options):
    nb = ob.NirvanaBlockBuilder(
        operation='multiple_changes',
        request={
            'data': {
                'options': options,
                'inputs': {
                    'input_config': 'lovenirvana.com',
                }
            },
            'context': {
                'workflow': {
                    'owner': 'autodasha'
                }
            }
        }
    ).build(session).obj

    return nb


def check_outputs(required_outputs):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        result = set(tuple(sorted(d.iteritems())) for d in list_of_dicts)
        assert len(result) == len(list_of_dicts)
        return result

    def exclude_errors(list_of_dicts):
        return [{key: val for key, val in d.iteritems() if key not in {'error', 'traceback'}} for d in list_of_dicts]

    assert to_set(OUTPUTS['success_report']) == to_set(required_outputs['success_report'])
    assert to_set(exclude_errors(OUTPUTS['fail_report'])) == to_set(required_outputs['fail_report'])
    assert to_set(OUTPUTS['exported_objects']) == to_set(required_outputs['exported_objects'])


@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {
        'input_rows': [],
        'input_config': CONFIG
    }
    OUTPUTS = {
        'success_report': [],
        'fail_report': [],
        'exported_objects': []
    }


@pytest.fixture()
def checks():
    return {
        'ContractNotCancelledChecker': True,
        'ContractActiveChecker': True,
        'CollateralNotCancelledChecker': True,
        'NoConflictingCollateralsChecker': True,
    }


def patches(f):
    def output_patch(self, name, data):
        OUTPUTS[name] = json.loads(data)

    patch_funcs = [
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.download',
            lambda nb, name: json.dumps(INPUTS[name], cls=mapper.BalanceJSONEncoder)
        ),
        ('balance.mapper.nirvana_processor.NirvanaBlock.upload', output_patch),

        # Not patching actual export types of objects because of current testing problems with AutoExport+OEBS_API
        # Objects will be enqueued to OEBS, but EntityProcessor will report OEBS_API
        (
            'balance.actions.nirvana.operations.multiple_changes.change_attributes.processor.AbstractProcessor.queue',
            mock.PropertyMock(return_value='OEBS_API')
        ),
        (
            'balance.actions.nirvana.operations.multiple_changes.change_attributes.processor.CollateralProcessor.contract_export_type',
            mock.PropertyMock(return_value='OEBS_API')
        ),
    ]

    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)
    return f


@patches
def test_empty_row(session, checks):
    contract_states = [
        State(ob.ContractBuilder.construct(session, external_id=eid), external_id='LOL/' + eid)
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]
    input_rows = [
        {'contract.external_id': cs.external_id[0], 'external_id': cs.external_id[1]}
        for cs in contract_states
    ]
    input_rows.insert(1, {'contract.external_id': None, 'external_id': None})

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': c.obj.id,
                'contract.client_id': c.obj.client_id,
                'external_id_old': c.external_id[0],
                'external_id': c.external_id[1],
                'error': None,
                'traceback': None
            }
            for c in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    for c in contract_states:
        assert c.obj.external_id == c.external_id[1]


@patches
def test_empty_id(session, checks):
    contract_states = [
        State(ob.ContractBuilder.construct(session, external_id=eid), external_id='LOL/' + eid)
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]
    input_rows = [
        {'contract.external_id': cs.external_id[0], 'external_id': cs.external_id[1]}
        for cs in contract_states
    ]
    input_rows[1].update({'contract.external_id': None, 'contract.id': contract_states[1].obj.id})

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': c.obj.id,
                'contract.client_id': c.obj.client_id,
                'external_id_old': c.external_id[0],
                'external_id': c.external_id[1],
                'error': None,
                'traceback': None
            }
            for c in [contract_states[0], contract_states[2]]
        ],
        'fail_report': [
            {
                'classname': 'contract',
                'success': False,
                'id': None,
            }
            for _ in [contract_states[1]]
        ],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in [contract_states[0], contract_states[2]]
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    assert 'cannot be null' in OUTPUTS['fail_report'][0]['error']

    passport = session.query(mapper.Passport).getone(login='autodasha')

    for c in [contract_states[0], contract_states[2]]:
        assert c.obj.external_id == c.external_id[1]
        assert c.obj.passport_id == passport.passport_id


@patches
def test_change_eid(session, checks):
    contract_states = [
        State(ob.ContractBuilder.construct(session, external_id=eid), external_id='LOL/'+eid)
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]
    input_rows = [
        {'contract.external_id': cs.external_id[0], 'external_id': cs.external_id[1]}
        for cs in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': c.obj.id,
                'contract.client_id': c.obj.client_id,
                'external_id_old': c.external_id[0],
                'external_id': c.external_id[1],
                'error': None,
                'traceback': None
            }
            for c in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    for c in contract_states:
        assert c.obj.external_id == c.external_id[1]


@patches
def test_change_zero_col(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                bank_details_id=510,
                commission=9,
                country=225,
                credit_type=1,
                currency=810,
                firm=13,
                netting=1,
                netting_pct=100,
                nds_for_receipt=-1,
                offer_confirmation_type='min-payment',
                partner_commission_type=2,
                payment_term=15,
                payment_type=3,
                is_deactivated=0,
                print_form_dt=None,
                services=[
                    605,
                    124,
                    111,
                    125,
                    128
                    ]
            ),
            offer_confirmation_type='no',
            is_deactivated=1,
            print_form_dt=dt.datetime.now()
                .replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
        )
        for _ in range(1)
    ]
    input_rows = [
        {
            'contract.client_id': c.obj.client_id,
            'contract.external_id': c.obj.external_id,
            'offer_confirmation_type': c.offer_confirmation_type[1],
            'is_deactivated': c.is_deactivated[1],
            'print_form_dt': c.print_form_dt[1]
        }
        for c in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'offer_confirmation_type_old': cs.offer_confirmation_type[0],
                'offer_confirmation_type': cs.offer_confirmation_type[1],
                'is_deactivated_old': cs.is_deactivated[0],
                'is_deactivated': cs.is_deactivated[1],
                'print_form_dt_old': cs.print_form_dt[0],
                'print_form_dt': cs.print_form_dt[1],
                'error': None,
                'traceback': None
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')
    for c in contract_states:
        assert c.obj.col0.offer_confirmation_type == c.offer_confirmation_type[1]
        attr, = [at for at in c.obj.col0.attributes
                 if at.code == 'OFFER_CONFIRMATION_TYPE']
        assert attr.passport_id == passport.passport_id


@patches
def test_change_zero_col_is_signed_new_personal_account_w_config_w_rules(session):
    session.config.__dict__['MULTIPLE_CHANGES_PROCESS_CONTRACT'] = 1
    checks = {
        'ContractNotCancelledChecker': True,
        'CollateralNotCancelledChecker': True,
        'NoConflictingCollateralsChecker': True,
        'contractruleschecker': True,
    }
    now = dt.datetime.now()
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                bank_details_id=509,
                commission=9,
                credit_type=1,
                currency=810,
                firm=111,
                netting=1,
                netting_pct=100,
                partner_credit=1,
                payment_term=15,
                payment_type=3,
                personal_account=1,
                services=[610, 612],
                unilateral=1,
                dt=now,
                is_signed=None,
                print_form_dt=now
            ),
            is_signed=now.isoformat()
        )
        for _ in range(1)
    ]
    input_rows = [
        {
            'contract.client_id': c.obj.client_id,
            'contract.external_id': c.obj.external_id,
            'is_signed': c.is_signed[1]
        }
        for c in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': now.isoformat(),
                'is_signed_old': None,
                'is_signed': now.strftime('%Y-%m-%dT00:00:00'),
                'error': None,
                'traceback': None
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')
    for c in contract_states:
        assert c.obj.col0.is_signed == now.replace(hour=0, minute=0, second=0, microsecond=0)
        assert c.obj.invoices


@patches
def test_change_zero_col_is_signed_new_personal_account_wo_config(session):
    checks = {
        'ContractNotCancelledChecker': True,
        'CollateralNotCancelledChecker': True,
        'NoConflictingCollateralsChecker': True,
        'contractruleschecker': True,
    }
    now = dt.datetime.now()
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                bank_details_id=509,
                commission=9,
                credit_type=1,
                currency=810,
                firm=111,
                netting=1,
                netting_pct=100,
                partner_credit=1,
                payment_term=15,
                payment_type=3,
                personal_account=1,
                services=[610, 612],
                unilateral=1,
                dt=now,
                is_signed=None,
                print_form_dt=now
            ),
            is_signed=now.isoformat()
        )
        for _ in range(1)
    ]
    input_rows = [
        {
            'contract.client_id': c.obj.client_id,
            'contract.external_id': c.obj.external_id,
            'is_signed': c.is_signed[1]
        }
        for c in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': now.isoformat(),
                'is_signed_old': None,
                'is_signed': now.strftime('%Y-%m-%dT00:00:00'),
                'error': None,
                'traceback': None
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')
    for c in contract_states:
        assert c.obj.col0.is_signed == now.replace(hour=0, minute=0, second=0, microsecond=0)
        assert not c.obj.invoices


@patches
def test_change_zero_col_is_signed_new_personal_account_w_config_wo_rules(session):
    checks = {
        'ContractNotCancelledChecker': True,
        'CollateralNotCancelledChecker': True,
        'NoConflictingCollateralsChecker': True,
    }
    session.config.__dict__['MULTIPLE_CHANGES_PROCESS_CONTRACT'] = 1
    now = dt.datetime.now()
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                bank_details_id=509,
                commission=9,
                credit_type=1,
                currency=810,
                firm=111,
                netting=1,
                netting_pct=100,
                partner_credit=1,
                payment_term=15,
                payment_type=3,
                personal_account=1,
                services=[610, 612],
                unilateral=1,
                dt=now,
                is_signed=None,
                print_form_dt=now
            ),
            is_signed=now.isoformat()
        )
        for _ in range(1)
    ]
    input_rows = [
        {
            'contract.client_id': c.obj.client_id,
            'contract.external_id': c.obj.external_id,
            'is_signed': c.is_signed[1]
        }
        for c in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': now.isoformat(),
                'is_signed_old': None,
                'is_signed': now.strftime('%Y-%m-%dT00:00:00'),
                'error': None,
                'traceback': None
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')
    for c in contract_states:
        assert c.obj.col0.is_signed == now.replace(hour=0, minute=0, second=0, microsecond=0)
        assert not c.obj.invoices


@patches
def test_change_last_col_unset_is_signed(session, checks):
    now = dt.datetime.now()
    contract = ob.ContractBuilder.construct(
        session,
        bank_details_id=88016007,
        commission=61,
        currency=810,
        firm=12,
        payment_type=2,
        services=[81],
        unilateral=1,
    )

    col = ob.CollateralBuilder.construct(
        session,
        num='01',
        contract=contract,
        collateral_type=general.collateral_types[1033],
        dt=now,
        credit_limit_single=2000000,
        credit_type=2,
        lift_credit_on_payment=1,
        payment_term=15,
        payment_type=3,
        personal_account=1,
        personal_account_fictive=1,
        print_form_type=0,
        is_signed=now
    )

    assert col.contract.current_signed().payment_type == 3

    INPUTS['input_rows'] = [
        {
            'contract.external_id': contract.external_id,
            'is_signed': None
        }
    ]

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': contract.id,
                'contract.client_id': contract.client_id,
                'contract.external_id': contract.external_id,
                'collateral.contract.external_id': contract.external_id,
                'collateral.contract.id': contract.id,
                'collateral.num': col.num,
                'collateral.dt': now.isoformat(),
                'is_signed_old': now.isoformat(),
                'is_signed': None,
                'error': None,
                'traceback': None,
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

    options = {'context': 'contract', 'last_col': True, 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')

    assert col.passport_id == passport.passport_id
    assert col.is_signed is None
    assert col.contract.current_signed().payment_type == 2

@patches
def test_change_last_col_is_signed_w_rules(session):
    checks = {
        'ContractNotCancelledChecker': True,
        'CollateralNotCancelledChecker': True,
        'NoConflictingCollateralsChecker': True,
        'ContractRulesChecker': True
    }
    now = dt.datetime.now()
    contract = ob.ContractBuilder.construct(
        session,
        bank_details_id=88016007,
        commission=61,
        currency=810,
        firm=12,
        payment_type=2,
        services=[81],
        unilateral=1,
    )

    assert not contract.invoices
    col = ob.CollateralBuilder.construct(
        session,
        num='01',
        contract=contract,
        collateral_type=general.collateral_types[1033],
        dt=now,
        credit_limit_single=2000000,
        credit_type=2,
        lift_credit_on_payment=1,
        payment_term=15,
        payment_type=3,
        personal_account=1,
        personal_account_fictive=1,
        print_form_type=0,
        is_signed=None
    )

    assert col.contract.current_signed().payment_type == 2

    INPUTS['input_rows'] = [
        {
            'contract.external_id': contract.external_id,
            'is_faxed': now.isoformat()
        }
    ]

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': contract.id,
                'contract.client_id': contract.client_id,
                'contract.external_id': contract.external_id,
                'collateral.contract.external_id': contract.external_id,
                'collateral.contract.id': contract.id,
                'collateral.num': col.num,
                'collateral.dt': now.isoformat(),
                'is_faxed_old': None,
                'is_faxed': now.strftime('%Y-%m-%dT00:00:00'),
                'error': None,
                'traceback': None,
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

    options = {'context': 'contract', 'last_col': True, 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')

    assert col.passport_id == passport.passport_id
    assert col.is_faxed == now.replace(hour=0, minute=0, second=0, microsecond=0)
    assert col.contract.current_signed().payment_type == 3

@patches
def test_change_zero_col_with_additional_fields(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                bank_details_id=510,
                commission=9,
                country=225,
                credit_type=1,
                currency=810,
                firm=13,
                netting=1,
                netting_pct=100,
                nds_for_receipt=-1,
                offer_confirmation_type='min-payment',
                partner_commission_type=2,
                payment_term=15,
                payment_type=3,
                is_deactivated=0,
                print_form_dt=None,
                services=[
                    605,
                    124,
                    111,
                    125,
                    128
                ]
            ),
            offer_confirmation_type='no'
        )
        for _ in range(3)
    ]
    input_rows = [
        {
            'contract.client_id': c.obj.client_id,
            'contract.external_id': c.obj.external_id,
            'offer_confirmation_type': c.offer_confirmation_type[1]
        }
        for c in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'offer_confirmation_type_old': cs.offer_confirmation_type[0],
                'offer_confirmation_type': cs.offer_confirmation_type[1],
                'error': None,
                'traceback': None,
                'collateral.contract.person.type': 'ph',
                'collateral.person.email':
                    'Can\'t get \'person.email\' from entity \'collateral\''
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }

    options = {
        'context': 'contract', 'processor': 'change_attributes',
        'additional_fields': [
            'collateral.contract.person.type',
            'contract.person.email',
            'collateral.person.email'
        ]
    }
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    for c in contract_states:
        assert c.obj.col0.offer_confirmation_type == c.offer_confirmation_type[1]


@patches
def test_change_col(session, checks):
    now = dt.datetime.now()
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ur').build(session).obj
    person.is_partner = 1
    contracts = [
        ob.ContractBuilder.construct(
            session,
            ctype='DISTRIBUTION',
            client=client,
            person=person,
            external_id=eid,
            atypical_conditions=0,
            contract_type=5,
            currency=643,
            currency_calculation=1,
            firm=1,
            nds=0,
            payment_type=1,
            pay_to=1,
            services=[300],
            service_start_dt=dt.datetime(2019, 1, 1),
            dt=dt.datetime(2019, 1, 1)
        )
        for eid in ['KEK/1', 'KEK/2']
    ]

    col_states = [
        State(
            ob.CollateralBuilder.construct(
                session,
                num='01',
                dt=now,
                nds=18,
                contract=c,
                collateral_type=distribution.collateral_types[3010]),
            nds=0
        )
        for c in contracts
    ]

    input_rows = [
        {
            'contract.external_id': col_state.obj.contract.external_id,
            'collateral.num': col_state.obj.num,
            'collateral.dt': now.strftime('%d.%m.%Y'),
            'nds': 0
        }
        for col_state in col_states
    ]
    # Check that processor adds num
    input_rows[1].pop('collateral.num')

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'collateral',
                'success': True,
                'collateral.contract.external_id': cs.obj.contract.external_id,
                'collateral.contract.id': cs.obj.contract.id,
                'id': cs.obj.id,
                'collateral.num': cs.obj.num,
                'collateral.dt': cs.obj.dt.isoformat(),
                'nds_old': cs.nds[0],
                'nds': cs.nds[1],
                'error': None,
                'traceback': None
            }
            for cs in col_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': c.id,
                'queue': 'OEBS_API'
            }
            for c in contracts
        ]
    }

    options = {'context': 'collateral', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    for cs in col_states:
        assert cs.obj.nds == cs.nds[1]


@patches
def test_change_col_with_additional_fields(session, checks):
    now = dt.datetime.now()
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ur').build(session).obj
    person.is_partner = 1
    contracts = [
        ob.ContractBuilder.construct(
            session,
            ctype='DISTRIBUTION',
            client=client,
            person=person,
            external_id=eid,
            atypical_conditions=0,
            contract_type=5,
            currency=643,
            currency_calculation=1,
            firm=1,
            nds=0,
            payment_type=1,
            pay_to=1,
            services=[300],
            service_start_dt=dt.datetime(2019, 1, 1),
            dt=dt.datetime(2019, 1, 1)
        )
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]

    col_states = [
        State(
            ob.CollateralBuilder.construct(
                session,
                num='01',
                dt=now,
                nds=18,
                contract=c,
                collateral_type=distribution.collateral_types[3010]),
            nds=0
        )
        for c in contracts
    ]

    input_rows = [
        {
            'contract.external_id': col_state.obj.contract.external_id,
            'collateral.num': col_state.obj.num,
            'collateral.dt': now.strftime('%d.%m.%Y'),
            'nds': 0
        }
        for col_state in col_states
    ]
    # Check that processor adds num
    input_rows[1].pop('collateral.num')

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'collateral',
                'success': True,
                'collateral.contract.external_id': cs.obj.contract.external_id,
                'collateral.contract.id': cs.obj.contract.id,
                'id': cs.obj.id,
                'collateral.num': cs.obj.num,
                'collateral.dt': cs.obj.dt.isoformat(),
                'nds_old': cs.nds[0],
                'nds': cs.nds[1],
                'error': None,
                'traceback': None,
                'collateral.contract.person.type': 'ur',
                'collateral.person.email':
                    'Can\'t get \'person.email\' from entity \'collateral\''
            }
            for cs in col_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': c.id,
                'queue': 'OEBS_API'
            }
            for c in contracts
        ]
    }

    options = {
        'context': 'collateral', 'processor': 'change_attributes',
        'additional_fields': [
            'collateral.contract.person.type',
            'contract.person.email',
            'collateral.person.email'
        ]
    }
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    for cs in col_states:
        assert cs.obj.nds == cs.nds[1]


@patches
def test_conflicting_col(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                external_id=eid,
                dt=dt.datetime(2018, 1, 1),
                finish_dt=dt.datetime(2019, 1, 1)
            ),
            finish_dt=dt.datetime(2020, 12, 12)
        )
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]

    col = ob.CollateralBuilder.construct(
            session,
            contract=contract_states[0].obj,
            collateral_type=general.collateral_types[80],
            num='01',
            dt=dt.datetime(2018, 2, 1),
            finish_dt=dt.datetime(2020, 1, 1)
        )

    input_rows = [
        {
            'contract.external_id': cs.obj.external_id,
            'finish_dt': '2020-12-12'  # Try parsing a string
        }
        for cs in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'finish_dt_old': cs.finish_dt[0].isoformat(),
                'finish_dt': cs.finish_dt[1].isoformat(),
                'error': None,
                'traceback': None
            }
            for cs in contract_states[1:]
        ],
        'fail_report': [
            {
                'classname': 'contract',
                'success': False,
                'id': contract_states[0].obj.id,
                'contract.external_id': contract_states[0].obj.external_id,
                'contract.client_id': contract_states[0].obj.client_id,
                'collateral.contract.external_id': contract_states[0].obj.external_id,
                'collateral.contract.id': contract_states[0].obj.id,
                'collateral.dt': contract_states[0].obj.col0.dt.isoformat(),
            }
        ],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states[1:]
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    checks['ContractActiveChecker'] = False
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    assert 'NoConflictingCollateralsChecker' in OUTPUTS['fail_report'][0]['error']

    assert contract_states[0].obj.current_state().finish_dt == col.finish_dt

    for cs in contract_states[1:]:
        assert cs.obj.col0.finish_dt == cs.finish_dt[1]


@patches
def test_fail_in_col(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(session, external_id=eid, is_signed=dt.datetime(2019, 1, 1)),
            external_id='LOL/' + eid,
            is_signed=dt.datetime(2020, 1, 1)
        )
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]
    input_rows = [
        {
            'contract.external_id': c.external_id[0],
            'external_id': c.external_id[1],
            'is_signed': '2020-01-01'
        }
        for c in contract_states
    ]

    # Mess up the date
    input_rows[0]['is_signed'] = '2020-13-13'

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'collateral.contract.external_id': 'LOL/' + cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'contract.client_id': cs.obj.client_id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'external_id_old': cs.external_id[0],
                'external_id': cs.external_id[1],
                'is_signed_old': cs.is_signed[0].isoformat(),
                'is_signed': cs.is_signed[1].isoformat(),
                'error': None,
                'traceback': None
            }
            for cs in contract_states[1:]
        ],
        'fail_report': [
            {
                'classname': 'contract',
                'success': False,
                'id': contract_states[0].obj.id,
                'contract.client_id': contract_states[0].obj.client_id,
                'collateral.contract.external_id': 'LOL/' +
                                                   contract_states[0].obj.external_id,
                'collateral.contract.id': contract_states[0].obj.id,
                'collateral.dt': contract_states[0].obj.col0.dt.isoformat(),
            }
        ],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states[1:]
        ]
    }

    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    assert 'Unexpected error' in OUTPUTS['fail_report'][0]['error']

    assert contract_states[0].obj.external_id == contract_states[0].external_id[0]
    assert contract_states[0].obj.col0.is_signed == contract_states[0].is_signed[0]

    passport = session.query(mapper.Passport).getone(login='autodasha')
    for cs in contract_states[1:]:
        assert cs.obj.external_id == cs.external_id[1]
        assert cs.obj.col0.is_signed == cs.is_signed[1]
        assert cs.obj.passport_id == passport.passport_id
        assert cs.obj.col0.passport_id == passport.passport_id


@patches
def test_last_col(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                external_id=eid,
                dt = dt.datetime(2018, 1, 1),
                finish_dt=dt.datetime(2019, 1, 1)
            ),
            external_id='LOL/' + eid,
            finish_dt=dt.datetime(2020, 1, 1)
        )
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]

    col = ob.CollateralBuilder.construct(
            session,
            num='01',
            contract=contract_states[0].obj,
            collateral_type=general.collateral_types[80],
            dt=dt.datetime(2019, 2, 1),
            finish_dt=dt.datetime(2019, 3, 1)
        )

    input_rows = [
        {
            'contract.external_id': cs.external_id[0],
            'external_id': cs.external_id[1],
            'finish_dt': '01.01.2020'
        }
        for cs in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': 'LOL/' + cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'external_id_old': cs.external_id[0],
                'external_id': cs.external_id[1],
                'finish_dt_old': cs.finish_dt[0].isoformat(),
                'finish_dt': cs.finish_dt[1].isoformat(),
                'error': None,
                'traceback': None
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }
    required_outputs['success_report'][0].update(
        {'finish_dt_old': col.finish_dt.isoformat(), 'collateral.num': col.num, 'collateral.dt': col.dt.isoformat()}
    )

    options = {'context': 'contract', 'last_col': True, 'processor': 'change_attributes'}
    checks['ContractActiveChecker'] = False
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    assert contract_states[0].obj.external_id == contract_states[0].external_id[1]
    assert contract_states[0].obj.col0.finish_dt == contract_states[0].finish_dt[0]
    assert col.finish_dt == contract_states[0].finish_dt[1]

    for cs in contract_states[1:]:
        assert cs.obj.external_id == cs.external_id[1]
        assert cs.obj.col0.finish_dt == cs.finish_dt[1]


@patches
def test_last_col_with_additional_fields(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                external_id=eid,
                dt = dt.datetime(2018, 2, 1),
                finish_dt=dt.datetime(2019, 1, 1)),
            external_id='LOL/' + eid,
            finish_dt=dt.datetime(2020, 1, 1)
        )
        for eid in ['KEK/1', 'KEK/2', 'KEK/3']
    ]

    col = ob.CollateralBuilder.construct(
            session,
            num='01',
            contract=contract_states[0].obj,
            collateral_type=general.collateral_types[80],
            dt=dt.datetime(2019, 1, 1),
            finish_dt=dt.datetime(2019, 2, 1)
        )

    input_rows = [
        {
            'contract.external_id': cs.external_id[0],
            'external_id': cs.external_id[1],
            'finish_dt': '01.01.2020'
        }
        for cs in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'contract.client_id': cs.obj.client_id,
                'collateral.contract.external_id': 'LOL/' + cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'external_id_old': cs.external_id[0],
                'external_id': cs.external_id[1],
                'finish_dt_old': cs.finish_dt[0].isoformat(),
                'finish_dt': cs.finish_dt[1].isoformat(),
                'error': None,
                'traceback': None,
                'collateral.contract.person.type': 'ph',
                'collateral.person.email':
                    'Can\'t get \'person.email\' from entity \'collateral\'',
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }
    required_outputs['success_report'][0].update(
        {'finish_dt_old': col.finish_dt.isoformat(), 'collateral.num': col.num, 'collateral.dt': col.dt.isoformat()}
    )

    options = {
        'context': 'contract', 'last_col': True, 'processor': 'change_attributes',
        'additional_fields': [
            'collateral.contract.person.type',
            'contract.person.email',
            'collateral.person.email'
        ]
    }
    checks['ContractActiveChecker'] = False
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    assert contract_states[0].obj.external_id == contract_states[0].external_id[1]
    assert contract_states[0].obj.col0.finish_dt == contract_states[0].finish_dt[0]
    assert col.finish_dt == contract_states[0].finish_dt[1]

    for cs in contract_states[1:]:
        assert cs.obj.external_id == cs.external_id[1]
        assert cs.obj.col0.finish_dt == cs.finish_dt[1]


@patches
def test_manager_change(session, checks):
    new_managers = [
        ob.SingleManagerBuilder.construct(session, domain_login=login) for login in ['kojima', 'isa', 'genius']
    ]
    contract_states = [
        State(
            ob.ContractBuilder.construct(
                session,
                external_id='KEK/{}'.format(i),
                services=7, dt=dt.datetime(2019, 1, 1)
            ),
            manager_code=man.manager_code
        )
        for i, man in enumerate(new_managers, 1)
    ]
    contract_states[0].obj.manager.domain_login = 'fired_dude'

    invoice_states = [
        State(
            ob.InvoiceBuilder.construct(
                session,
                contract=cs.obj,
                client=cs.obj.client,
                person=cs.obj.person,
                dt=cs.obj.col0.dt,
                manager_code=cs.manager_code[0]
            ),
            manager_code=cs.manager_code[1]
        )
        for cs in contract_states
    ]

    for is_ in invoice_states:
        is_.obj.type = 'personal_account'

    cs_w_sp = contract_states[0]
    spendable_state = State(
        ob.ContractBuilder.construct(
            session,
            ctype='SPENDABLE',
            client=cs_w_sp.obj.client,
            link_contract_id=cs_w_sp.obj.id,
            manager_code=cs_w_sp.manager_code[0]
        ),
        manager_code=cs_w_sp.manager_code[1]
    )

    input_rows = [
        {
            'contract.external_id': cs.obj.external_id,
            'new_manager_login': man.domain_login
        }
        for cs, man in zip(contract_states, new_managers)
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'manager_code_old': cs.manager_code[0],
                'manager_code': cs.manager_code[1],
                'manager_old': cs.obj.manager.domain_login or 'no manager',
                'manager': filter(lambda x: x.manager_code == cs.manager_code[1], new_managers)[0].domain_login,
                'error': None,
                'traceback': None
            }
            for cs in contract_states
        ],
        'fail_report': [],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in contract_states
        ]
    }
    required_outputs['success_report'].extend(
        {
            'classname': 'invoice',
            'success': True,
            'id': is_.obj.id,
            'invoice.external_id': is_.obj.external_id,
            'manager_code_old': is_.manager_code[0],
            'manager_code': is_.manager_code[1],
            'manager_old': is_.obj.manager.domain_login or 'no manager',
            'manager': filter(lambda x: x.manager_code == is_.manager_code[1], new_managers)[0].domain_login,
            'error': None,
            'traceback': None
        }
        for is_ in invoice_states
    )

    required_outputs['success_report'].append(
        {
            'classname': 'contract',
            'success': True,
            'id': spendable_state.obj.id,
            'collateral.contract.external_id': spendable_state.obj.external_id,
            'collateral.contract.id': spendable_state.obj.id,
            'collateral.dt': spendable_state.obj.col0.dt.isoformat(),
            'contract.external_id': spendable_state.obj.external_id,
            'contract.client_id': spendable_state.obj.client_id,
            'manager_code_old': spendable_state.manager_code[0],
            'manager_code': spendable_state.manager_code[1],
            'manager_old': spendable_state.obj.manager.domain_login or 'no manager',
            'manager':
                filter(lambda x: x.manager_code == spendable_state.manager_code[1], new_managers)[0].domain_login,
            'error': None,
            'traceback': None
        }
    )

    required_outputs['exported_objects'].extend(
        {
            'classname': 'Invoice',
            'object_id': is_.obj.id,
            'queue': 'OEBS_API'
        }
        for is_ in invoice_states
    )
    required_outputs['exported_objects'].append(
        {
            'classname': 'Contract',
            'object_id': spendable_state.obj.id,
            'queue': 'OEBS_API'
        }
    )

    options = {
        'mode': 'manager_change',
        'context': 'contract',
        'with_invoices': True,
        'with_spendables': True,
        'processor': 'change_attributes'
    }
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    passport = session.query(mapper.Passport).getone(login='autodasha')

    for cs in contract_states:
        assert cs.obj.col0.manager_code == cs.manager_code[1]
        attr, = [at for at in cs.obj.col0.attributes if at.code == 'MANAGER_CODE']
        assert attr.passport_id == passport.passport_id

    for is_ in invoice_states:
        assert is_.obj.manager_code == is_.manager_code[1]
        assert is_.obj.passport_id == passport.passport_id

    assert spendable_state.obj.col0.manager_code == spendable_state.manager_code[1]
    attr, = [at for at in spendable_state.obj.col0.attributes
             if at.code == 'MANAGER_CODE']
    assert attr.passport_id == passport.passport_id


@patches
def test_rules_fail(session, checks):
    contract_states = [
        State(
            ob.ContractBuilder.construct(session, is_signed=trunc_date(dt.datetime.now())),
            is_signed=trunc_date(dt.datetime.now()) - dt.timedelta(1)
        )
        for _ in range(3)
    ]
    # Set up fail in the middle
    contract_states[1].is_signed = trunc_date(dt.datetime.now()) + dt.timedelta(1)

    input_rows = [
        {
            'contract.id': cs.obj.id,
            'is_signed': cs.is_signed[1]
        }
        for cs in contract_states
    ]

    INPUTS['input_rows'] = input_rows

    required_outputs = {
        'success_report': [
            {
                'classname': 'contract',
                'success': True,
                'id': cs.obj.id,
                'collateral.contract.external_id': cs.obj.external_id,
                'collateral.contract.id': cs.obj.id,
                'collateral.dt': cs.obj.col0.dt.isoformat(),
                'contract.external_id': cs.obj.external_id,
                'contract.client_id': cs.obj.client_id,
                'is_signed_old': cs.obj.col0.is_signed.isoformat(),
                'is_signed': cs.is_signed[1].isoformat(),
                'error': None,
                'traceback': None
            }
            for cs in [contract_states[0], contract_states[2]]
        ],
        'fail_report': [
            {
                'classname': 'contract',
                'success': False,
                'id': contract_states[1].obj.id,
                'collateral.contract.external_id': contract_states[1].obj.external_id,
                'collateral.contract.id': contract_states[1].obj.id,
                'collateral.dt': contract_states[1].obj.col0.dt.isoformat(),
                'contract.external_id': contract_states[1].obj.external_id,
                'contract.client_id': contract_states[1].obj.client_id,
            }
        ],
        'exported_objects': [
            {
                'classname': 'Contract',
                'object_id': cs.obj.id,
                'queue': 'OEBS_API'
            }
            for cs in [contract_states[0], contract_states[2]]
        ]
    }

    checks['ContractRulesChecker'] = True
    options = {'context': 'contract', 'processor': 'change_attributes'}
    options.update(checks)

    nb = create_nirvana_block(session, options)

    with mock_transactions():
        result = mc.process(nb)

    assert result.is_finished()

    check_outputs(required_outputs)

    assert 'ContractRulesChecker' in OUTPUTS['fail_report'][0]['error']

    assert contract_states[1].obj.col0.is_signed == contract_states[1].is_signed[0]

    for cs in [contract_states[0], contract_states[2]]:
        assert cs.obj.col0.is_signed == cs.is_signed[1]
