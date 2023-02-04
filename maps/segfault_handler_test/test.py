import tempfile
import shutil
import signal
from pathlib import Path

import requests

import yatest.common as yc


def test_backtrace_logging_on_segfault(testapp):
    trace_dump_path = Path(tempfile.gettempdir()) / 'yacare-testapp.trace'
    trace_dump_backup_path = Path(yc.output_path('yacare-testapp.trace'))

    try:
        testapp.start(attempts=30)  # increase wait time to prevent flapping under sanitizers
        testapp.send_signal(signal.SIGSEGV)
        testapp.process.wait(check_exit_code=False, timeout=10)

        assert trace_dump_path.exists()
        shutil.copy(trace_dump_path, trace_dump_backup_path)

        testapp.start()
        testapp.stop(timeout=30)

        log = testapp.stderr
        assert b'Previous run crashed:' in log
        assert b'dumpStacktraceToFile' in log

        assert not trace_dump_path.exists()
    finally:
        if trace_dump_path.exists():
            trace_dump_path.unlink()


def test_termination_handlers(testapp, tmp_path, freeport):
    testapp.listen_port = freeport
    testapp.start(attempts=30)  # increase wait time to prevent flapping under sanitizers

    goodfile = tmp_path / 'file.lock'
    badfile = tmp_path / 'will.remove'

    with requests.Session() as s:
        for path in (goodfile, badfile):
            response = s.post(
                f'http://localhost:{freeport}/filelock',
                params={'path': str(path)})

            assert response.status_code == 200
            assert path.is_file()

        badfile.unlink()

        testapp.send_signal(signal.SIGTERM)
        testapp.wait(30)

        assert not goodfile.exists()
        assert not badfile.exists()
