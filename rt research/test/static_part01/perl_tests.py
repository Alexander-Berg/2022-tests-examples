import yatest.common
import os
import pytest
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
        check_exit_code=True
    )
    with open(os.path.join(os.getcwd(), '{}.report'.format(shard)), 'r') as f:
        report = json.load(f)
        for key in report:
            metrics.set(key, report[key])


TOTAL = 32


def test_static_check_part16(unpack, metrics):
    tst_perl(16, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part17(unpack, metrics):
    tst_perl(17, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part18(unpack, metrics):
    tst_perl(18, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part19(unpack, metrics):
    tst_perl(19, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part20(unpack, metrics):
    tst_perl(20, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part21(unpack, metrics):
    tst_perl(21, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part22(unpack, metrics):
    tst_perl(22, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part23(unpack, metrics):
    tst_perl(23, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part24(unpack, metrics):
    tst_perl(24, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part25(unpack, metrics):
    tst_perl(25, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part26(unpack, metrics):
    tst_perl(26, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part27(unpack, metrics):
    tst_perl(27, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part28(unpack, metrics):
    tst_perl(28, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part29(unpack, metrics):
    tst_perl(29, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part30(unpack, metrics):
    tst_perl(30, TOTAL, unpack[0], unpack[1], metrics)


def test_static_check_part31(unpack, metrics):
    tst_perl(31, TOTAL, unpack[0], unpack[1], metrics)
