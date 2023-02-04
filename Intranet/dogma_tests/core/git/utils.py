# coding: utf-8

import yatest

import subprocess

subprocess.check_call(
    ['tar', '-xzf', 'git/resource.tar.gz'],
    cwd=yatest.common.runtime.work_path()
)

subprocess.check_call(
    ['tar', '-xzf', 'magiclinks/resource.tar.gz'],
    cwd=yatest.common.runtime.work_path()
)

GIT_ROOT = yatest.common.runtime.work_path('git')

GIT_ML_ROOT = yatest.common.runtime.work_path('magiclinks')
