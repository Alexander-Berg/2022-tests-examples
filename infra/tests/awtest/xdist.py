import atexit
import contextlib

import filelock
import os
import ujson


@contextlib.contextmanager
def run_once(tmpdir_factory, worker_id, config_name, config):
    # https://pytest-xdist.readthedocs.io/en/latest/how-to.html#making-session-scoped-fixtures-execute-only-once
    already_running = False
    config_path = str(tmpdir_factory.getbasetemp() / '..' / config_name)
    lock = filelock.FileLock(config_path + ".lock")
    if worker_id != 'master':
        lock.acquire()
        if os.path.exists(config_path):
            with open(config_path, 'r') as f:
                config.update(ujson.load(f))
            already_running = True
    try:
        yield config, already_running
    except:
        raise
    else:
        if worker_id != 'master':
            with open(config_path, 'w') as f:
                ujson.dump(config, f)
    finally:
        if worker_id != 'master':
            lock.release()


def register_cleanup(request, worker_id, func):
    if worker_id == 'master':
        request.addfinalizer(func)
    else:
        # if we have multiple workers, request.addfinalizer() would run in each worker independently,
        # so we run it at the process exit instead
        atexit.register(func)
