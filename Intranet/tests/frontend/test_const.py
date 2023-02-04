# coding: utf-8
from review.oebs import (
    admin,
    const,
)

from tests import helpers


def test_currencies_in_const_sorted_rub_first_alpha_second_by_default(
    client,
    person,
):
    admin.reset_currencies_to_default()

    response_data = helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person.login,
    )

    without_rub = (
        it for it in const.DEFAULT_CURRENCIES.ALL
        if it != const.DEFAULT_CURRENCIES.RUB
    )
    expected_order = [const.DEFAULT_CURRENCIES.RUB] + sorted(without_rub)

    helpers.assert_is_substructure(
        {
            'currency': expected_order,
        },
        response_data,
    )
