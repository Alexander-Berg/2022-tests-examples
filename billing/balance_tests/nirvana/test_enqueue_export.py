# -*- coding: utf-8 -*-

import datetime as dt
import httpretty
import json
import pytest

from tests import object_builder as ob

from balance.actions.nirvana.operations import enqueue_export
from balance import mapper
from balance.mapper.object_batch import ObjectBatch, ObjectBatchItem
from balance import muzzle_util as ut
from butils import exc


def nirvana_block(session, options, **kwargs):
    return ob.NirvanaBlockBuilder(
        operation='enqueue_export',
        request={
            'data': {
                'inputs': {
                    'input': {
                        'items': [
                            {
                                'downloadURL': 'http://yandex.ru'
                            }
                        ]
                    }
                },
                'options': options
            }
        }
    ).build(session).obj


@pytest.mark.parametrize('input_, queue, classname, bad_param', [
    ({'for_month': '2020-10-10', 'forecast': 1, 'mode': 'generate_only', 'reason': 'PAYSUP', 'service_ids': None},
     'PARTNER_ACTS', 'Contract', 'forecast'),
    ({'for_month': '2020.01.01', 'forecast': False, 'mode': 'generate_only', 'reason': 'PAYSUP', 'service_ids': []},
     'PARTNER_ACTS', 'Contract', 'for_month'),
    ({'reason': True}, 'ENTITY_CALC', 'ContractMonth', 'reason'),
    ({
        'completion_source': 'source',
        'filters': ['not a dict'],
        'spawn_rules': None,
        }, 'STAT_AGGREGATOR', 'StatAggregatorMetadata', 'filters'),
    ({
        'completion_source': 'source',
        'filters': {'am': 'dict'},
        'spawn_rules': 'not a list',
        }, 'STAT_AGGREGATOR', 'StatAggregatorMetadata', 'spawn_rules'),
    ({
        'for_month': '2020-10-01',
        'force': False,
        'force_full': False,
        'dps': 'not a list',
        'invoices': None,
        'enq_operation_id': 42,
     }, 'MONTH_PROC', 'Client', 'dps'),
    ({
        'for_month': '2020-10-01',
        'force': False,
        'force_full': False,
        'dps': [1],
        'invoices': None,
        'enq_operation_id': 'not an int',
     }, 'MONTH_PROC', 'Client', 'enq_operation_id'),
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_parse_error(session, input_, queue, classname, bad_param):
    options = dict(
        queue=queue,
        classname=classname,
        priority=0,
        input=json.dumps(input_),
        use_batches=False,
    )

    nb = nirvana_block(session, options)

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], '[123666]')

    with pytest.raises(exc.INVALID_PARAM) as e:
        enqueue_export.process(nb)
    assert u'Error parsing parameter \'{}\':'.format(bad_param) in e.value.msg


@pytest.mark.parametrize('input_', [
    {'for_month': '2020-10-10'},
    {'for_month': '2020-10-10', 'forecast': False, 'extra_param': 666},
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_malformed_input(session, input_):
    options = dict(
        queue='PARTNER_ACTS',
        classname='Contract',
        priority=0,
        input=json.dumps(input_),
        use_batches=False,
    )

    nb = nirvana_block(session, options)

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], '[123666]')

    with pytest.raises(exc.INVALID_PARAM) as e:
        enqueue_export.process(nb)
    assert (
        u'Invalid input params for queue \'{}\' and classname \'{}\''.format(options['queue'], options['classname'])
        in e.value.msg
    )


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_queue_not_supported(session):
    options = dict(
        queue='THERE_IS_NO_QUEUE',
        classname='Contract',
        priority=0,
        input='null',
        use_batches=False,
    )

    nb = nirvana_block(session, options)

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], '[123666]')

    with pytest.raises(exc.INVALID_PARAM) as e:
        enqueue_export.process(nb)
    assert u'Queue \'{}\' does not exist or is not supported'.format(options['queue']) in e.value.msg


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_class_not_supported(session):
    options = dict(
        queue='PARTNER_ACTS',
        classname='Contractt',
        priority=0,
        input='null',
        use_batches=False,
    )

    nb = nirvana_block(session, options)

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], '[123666]')

    with pytest.raises(exc.INVALID_PARAM) as e:
        enqueue_export.process(nb)
    assert (
        u'Classname \'{}\' is not supported for queue \'{}\''.format(options['classname'], options['queue'])
        in e.value.msg
    )


