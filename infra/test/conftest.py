import tempfile
import shutil
import pytest
import os
import subprocess
import logging

log = logging.getLogger()


def run(args, **kwargs):
    log.info("exec: '%s'",  " ".join(args))
    return subprocess.check_call(args, **kwargs)


def run_nocheck(args, **kwargs):
    log.info("exec: '%s'",  " ".join(args))
    return subprocess.call(args, **kwargs)


@pytest.fixture
def make_mount_fs(request):
    dirs = []
    mounts = []

    def _make_mount_fs(mnt_opts=[]):
        tdir = tempfile.mkdtemp()
        dirs.append(tdir)
        img = os.path.join(tdir, "img")
        mpath = os.path.join(tdir, "mnt")
        with open(img, "w") as f:
            f.truncate(1024 ** 3)
        run(["mkfs.ext4", "-q", "-F", img])
        mnt_opts.append("loop")
        os.makedirs(mpath)
        run(["mount", "-t", "ext4", img, mpath, "-o" + ",".join(mnt_opts)])
        mounts.append(mpath)
        return mpath

    yield _make_mount_fs

    for m in mounts:
        run_nocheck(["umount", m])
    for d in dirs:
        shutil.rmtree(d)
