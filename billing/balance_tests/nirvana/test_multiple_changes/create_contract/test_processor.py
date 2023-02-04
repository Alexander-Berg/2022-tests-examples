# -*- coding: utf-8 -*-

"test_processor"

from __future__ import unicode_literals

import copy
import os
from collections import OrderedDict
from decimal import Decimal

import mock
import pytest
import json
import random
import datetime as dt

from balance.actions.nirvana.operations import multiple_changes as mc
from balance import mapper
from butils.passport import INTRA_MIN, INTRA_MAX
from tests.tutils import mock_transactions
from tests import object_builder as ob
from tests.balance_tests.nirvana.test_multiple_changes import create_contract

path_to_config = os.path.dirname(create_contract.__file__) + '/test_config.json'
with open(path_to_config, 'r') as f:
    CONFIG = json.load(f)

OUTPUTS = {}
INPUTS = {}

mc.batch_size = 2
date = dt.datetime(2021, 9, 3, 11, 33, 15)

DEFAULT_OPTIONS = {
                        'opcode': 'multiple_changes',
                        'processor': 'create_contract',
                    }

DEFAULT_PARAMS = {
    'type': 'GENERAL',
    'dt': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
    'commission': 0,
    'payment_type': 2,
    'bank_details_id': 2,
    'firm': 1,
    'currency': 'RUB',
    'services': [35],
    'is_faxed': date.strftime('%Y-%m-%dT%H:%M:%S+0300'),
}

DISTRIBUTION_PARAMS = {
    'type': 'DISTRIBUTION',
    'dt': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
    'distribution_contract_type': '4',
    'currency': 'USD',
    'currency_calculation': '1',
    'distribution_products': 'Я.маркет Беру',
    'distribution_tag': '666999666',
    'firm': '33',
    'partner_resources': 'bloggers',
    'payment_type': '1',
    'products_revshare': '[{"price": 100, "scale": "", "num": "10004"}, {"price": "", "scale": "NET-711_market_RS_3rd", "num": "10003"}]',
    'reward_type': '1',
    'service_start_dt': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
    'supplements': '1',
    'tail_time': '0',
    'use_geo_filter': '0',
    'nds': '0',
    'is_signed': date.strftime('%Y-%m-%dT%H:%M:%S+0300'),
    'test_mode': '0',
    'product_searchf': '666'
}

CORP_TAXI_PARAMS = {
    'type': 'SPENDABLE',
    'dt': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
    'payment_type': 1,
    'pay_to': 1,
    'firm': 13,
    'currency': 'RUB',
    'country': 225,
    'services': [135, 651],
    'is_signed': date.strftime('%Y-%m-%dT%H:%M:%S+0300'),
    'is_offer': 1,
    'nds': 0,
    'commission': 9
}

PARTNERS_PARAMS = {
    'type': 'PARTNERS',
    'dt': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
    'contract_type': '3',
    'currency': 'RUB',
    'doc_set': '2',
    'domains': 'rat.ru',
    'firm': '1',
    'nds': '0',
    'partner_pct': '45',
    'payment_type': '1',
    'is_signed': date.strftime('%Y-%m-%dT%H:%M:%S+0300')
}

@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {'input_rows': [], 'input_config': CONFIG}
    OUTPUTS = {'success_report': [], 'fail_report': [], 'exported_objects': []}


def get_domain_uid_value():
    return random.randint(INTRA_MIN(), INTRA_MAX())


@pytest.fixture
def manager(session):
    return ob.SingleManagerBuilder().build(session).obj


@pytest.fixture
def passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def domain_passport(session):
    return ob.PassportBuilder(passport_id=get_domain_uid_value()).build(session).obj


def check_outputs(req_output):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        result = set(tuple(sorted(d.iteritems())) for d in list_of_dicts)
        assert len(result) == len(list_of_dicts)
        return result

    def rm_from_report(dict_list, name):
        return [{k: v for k, v in d.iteritems() if k != name} for d in dict_list]

    assert to_set(OUTPUTS['success_report']) == to_set(req_output['success_report'])
    assert to_set(rm_from_report(OUTPUTS['fail_report'], 'traceback')) == to_set(
        req_output['fail_report']
    )
    assert to_set(OUTPUTS['exported_objects']) == to_set(req_output['exported_objects'])


