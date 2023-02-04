# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

import itertools
import os.path

import pytest

from django.conf import settings
from django.core.management import call_command


@pytest.mark.parametrize('environment', [
    'development',
    'testing',
    'production',
])
def test_settings_diff(capsys, environment):
    expected_diffsettings = get_expected_diffsettings(environment)
    captured_diffsettings = get_captured_diffsettings(environment, capsys)
    for captured_setting, expected_setting in itertools.izip(
            captured_diffsettings, expected_diffsettings):
        assert_settings_are_equal(captured_setting, expected_setting)


def get_expected_diffsettings(environment):
    diffsettings_filename = os.path.join(
        settings.PROJECT_PATH, 'internal/tests/diffsettings.%s' % environment)
    with open(diffsettings_filename) as diffsettings:
        yield diffsettings.readline().rstrip()


def get_captured_diffsettings(environment, capsys):
    call_command('diffsettings')
    out, _ = capsys.readouterr()
    captured_diffsettings = out.split('\n')
    captured_diffsettings.pop()
    captured_diffsettings.sort() 
    return captured_diffsettings


def assert_settings_are_equal(captured_setting, expected_setting):
    if expected_setting.startswith('PROJECT = '):
        return
    elif setting_is_object(expected_setting):
        captured_setting = get_setting_object(captured_setting)
        expected_setting = get_setting_object(expected_setting)
    assert expected_setting == captured_setting

    
def setting_is_object(setting):
    return setting.endswith('>  ###')


def get_setting_object(setting):
    return setting.split(' object at 0x', 1)[0]
