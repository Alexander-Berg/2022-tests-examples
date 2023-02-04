# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock
import pytest
import json
import os

from balance.actions.nirvana.operations import multiple_changes as mc
from tests.balance_tests.nirvana.test_multiple_changes import create_client_person
from balance import mapper
from tests.tutils import mock_transactions
from tests import object_builder as ob


INPUTS = {}
OUTPUTS = {}


path_to_config = os.path.dirname(create_client_person.__file__) + '/test_config.json'
with open(path_to_config, 'r') as f:
    CONFIG = json.load(f)


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


def split_output(data, ignore_keys):
    output_ignore_keys = []
    output_not_ignore_keys = []

    for row in data:
        ignore_keys_row = dict()
        not_ignore_keys_row = dict()

        for k, v in row.iteritems():
            if k in ignore_keys:
                ignore_keys_row[k] = v
            else:
                not_ignore_keys_row[k] = v

        output_ignore_keys.append(ignore_keys_row)
        output_not_ignore_keys.append(not_ignore_keys_row)

    return output_ignore_keys, output_not_ignore_keys


def to_set(list_of_dicts):
    result = set(tuple(sorted(d.iteritems())) for d in list_of_dicts)
    assert len(result) == len(list_of_dicts)
    return result


def to_boolean_values(list_of_dicts):
    return [{k: bool(v) for k, v in row.iteritems()} for row in list_of_dicts]


def check_outputs(res_output, req_output, ignore_keys):
    for row in req_output:
        for k in ignore_keys:
            row[k] = True

    res_output_ignore_keys, res_output_not_ignore_keys = split_output(res_output, ignore_keys)
    req_output_ignore_keys, req_output_not_ignore_keys = split_output(req_output, ignore_keys)

    assert len(res_output_ignore_keys) == len(req_output_ignore_keys)
    assert len(res_output_not_ignore_keys) == len(req_output_not_ignore_keys)
    assert to_set(res_output_not_ignore_keys) == to_set(req_output_not_ignore_keys)
    assert to_boolean_values(res_output_ignore_keys) == to_boolean_values(req_output_ignore_keys)


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
        )
    ]

    for target, func in patch_funcs:
        f = mock.patch(target, func)(f)

    return f


def create_nirvana_block(session):
    nb = ob.NirvanaBlockBuilder(
        operation='create_client_person',
        request={
            'data': {
                'options': {'processor': 'create_client_person'},
                'inputs': {'input_config': 'config'}
            },
            'context': {
                'workflow': {
                    'owner': 'autodasha'
                }
            }
        }
    ).build(session).obj

    return nb


@patcher
def test_create_client_person(session):
    agency = ob.ClientBuilder(is_agency=1).build(session).obj

    INPUTS['input_rows'] = [
        {
            'client.name': 'client',
            'client.is_agency': 0,
            'client.agency_id': agency.id,
            'client.region_id': 225,
            'client.email': '666@ya.ru',
            'person.type': 'ph',
            'person.longname': 'person_long_name',
            'person.legaladdress': 'Королева 21',
            'person.inn': '7702318380',
            'person.bik': '044525774',
            'person.postcode': '109044',
            'person.postaddress': 'Королева 12',
            'person.email': '666@ya.ru',
            'person.phone': '9379992',
            'person.lname': 'lname',
            'person.fname': 'fname',
            "person.mname": 'mname'
        }
    ]

    nb = create_nirvana_block(session)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    req_output = {
        'success_report': [
            {'new_' + k: v for k, v in row.items()} for row in INPUTS['input_rows']
        ],
        'exported_objects': [
            {
                'classname': 'Client',
                'object_id': True,  # fake
                'queue': 'OEBS_API'
            },
            {
                'classname': 'Person',
                'object_id': True,  # fake
                'queue': 'OEBS_API'
            }
        ]
    }

    check_outputs(
        OUTPUTS['success_report'],
        req_output['success_report'],
        ['new_client.id', 'new_person.id']
    )

    check_outputs(
        OUTPUTS['exported_objects'],
        req_output['exported_objects'],
        ['object_id']
    )