def patcher(f):
    def output_patch(self, name, data):
        OUTPUTS[name] = json.loads(data)

    patch_funcs = [
        (
            'balance.mapper.nirvana_processor.NirvanaBlock.download',
            lambda nb, name: json.dumps(INPUTS[name], cls=mapper.BalanceJSONEncoder),
        ),
        ('balance.mapper.nirvana_processor.NirvanaBlock.upload', output_patch),
    ]

    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)
    return f


def create_nirvana_block(session, options):
    nb = (
        ob.NirvanaBlockBuilder(
            operation='universal',
            request={
                'data': {
                    'options': options,
                    'inputs': {
                        'input_config': 'lovenirvana.com',
                        'input_rows': 'lovenevskiy.net'
                    },
                },
                'context': {'workflow': {'owner': 'autodasha'}},
            },
        )
        .build(session)
        .obj
    )

    return nb


def checks_default_params(c):
    cs = c.current_signed()
    for k, v in DEFAULT_PARAMS.items():
        if k in ['commission', 'payment_type', 'bank_details_id']:
            assert getattr(cs, k) == v
        if k == 'start_dt':
            assert getattr(cs, 'dt') == dt.datetime(2021, 9, 3)
            assert getattr(cs, 'is_faxed') == date
        if k == 'services':
            assert getattr(cs, k) == {35}
        if k == 'firm_id':
            assert cs.firm == v
        if k == 'currency':
            assert cs.get_currency().iso_code == v
        if k == 'type':
            assert c.type == v


def get_success_row(client_id, person_id, contract_id, external_id, number_of_invoices):
    result = OrderedDict()
    result['client_id'] = client_id
    result['person_id'] = person_id
    result['contract_id'] = contract_id
    result['contract'] = external_id
    result['number_of_invoices'] = number_of_invoices
    return result


