# -*- coding: utf-8 -*-

import pytest
import mock
import json
import datetime as dt

import balance.actions.nirvana.operations.check_object_export as ce

from balance import mapper
from tests import object_builder as ob
from butils import exc


OUTPUTS = {}
INPUTS = {}

ce.batch_size = 2

TIMEOUT = 5


def nb_dt(old=False):
    res = dt.datetime.now()

    if old:
        res -= dt.timedelta(minutes=(2 * TIMEOUT))
    return res


def create_nirvana_block(session, options, **kwargs):
    nb = ob.NirvanaBlockBuilder(
        operation='check_object_export',
        request={
            'data': {
                'options': options
            }
        },
        **kwargs
    ).build(session).obj

    return nb


def check_outputs(required_outputs):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        return set(tuple(sorted(d.iteritems())) for d in list_of_dicts)

    def exclude_errors(list_of_dicts):
        return [{key: val for key, val in d.iteritems() if key not in {'error', 'traceback'}} for d in list_of_dicts]

    assert to_set(OUTPUTS['success_report']) == to_set(required_outputs['success_report'])
    assert to_set(OUTPUTS['fail_report']) == to_set(required_outputs['fail_report'])


def patches(f):
    def output_patch(self, name, data):
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


@pytest.fixture(autouse=True)
def clear_io():
    global INPUTS, OUTPUTS
    INPUTS = {
        'success_report': [],
        'fail_report': []
    }
    OUTPUTS = {
        'success_report': [],
        'fail_report': []
    }


@pytest.fixture()
def exports(session):
    return [
        ob.ExportBuilder.construct(
            session,
            state=0,
            rate=666,
            type='OEBS',
            classname='Contract',
            object_id=id_,
            export_dt=dt.datetime(2019, 1, 1)
        )
        for id_ in xrange(10)
    ]


@pytest.fixture()
def options():
    return {
        'queue': 'OEBS',
        'check_delay': 1,
        'timeout': 5,
        'just_wait_successful_state': False
    }


@patches
def test_timeout(session, exports, options):
    exports[-1].state = 1
    exports[-2].rate = 0
    for ex in exports[:-2]:
        ex.error = 'iddqd'

    nb = create_nirvana_block(session, options, dt=nb_dt(old=True))

    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': None
            }
            for ex in exports[-1:]
        ],
        'fail_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': 'iddqd'
            }
            for ex in exports[:-2]
        ]
    }
    required_output['fail_report'].extend(
        {
            'queue': 'OEBS',
            'classname': ex.classname,
            'object_id': ex.object_id,
            'state': ex.state,
            'rate': ex.rate,
            'export_dt': dt.datetime(2019, 1, 1).isoformat(),
            'error': 'Export is taking too long, check manually'
        }
        for ex in exports[-2:-1]
    )

    result = ce.process(nb)
    check_outputs(required_output)

    assert result.is_finished()


@patches
@pytest.mark.parametrize('just_wait_successful_state', [True, False])
def test_delay(session, exports, options, just_wait_successful_state):
    if just_wait_successful_state:
        options['just_wait_successful_state'] = True

    nb = create_nirvana_block(session, options, dt=nb_dt())
    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [],
        'fail_report': []
    }

    with pytest.raises(exc.DEFERRED_ERROR) as e:
        ce.process(nb)
    assert e.value.delay == options['check_delay']

    check_outputs(required_output)


@patches
def test_all_success(session, exports, options):
    for ex in exports:
        ex.state = 1

    nb = create_nirvana_block(session, options, dt=nb_dt())
    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': None
            }
            for ex in exports
        ],
        'fail_report': []
    }

    result = ce.process(nb)
    check_outputs(required_output)

    assert result.is_finished()


@patches
def test_timeout_w_fail(session, exports, options):
    exports[0].state = 1
    exports[1].rate = 0
    exports[-1].state = 2
    for ex in exports[2:]:
        ex.error = 'I done goofed'

    nb = create_nirvana_block(
        session, options, dt=nb_dt(old=True)

    )
    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': None
            }
            for ex in exports[:1]
        ],
        'fail_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': 'Export is taking too long, check manually'
            }
            for ex in exports[1:2]
        ],
    }
    required_output['fail_report'].extend(
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': ex.error
            }
            for ex in exports[2:]
    )

    result = ce.process(nb)
    check_outputs(required_output)

    assert result.is_finished()


@patches
def test_done_w_fail(session, exports, options):
    for ex in exports[:-1]:
        ex.state = 1

    exports[-1].state = 2
    exports[-1].error = 'I done goofed'

    nb = create_nirvana_block(session, options, dt=nb_dt())
    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': None
            }
            for ex in exports[:-1]
        ],
        'fail_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': ex.error
            }
            for ex in exports[-1:]
        ]
    }

    result = ce.process(nb)
    check_outputs(required_output)

    assert result.is_finished()


@patches
def test_unknown_queue(session, exports, options):
    options['queue'] = 'kek'
    nb = create_nirvana_block(session, options, dt=nb_dt())
    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [],
        'fail_report': []
    }

    with pytest.raises(ce.UnknownQueue) as e:
        ce.process(nb)
    assert e.value.queue == options['queue']

    check_outputs(required_output)


@patches
def test_missing_object(session, exports, options):
    for ex in exports:
        ex.state = 1
    nb = create_nirvana_block(session, options, dt=nb_dt())
    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]
    INPUTS['input_rows'].append(
        {
            'classname': 'Contractt',
            'object_id': 1337
        }
    )

    required_output = {
        'success_report': [
            {
                'queue': 'OEBS',
                'classname': ex.classname,
                'object_id': ex.object_id,
                'state': ex.state,
                'rate': ex.rate,
                'export_dt': dt.datetime(2019, 1, 1).isoformat(),
                'error': ex.error
            }
            for ex in exports
        ],
        'fail_report': [
            {
                'queue': 'OEBS',
                'classname': 'Contractt',
                'object_id': 1337,
                'state': None,
                'rate': None,
                'export_dt': None,
                'error': 'Object not found in export'
            }
        ]
    }

    result = ce.process(nb)
    check_outputs(required_output)

    assert result.is_finished()


@patches
def test_just_wait_successful_state(session, exports, options):
    for ex in exports:
        ex.state = 1

    options['just_wait_successful_state'] = True

    nb = create_nirvana_block(
        session, options, dt=nb_dt()
    )

    INPUTS['input_rows'] = [
        {
            'classname': ex.classname,
            'object_id': ex.object_id,
            'queue': 'OEBS'
        }
        for ex in exports
    ]

    required_output = {
        'success_report': [],
        'fail_report': []
    }

    result = ce.process(nb)
    check_outputs(required_output)

    assert result.is_finished()
