import pytest
from subprocess32 import CalledProcessError
from checks import walle_fs_check

EXT4_ERRORS_GOOD_RAW = """
{ "__CURSOR" : "s=2c2f7f3d64d844aa9bc1844a48026313;i=1f345;b=c4b33c96f240457abc130dcb73da2b24;m=192b58ccf5;t=5aec659160d52;x=eb931437dac873e3", "__REALTIME_TIMESTAMP" : "1599541804928338", "__MONOTONIC_TIMESTAMP" : "108101422325", "_BOOT_ID" : "c4b33c96f240457abc130dcb73da2b24", "_MACHINE_ID" : "6641fb6ac4e235aef340bea75f556792", "_HOSTNAME" : "vla2-6758.search.yandex.net", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "_SOURCE_MONOTONIC_TIMESTAMP" : "108101136410", "PRIORITY" : "2", "MESSAGE" : "EXT4-fs error (device sdc2): ext4_wait_block_bitmap:519: comm Trash:store1: Cannot read block bitmap - block_group = 2571, block_bitmap = 83886091" }
{ "__CURSOR" : "s=2c2f7f3d64d844aa9bc1844a48026313;i=1f346;b=c4b33c96f240457abc130dcb73da2b24;m=192b594fbf;t=5aec65916901c;x=3ed7ff8758066553", "__REALTIME_TIMESTAMP" : "1599541804961820", "__MONOTONIC_TIMESTAMP" : "108101455807", "_BOOT_ID" : "c4b33c96f240457abc130dcb73da2b24", "_MACHINE_ID" : "6641fb6ac4e235aef340bea75f556792", "_HOSTNAME" : "vla2-6758.search.yandex.net", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "PRIORITY" : "2", "_SOURCE_MONOTONIC_TIMESTAMP" : "108101169896", "MESSAGE" : "EXT4-fs error (device sdc2) in ext4_free_blocks:4969: IO failure" }
"""

EXT4_ERRORS_GOOD_EXPECTED = [
    "EXT4-fs error (device sdc2): ext4_wait_block_bitmap:519: comm Trash:store1: Cannot read block bitmap - block_group = 2571, block_bitmap = 83886091",
    "EXT4-fs error (device sdc2) in ext4_free_blocks:4969: IO failure"
]

EXT4_ERRORS_FAIL_RAW = """
{ "__CURSOR" : "s=7bd0fc5b3b334da3af600f1e95e17d0a;i=4f715;b=ffc57caa40a74f089f30bd98f21bbb54;m=3a45de0e6d;t=5aeb91feab28c;x=6f01b46423f237d8", "__REALTIME_TIMESTAMP" : "1599485011210892", "__MONOTONIC_TIMESTAMP" : "250280283757", "_BOOT_ID" : "ffc57caa40a74f089f30bd98f21bbb54", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "_MACHINE_ID" : "eece1da8102929dca99cd50c5f525eb5", "_HOSTNAME" : "vla3-3255.search.yandex.net", "_SOURCE_MONOTONIC_TIMESTAMP" : "250278904537", "PRIORITY" : "2", "MESSAGE" : "EXT4-fs error (device sdh2): ext4_wait_block_bitmap:519: comm Trash:store6: Cannot read block bitmap - block_group = 4467, block_bitmap = 146276355" }
{ "__CURSOR" : "s=7bd0fc5b3b334da3af600f1e95e17d0a;i=4f725;b=ffc57caa40a74f089f30bd98f21bbb54;m=3a45dfdcb8;t=5aeb91fec80d6;x=298432215afb5507", "__REALTIME_TIMESTAMP" : "1599485011329238", "__MONOTONIC_TIMESTAMP" : "250280402104", "_BOOT_ID" : "ffc57caa40a74f089f30bd98f21bbb54", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "_MACHINE_ID" : "eece1da8102929dca99cd50c5f525eb5", "_HOSTNAME" : "vla3-3255.search.yandex.net", "PRIORITY" : "2", "_SOURCE_MONOTONIC_TIMESTAMP" : "250279023338", "MESSAGE" : "EXT4-fs error (device sdh2) in ext4_free_blocks:4969: IO failure" }
{ "__CURSOR" : "s=7bd0fc5b3b334da3af600f1e95e17d0a;i=500e8;b=ffc57caa40a74f089f30bd98f21bbb54;m=3ab2256745;t=5aeb98c320b64;x=28106226b13d6105", "__REALTIME_TIMESTAMP" : "1599486827826020", "__MONOTONIC_TIMESTAMP" : "252096898885", "_BOOT_ID" : "ffc57caa40a74f089f30bd98f21bbb54", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "_MACHINE_ID" : "eece1da8102929dca99cd50c5f525eb5", "_HOSTNAME" : "vla3-3255.search.yandex.net", "PRIORITY" : "2", "_SOURCE_MONOTONIC_TIMESTAMP" : "252095506471", "MESSAGE" : "EXT4-fs (sdh2): Delayed block allocation failed for inode 319631089 at logical offset 69632 with max blocks 2048 with error 117" }
{ "__CURSOR" : "s=7bd0fc5b3b334da3af600f1e95e17d0a;i=500e9;b=ffc57caa40a74f089f30bd98f21bbb54;m=3ab225678d;t=5aeb98c320bac;x=67d3a15f33c2724b", "__REALTIME_TIMESTAMP" : "1599486827826092", "__MONOTONIC_TIMESTAMP" : "252096898957", "_BOOT_ID" : "ffc57caa40a74f089f30bd98f21bbb54", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "_MACHINE_ID" : "eece1da8102929dca99cd50c5f525eb5", "_HOSTNAME" : "vla3-3255.search.yandex.net", "PRIORITY" : "2", "_SOURCE_MONOTONIC_TIMESTAMP" : "252095506519", "MESSAGE" : "EXT4-fs (sdh2): This should not happen!! Data will be lost\\n" }
"""

