# coding: utf-8

import httplib

import pytest
from requests.exceptions import HTTPError

from balance.balance_steps import other_steps as steps


def test_payment_not_found():
    purchase_token = 'non-exists-purchase-token'

    with pytest.raises(HTTPError) as excinfo:
        steps.MediumHttpSteps.get_payouts_by_purchase_token(purchase_token)

    response = excinfo.value.response
    assert response.status_code == httplib.NOT_FOUND

    error_message = response.json()['error']
    expected_message = 'purchase_token {} not found'.format(purchase_token)
    assert error_message == expected_message
