# -*- coding: utf-8 -*-
import json
import pytest
import mock
import hamcrest as hm

from balance import (
    constants as cst,
)
from balance.actions.nirvana.operations.log_processing.postrestore_db import process

from tests import object_builder as ob
from tests.tutils import has_exact_entries

from tests.balance_tests.nirvana.log_processing.common import (
    create_invoice,
    create_act,
)

RUN_ID = '2020-12-10T23:23:12'


@pytest.fixture
def direct_tasks(session):
    direct_task_type = ob.LogTariffTypeBuilder.construct(session)
    direct_act_task_type = ob.LogTariffTypeBuilder.construct(session)
    direct_migration_task_type = ob.LogTariffTypeBuilder.construct(session)

    ob.LogTariffTaskBuilder.construct(session, task_type=direct_task_type,
                                      metadata={'data_old': 'direct'}, state=cst.NirvanaProcessingTaskState.ARCHIVED)
    ob.LogTariffTaskBuilder.construct(session, task_type=direct_act_task_type,
                                      metadata={'data_old': 'direct_act'}, state=cst.NirvanaProcessingTaskState.ARCHIVED)
    ob.LogTariffTaskBuilder.construct(session, task_type=direct_migration_task_type,
                                      metadata={'data_old': 'direct_migration'}, state=cst.NirvanaProcessingTaskState.ARCHIVED)

    return [
        ob.LogTariffTaskBuilder.construct(session, task_type=direct_task_type,
                                          metadata={'data': 'direct'}, state=cst.NirvanaProcessingTaskState.IN_PROGRESS),
        ob.LogTariffTaskBuilder.construct(session, task_type=direct_act_task_type,
                                          metadata={'data': 'direct_act'}, state=cst.NirvanaProcessingTaskState.IN_PROGRESS),
        ob.LogTariffTaskBuilder.construct(session, task_type=direct_migration_task_type,
                                          metadata={'data': 'direct_migration'}, state=cst.NirvanaProcessingTaskState.FINISHED),
    ]


@pytest.fixture
def nirvana_block(session, direct_tasks):
    block = ob.NirvanaBlockBuilder.construct(
        session,
        operation='prepare_migration',
        request={'data': {'options': {'metadata_types': json.dumps([tsk.task_type.id for tsk in direct_tasks])}}},
    )
    with mock.patch.object(block, 'upload'), mock.patch.object(block, 'download'):
        yield block


@pytest.fixture()
def act_rows(session):
    acts = [create_act(create_invoice(session)) for _i in range(2)]
    for act, id_ in zip(acts, [9, 52]):
        act.external_id = 'YB-%s' % id_
    session.flush()
    return acts


@pytest.mark.usefixtures('act_rows')
def test_base(session, nirvana_block, direct_tasks):
    process(nirvana_block)

    session.expire_all()
    for tsk in direct_tasks:
        assert tsk.state == cst.NirvanaProcessingTaskState.FINISHED, 'Invalid status for task_type=%s' % tsk.task_type.id

    assert len(nirvana_block.upload.call_args_list) == 1
    metadata_data, = nirvana_block.upload.call_args_list

    tag, data = metadata_data[0]
    assert tag == 'out'
    data = json.loads(data)
    assert 'run_id' in data
    assert data['act_sequence_pos'] == 53
    hm.assert_that(
        data['metadata'],
        has_exact_entries({
            tsk.task_type.id: has_exact_entries(tsk.metadata)
            for tsk in direct_tasks
        })
    )
