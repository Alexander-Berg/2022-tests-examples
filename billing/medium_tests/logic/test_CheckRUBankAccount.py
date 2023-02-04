# -*- coding: utf-8 -*-

import pytest


@pytest.mark.parametrize(
    'args, expected_response',
    [
        (('445259740', '40802810000000481224'), (1, 'Bank with BIK 445259740 does not exist')),
        (('000000000', '40802810000000481224'), (1, 'Bank with BIK 000000000 does not exist')),

        (('000000000', ''), (1, 'Bank with BIK 000000000 does not exist')),
        (('000000000',), (1, 'Bank with BIK 000000000 does not exist')),

        (('014442501', ''), (0, 'BIK is valid')),
        (('014442501',), (0, 'BIK is valid')),

        (('044525974', '40802810000000481224'), (0, 'BIK/account pair is valid')),
        (('014442501', '0' * 20), (0, 'BIK/account pair is valid')),

        (('044525974', '40802810000000481225'), (1, 'BIK/account pair is invalid')),
        (('014442501', '0' * 19), (1, 'BIK/account pair is invalid')),
        (('014442501', '0' * 21), (1, 'BIK/account pair is invalid')),
        (('014442501', '1' * 20), (1, 'BIK/account pair is invalid'))
    ]
)
def test_endpoint(session, xmlrpcserver, args, expected_response):
    response = xmlrpcserver.CheckRUBankAccount(*args)
    assert tuple(response) == expected_response
