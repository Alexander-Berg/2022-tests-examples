# coding: utf-8


import os

import mock
from granular_settings import get

import pytest


@pytest.mark.parametrize(
    'envtype',
    [
        'development',
        'unstable',
        'testing',
        'prestable',
        'production',
    ]
)
def test_env_settings(envtype):
    with mock.patch('yenv.type', envtype):
        config = get(path=_get_settings_dir_path())

    if envtype in ('development'):
        assert config.DEBUG
    else:
        assert not config.DEBUG


def _get_settings_dir_path():
    path_pieces = os.path.abspath(__file__).split('/')
    root_dir_index = len(path_pieces) - len([
        'tests', 'settings', 'test_env_settings.py'
    ])
    repo_root = '/'.join(path_pieces[:root_dir_index])
    settings_dir_path = repo_root + '/review/settings'
    return settings_dir_path