@patcher
def test_create_person_wo_client(session):
    client = ob.ClientBuilder().build(session).obj

    INPUTS['input_rows'] = [
        {
            'person.client_id': client.id,
            'person.type': 'ph',
            'person.longname': 'person_long_name',
            'person.legaladdress': 'Королева 21',
            'person.inn': '7702318380',
            'person.bik': '044525774',
            'person.postcode': '109044',
            'person.postaddress': 'Королева 12',
            'person.email': '666@ya.ru',
            'person.phone': '9379992',
            'person.lname': 'lname',
            'person.fname': 'fname',
            "person.mname": 'mname'
        }
    ]

    nb = create_nirvana_block(session)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    req_output = {
        'success_report': [
            {'new_' + k: v for k, v in row.items()} for row in INPUTS['input_rows']
        ],
        'exported_objects': [
            {
                'classname': 'Person',
                'object_id': True,  # fake
                'queue': 'OEBS_API'
            }
        ]
    }

    check_outputs(
        OUTPUTS['success_report'],
        req_output['success_report'],
        ['new_person.id']
    )

    check_outputs(
        OUTPUTS['exported_objects'],
        req_output['exported_objects'],
        ['object_id']
    )


@patcher
def test_invalid_entity_type(session):
    INPUTS['input_rows'] = [
        {
            'contract.id': '777',
            'person.client_id': '666',
            'person.type': 'ph',
            'person.longname': 'person_long_name',
            'person.legaladdress': 'Королева 21',
            'person.inn': '7702318380',
            'person.bik': '044525774',
            'person.postcode': '109044',
            'person.postaddress': 'Королева 12',
            'person.email': '666@ya.ru',
            'person.phone': '9379992',
            'person.lname': 'lname',
            'person.fname': 'fname',
            "person.mname": 'mname'
        }
    ]

    nb = create_nirvana_block(session)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(
        OUTPUTS['fail_report'],
        [
            {
                'row_content': True,
                'error': 'Column name must be starts with "client." or "person."',
                'traceback': True
            }
        ],
        ['row_content', 'traceback']
    )


@patcher
def test_create_client_and_person_with_client_id(session):
    INPUTS['input_rows'] = [
        {
            'client.name': 'client',
            'client.is_agency': 0,
            'client.region_id': 225,
            'client.email': '666@ya.ru',
            'person.client_id': 666,
            'person.type': 'ph',
            'person.longname': 'person_long_name',
            'person.legaladdress': 'Королева 21',
            'person.inn': '7702318380',
            'person.bik': '044525774',
            'person.postcode': '109044',
            'person.postaddress': 'Королева 12',
            'person.email': '666@ya.ru',
            'person.phone': '9379992',
            'person.lname': 'lname',
            'person.fname': 'fname',
            "person.mname": 'mname'
        }
    ]

    nb = create_nirvana_block(session)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(
        OUTPUTS['fail_report'],
        [
            {
                'row_content': True,
                'error': 'When creating a new client, the client_id for person is now allowed',
                'traceback': True
            }
        ],
        ['row_content', 'traceback']
    )


@patcher
def test_create_person_wo_client_id(session):
    INPUTS['input_rows'] = [
        {
            'person.type': 'ph',
            'person.longname': 'person_long_name',
            'person.legaladdress': 'Королева 21',
            'person.inn': '7702318380',
            'person.bik': '044525774',
            'person.postcode': '109044',
            'person.postaddress': 'Королева 12',
            'person.email': '666@ya.ru',
            'person.phone': '9379992',
            'person.lname': 'lname',
            'person.fname': 'fname',
            "person.mname": 'mname'
        }
    ]

    nb = create_nirvana_block(session)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(
        OUTPUTS['fail_report'],
        [
            {
                'row_content': True,
                'error': 'Attribute \'client_id\' is obligatory',
                'traceback': True
            }
        ],
        ['row_content', 'traceback']
    )


@patcher
def test_create_person_with_not_allowed_attr(session):
    INPUTS['input_rows'] = [
        {
            'person.now_allowed_attr': 'lalka',
            'person.type': 'ph',
            'person.longname': 'person_long_name',
            'person.legaladdress': 'Королева 21',
            'person.inn': '7702318380',
            'person.bik': '044525774',
            'person.postcode': '109044',
            'person.postaddress': 'Королева 12',
            'person.email': '666@ya.ru',
            'person.phone': '9379992',
            'person.lname': 'lname',
            'person.fname': 'fname',
            "person.mname": 'mname'
        }
    ]

    nb = create_nirvana_block(session)

    with mock_transactions():
        res = mc.process(nb)

    assert res.is_finished()

    check_outputs(
        OUTPUTS['fail_report'],
        [
            {
                'row_content': True,
                'error': 'Changing attribute \'now_allowed_attr\' of entity type \'person\' is not allowed',
                'traceback': True
            }
        ],
        ['row_content', 'traceback']
    )