@patcher
def test_create_contract_from_block_options_success(session, domain_passport, manager):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj

    manager.domain_login = domain_passport.login
    manager.manager_type = 1
    passport_id = domain_passport.passport_id
    block_options = copy.deepcopy(DEFAULT_OPTIONS)
    block_options.update(DEFAULT_PARAMS)
    block_options.update({'manager_code': manager.manager_code, 'external_id': passport_id})

    INPUTS['input_rows'] = [{'client_id': client.id, 'person_id': person.id}]

    nb = create_nirvana_block(session, block_options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    c = session.query(mapper.Contract).getone(
        external_id=unicode(passport_id), client_id=client.id
    )
    checks_default_params(c)

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_distribution_contract_from_file_success(
    session, domain_passport, manager
):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='hk_yt').build(session).obj
    person.is_partner = 1
    person.country_id = 181
    manager.domain_login = domain_passport.login
    manager.manager_type = 4
    passport_id = domain_passport.passport_id
    ob.DistributionTagBuilder(client_id=client.id, tag_id=666999666).build(session)

    input_rows = {'client_id': client.id, 'person_id': person.id}
    input_rows.update(DISTRIBUTION_PARAMS)
    input_rows.update(
        {
            'manager_code': manager.manager_code,
            'external_id': passport_id,
        }
    )

    INPUTS['input_rows'] = [input_rows]

    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    products_revshare = {
        10003: {'value_num': None, 'value_str': u'NET-711_market_RS_3rd'},
        10004: {'value_num': Decimal('100'), 'value_str': None},
    }

    c = session.query(mapper.Contract).getone(
        external_id=passport_id, client_id=client.id
    )
    cs = c.current_signed()
    for k, v in products_revshare.items():
        cs_products_revshare = cs.products_revshare
        assert cs_products_revshare[k] == v

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_contract_from_file_success(session, domain_passport, manager):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj

    manager.domain_login = domain_passport.login
    manager.manager_type = 1
    passport_id = domain_passport.passport_id
    input_rows = {'client_id': client.id, 'person_id': person.id}
    input_rows.update(DEFAULT_PARAMS)
    input_rows.update(
        {'manager_code': manager.manager_code, 'external_id': passport_id, 'services': '35'}
    )

    INPUTS['input_rows'] = [input_rows]


    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    c = session.query(mapper.Contract).getone(
        external_id=unicode(passport_id), client_id=client.id
    )
    checks_default_params(c)

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_contract_corp_taxi_from_file_success(session, domain_passport, manager):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ur').build(session).obj
    person.is_partner = 1

    manager.domain_login = domain_passport.login
    manager.manager_type = 1
    passport_id = domain_passport.passport_id
    input_rows = {'client_id': client.id, 'person_id': person.id}
    input_rows.update(CORP_TAXI_PARAMS)
    input_rows.update(
        {'manager_code': manager.manager_code, 'external_id': passport_id}
    )

    INPUTS['input_rows'] = [input_rows]

    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    c = session.query(mapper.Contract).getone(
        external_id=unicode(passport_id), client_id=client.id
    )

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_partners_from_file_success(session, domain_passport, manager):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj
    person.is_partner = 1

    manager.domain_login = domain_passport.login
    manager.manager_type = 3
    passport_id = domain_passport.passport_id
    input_rows = {'client_id': client.id, 'person_id': person.id}
    input_rows.update(PARTNERS_PARAMS)
    input_rows.update(
        {
            'external_id': passport_id,
            'manager_code': manager.manager_code
        }
    )

    INPUTS['input_rows'] = [input_rows]

    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    c = session.query(mapper.Contract).getone(
        external_id=unicode(passport_id), client_id=client.id
    )

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_contract_from_file_with_block_options_success(
    session, domain_passport, manager
):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj

    manager.domain_login = domain_passport.login
    manager.manager_type = 1
    passport_id = domain_passport.passport_id

    block_options = copy.deepcopy(DEFAULT_OPTIONS)
    block_options.update({'firm': 'blabla', 'payment_type': 'whatever'})

    input_rows = {'client_id': client.id, 'person_id': person.id}
    input_rows.update(DEFAULT_PARAMS)
    input_rows.update({'manager_code': manager.manager_code, 'external_id': passport_id})

    INPUTS['input_rows'] = [input_rows]

    nb = create_nirvana_block(session, block_options)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    c = session.query(mapper.Contract).getone(
        external_id=unicode(passport_id), client_id=client.id
    )
    checks_default_params(c)

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_contract_from_file_two_rows_success(session):
    input_data = []
    input_rows = []
    success_report = []
    exported_objects = []

    for id_ in range(2):
        client = ob.ClientBuilder().build(session).obj
        person = ob.PersonBuilder(client=client, type='ph').build(session).obj
        manager = ob.SingleManagerBuilder().build(session).obj
        manager.manager_type = 1
        domain_passport = (
            ob.PassportBuilder(passport_id=get_domain_uid_value()).build(session).obj
        )
        manager.domain_login = domain_passport.login

        input_data.append((client, person, domain_passport.passport_id))
        temp_rows = {'client_id': client.id, 'person_id': person.id}
        temp_rows.update(
            {
                'manager_code': manager.manager_code,
                'external_id': domain_passport.passport_id,
            }
        )
        if id_ == 1:
            temp_rows.update(DEFAULT_PARAMS)
        else:
            temp_rows.update({
                'type': 'GENERAL',
                'bank_details_id': '509',
                'commission': '9',
                'credit_type': '1',
                'currency': 'RUB',
                'firm': '111',
                'netting': '1',
                'netting_pct': '100',
                'partner_credit': '1',
                'payment_term': '15',
                'payment_type': '3',
                'personal_account': '1',
                'services': '610, 612',
                'unilateral': '1',
                'dt': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'is_signed': date.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'print_form_dt': date.strftime('%Y-%m-%dT%H:%M:%SZ')
            })
        input_rows.append(temp_rows)

    INPUTS['input_rows'] = input_rows

    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    for num, row in enumerate(input_data):
        client, person, passport_id = row
        c = session.query(mapper.Contract).getone(
            external_id=unicode(passport_id), client_id=client.id
        )
        if num == 0:
            cs = c.current_signed()
            assert getattr(cs, 'lift_credit_on_payment')
            assert getattr(cs, 'is_signed')
            assert getattr(cs, 'bank_details_id') == 509
            assert c.col0.services == {610: 1, 612: 1}
            assert getattr(cs, 'print_form_dt')
            success_report.append(
                get_success_row(client.id, person.id, c.id, unicode(passport_id), 2)
            )
        else:
            checks_default_params(c)
            success_report.append(
                get_success_row(client.id, person.id, c.id, unicode(passport_id), 0)
            )
        exported_objects.append(
            {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
        )

    required_outputs = {
        'success_report': success_report,
        'fail_report': [],
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


@patcher
def test_create_contract_from_file_one_failed(session, manager, domain_passport):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj

    bad_client = ob.ClientBuilder().build(session).obj
    bad_person = ob.PersonBuilder(client=bad_client, type='ph').build(session).obj

    manager.domain_login = domain_passport.login
    manager.manager_type = 1
    passport_id = domain_passport.passport_id

    good_row = {'client_id': client.id, 'person_id': person.id}
    good_row.update(DEFAULT_PARAMS)
    good_row.update({'manager_code': manager.manager_code, 'external_id': passport_id})

    bad_row = {'client_id': bad_client.id, 'person_id': bad_person.id}
    bad_row.update(DEFAULT_PARAMS)
    bad_row['external_id'] = unicode(passport_id) + '666'

    INPUTS['input_rows'] = [good_row, bad_row]

    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    c = session.query(mapper.Contract).getone(
        external_id=unicode(passport_id), client_id=client.id
    )
    checks_default_params(c)

    success_report = [get_success_row(client.id, person.id, c.id, unicode(passport_id), len(c.invoices))]

    fail_report = [{'row_number': 2, 'client_id': bad_client.id, 'person_id': bad_person.id, 'error': "Rule violation: 'Не выбран менеджер'"}]

    exported_objects = [
        {'classname': 'Contract', 'object_id': c.id, 'queue': 'OEBS_API'}
    ]

    required_outputs = {
        'success_report': success_report,
        'fail_report': fail_report,
        'exported_objects': exported_objects,
    }

    check_outputs(required_outputs)


# @patcher
# def test_unavailable_attribute(session, domain_passport, manager):
#     client = ob.ClientBuilder().build(session).obj
#     person = ob.PersonBuilder(client=client, type='ph').build(session).obj
#
#     manager.domain_login = domain_passport.login
#     manager.manager_type = 1
#     passport_id = domain_passport.passport_id
#
#     block_options = copy.deepcopy(DEFAULT_OPTIONS)
#     block_options.update({'manager_code': manager.manager_code, 'external_id': passport_id, 'product_searchf': '666'})
#
#     INPUTS['input_rows'] = [{'client_id': client.id, 'person_id': person.id, 'type': 'GENERAL', 'currency': 'USD', 'firm_id': '1'}]
#
#     nb = create_nirvana_block(session, block_options)
#
#     with mock_transactions():
#         res = mc.process(nb)
#
#     assert res.is_finished()
#
#     fail_report = [{'row_number': 1, 'client_id': client.id, 'person_id': person.id, 'error': "Attributes product_searchf is not allowed for contract type GENERAL"}]
#
#     required_outputs = {
#         'success_report': [],
#         'fail_report': fail_report,
#         'exported_objects': [],
#     }
#
#     check_outputs(required_outputs)


@patcher
def test_wo_mandatory_attribute(session, domain_passport, manager):
    client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type='ph').build(session).obj

    manager.domain_login = domain_passport.login
    manager.manager_type = 1
    passport_id = domain_passport.passport_id

    INPUTS['input_rows'] = [{'person_id': person.id, 'manager_code': manager.manager_code, 'external_id': passport_id}]

    nb = create_nirvana_block(session, DEFAULT_OPTIONS)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    fail_report = [{'row_number': 1, 'client_id': 'Отсутствует', 'person_id': person.id, 'error': "Attribute '%s' is obligatory" % ', '.join(['client_id', 'type', 'firm', 'currency'])}]

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report,
        'exported_objects': [],
    }

    check_outputs(required_outputs)
