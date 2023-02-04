# -*- coding: utf-8 -*-
import subprocess
from collections import namedtuple


CallResult = namedtuple('CallResult', ['stdout', 'stderr', 'return_code'])


def call_weak(cmd, text=None):
    cmd = [str(s) for s in cmd]
    process = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate(input=text)
    return CallResult(stdout, stderr, process.returncode)


def call(cmd, text=None):
    result = call_weak(cmd, text=text)
    assert result.return_code == 0, result.stderr
    return result


def openssl(cmd, text=None):
    return call(['openssl'] + cmd, text=text)
