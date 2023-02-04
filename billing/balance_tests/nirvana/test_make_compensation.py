# -*- coding: utf-8 -*-

"test_make_compensation"

import contextlib
import decimal
from functools import partial

import mock
import pytest
import json
from datetime import datetime as dt
from collections import OrderedDict

from balance.actions.nirvana.operations import make_compensation as mk
from balance.actions.unified_account import UnifiedAccountRelations
from balance import mapper, constants as cst, exc
from balance.constants import DIRECT_PRODUCT_RUB_ID, DIRECT_PRODUCT_ID
from balance.queue_processor import process_object
from tests.tutils import mock_transactions
from tests import object_builder as ob


PREV_RUN_ID = 0
CURR_RUN_ID = 1

INPUT_ROW_TEMPLATE = '''
{
    "service_order_id": "%s",
    "sum": "%s"
}'''

OPTIONS = '''
{
    "ticket": "PAYSUP-666",
    "service_id": 7,
    "compensation_reason": "promo",
    "compensation_data": "",
    "paysys_cc": ""
}'''

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


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


@contextlib.contextmanager
def mock_patch_type(type_id):
    with mock.patch(
        'balance.actions.nirvana.operations.make_compensation.Context.type', type_id
    ), mock.patch(
        'balance.actions.nirvana.task.TASK_ITEM_OP_MAP', {type_id: mk.process_task_item}
    ):
        yield


@pytest.fixture(autouse=True)
def task_type(session):
    res = ob.NirvanaTaskTypeBuilder.construct(session)
    with mock_patch_type(res.id):
        yield res


@contextlib.contextmanager
def does_not_raise():
    yield


def async_execution(session, flag):
    if flag:
        session.config.__dict__['PARALLELIZED_NIRVANA_OPERATIONS'] = ['make_compensation']
    else:
        session.config.__dict__['PARALLELIZED_NIRVANA_OPERATIONS'] = []


@pytest.fixture(autouse=True)
def mock_batch_processor():
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    calls = []

    def _process_batches(_s, func, batches, **kw):
        calls.append(batches)
        return map(partial(func, **kw), batches)

    with mock.patch(patch_path, _process_batches):
        yield calls


def create_order(client, product_id=DIRECT_PRODUCT_RUB_ID, **kwargs):
    return ob.OrderBuilder.construct(
        client.session,
        client=client,
        product_id=product_id,
        **kwargs
    )


def link_orders(main_order, children):
    UnifiedAccountRelations().link(main_order, children)
    main_order.turn_on_optimize()


def check_outputs(req_output):
    # Don't care about the order for convenience
    def to_set(list_of_dicts):
        result = set(tuple(sorted(d.iteritems())) for d in list_of_dicts)
        assert len(result) == len(list_of_dicts)
        return result

    assert to_set(OUTPUTS['success_report']) == to_set(req_output['success_report'])
    assert to_set(OUTPUTS['fail_report']) == to_set(req_output['fail_report'])


def check_outputs_1475(req_output):
    def _cast_list_to_dict(list_of_dicts):
        orders_d = {}
        for _num, _d in enumerate(sorted(list_of_dicts, key=lambda _d: _d['Consume'])):
            temp_d = {}
            for row in set(tuple(sorted(_d.iteritems()))):
                _k, _v = row
                temp_d[_k] = _v
            orders_d[_num] = temp_d
        return orders_d

    req_out = _cast_list_to_dict(req_output['success_report'])
    nb_out = _cast_list_to_dict(OUTPUTS['success_report'])
    assert len(req_out) == len(nb_out)
    for num, req_d in req_out.iteritems():
        nb_d = nb_out[num]
        for k, v in req_d.items():
            if k == 'Qty':
                assert decimal.Decimal(v) - decimal.Decimal(nb_d[k]) < decimal.Decimal('1'), k
            else:
                assert nb_d[k] == v, k


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
    if 'run_id' not in options:
        options['run_id'] = session.now().strftime('%Y-%m-%dT%H:%M:%S')
    nb = ob.NirvanaBlockBuilder(
        operation='make_compensation',
        request={
            'data': {
                'options': options,
                'inputs': ['input_rows']
            },
            'context': {
                'workflow': {
                    'owner': 'autodasha'
                }
            }
        }
    ).build(session).obj

    return nb


@pytest.fixture(name='task')
def create_task(task_type, block_id, state=cst.NirvanaProcessingTaskState.NEW, metadata=None):
    if metadata:
        run_id = metadata['parameters']['run_id']
    else:
        run_id = task_type.session.now().strftime('%Y-%m-%dT%H:%M:%S')
    return ob.NirvanaTaskBuilder.construct(
        task_type.session,
        task_type=task_type,
        state=state,
        block_id=block_id,
        run_id=run_id,
        metadata=metadata,
    )


