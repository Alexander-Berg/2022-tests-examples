#!/usr/bin/env python

import pytest
import logging
import subprocess
from infra.diskmanager.lib import mount
import yatest.common  # arcadia speciffic


log = logging.getLogger()
remount_bin = yatest.common.build_path("infra/diskmanager/utils/remount/remount")


def run(args, **kwargs):
    log.info("exec: '%s'",  " ".join(args))
    return subprocess.check_call(args, **kwargs)


@pytest.mark.parametrize("init_opts, init_want, remount_opts, remount_want", [
    (
        [""],
        ["relatime", "rw"],
        "lazytime",
        ["relatime", "rw", "lazytime"],
    ),
    (
        ["lazytime"],
        ["relatime", "rw", "lazytime"],
        "nolazytime",
        ["relatime", "rw"],
    ),
    (
        [""],
        ["relatime", "rw"],
        "noatime",
        ["rw", "noatime"],
    ),
    (
        ["discard,barrier=1"],
        ["relatime", "rw", "discard"],
        "nodiscard,barrier=0,lazytime",
        ["relatime", "rw", "nobarrier", "lazytime"],
    ),
])
def test_remount(make_mount_fs, init_opts, init_want, remount_opts, remount_want):
    mpath = make_mount_fs(init_opts)
    mi = mount.find_by_path(mpath)
    assert mi.path == mpath
    assert init_want == mi.opts

    run([remount_bin, mpath,  remount_opts])
    mi = mount.find_by_path(mpath)
    assert mi.path == mpath
    assert remount_want == mi.opts
