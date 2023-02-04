import json
import random
import string
import urllib

import pytest

from intranet.webauth.lib import role_cache


def random_slug(length=8):
    return ''.join(random.sample(string.lowercase, k=length))


@pytest.mark.parametrize('fields_data', [None, '', {}])
def test_get_role_cache_key__empty(fields_data):
    login = random_slug()
    role_path = '/'.join(['', random_slug(), random_slug(), random_slug()])
    assert role_cache.get_role_cache_key(login, role_path, fields_data) == \
        urllib.quote(role_path, safe='') + '/' + login + '/' + urllib.quote('[]')


def test_get_role_cache_key():
    login = random_slug()
    role_path = '/'.join(['', random_slug(), random_slug(), random_slug()])
    field_name, field_value = random_slug(), random_slug()
    assert role_cache.get_role_cache_key(login, role_path, {field_name: field_value}) == \
        urllib.quote(role_path, safe='') + '/' + login + '/' + urllib.quote(json.dumps([(field_name, field_value)]))


def test_normalize_fields():
    value_a, value_b, value_c = random_slug(), random_slug(), random_slug()
    assert role_cache.normalize_fields_data({
        'alpha': value_a,
        'beta': value_b,
        'omega': value_c,
    }) == role_cache.normalize_fields_data({
        'omega': value_c,
        'alpha': value_a,
        'beta': value_b,
    }) == role_cache.normalize_fields_data({
        'beta': value_b,
        'alpha': value_a,
        'omega': value_c,
    })
