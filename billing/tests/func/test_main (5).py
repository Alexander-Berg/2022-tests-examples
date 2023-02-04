from contextlib import contextmanager
import json
from unittest import mock

import pytest

from billing.monthclosing.operations.monoconfig_builder.lib.main import main


@contextmanager
def does_not_raise():
    yield


def patched_main(ls, cat, options, mono_cfg):
    args = [
        __name__,
        '-e',
        options['environment'],
        '-v',
        options['version'],
        '-p',
        options['path'],
        '-o',
        str(mono_cfg),
    ]

    with mock.patch('sys.argv', args), mock.patch(
        'bunker.BunkerAPI.ls', side_effect=ls
    ), mock.patch('bunker.BunkerAPI.cat', side_effect=cat):
        main()

    return


@pytest.mark.parametrize(
    'ls, cat, options, expectation, error, expected_side_result',
    [
        (
            [],
            [],
            {'environment': 'load', 'path': '', 'version': 'stable'},
            pytest.raises(SystemExit),
            'SystemExit',
            None,
        ),
        (
            [],
            [],
            {'environment': 'development', 'path': '', 'version': '1'},
            pytest.raises(SystemExit),
            'SystemExit',
            None,
        ),
        (
            [Exception('404 Client Error')],
            [],
            {'environment': 'development', 'path': '', 'version': 'stable'},
            pytest.raises(Exception),
            '404 Client Error',
            None,
        ),
        (
            [[{'fullName': 'path1'}, {'fullName': 'path2'}]],
            [Exception('404 Client Error')],
            {'environment': 'development', 'path': '', 'version': 'stable'},
            does_not_raise(),
            None,
            [],
        ),
        (
            [[{'fullName': 'path1'}, {'fullName': 'path2'}]],
            [[], Exception('404 Client Error')],
            {'environment': 'development', 'path': '', 'version': 'stable'},
            does_not_raise(),
            None,
            [],
        ),
        (
            [[{'fullName': 'path1'}, {'fullName': 'path2'}], [{'fullName': 'path3'}]],
            [[[{'test': '0'}], [], {}, [{'test': '1'}]], Exception('404 Client Error')],
            {'environment': 'development', 'path': '', 'version': 'stable'},
            does_not_raise(),
            None,
            [[{'test': '0'}], [], {}, [{'test': '1'}]],
        ),
        (
            [[{'fullName': 'path1'}, {'fullName': 'path2'}]],
            [[], []],
            {'environment': 'development', 'path': '', 'version': 'stable'},
            does_not_raise(),
            None,
            [],
        ),
        (
            [[{'fullName': 'path{}'.format(i)} for i in range(6)]],
            [
                'test',
                [],
                {},
                [{'id': 'report0'}],
                [{'id': 'report1'}, {'id': 'report2'}],
                [{}],
            ],
            {'environment': 'development', 'path': '', 'version': 'stable'},
            does_not_raise(),
            None,
            [{'id': 'report0'}, {'id': 'report1'}, {'id': 'report2'}, {}],
        ),
    ],
)
def test_main(tmp_path, ls, cat, options, expectation, error, expected_side_result):
    mono_cfg = tmp_path / 'mono_cfg'
    with expectation as exc:
        patched_main(ls, cat, options, mono_cfg)

    if error:
        assert error in str(exc)
    else:
        with open(mono_cfg, 'r') as f:
            side_result = json.load(f)
            assert side_result == expected_side_result

    return
