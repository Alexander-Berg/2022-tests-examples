pytest_plugins = ['kernel.util.pytest']

import pyjack
import cov_core


def _on_py_fork_exits(proc):
    cov = cov_core._cov_data.pop(proc)
    if cov:
        cov_core.multiprocessing_finish(cov)


# pyjack.replace_all_refs(cov_core.on_py_fork_exits, _on_py_fork_exits)