EXT4_ERRORS_FAIL_EXPECTED = [
    "EXT4-fs error (device sdh2): ext4_wait_block_bitmap:519: comm Trash:store6: Cannot read block bitmap - block_group = 4467, block_bitmap = 146276355",
    "EXT4-fs error (device sdh2) in ext4_free_blocks:4969: IO failure",
    "EXT4-fs (sdh2): Delayed block allocation failed for inode 319631089 at logical offset 69632 with max blocks 2048 with error 117",
    "EXT4-fs (sdh2): This should not happen!! Data will be lost\n"
]

EXT4_ERRORS_RANDOM_RAW = """
{ "__CURSOR" : "s=7bd0fc5b3b334da3af600f1e95e17d0a;i=4f715;b=ffc57caa40a74f089f30bd98f21bbb54;m=3a45de0e6d;t=5aeb91feab28c;x=6f01b46423f237d8", "__REALTIME_TIMESTAMP" : "1599485011210892", "__MONOTONIC_TIMESTAMP" : "250280283757", "_BOOT_ID" : "ffc57caa40a74f089f30bd98f21bbb54", "_TRANSPORT" : "kernel", "SYSLOG_FACILITY" : "0", "SYSLOG_IDENTIFIER" : "kernel", "_MACHINE_ID" : "eece1da8102929dca99cd50c5f525eb5", "_HOSTNAME" : "vla3-3255.search.yandex.net", "_SOURCE_MONOTONIC_TIMESTAMP" : "250278904537", "PRIORITY" : "2", "MESSAGE" : "RANDOM" }
"""

EXT4_ERRORS_RANDOM_EXPECTED = []


@pytest.mark.parametrize("raw_output,expected", [
    (EXT4_ERRORS_GOOD_RAW, EXT4_ERRORS_GOOD_EXPECTED),
    (EXT4_ERRORS_FAIL_RAW, EXT4_ERRORS_FAIL_EXPECTED),
    (EXT4_ERRORS_RANDOM_RAW, EXT4_ERRORS_RANDOM_EXPECTED),
])
def test_journalctl_success_call(raw_output, expected):

    def check_output(command, **kwargs):
        assert command[0] == "journalctl"
        return raw_output

    checker = walle_fs_check.Checker(check_output=check_output)
    assert checker.collect_ext4_fs_errors() == expected


def test_journalctl_failed_call():

    def check_output(command, **kwargs):
        assert command[0] == "journalctl"
        raise CalledProcessError(2, command, output="failed")

    checker = walle_fs_check.Checker(check_output=check_output)
    assert checker.collect_ext4_fs_errors() == []


def test_journalctl_not_found_call():

    def check_output(command, **kwargs):
        assert command[0] == "journalctl"
        raise OSError()

    checker = walle_fs_check.Checker(check_output=check_output)
    assert checker.collect_ext4_fs_errors() == []


@pytest.mark.parametrize("partition,error_count,raw_output,status,message,crit,warn", [
    ("sdc2", 2, EXT4_ERRORS_GOOD_RAW, walle_fs_check.DEV_STATUS_FAILED, "partition sdc2 has 2 ext4 errors", 1, 0),
    ("sdh2", 5, EXT4_ERRORS_GOOD_RAW, walle_fs_check.DEV_STATUS_FAILED, "partition sdh2 has 5 ext4 errors", 1, 0),
    ("sda2", 0, EXT4_ERRORS_FAIL_RAW, walle_fs_check.DEV_STATUS_OK, "no errors", 0, 0),
    ("sdh2", 1, EXT4_ERRORS_FAIL_RAW, walle_fs_check.DEV_STATUS_FAILED, "partition sdh2 has 1 ext4 errors", 1, 0),
    ("sda2", 0, EXT4_ERRORS_RANDOM_RAW, walle_fs_check.DEV_STATUS_OK, "no errors", 0, 0),
])
def test_find_error_with_ext4_errors(partition, error_count, raw_output, status, message, crit, warn):
    checker = walle_fs_check.Checker(check_output=lambda command, **kwargs: raw_output)
    ext4_fs_errors = checker.collect_ext4_fs_errors()
    assert checker.find_error(partition, error_count, ext4_fs_errors) == ({
        'name': partition,
        'error_count': error_count,
        'threshold': walle_fs_check.EXT4_ERROR_THRESHOLD,
        'status': status,
        'message': message
    }, crit, warn)
