# coding: utf-8
from string import ascii_lowercase as lowercase
from random import choice, randint

import pytest

from review.lib import encryption


SMALL_STRINGS = [
    '',
    'x',
    '12'
    'wow',
    '"?-\n,.'
]
LONG_STRING_EXAMPLE = "".join(choice(lowercase) for i in range(10000))
LONG_STRINGS = [
    LONG_STRING_EXAMPLE[:randint(0, 10000)]
    for _ in range(30)
]


@pytest.mark.parametrize('string', SMALL_STRINGS + LONG_STRINGS)
def test_encrypt_decypt(string):
    assert string == encryption.decrypt(encryption.encrypt(string))