def create_task_item(task, metadata=None, output=None, processed=0):
    return ob.NirvanaTaskItemBuilder.construct(
        task.session,
        task=task,
        metadata=metadata,
        output=output,
        processed=processed
    )


def patched_process(nirvana_block):
    session = nirvana_block.session

    with mock_transactions():
        nirvana_status = mk.process(nirvana_block)

        if nirvana_block.real_operation in session.config.get('PARALLELIZED_NIRVANA_OPERATIONS', []):
            task = session.query(mapper.NirvanaTask).getone(block_id=nirvana_block.id)
            for item in task.items:
                process_object(session, 'NIRVANA_TASK_ITEM', 'NirvanaTaskItem', item.id)

            nirvana_status = mk.process(nirvana_block)

    return nirvana_status


def update_options(options, **kwargs):
    for k, v in kwargs.iteritems():
        options[k] = v


@pytest.mark.parametrize(
    'prev_run_id, curr_run_id, prev_state, new_prev_state',
    [
        pytest.param(
            CURR_RUN_ID, PREV_RUN_ID,
            cst.NirvanaProcessingTaskState.FINISHED, cst.NirvanaProcessingTaskState.FINISHED,
            id='unordered runs'
        ),

        pytest.param(
            PREV_RUN_ID, CURR_RUN_ID,
            cst.NirvanaProcessingTaskState.NEW, cst.NirvanaProcessingTaskState.NEW,
            id='concurrent independent attempts'
        ),
        pytest.param(
            PREV_RUN_ID, CURR_RUN_ID,
            cst.NirvanaProcessingTaskState.IN_PROGRESS, cst.NirvanaProcessingTaskState.IN_PROGRESS,
            id='concurrent independent attempts'
        ),
        pytest.param(
            PREV_RUN_ID, CURR_RUN_ID,
            cst.NirvanaProcessingTaskState.FINISHED, cst.NirvanaProcessingTaskState.FINISHED,
            id='concurrent independent attempts - no archivation'
        ),

        pytest.param(
            PREV_RUN_ID, PREV_RUN_ID,
            cst.NirvanaProcessingTaskState.NEW, cst.NirvanaProcessingTaskState.IN_PROGRESS,
            id='concurrent dependent attempts - unstarted runs are retried'
        ),
        pytest.param(
            PREV_RUN_ID, PREV_RUN_ID,
            cst.NirvanaProcessingTaskState.IN_PROGRESS, cst.NirvanaProcessingTaskState.IN_PROGRESS,
            id='concurrent dependent attempts - started runs are rechecked'
        ),
        pytest.param(
            PREV_RUN_ID, PREV_RUN_ID,
            cst.NirvanaProcessingTaskState.FINISHED, cst.NirvanaProcessingTaskState.FINISHED,
            id='concurrent dependent attempts - prod runs results are reuploaded'
        ),
    ]
)
def test_states_transitions(
    session, task_type,
    prev_run_id, curr_run_id, prev_state, new_prev_state
):
    async_execution(session, True)

    nirvana_block = create_nirvana_block(
        session, {'service_id': 7, 'ticket': 'Q-0', 'compensation_reason': 'promo', 'run_id': curr_run_id}
    )
    nirvana_block.download = mock.Mock()
    nirvana_block.upload = mock.Mock()

    prev_task = create_task(
        task_type, nirvana_block.id, prev_state, metadata={'parameters': {'run_id': prev_run_id}}
    )
    task_item = create_task_item(prev_task, metadata={'item_idx': 0}, output={'success_report': 1}, processed=1)

    with mock_patch_type(task_type.id),\
        mock.patch('balance.actions.nirvana.operations.make_compensation.TaskLogic._check_task_active', return_value=1),\
        mock.patch('balance.actions.nirvana.operations.make_compensation.Context.compensation_data', []):
        mk.process(nirvana_block)
    session.expire_all()

    assert prev_task.state == new_prev_state


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_success_RUB_direct_product_compensation(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    orders = []
    success_report = []

    for _ in range(2):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    for o in orders:
        if o.consumes:
            q = o.consumes[0]
            invoice = q.invoice
            success_report.append(
                OrderedDict([
                    ('Invoice', invoice.external_id),
                    ('Paysys', invoice.paysys.name),
                    ('Sum', unicode(o.id)),
                    ('Qty', unicode(q.current_qty.as_decimal().quantize(1))),
                    ('Order', o.service_order_id),
                    ('Consume', q.id)
                ]))

            assert invoice.compensation_ticket_id == 'PAYSUP-666'
            assert invoice.compensation_reason == 'promo'
            assert invoice.passport_id
            assert invoice.passport_id == nb.passport_id

    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_success_direct_product_id_compensation(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    orders = []
    success_report = []

    for _ in range(2):
        o = create_order(client, product_id=DIRECT_PRODUCT_ID)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    for o in orders:
        if o.consumes:
            q = o.consumes[0]
            invoice = q.invoice
            success_report.append(
                OrderedDict([
                    ('Invoice', invoice.external_id),
                    ('Paysys', invoice.paysys.name),
                    ('Sum', unicode(o.id)),
                    ('Qty', unicode(q.current_qty.as_decimal().quantize(1))),
                    ('Order', o.service_order_id),
                    ('Consume', q.id)
                ]))

            assert invoice.compensation_ticket_id == 'PAYSUP-666'
            assert invoice.compensation_reason == 'promo'
            assert invoice.passport_id
            assert invoice.passport_id == nb.passport_id

    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    assert res.is_finished()

    check_outputs_1475(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_success_compensation_data_from_options(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    orders = []
    success_report = []

    for _ in range(2):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    options['compensation_data'] = '[' + ', '.join(input_rows) + ']'

    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    for o in orders:
        if o.consumes:
            q = o.consumes[0]
            invoice = q.invoice
            success_report.append(
                OrderedDict([
                    ('Invoice', invoice.external_id),
                    ('Paysys', invoice.paysys.name),
                    ('Sum', unicode(o.id)),
                    ('Qty', unicode(q.current_qty.as_decimal().quantize(1))),
                    ('Order', o.service_order_id),
                    ('Consume', q.id)
                ]))

            assert invoice.compensation_ticket_id == 'PAYSUP-666'
            assert invoice.compensation_reason == 'promo'
            assert invoice.passport_id
            assert invoice.passport_id == nb.passport_id

    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_success_child_order_compensation(session, client, async):
    async_execution(session, async)

    client.set_currency(7, 'RUB', dt.now(), None, force=True)
    options = json.loads(OPTIONS)
    success_report = []
    main_order = create_order(client, main_order=True)
    child_order = create_order(client)
    link_orders(main_order, [child_order])

    input_rows = [INPUT_ROW_TEMPLATE % (child_order.service_order_id, child_order.id)]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    q = main_order.consumes[0]
    invoice = q.invoice
    success_report.append(
        OrderedDict([
            ('Invoice', invoice.external_id),
            ('Paysys', invoice.paysys.name),
            ('Sum', unicode(child_order.id)),
            ('Qty', unicode(q.current_qty.as_decimal().quantize(1))),
            ('Order', main_order.service_order_id),
            ('Consume', q.id)
        ]))

    assert invoice.compensation_ticket_id == 'PAYSUP-666'
    assert invoice.compensation_reason == 'promo'
    assert invoice.passport_id
    assert invoice.passport_id == nb.passport_id

    required_outputs = {
        'success_report': success_report,
        'fail_report': []
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_wrong_service(session, client, async):
    async_execution(session, async)

    service_id = 666
    options = json.loads(OPTIONS)
    options['service_id'] = service_id
    orders = []

    for _ in range(1):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    with pytest.raises(Exception) as exc:
        patched_process(nb)
    assert exc.value.message == 'Unexpected service_id - %s, allowed only: %s' % (service_id, 7)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_empty_ticket_option(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    options['ticket'] = ''
    orders = []

    for _ in range(1):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    with pytest.raises(Exception) as exc:
        patched_process(nb)
    assert exc.value.message == 'Ticket number is required'


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_wrong_ticket_number(session, client, async):
    async_execution(session, async)

    ticket = '1aa-111'
    options = json.loads(OPTIONS)
    options['ticket'] = ticket
    orders = []

    for _ in range(1):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    with pytest.raises(Exception) as exc:
        patched_process(nb)
    assert exc.value.message == 'Unexpected ticket number - %s, it should be like EXAMPLE-123456' % ticket


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_empty_compensation_reason_option(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    options['compensation_reason'] = ''
    orders = []

    for _ in range(1):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    with pytest.raises(Exception) as exc:
        patched_process(nb)
    assert exc.value.message == 'Unexpected compensation reason'


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_unexpected_compensation_reason_option(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    options['compensation_reason'] = 'Ooh Eeh Ooh Ah Aah Ting Tang Walla Walla Bing Bang'
    orders = []

    for _ in range(1):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    with pytest.raises(Exception) as exc:
        patched_process(nb)
    assert exc.value.message == 'Unexpected compensation reason'


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_empty_compensation_data(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)

    INPUTS['input_rows'] = ''
    nb = create_nirvana_block(session, options)

    with pytest.raises(Exception) as exc:
        patched_process(nb)
    assert exc.value.message == 'Compensation data is required'


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_empty_mandatory_filed(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    order = create_order(client)
    order2 = create_order(client)

    input_row = json.loads(INPUT_ROW_TEMPLATE % ('', order.id))
    input_row2 = json.loads(INPUT_ROW_TEMPLATE % (order2.service_order_id, ''))

    INPUTS['input_rows'] = [input_row, input_row2]
    nb = create_nirvana_block(session, options)

    fail_report = [
        OrderedDict([
            ('Service_order_id', ''),
            ('Error', 'service_order_id is required field')
        ]),
        OrderedDict([
            ('Service_order_id', unicode(order2.service_order_id)),
            ('Error', 'sum is required field')
        ])
    ]

    res = patched_process(nb)

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_wrong_sum_format(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    order = create_order(client)

    input_row = json.loads(INPUT_ROW_TEMPLATE % (order.service_order_id, 'aaa'))

    INPUTS['input_rows'] = [input_row]
    nb = create_nirvana_block(session, options)

    fail_report = [
        OrderedDict([
            ('Service_order_id', unicode(order.service_order_id)),
            ('Error', 'Unexpected sum format')
            ])
    ]

    res = patched_process(nb)

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_zero_compensation_sum(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    fail_report = []

    order = create_order(client)
    fail_report.append(
        OrderedDict([
            ('Service_order_id', unicode(order.service_order_id)),
            ('Error', 'Wrong compensation sum')
        ]))

    input_rows = [INPUT_ROW_TEMPLATE % (order.service_order_id, 0)]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_not_allowed_compensation_currency(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    fail_report = []

    order = create_order(client, product_id=cst.DIRECT_PRODUCT_USD_ID)
    fail_report.append(
        OrderedDict([
            ('Service_order_id', unicode(order.service_order_id)),
            ('Error', 'Not allowed product currency')
        ]))

    input_rows = [INPUT_ROW_TEMPLATE % (order.service_order_id, order.id)]
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')
    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    required_outputs = {
        'success_report': [],
        'fail_report': fail_report
    }

    assert res.is_finished()

    check_outputs(required_outputs)


@pytest.mark.parametrize('async', [False, True])
@patcher
def test_mixed_compensation(session, client, async):
    async_execution(session, async)

    options = json.loads(OPTIONS)
    success_report = []
    fail_report = []
    orders = []

    order_usd = create_order(client, product_id=cst.DIRECT_PRODUCT_USD_ID)
    order_zero_sum = create_order(client)
    orders.append(order_usd)

    fail_report.extend(
        [
            OrderedDict([
                ('Service_order_id', unicode(order_usd.service_order_id)),
                ('Error', 'Not allowed product currency')
            ]),
            OrderedDict([
                ('Service_order_id', unicode(order_zero_sum.service_order_id)),
                ('Error', 'Wrong compensation sum')
            ])
        ]
    )

    for _ in range(2):
        o = create_order(client)
        orders.append(o)
    input_rows = [INPUT_ROW_TEMPLATE % (o.service_order_id, o.id) for o in orders]
    input_rows.append(INPUT_ROW_TEMPLATE % (order_zero_sum.service_order_id, 0))
    INPUTS['input_rows'] = json.loads('[' + ', '.join(input_rows) + ']')

    nb = create_nirvana_block(session, options)

    res = patched_process(nb)

    for o in orders:
        if o.consumes:
            q = o.consumes[0]
            invoice = q.invoice
            success_report.append(
                OrderedDict([
                    ('Invoice', invoice.external_id),
                    ('Paysys', invoice.paysys.name),
                    ('Sum', unicode(o.id)),
                    ('Qty', unicode(q.current_qty.as_decimal().quantize(1))),
                    ('Order', o.service_order_id),
                    ('Consume', q.id)
                ]))

            assert invoice.compensation_ticket_id == 'PAYSUP-666'
            assert invoice.compensation_reason == 'promo'
            assert invoice.passport_id
            assert invoice.passport_id == nb.passport_id

    required_outputs = {
        'success_report': success_report,
        'fail_report': fail_report
    }

    assert res.is_finished()

    check_outputs(required_outputs)



