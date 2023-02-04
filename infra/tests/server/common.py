import os
import logging

log = logging.getLogger(__name__)


def prepare_utils(build_root):
    if not os.path.exists("/opt/diskmanager/utils"):
        os.makedirs("/opt/diskmanager/utils")
    for u in ["dqsync", "remount"]:
        src = build_root(os.path.join("infra/diskmanager/utils", u, u))
        dst = os.path.join("/opt/diskmanager/utils", u)
        log.info("check util: %s", src)
        assert os.path.exists(src)
        if not os.path.exists(dst):
            os.symlink(src, dst)
