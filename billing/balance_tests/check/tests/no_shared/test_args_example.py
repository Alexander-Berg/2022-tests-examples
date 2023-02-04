# coding=utf-8

import pytest

check_all = {'aob_all': ['aob_sw', 'aob_tr', 'aob_us', 'aob', 'aob_ua', 'aob_taxi', 'aob_vertical'],
             'iob_all': ['iob_sw', 'iob_tr', 'iob_us', 'iob', 'iob_ua', 'iob_taxi', 'iob_vertical']
             }
DEFAULT_CHECK_LIST = ['aob']

# --checklist arg: list of checks to run. 'aob_all' will be replaced with all aob checks
# usage: --checklist "aob_sw,aob_us"
arg = pytest.config.getoption("--checklist")
check_list = arg.split(',') if arg else DEFAULT_CHECK_LIST
for check in check_all:
    if check in check_list:
        check_list.pop(check_list.index[check])
        check_list.extend(check_all[check])

# --force: flag for data re-generation.
# usage: --force
arg = pytest.config.getoption("--force")
force = True if arg else False

def _one():
    assert force

def _twa():
    assert 'aob_sw' in check_list


if __name__ == "__main__":
    pytest.main('-v --checklist "aob_sw,aob"')