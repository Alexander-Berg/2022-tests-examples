# coding=utf-8

import pytest
from datetime import datetime

from butils.application import HOST

from pycron.sqlalchemy_mappers import JobLock, JobState, JobDescr

from cluster_tools.nirvana_monitor import NirvanaMonitorApp

from balance.actions.nirvana.operations import OP_MAP

from tests import object_builder as ob


def create_job(session, pid):
    job_desc = session.query(JobDescr) \
        .filter(JobDescr.name == 'nirvana_block-processor') \
        .first()
    job_desc.create_lock_and_state(session, HOST)

    lock = JobLock(job_desc.name, HOST)
    now = datetime.now()
    lock.state = JobState(started=now, finished=now)
    session.add(lock)
    session.flush()

    lock.state.initialize(session)
    lock.state.set_started(pid)
    session.flush()


def create_nirvana_block(session, opcode, **kwargs):
    nb = ob.NirvanaBlockBuilder(
        operation='universal',
        request=
        {
            'data': {
                'options': {
                    'opcode': opcode,
                }
            }
        },
        **kwargs
    ).build(session).obj

    nb.enqueue('NIRVANA_BLOCK')
    session.flush()

    return nb


@pytest.mark.parametrize("opcode", ["balance_dcs_run_action", "run_firm_interbranch_calc"])
def test_get_blocks_to_kill_one(session, opcode):
    nb = create_nirvana_block(session, opcode, terminate=1)
    create_job(session, nb.pid)
    assert len(NirvanaMonitorApp.get_blocks_to_kill(session)) == 1


@pytest.mark.parametrize("opcode", ["opcode_not_in_whitelist", "load_yt_log"])
def test_get_blocks_to_kill_zero(session, opcode):
    nb = create_nirvana_block(session, opcode, terminate=1)
    create_job(session, nb.pid)
    assert len(NirvanaMonitorApp.get_blocks_to_kill(session)) == 0


@pytest.mark.parametrize("opcode", ["balance_dcs_run_action", "load_yt_log"])
def test_get_blocks_to_kill_not_terminated(session, opcode):
    nb = create_nirvana_block(session, opcode, terminate=0)
    create_job(session, nb.pid)
    assert len(NirvanaMonitorApp.get_blocks_to_kill(session)) == 0


def test_get_blocks_to_kill_multiple(session):
    opcodes = OP_MAP.iterkeys()

    blocks = []
    for pid, opcode in enumerate(opcodes, 1):
        blocks.append(create_nirvana_block(session, opcode, terminate=0, pid=2 * pid))
        blocks.append(create_nirvana_block(session, opcode, terminate=1, pid=2 * pid + 1))

    for block in blocks:
        create_job(session, block.pid)

    # толко две операции из списка входят в whitelist и помечены terminated=1
    assert len(NirvanaMonitorApp.get_blocks_to_kill(session)) == 2
