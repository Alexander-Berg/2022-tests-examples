# encoding: utf-8
from __future__ import unicode_literals

import pytest

from intranet.webauth.lib.crypto_utils import sign, verify, encrypt_and_sign, decrypt_signed_value


@pytest.mark.parametrize('message', ['message1', 'randomMessage123456', 'some\nformatting\tadded'])
def test_sign_and_verify(message):
    signed = sign(message.encode('utf-8'))
    assert verify(signed) == (True, message)


@pytest.mark.parametrize('message', ['message1', 'randomMessage123456', 'some\nformatting\tadded'])
def test_sign_break_and_verify(message):
    signed = sign(message.encode('utf-8'))
    signed += b'noise'
    assert verify(signed) == (False, None)


@pytest.mark.parametrize('message', ['message1', 'randomMessage123456', 'some\nformatting\tadded',
                         'немного юникода', 'Ещё немного юникода!'.encode('utf-8')])
def test_full_cycle(message):
    encrypted = encrypt_and_sign(message)
    decrypted = decrypt_signed_value(encrypted)
    decoded = message if type(message) == unicode else message.decode('utf-8')
    assert decrypted == decoded


@pytest.mark.parametrize('message', ['message1', 'randomMessage123456', 'some\nformatting\tadded',
                         'немного юникода', 'Ещё немного юникода!'.encode('utf-8')])
def test_broken_message(message):
    encrypted = encrypt_and_sign(message) + b'some_noise'
    decrypted = decrypt_signed_value(encrypted)
    assert decrypted is None
