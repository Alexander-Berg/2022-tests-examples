import os
import sys
import ctypes
import signal


def prepare_kill_pg():
    PR_SET_PDEATHSIG = 1
    libc = ctypes.CDLL(None)
    try:
        libc.prctl(PR_SET_PDEATHSIG, signal.SIGINT)
    except BaseException as e:
        print(f"Exception in setting signals: {e}, skipping", file=sys.stderr, flush=True)

    os.setpgrp()
