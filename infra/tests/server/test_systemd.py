#!/usr/bin/env python

import os
import pytest
import subprocess
import time
from infra.diskmanager.lib import consts
import yatest.common  # arcadia speciffic

diskmanager_bin = yatest.common.build_path("infra/diskmanager/server/diskmanager")
dmctl_bin = yatest.common.build_path("infra/diskmanager/client/dmctl")


@pytest.mark.skipif(os.geteuid() != 0, reason='Not privileged user')
@pytest.mark.skipif(not os.path.exists('/lib/systemd/system/yandex-diskmanager.service'), reason='yandex-diskmanager.service is not available')
class TestSystemdService(object):
    def test_toggle_service(self):
        subprocess.check_call(['systemctl', 'disable', 'yandex-diskmanager.service'])
        subprocess.check_call(['systemctl', 'enable', 'yandex-diskmanager.service'])
        subprocess.check_call(['systemctl', 'disable', 'yandex-diskmanager.service'])
        subprocess.check_call(['systemctl', 'enable', 'yandex-diskmanager.service'])

    def test_01_start_service(self):
        subprocess.check_call(['systemctl', 'disable', 'yandex-diskmanager.service'])
        subprocess.check_call(['systemctl', 'stop', 'yandex-diskmanager.service'])
        subprocess.check_call(['systemctl', 'enable', 'yandex-diskmanager.service'])
        subprocess.check_call(['systemctl', 'start', 'yandex-diskmanager.service'])
        assert os.path.exists(consts.DEFAULT_SERVER_UNIX_SOCK)
        assert os.stat(consts.DEFAULT_SERVER_UNIX_SOCK).st_uid == 0
        assert os.stat(consts.DEFAULT_SERVER_UNIX_SOCK).st_gid == consts.DEFAULT_GROUP_ID
        # Prevent false positive failures
        ret = -1
        for left in range(0, 10):
            ret = subprocess.call([dmctl_bin, "list"])
            if not ret:
                break
            time.sleep(3)
        assert ret == 0
