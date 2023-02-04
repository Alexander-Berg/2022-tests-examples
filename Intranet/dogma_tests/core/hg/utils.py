# coding: utf-8



import yatest

import subprocess

subprocess.check_call(
    ['tar', '-xzf', 'hg/resource.tar.gz'],
    cwd=yatest.common.runtime.work_path()
)

HG_ROOT = yatest.common.runtime.work_path('hg/auto')
