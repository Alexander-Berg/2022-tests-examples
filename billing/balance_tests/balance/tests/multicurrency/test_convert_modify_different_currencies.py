import hamcrest
import pytest

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.MULTICURRENCY, Features.CLIENT)
              ]


@reporter.feature(Features.TO_UNIT)
def generate_data_for_test_convert_modify_different_currencies():
    return ['EUR'
        , 'UAH'
        , 'RUB'
        , 'TRY'
        , 'BYR'
        , 'KZT'
        , 'CHF'
        , 'USD'
        , 'BYN'
 ]


@pytest.mark.parametrize('currency',
                         generate_data_for_test_convert_modify_different_currencies()
                         )
def test_convert_modify_different_currencies(currency):
    try:
        steps.ClientSteps.create_multicurrency(currency=currency)
        utils.check_that(currency, hamcrest.equal_to('RUB'))
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc, 'contents'),
                         hamcrest.equal_to("Invalid parameter for function: Wrong currency u'{0}' for convert type u'MODIFY'".format(currency)))


if __name__ == "__main__":
    pytest.main("test_convert_modify_different_currencies.py -v")
