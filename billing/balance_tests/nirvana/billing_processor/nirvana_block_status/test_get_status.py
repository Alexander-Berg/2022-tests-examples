from balance.actions.nirvana.status import get_block_status


def test_get_running(session, nirvana_block):
    nirvana_block.status = nirvana_block.Status.RUNNING
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'RUNNING'
    assert res['result'] == 'UNDEFINED'
    assert res['message'] == 'Operation is running'
    assert res['progress'] == 0.5


def test_get_waiting(session, nirvana_block):
    nirvana_block.status = nirvana_block.Status.WAITING
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'WAITING'
    assert res['result'] == 'UNDEFINED'
    assert res['message'] == 'Operation is waiting'
    assert res['progress'] == 0.5


def test_get_finished(session, nirvana_block):
    nirvana_block.status = nirvana_block.Status.FINISHED
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'FINISHED'
    assert res['result'] == 'SUCCESS'
    assert res['message'] == 'Operation finished'
    assert res['progress'] == 1.0


def test_get_failed(session, nirvana_block, export):
    nirvana_block.status = nirvana_block.Status.FAILED
    export.traceback = 'Some traceback'
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'FINISHED'
    assert res['result'] == 'FAILURE'
    assert res['message'] == 'Operation failed'
    assert res['progress'] == 0.5
    assert res['details'] == 'Some traceback'


def test_get_failed_no_traceback(session, nirvana_block, export):
    nirvana_block.status = nirvana_block.Status.FAILED
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'FINISHED'
    assert res['result'] == 'FAILURE'
    assert res['message'] == 'Operation failed'
    assert res['progress'] == 0.5
    assert res['details'] is None


def test_get_cancelled(session, nirvana_block, export):
    nirvana_block.status = nirvana_block.Status.CANCELLED
    export.traceback = 'Some traceback'
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'FINISHED'
    assert res['result'] == 'STOP'
    assert res['message'] == 'Operation cancelled'
    assert res['progress'] == 0.5
    assert res['details'] == 'Some traceback'


def test_get_cancelled_no_traceback(session, nirvana_block, export):
    nirvana_block.status = nirvana_block.Status.CANCELLED
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'FINISHED'
    assert res['result'] == 'STOP'
    assert res['message'] == 'Operation cancelled'
    assert res['progress'] == 0.5
    assert res['details'] is None


def test_get_custom_running(session, nirvana_block):
    nirvana_block.status = nirvana_block.Status.RUNNING
    nirvana_block.progress = 0.42
    nirvana_block.message = 'Custom message'
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'RUNNING'
    assert res['result'] == 'UNDEFINED'
    assert res['message'] == 'Custom message'
    assert res['progress'] == 0.42


def test_get_custom_running_default_message(session, nirvana_block):
    nirvana_block.status = nirvana_block.Status.RUNNING
    nirvana_block.progress = 0.42
    session.flush()

    res = get_block_status(nirvana_block)

    assert res['status'] == 'RUNNING'
    assert res['result'] == 'UNDEFINED'
    assert res['message'] == 'Operation is running'
    assert res['progress'] == 0.42
