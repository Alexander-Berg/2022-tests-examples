# -*- coding: utf-8 -*-

import datetime as dt
import pytest
import mock
import json

from zipfile import ZipFile
from StringIO import StringIO
from contextlib import closing

from billing.contract_iface.cmeta import general

import balance.actions.nirvana.operations.print_form_attachments as pfa
from balance import mapper
from balance.constants import FirmId, PREPAY_PAYMENT_TYPE, ServiceId, ContractPrintTpl
from tests import object_builder as ob
from tests.tutils import mock_transactions

OUTPUTS = {}
INPUTS = {}

wh_patch = 'balance.actions.nirvana.operations.print_form_attachments.WikiHandler'
rules_patch = 'balance.actions.nirvana.operations.print_form_attachments.run_rules'


@pytest.fixture(autouse=True)
def wh_mock():
    with mock.patch(wh_patch) as wh_mock:
        wh_mock.return_value.pdf_binary_content = mock.Mock(return_value='I am a pdf !')

        # to satisfy retry
        wh_mock.return_value.erase_print_form.__name__ = 'erase_print_form'
        wh_mock.return_value.pdf_binary_content.__name__ = 'pdf_binary_content'

        yield wh_mock


@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {
        'input_rows': []
    }
    OUTPUTS = {
        'output_rows': [],
        'failed_rows': [],
        'mapping': [],
        'attachments': None
    }


def create_nirvana_block(session, options=None, **kwargs):
    options = options or {}
    nb = ob.NirvanaBlockBuilder(
        operation='print_form_attachments',
        request={
            'data': {
                'options': options
            }
        },
        **kwargs
    ).build(session).obj

    return nb


def create_contract_w_pf(session):
    contract = ob.ContractBuilder.construct(
        session,
        ctype='GENERAL',
        dt=dt.datetime.now(),
        firm=FirmId.TAXI,
        payment_type=PREPAY_PAYMENT_TYPE,
        services={ServiceId.TAXI_CORP: 1, ServiceId.TAXI_CORP_CLIENTS: 1},
        print_template=ContractPrintTpl.TAXI_MARKETING
    )
    session.flush()

    return contract


def check_outputs(required_outputs):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        return set(tuple(sorted(d.iteritems())) for d in list_of_dicts)

    def exclude_errors(list_of_dicts):
        return [{key: val for key, val in d.iteritems() if key not in {'ERROR', 'TRACEBACK'}} for d in list_of_dicts]

    assert to_set(OUTPUTS['output_rows']) == to_set(required_outputs['output_rows'])
    assert to_set(OUTPUTS['mapping']) == to_set(required_outputs['mapping'])
    assert to_set(exclude_errors(OUTPUTS['failed_rows'])) == to_set(required_outputs['failed_rows'])


def patches(f):
    def output_patch(self, name, data):
        if name == 'attachments':
            OUTPUTS[name] = data
        else:
            OUTPUTS[name] = json.loads(data)
    patch_funcs = [
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.download',
            lambda nb, name: json.dumps(INPUTS[name], cls=mapper.BalanceJSONEncoder)
        ),
        ('balance.mapper.nirvana_processor.NirvanaBlock.upload', output_patch),
    ]
    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)
    return f


@patches
def test_bad_object_type(session):
    nb = create_nirvana_block(session)
    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'ccontract', 'OBJECT_ID': 666}
    ]

    required_output = {
        'output_rows': [],
        'mapping': [],
        'failed_rows': [
            {
                'REASON': u'Unknown object type: ccontract',
                'OBJECT_ID': 666,
                'OBJECT_TYPE': 'ccontract'
            }
        ]
    }

    with mock_transactions():
        pfa.process(nb)
    check_outputs(required_output)


@patches
def test_contract_not_found(session):
    nb = create_nirvana_block(session)
    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'contract', 'OBJECT_ID': 666}
    ]

    with mock_transactions():
        pfa.process(nb)
    assert 'CONTRACT_NOT_FOUND' in OUTPUTS['failed_rows'][0]['TRACEBACK']


@patches
def test_no_email(session):
    nb = create_nirvana_block(session)
    contract = create_contract_w_pf(session)

    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'contract', 'OBJECT_ID': contract.id}
    ]

    with mock_transactions():
        pfa.process(nb)

    assert u'No email' in OUTPUTS['failed_rows'][0]['REASON']


@patches
def test_no_print_template(session):
    nb = create_nirvana_block(session)
    contract = ob.ContractBuilder.construct(session)
    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'contract', 'OBJECT_ID': contract.id, 'EMAIL': 'spam@me'}
    ]

    with mock.patch(rules_patch) as rules_mock:
        with mock_transactions():
            pfa.process(nb)
        assert rules_mock.called


@patches
def test_erase_print_form(session, wh_mock):
    nb = create_nirvana_block(session, options={'erase_print_form': True})
    contract = create_contract_w_pf(session)

    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'contract', 'OBJECT_ID': contract.id, 'EMAIL': 'why@me'}
    ]

    with mock_transactions():
        pfa.process(nb)
    assert not OUTPUTS['failed_rows']
    assert wh_mock.return_value.erase_print_form.called


