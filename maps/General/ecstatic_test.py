import sys

import yatest

from maps.infra.ecstatic.tool.ecstatic_api import ecstatic_api
from maps.infra.ecstatic.tool.client_interface.local_lock import LocalLockContextDecorator

tests_data_path = yatest.common.source_path("maps/infra/ecstatic/tool/ut/data")
ECSTATIC_BIN = yatest.common.binary_path('maps/infra/ecstatic/tool/ecstatic')


def run_ecstatic(cmds, expect_stdout="", expect_stderr="", expect_exit_code=0):
    cmdline = [ECSTATIC_BIN, '--dry-run'] + cmds
    res = yatest.common.execute(cmdline, wait=True, check_exit_code=False)

    def err(msg):
        print(msg + ":", file=sys.stderr)
        print("Exit code is ", res.exit_code, file=sys.stderr)
        if res.std_out:
            print("--- stdout ---", file=sys.stderr)
            print(res.std_out, file=sys.stderr)
            print("--- expected ---\n", file=sys.stderr)
            print(expect_stdout, file=sys.stderr)
            print("--------------\n", file=sys.stderr)
        if res.std_err:
            print("--- stderr ---", file=sys.stderr)
            print(res.std_err, file=sys.stderr)
            print("--- expected ---\n", file=sys.stderr)
            print(expect_stderr, file=sys.stderr)
            print("--------------\n", file=sys.stderr)
        raise Exception("Fail: " + msg)

    expect_stdout = expect_stdout.encode("ascii")
    expect_stderr = expect_stderr.encode("ascii")

    if res.exit_code != expect_exit_code:
        err("Exit code mismatch")

    if res.std_out.strip() != expect_stdout.strip():
        err("Stdout mismatch")

    if res.std_err.strip() != expect_stderr.strip():
        err("Stderr mismatch")


