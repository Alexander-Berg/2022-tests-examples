# coding: utf-8

__author__ = 'a-vasin'

import json
import pytest
from hamcrest import contains_string, empty

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import Services, Currencies, Regions
from balance.features import Features

# expected config from T_THIRDPARTY_SERVICE
# {
#     "service_product_options": {
#         "1631186": "delay"
#     },
#     "service_fee_product_mapping": {
#         "RUB": {
#             "test": "delay"
#         }
#     },
#     "default_product_mapping": {
#         "RUB": {
#             "default": 503782
#         },
#         "default": "no_process"
#     }
# }

SERVICE = Services.MUSIC

pytestmark = [
    pytest.mark.usefixtures("switch_to_pg")
]

DELAY_BALANCE_SERVICE_PRODUCT_ID = 1795529
SKIP_ROW_SERVICE_FEE = 78156  # just random number
NO_PROCESS_SERVICE_FEE = 81235  # just random number
SKIP_CURRENCY = Currencies.BYN


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@reporter.feature(Features.TRUST)
def test_interrupt_service_product_id():
    set_up_config()

    service_product_id = steps.SimpleApi.create_service_product(SERVICE)
    _, _, _, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None)
    set_service_product_id(payment_id, service_product_id)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
        steps.CommonPartnerSteps.export_payment(payment_id)

    expected_error = 'delayed due to options for ServiceProduct ({})'.format(DELAY_BALANCE_SERVICE_PRODUCT_ID)
    utils.check_that(error.value.response, contains_string(expected_error), u'Проверяем текст ошибки экспорта')


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('service_fee',
                         [SKIP_ROW_SERVICE_FEE, NO_PROCESS_SERVICE_FEE],
                         ids=['SKIP_ROW', 'NO_PROCESS'])
def test_interrupt_currency_and_service_fee(service_fee):
    set_up_config()

    service_product_id = steps.SimpleApi.create_service_product(SERVICE, service_fee=service_fee)

    _, _, _, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None,
                                             currency=Currencies.RUB)

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, empty(), u'Проверяем, что платежи не созданы')


@reporter.feature(Features.TRUST)
def test_interrupt_default_currency():
    set_up_config()

    prices = [{
        'region_id': Regions.RU.id,
        'dt': 1347521693,
        'price': '10',
        'currency': SKIP_CURRENCY.iso_code
    }]
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, prices=prices)

    _, _, _, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id, commission_category=None, price=None,
                                             currency=SKIP_CURRENCY)

    output = steps.CommonPartnerSteps.export_payment(payment_id)['output']
    expected_output = 'skipped due to options for Currency and Nds ({} {})' \
        .format(SKIP_CURRENCY.iso_code, None)
    utils.check_that(output, contains_string(expected_output), u'Проверяем output')

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, empty(), u'Проверяем, что платежи не созданы')


# -----------------------------------------------

def add_key(config, key):
    if key not in config:
        config[key] = {}


def set_up_config():
    config = steps.CommonPartnerSteps.get_product_mapping_config(SERVICE)

    # set up for delay based on service product
    # relying on that we have row for DELAY_BALANCE_SERVICE_PRODUCT_ID in T_SERVICE_PRODUCT
    add_key(config, u'service_product_options')
    config[u'service_product_options'][unicode(DELAY_BALANCE_SERVICE_PRODUCT_ID)] = u'delay'

    # for delay on currency + service_fee
    add_key(config, u'service_fee_product_mapping')
    add_key(config[u'service_fee_product_mapping'], Currencies.RUB.iso_code)
    config[u'service_fee_product_mapping'][Currencies.RUB.iso_code][unicode(SKIP_ROW_SERVICE_FEE)] = u'skip_row'
    config[u'service_fee_product_mapping'][Currencies.RUB.iso_code][unicode(NO_PROCESS_SERVICE_FEE)] = u'no_process'

    # for no_process on currency
    add_key(config, u'default_product_mapping')
    config[u'default_product_mapping'][u'default'] = u'skip'

    steps.CommonPartnerSteps.set_product_mapping_config(SERVICE, config)


def set_service_product_id(payment_id, trust_service_product_id):
    with reporter.step(u"Обновляем ID сервисного продукта в t_payment: {}".format(trust_service_product_id)):
        payment_rows = db.balance().execute('select payment_rows from bo.t_payment where id = :payment_id',
                                            {'payment_id': payment_id})[0]['payment_rows']
        payment_rows = json.loads(payment_rows)
        payment_rows[0]['order']['service_product_id'] = DELAY_BALANCE_SERVICE_PRODUCT_ID
        payment_rows[0]['order']['service_product_external_id'] = None
        return db.balance().execute('update bo.t_payment set payment_rows = :payment_rows where id = :payment_id',
                                    {'payment_id': payment_id,
                                     'payment_rows': json.dumps(payment_rows),
                                     })
