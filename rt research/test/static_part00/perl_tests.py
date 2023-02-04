import yatest.common
import os
import pytest
import sys
import json
from bmtest.env import make_fs_root, make_merged_root, get_env


@pytest.fixture()
def unpack():
    return [make_fs_root(), make_merged_root()]


def tst_perl(shard, total, fs_root, merged_root, metrics):
    cmd = [
        '{}/rt-research/broadmatching/scripts/tests/static/check.pl'.format(merged_root),
        '--cwd={}/rt-research/broadmatching'.format(merged_root),
        '--threads=1',
        '--shard-pos={}'.format(shard),
        '--shards-total={}'.format(total),
        '--report-file', os.path.join(os.getcwd(), '{}.report'.format(shard)),
        '--silent',
    ]
    yatest.common.execute(
        cmd,
        env=get_env(fs_root),
        check_exit_code=True,
        stderr=sys.stderr,
        stdout=sys.stdout
    )
    with open(os.path.join(os.getcwd(), '{}.report'.format(shard)), 'r') as f:
        report = json.load(f)
        for key in report:
            metrics.set(key, report[key])


TOTAL = 32


def test_static_check_part00(unpack, metrics):
    tst_perl(0, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part01(unpack, metrics):
    tst_perl(1, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part02(unpack, metrics):
    tst_perl(2, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part03(unpack, metrics):
    tst_perl(3, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part04(unpack, metrics):
    tst_perl(4, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part05(unpack, metrics):
    tst_perl(5, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part06(unpack, metrics):
    tst_perl(6, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part07(unpack, metrics):
    tst_perl(7, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part08(unpack, metrics):
    tst_perl(8, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part09(unpack, metrics):
    tst_perl(9, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part10(unpack, metrics):
    tst_perl(10, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part11(unpack, metrics):
    tst_perl(11, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part12(unpack, metrics):
    tst_perl(12, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part13(unpack, metrics):
    tst_perl(13, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part14(unpack, metrics):
    tst_perl(14, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part15(unpack, metrics):
    tst_perl(15, TOTAL, unpack[0], unpack[1], metrics)
