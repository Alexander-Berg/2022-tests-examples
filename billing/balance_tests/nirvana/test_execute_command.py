# -*- coding: utf-8 -*-

from contextlib import contextmanager

import mock
import pytest

from balance.actions.nirvana.operations import execute_command
from tests import object_builder as ob


@contextmanager
def does_not_raise():
    yield


@pytest.mark.parametrize(
    'config, executable, params, expectation, error',
    [
        (
            {},
            'ls',
            ['-l'],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'illegal command',
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}}},
            'ls',
            ['-l ', ' -a '],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'unrecognized arguments',
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}}},
            'ls',
            ['   -l   -a   '],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'unrecognized arguments',
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}}},
            'ls',
            ['   -l   --block-size M   '],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'unrecognized arguments',
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}}},
            'ls',
            ['-l', '--block-size M   '],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'unrecognized arguments',
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}, '--block-size': {}}},
            'ls',
            ['-l', '--block-size M   '],
            does_not_raise(),
            None,
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}, '--block-size': {}}},
            'ls',
            [' --block-size M   -l'],
            does_not_raise(),
            None,
        ),
        (
            {'ls': {'-l': {'action': 'store_true'}, '--block-size': {}}},
            'ls',
            [],
            does_not_raise(),
            None,
        ),
        (
            {'ls': {'--test': {'action': 'store_true'}, '--block-size': {}}},
            'ls',
            ['--test'],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'exited with non-zero code',
        ),
        (
            {'non_existent_executable': {'-l': {'action': 'store_true'}}},
            'non_existent_executable',
            ['-l', '--block-size M   '],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'unrecognized arguments',
        ),
        (
            {'non_existent_executable': {'-l': {'action': 'store_true'}}},
            'non_existent_executable',
            ['-l'],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'such file or directory',
        ),
    ],
)
def test_execute_command(session, config, executable, params, expectation, error):
    nirvana_block = (
        ob.NirvanaBlockBuilder(
            operation='execute_command',
            request={'data': {'options': {'executable': executable, 'params': params}}},
        )
        .build(session)
        .obj
    )

    with expectation as exc, mock.patch.dict(
        'balance.actions.nirvana.operations.execute_command.CONFIG', config
    ):
        execute_command.process(nirvana_block)

    if error:
        assert error in str(exc)


@pytest.mark.parametrize(
    'config, executable, params, expectation, error',
    [
        (
            {'ls': {'-l': {'action': 'store_true'}, '--block-size': {}}},
            'ls',
            [],
            pytest.raises(execute_command.EXCEPTION_TYPE),
            'metaprocess failed',
        ),
    ]
)
def test_execute_command_w_patch(session, config, executable, params, expectation, error):
    nirvana_block = (
        ob.NirvanaBlockBuilder(
            operation='execute_command',
            request={'data': {'options': {'executable': executable, 'params': params}}},
        )
        .build(session)
        .obj
    )

    with expectation as exc, mock.patch.dict(
        'balance.actions.nirvana.operations.execute_command.CONFIG', config
    ), mock.patch('subprocess.Popen.communicate', side_effect=Exception('metaprocess failed')):
        execute_command.process(nirvana_block)

    if error:
        assert error in str(exc)