def test_version_1():
    run_ecstatic(["versions"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic versions [OPTIONS] DATASET [BRANCH]
Try 'ecstatic versions --help' for help.

Error: Missing argument 'DATASET'.
""")


def test_version_2():
    run_ecstatic(["versions", "dataset"], expect_stdout="""
Dry-run with arguments:
 branch: None
 dataset: dataset
 with_status: False

""")


def test_version_3():
    run_ecstatic(["versions", "dataset", "--with-status"], expect_stdout="""
Dry-run with arguments:
 branch: None
 dataset: dataset
 with_status: True

""")


def test_remove_1():
    run_ecstatic(["remove"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic remove [OPTIONS] <dataset>=<version>
Try 'ecstatic remove --help' for help.

Error: Missing argument '<dataset>=<version>'.
""")


def test_remove_2():
    run_ecstatic(["remove", "dataset"], expect_exit_code=1, expect_stderr="""
ecstatic: Expected <dataset>=<version>
""")


def test_remove_3():
    run_ecstatic(["remove", "dataset=123"], expect_stdout="""
Dry-run with arguments:
 dataset: dataset=123
 name: dataset
 version: 123
""")


def test_move_1():
    run_ecstatic(["move"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic move [OPTIONS] <dataset>=<version> [BRANCHES]...
Try 'ecstatic move --help' for help.

Error: Missing argument '<dataset>=<version>'.
""")


def test_move_2():
    run_ecstatic(["move", "dataset"], expect_exit_code=1, expect_stderr="""
ecstatic: Expected <dataset>=<version>
""")


def test_move_3():
    run_ecstatic(["move", "dataset=123"], expect_exit_code=1, expect_stderr="""
ecstatic: At least 1 Branch ID should be specified
""")


def test_move_4():
    run_ecstatic(["move", "dataset=123", "+stable"], expect_stdout="""
Dry-run with arguments:
 branches: ('+stable',)
 dataset: dataset=123
 name: dataset
 version: 123
""")


def test_upload_1():
    run_ecstatic(["upload"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic upload [OPTIONS] <dataset>=<version> DIRECTORY [BRANCHES]...
Try 'ecstatic upload --help' for help.

Error: Missing argument '<dataset>=<version>'.
""")


def test_upload_2():
    run_ecstatic(["upload", "dataset=123", "thisdir"], expect_stdout="""
Dry-run with arguments:
 backbone_limit: None
 branches: ()
 dataset: dataset=123
 debug_torrent: False
 directory: thisdir
 fastbone_limit: None
 libtorrent_option: ()
 name: dataset
 version: 123
""")


def test_upload_3():
    run_ecstatic(["upload", "dataset=123", "thisdir", "+stable"], expect_stdout="""
Dry-run with arguments:
 backbone_limit: None
 branches: ('+stable',)
 dataset: dataset=123
 debug_torrent: False
 directory: thisdir
 fastbone_limit: None
 libtorrent_option: ()
 name: dataset
 version: 123
""")


def test_upload_4():
    run_ecstatic(["upload", "dataset=123", "thisdir", "+stable", "+testing"], expect_stdout="""
Dry-run with arguments:
 backbone_limit: None
 branches: ('+stable', '+testing')
 dataset: dataset=123
 debug_torrent: False
 directory: thisdir
 fastbone_limit: None
 libtorrent_option: ()
 name: dataset
 version: 123
""")


def test_download_1():
    run_ecstatic(["download"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic download [OPTIONS] <dataset>=<version>
Try 'ecstatic download --help' for help.

Error: Missing argument '<dataset>=<version>'.
""")


def test_download_2():
    run_ecstatic(["download", "dataset=123"], expect_stdout="""
Dry-run with arguments:
 args: [('dataset', '123', 'dataset_123')]
 backbone_limit: None
 dataset: ('dataset=123',)
 debug_torrent: False
 dest: dataset_123
 fastbone_limit: None
 i: 0
 libtorrent_option: ()
 name: dataset
 out: ()
 version: 123
""")


def test_download_3():
    run_ecstatic(["download", "dataset=123", "-o", "outdir"], expect_stdout="""
Dry-run with arguments:
 args: [('dataset', '123', 'outdir')]
 backbone_limit: None
 dataset: ('dataset=123',)
 debug_torrent: False
 dest: outdir
 fastbone_limit: None
 i: 0
 libtorrent_option: ()
 name: dataset
 out: ('outdir',)
 version: 123
""")


def test_download_4():
    run_ecstatic(["download", "dataset=123", "-o", "outdir1", "dataset=456", "-o", "outdir2"], expect_stdout="""
Dry-run with arguments:
 args: [('dataset', '123', 'outdir1'), ('dataset', '456', 'outdir2')]
 backbone_limit: None
 dataset: ('dataset=123', 'dataset=456')
 debug_torrent: False
 dest: outdir2
 fastbone_limit: None
 i: 1
 libtorrent_option: ()
 name: dataset
 out: ('outdir1', 'outdir2')
 version: 456
""")


def test_download_5():
    run_ecstatic(["download", "dataset=123", "dataset=456", "-o", "outdir"], expect_stdout="""
Dry-run with arguments:
 args: [('dataset', '123', 'outdir'), ('dataset', '456', 'outdir')]
 backbone_limit: None
 dataset: ('dataset=123', 'dataset=456')
 debug_torrent: False
 dest: outdir
 fastbone_limit: None
 i: 1
 libtorrent_option: ()
 name: dataset
 out: ('outdir',)
 version: 456
""")


def test_download_6():
    run_ecstatic(["download", "dataset=123", "-o", "outdir1", "dataset=456", "-o", "outdir2", "dataset=789"], expect_exit_code=1, expect_stderr="""
ecstatic: --dataset / --out parameters imbalance
""")


def test_reset_errors_1():
    run_ecstatic(["reset_errors"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic reset_errors [OPTIONS] <dataset>=<version> GROUP
Try 'ecstatic reset_errors --help' for help.

Error: Missing argument '<dataset>=<version>'.
""")


def test_reset_errors_2():
    run_ecstatic(["reset_errors", "dataset", "group"], expect_exit_code=1, expect_stderr="""
ecstatic: Expected <dataset>=<version>
""")


def test_reset_errors_3():
    run_ecstatic(["reset_errors", "dataset=123", "groupA"], expect_stdout="""
Dry-run with arguments:
 dataset: dataset=123
 group: groupA
 name: dataset
 version: 123
""")


def test_wait_1():
    run_ecstatic(["wait"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic wait [OPTIONS] <dataset>=<version> BRANCH
Try 'ecstatic wait --help' for help.

Error: Missing argument '<dataset>=<version>'.
""")


def test_wait_2():
    run_ecstatic(["wait", "dataset=123"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic wait [OPTIONS] <dataset>=<version> BRANCH
Try 'ecstatic wait --help' for help.

Error: Missing argument 'BRANCH'.
""")


def test_wait_3():
    run_ecstatic(["wait", "dataset=123", "testing"], expect_stdout="""
Dry-run with arguments:
 branch: testing
 dataset: dataset=123
 name: dataset
 target_state: Active
 timeout: 3600
 version: 123
""")


def test_wait_4():
    run_ecstatic(["wait", "dataset=123", "testing", "--target-state", "AllReady"], expect_stdout="""
Dry-run with arguments:
 branch: testing
 dataset: dataset=123
 name: dataset
 target_state: AllReady
 timeout: 3600
 version: 123
""")


def test_wait_5():
    run_ecstatic(["wait", "dataset=123", "testing", "--target-state", "AllReady", "--timeout", "abc"], expect_exit_code=2, expect_stderr="""
Usage: ecstatic wait [OPTIONS] <dataset>=<version> BRANCH
Try 'ecstatic wait --help' for help.

Error: Invalid value for '--timeout': 'abc' is not a valid integer.
""")


def test_wait_6():
    run_ecstatic(["wait", "dataset=123", "testing", "--target-state", "AllReady", "--timeout", "123"], expect_stdout="""
Dry-run with arguments:
 branch: testing
 dataset: dataset=123
 name: dataset
 target_state: AllReady
 timeout: 123
 version: 123
""")


def test_unqualify_dataset_name():
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data:nofile-6') == \
        ('yandex-maps-graph-router-data', 'nofile-6')
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data-6.123_44') == \
           ('yandex-maps-graph-router-data-6.123_44', '')
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data:your_tag') == \
           ('yandex-maps-graph-router-data', 'your_tag')
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data') == \
           ('yandex-maps-graph-router-data', '')
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data-a6') == \
           ('yandex-maps-graph-router-data-a6', '')
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data-6') == \
           ('yandex-maps-graph-router-data-6', '')
    assert ecstatic_api.unqualify_dataset_name(
        'yandex-maps-graph-router-data:6') == \
           ('yandex-maps-graph-router-data', '6')


def test_run_with_local_lock_1():
    run_ecstatic(["client", "run_with_local_lock", "echo", "1"],
                 expect_exit_code=0,
                 expect_stdout="1\n")


def test_run_with_local_lock_2():
    @LocalLockContextDecorator(timeout=0.1)
    def local_lock():
        run_ecstatic(["client", "run_with_local_lock", "echo", "1", "--lock-timeout", "0"],
                     expect_exit_code=1,
                     expect_stderr="ecstatic: Unable to take a lock\n")

    local_lock()
