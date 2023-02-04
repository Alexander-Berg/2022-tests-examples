import sys
from inspect import currentframe, getframeinfo
from pathlib import Path


cur_filename = getframeinfo(currentframe()).filename
cur_folder = Path(cur_filename).resolve().parent
flow_runner_folder = cur_folder.parent
ok_folder = flow_runner_folder.parent
sys.path.append(str(ok_folder))
sys.path.append(str(flow_runner_folder))
sys.path.append(str(cur_folder))

try:
    import pytest
except ImportError:
    # runned outside container
    pass

from fixtures import run_app


def test_run_script():
    with run_app() as (conn, proc):
        code, resp = conn.get_data()
        assert code == 0
        assert resp == {'status': 'container_start'}
    assert proc.poll() == 0


def test_run_code():
    with run_app() as (conn, _):
        _ = conn.get_data()  # listen to start app msg
        conn.send_data({
            'type': 'run_script',
            'code': 'dump_ctx_keys()',
            'context': {'asd': 1},
        })
        _, resp = conn.get_data()
        assert resp['status'] == 'flow_completed', resp
        debug_info = resp['debug_info']
        assert 'asd' in debug_info['ctx_keys_dump'], debug_info


if __name__ == '__main__':
    # can place here any function to debug without Docker
    test_run_code()
