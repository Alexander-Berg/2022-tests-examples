# -*- coding: utf-8 -*-

import pytest

from tests import object_builder as ob


@pytest.fixture()
def nirvana_block(session):
    rv = ob.NirvanaBlockBuilder.construct(
        session,
        request={
            'data': {
                'options': {
                    'mnclose_task': 'some_mnclose_task',
                    'check_delay': 19,
                },
            },
        },
    )
    session.flush()
    return rv


@pytest.fixture()
def db_task(session, nirvana_block):
    return ob.NirvanaMnCloseSyncBuilder.construct(
        session,
        task_id=nirvana_block.options['mnclose_task'],
    )
