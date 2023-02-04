# coding: utf-8
import os

import pytest
import yatest
import subprocess
THIS_REPO_PATH = yatest.common.runtime.work_path('git')

subprocess.check_call(
    ['tar', '-xzf', 'git/resource.tar.gz'],
    cwd=yatest.common.runtime.work_path()
)

skipif_no_git = pytest.mark.skipif(
    not os.path.exists(THIS_REPO_PATH),
    reason='No git repository',
)
