# coding: utf-8

from __future__ import unicode_literals

import asyncio
import functools
import json
import os

import mock


def CoroMock(**kwargs):
    coro = mock.Mock(name="CoroutineResult", **kwargs)
    if 'return_value' in kwargs:
        coro.return_value = kwargs.pop('return_value')
    corofunc = mock.Mock(name="CoroutineFunction", side_effect=asyncio.coroutine(coro))
    corofunc.coro = coro
    return corofunc


coro_patch = functools.partial(mock.patch, new_callable=CoroMock)


class MockValueError(Exception):
    pass


def _mock_telethon_api_fetch_chats():
    file_path = '/app/tasha/tests/data/fetch_chats.json'
    if not os.path.exists(file_path):
        raise MockValueError('No test data file %s' % file_path)
    with open(file_path) as f:
        return json.load(f)


def _mock_telethon_api_get_self():
    file_path = 'tasha/tests/data/get_self.json'
    if not os.path.exists(file_path):
        raise MockValueError('No test data file %s' % file_path)
    with open(file_path) as f:
        return json.load(f)


def _mock_telethon_api_fetch_chat_members(chat_obj):
    file_path = 'tasha/tests/data/fetch_chat_members__%s.json' % chat_obj['id']
    if not os.path.exists(file_path):
        return []
    with open(file_path) as f:
        return json.load(f)


def _mock_telethon_api_get_chat_admins(chat_obj):
    file_path = 'tasha/tests/data/get_chat_admins__%s.json' % chat_obj['id']
    if not os.path.exists(file_path):
        return []
    with open(file_path) as f:
        return json.load(f)
