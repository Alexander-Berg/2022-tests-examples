from unittest import mock

from infra.reconf_juggler.tools import jdiff


def _run_differ(filename1, filename2, *args):
    try:
        with mock.patch('sys.argv', ['jdiff', filename1, filename2, *args]):
            jdiff.main()
        return 0

    except SystemExit as e:
        return e.code


def test_jsdk_diff_absent(capsys, file_in_test_dir):
    code = _run_differ(
        '--ifmt', 'jsdk',
        file_in_test_dir('a.jsdk.json'),
        file_in_test_dir('a.jsdk.json'),
    )

    assert 0 == code

    captured = capsys.readouterr()
    assert '' == captured.out
    assert '' == captured.err


def test_jsdk_diff_present(capsys, file_in_test_dir):
    code = _run_differ(
        '--ifmt', 'jsdk',
        file_in_test_dir('a.jsdk.json'),
        file_in_test_dir('b.jsdk.json'),
    )

    assert 8 == code

    captured = capsys.readouterr()

    assert '' == captured.err

    expected = """\
  {'parent:ssh'}
    {'children'}
      {'child:ssh'}
        {'tags'}
          [0]
-           'foo'
+           'bar'
"""

    assert expected == captured.out


def test_tree_diff_absent(capsys, file_in_test_dir):
    code = _run_differ(
        file_in_test_dir('a.tree.json'),
        file_in_test_dir('a.tree.json'),
    )

    assert 0 == code

    captured = capsys.readouterr()

    assert '' == captured.out
    assert '' == captured.err


def test_tree_diff_present__unified(capsys, file_in_test_dir):
    code = _run_differ(
        '--ofmt=unified',
        file_in_test_dir('a.tree.json'),
        file_in_test_dir('b.tree.json'),
    )

    assert 8 == code

    captured = capsys.readouterr()

    assert '' == captured.err

    # slice to throw away header with temp filenames and dates
    diff = captured.out.splitlines()[2:]

    expected = """\
@@ -1,26 +1,32 @@
 {
    "all_infra_prestable:ssh": {
       "active": "ssh",
       "active_kwargs": {
          "timeout": 15
       },
       "aggregator": "timed_more_than_limit_is_problem",
+      "check_options": null,
       "children": {
          "WALLE%PROD@prj=rtc-mtn-prestable:ssh": null,
          "WALLE%PROD@prj=rtc-prestable:ssh": null
       },
+      "creation_time": null,
+      "description": "",
       "flaps": {
          "boost": 0,
          "critical": 1200,
          "stable": 600
       },
+      "mtime": null,
       "namespace": "RTC",
+      "notifications": [],
       "refresh_time": 90,
       "tags": [
          "category_infra",
-         "maintainer_yandex_mnt_sa_runtime_cross",
-         "a_mark_reconf-rtc"
+         "level_leaf",
+         "level_root",
+         "maintainer_yandex_mnt_sa_runtime_cross"
       ],
       "ttl": 900
    }
 }
\ No newline at end of file\
"""
    assert expected == '\n'.join(diff)


def test_tree_diff_present__nested(capsys, file_in_test_dir):
    code = _run_differ(
        '--ofmt', 'nested',
        file_in_test_dir('a.tree.json'),
        file_in_test_dir('b.tree.json'),
    )

    assert 8 == code

    captured = capsys.readouterr()

    assert '' == captured.err

    expected = """\
  {'all_infra_prestable:ssh'}
+   {'check_options'}
+     None
+   {'creation_time'}
+     None
+   {'description'}
+     ''
+   {'mtime'}
+     None
+   {'notifications'}
+     []
    {'tags'}
+     [1]
+       'level_leaf'
+     [2]
+       'level_root'
-     [2]
-       'a_mark_reconf-rtc'
"""
    assert expected == captured.out


def test_ignore_default_opts(capsys, file_in_test_dir):
    code = _run_differ(
        '--ignore-default-opts',
        file_in_test_dir('a.tree.json'),
        file_in_test_dir('b.tree.json'),
    )

    assert 8 == code

    captured = capsys.readouterr()

    assert '' == captured.err

    expected = """\
  {'all_infra_prestable:ssh'}
    {'tags'}
+     [1]
+       'level_leaf'
+     [2]
+       'level_root'
-     [2]
-       'a_mark_reconf-rtc'
"""

    assert expected == captured.out


def test_ignore_mark_tags(capsys, file_in_test_dir):
    code = _run_differ(
        '--ignore-mark-tags',
        file_in_test_dir('a.tree.json'),
        file_in_test_dir('b.tree.json'),
    )

    assert 8 == code

    captured = capsys.readouterr()

    assert '' == captured.err

    expected = """\
  {'all_infra_prestable:ssh'}
+   {'check_options'}
+     None
+   {'creation_time'}
+     None
+   {'description'}
+     ''
+   {'mtime'}
+     None
+   {'notifications'}
+     []
    {'tags'}
+     [1]
+       'level_leaf'
+     [2]
+       'level_root'
"""

    assert expected == captured.out