@pytest.mark.parametrize('input_', ['null', '{}'])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_empty_input(session, input_):
    payments = [ob.CardPaymentBuilder.construct(session) for _ in range(3)]
    options = dict(
        queue='THIRDPARTY_TRANS',
        classname='Payment',
        priority=0,
        input=input_,
        use_batches=False,
    )

    nb = nirvana_block(session, options)

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], json.dumps([p.id for p in payments]))

    enqueue_export.process(nb)

    for p in payments:
        export = session.query(mapper.Export).getone(
            type=options['queue'], classname=options['classname'], object_id=p.id
        )

        req_input = json.loads(options['input'])
        assert export.input == req_input
        assert export.priority == mapper.Payment._fix_priority(options['priority'], options['queue'])


@pytest.mark.parametrize('input_', [
    {'for_month': '2020-10-10', 'forecast': False, 'mode': 'generate_only', 'reason': 'PAYSUP', 'service_ids': None},
    {'for_month': '2020-10-10', 'forecast': True, 'mode': 'generate_only', 'reason': 'PAYSUP', 'service_ids': []},
    {'for_month': '2020-10-10', 'forecast': False, 'mode': 'hide_and_generate', 'reason': 'PAYSUP', 'service_ids': [111]},
    {'for_month': '2020-10-10', 'forecast': True, 'mode': 'hide_only', 'reason': 'PAYSUP', 'service_ids': [111, 128]},
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_enqueue_partners(session, input_):
    cons = [ob.ContractBuilder.construct(session, ctype='PARTNERS') for _ in range(3)]

    options = dict(
        queue='PARTNER_ACTS',
        classname='Contract',
        priority=0,
        input=json.dumps(input_),
        use_batches=False,
    )

    nb = nirvana_block(session, options)

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], json.dumps([c.id for c in cons]))

    enqueue_export.process(nb)

    for c in cons:
        export = session.query(mapper.Export).getone(
            type=options['queue'], classname=options['classname'], object_id=c.id
        )

        req_input = json.loads(options['input'])
        req_input['for_month'] = ut.trunc_date(ut.parse_date(req_input['for_month']).replace(day=1))
        req_input['service_ids'] = req_input['service_ids'] or []
        assert export.input == req_input
        assert export.priority == mapper.Contract._fix_priority(options['priority'], options['queue'])


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_enqueue_batches(session):
    cons = [ob.ContractBuilder.construct(session, ctype='PARTNERS') for _ in range(3)]

    options = dict(
        queue='ENQUEUE_PARTNER_ACTS',
        classname='Contract',
        priority=0,
        input=json.dumps(dict(for_month='2020-10-10')),
        use_batches=True,
    )

    nb = nirvana_block(session, options)
    req_month = ut.parse_date('2020-10-01')

    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], json.dumps([c.id for c in cons]))

    enqueue_export.process(nb)

    batch = session.query(ObjectBatch)\
        .join(ObjectBatchItem)\
        .filter(ObjectBatch.classname == 'Contract', ObjectBatchItem.object_id == cons[0].id).one()

    assert set(i.object_id for i in batch.items) == set(c.id for c in cons)
    assert batch.context == 'month'
    assert batch.context_value == dt.datetime.strftime(req_month, '%Y-%m-%d')

    export = session.query(mapper.Export).getone(
        type='ENQUEUE_PARTNER_ACTS', classname='ObjectBatch', object_id=batch.batch_id
    )
    assert export.priority == ObjectBatch._fix_priority(options['priority'], options['queue'])
