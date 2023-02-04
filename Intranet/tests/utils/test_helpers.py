# coding: utf-8

from __future__ import unicode_literals

import pytest
from django.conf import settings

from cab.utils import helpers


settings.FORBIDDEN_TO_LOG_REGEX = ['session']
settings.FORCE_ALLOWED_TO_LOG = ['not_session']


@pytest.mark.parametrize(
    'arg_names,args,kwargs,log_kwargs', [
        # check simple logic
        [
            (),
            [],
            {},
            {},
        ],
        [
            ('asd', 'blabla'),
            [],
            {},
            {},
        ],
        [
            ('asd', 'blabla'),
            [1, 2],
            {},
            {'asd': '1', 'blabla': '2'}
        ],
        [
            ('asd', 'blabla'),
            [],
            {'asd': 1, 'blabla': 2},
            {'asd': '1', 'blabla': '2'}
        ],
        [
            ('asd', 'blabla'),
            [1],
            {'blabla': 2},
            {'asd': '1', 'blabla': '2'}
        ],
        # check filtering from logs by settings
        [
            ('session_asd', 'not_session'),
            [1],
            {'not_session': 2},
            {'not_session': '2'}
        ],
    ]
)
def test_get_allowed_to_log(arg_names, args, kwargs, log_kwargs):
    received = dict(helpers.get_kwargs_to_log(arg_names, args, kwargs))
    assert log_kwargs == received