@patches
def test_send_to_person(session, wh_mock):
    nb = create_nirvana_block(session, options={'send_to_person': True})
    contract = create_contract_w_pf(session)
    contract.person.email = 'stop_sending@me.this'
    session.flush()

    # Will compare w/ auto-generated name
    pf_name = '{}_{}.pdf'.format(contract.id, contract.external_id.replace('/', '_'))

    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'contract', 'OBJECT_ID': contract.id}
    ]

    required_output = {
        'output_rows': [
            {'OBJECT_TYPE': 'contract', 'OBJECT_ID': contract.id, 'EMAIL': 'stop_sending@me.this'}
        ],
        'mapping': [
            {'DATA_ROWNUM': 0, 'REAL_FILENAME': pf_name, 'MIME_TYPE': None, 'FILENAME': None}
        ],
        'failed_rows': []
    }

    with mock_transactions():
        pfa.process(nb)
    assert wh_mock.return_value.pdf_binary_content.called

    check_outputs(required_output)

    with closing(StringIO(OUTPUTS['attachments'])) as buff:
        with ZipFile(buff) as zf:
            data = zf.read(pf_name)
            assert data == 'I am a pdf !'


@patches
def test_multiple_rows_w_fail(session, wh_mock):
    nb = create_nirvana_block(session, options={'erase_print_form': True})
    contracts = [create_contract_w_pf(session) for _ in range(5)]
    contracts[0].col0.print_template = None  # This will trigger the rules run

    pf_names = ['{}_{}.pdf'.format(c.id, c.external_id.replace('/', '_')) for c in contracts]

    INPUTS['input_rows'] = [
        {
            'OBJECT_TYPE': 'contract',
            'OBJECT_ID': contract.id,
            'FILENAME': 'test.pdf',
            'EMAIL': 'howdoi%d@unsubscribe' % i
        }
        for i, (contract, pf_name) in enumerate(zip(contracts, pf_names), start=1)
    ]
    INPUTS['input_rows'][1]['OBJECT_TYPE'] = 'contractt'  # Set up fail

    required_output = {}
    required_output['output_rows'] = list(INPUTS['input_rows'])
    required_output['output_rows'].pop(1)
    pf_names.pop(1)

    required_output['mapping'] = [
        {'DATA_ROWNUM': i, 'REAL_FILENAME': pf_name, 'MIME_TYPE': None, 'FILENAME': row['FILENAME']}
        for i, (row, pf_name) in enumerate(zip(required_output['output_rows'], pf_names))
    ]

    required_output['failed_rows'] = [list(INPUTS['input_rows'])[1]]
    required_output['failed_rows'][0].update({'REASON': u'Unknown object type: contractt'})

    with mock.patch(rules_patch) as rules_mock:

        with mock_transactions():
            pfa.process(nb)
        assert rules_mock.call_count == 1
        assert wh_mock.return_value.pdf_binary_content.call_count == 4
        assert wh_mock.return_value.erase_print_form.call_count == 4

    check_outputs(required_output)

    with closing(StringIO(OUTPUTS['attachments'])) as buff:
        with ZipFile(buff) as zf:
            for row in required_output['mapping']:
                data = zf.read(row['REAL_FILENAME'])
                assert data == 'I am a pdf !'


@patches
def test_send_collateral(session, wh_mock):
    nb = create_nirvana_block(session, options={'send_to_person': True})
    contract = create_contract_w_pf(session)
    contract.person.email = 'what_even_is@billing'

    collateral_type = general.collateral_types[90]  # Cancellation
    col = ob.CollateralBuilder.construct(
        session,
        contract=contract,
        collateral_type=collateral_type,
        dt=dt.datetime.now(),
        finish_dt=dt.datetime.now() + dt.timedelta(1),
        num=u'У-01 ',
        print_template=u'dumb/filler'
    )
    session.flush()

    pf_name = u'{}_{}_{}.pdf'.format(contract.id, contract.external_id.replace('/', '_'), u'У-01')

    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'collateral', 'OBJECT_ID': col.id}
    ]

    required_output = {
        'output_rows': [
            {'OBJECT_TYPE': 'collateral', 'OBJECT_ID': col.id, 'EMAIL': 'what_even_is@billing'}
        ],
        'mapping': [
            {'DATA_ROWNUM': 0, 'REAL_FILENAME': pf_name, 'MIME_TYPE': None, 'FILENAME': None}
        ],
        'failed_rows': []
    }

    with mock_transactions():
        pfa.process(nb)
    assert wh_mock.return_value.pdf_binary_content.called

    check_outputs(required_output)

    with closing(StringIO(OUTPUTS['attachments'])) as buff:
        with ZipFile(buff) as zf:
            data = zf.read(pf_name)
            assert data == 'I am a pdf !'


@patches
def test_rules_fail(session):
    nb = create_nirvana_block(session, options={'send_to_person': True})
    contract = create_contract_w_pf(session)
    contract.person.email = 'violated@the.law'
    contract.col0.print_template = None
    session.flush()

    INPUTS['input_rows'] = [
        {'OBJECT_TYPE': 'contract', 'OBJECT_ID': contract.id}
    ]

    with mock.patch(rules_patch) as rules_mock:
        rules_mock.side_effect = pfa.PrintFormException(reason=u'Rules fail', error=u'Stop right there, criminal scum!')
        with mock_transactions():
            pfa.process(nb)
        assert rules_mock.called

    assert u'Rules fail' in OUTPUTS['failed_rows'][0]['REASON']
