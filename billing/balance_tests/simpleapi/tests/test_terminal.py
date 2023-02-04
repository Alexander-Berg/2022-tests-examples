# coding=utf-8
import pytest
from hamcrest import has_items, empty

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common.utils import remove_empty
from simpleapi.data import features, stories, marks
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import simple_steps as simple

pytestmark = marks.simple_internal_logic


class Data(object):
    shop_params = ({'card': {'ym_shop_id': '139335',
                             'ym_shop_article_id': '697224'},
                    'yandex_money': {'ym_shop_id': '13629',
                                     'ym_shop_article_id': '136291',
                                     'ym_scid': '5581'}},
                   {'card': {'ym_shop_id': '139335',
                             'ym_shop_article_id': '697224'}},
                   {'yandex_money': {'ym_shop_id': '13629',
                                     'ym_shop_article_id': '136291',
                                     'ym_scid': '5581'}})
    services = (Services.MARKETPLACE,
                Services.RED_MARKET_PAYMENTS,)


processing_ids = {'card': 50015,
                  'yandex_money': 50002}
payment_method_ids = {'card': 1101,
                      'yandex_money': 1201}


def idservice(val):
    return "service={}".format(val)


def map_shops_params_to_terminal(shop_params):
    rule = {'ym_shop_id': 'pri_id',
            'ym_shop_article_id': 'sec_id',
            'ym_scid': 'aux_id'}

    terminals = []
    for shop_param in shop_params.items():
        terminal = dict((rule.get(k), v) for (k, v) in shop_param[1].items())
        terminal.update({'processing_id': processing_ids.get(shop_param[0])})
        terminal.update({'payment_method_id': payment_method_ids.get(shop_param[0])})
        terminals.append(terminal)

    return terminals


@reporter.feature(features.General.Terminal)
class TestTerminal(object):
    @reporter.story(stories.Terminal.Autocreation)
    @pytest.mark.parametrize('shop_params', Data.shop_params)
    @pytest.mark.parametrize('service', Data.services, ids=idservice)
    def test_terminal_autocreates_if_shop_params(self, shop_params, service):
        service_product_id = simple.get_service_product_id(service)
        _, partner_id = simple.create_partner(service)

        simple.create_service_product(service,
                                      service_product_id=service_product_id,
                                      partner_id=partner_id,
                                      shop_params=shop_params)

        product_id = \
            db_steps.bs().get_product_by_external_id(service_product_id, service).get('id')

        created_terminals = db_steps.bs().get_terminal_by(service_product_id=product_id,
                                                          partner_id=partner_id)

        created_terminals_details = [remove_empty({'pri_id': created_terminal['pri_id'],
                                                   'sec_id': created_terminal['sec_id'],
                                                   'aux_id': created_terminal['aux_id'],
                                                   'processing_id': created_terminal['processing_id'],
                                                   'payment_method_id': created_terminal['payment_method_id']})
                                     for created_terminal in created_terminals]

        expected_terminals = map_shops_params_to_terminal(shop_params)

        check.check_that(created_terminals_details, has_items(*expected_terminals),
                         step=u'Проверяем что в базе создались терминалы',
                         error=u'В базе нет терминала(ов)')

    @reporter.story(stories.Terminal.Autocreation)
    @pytest.mark.parametrize('service', Data.services, ids=idservice)
    def test_terminal_nocreates_if_no_shop_params(self, service):
        service_product_id = simple.get_service_product_id(service)
        _, partner_id = simple.create_partner(service)
        simple.create_service_product(service,
                                      service_product_id=service_product_id,
                                      partner_id=partner_id,
                                      shop_params=None)

        product_id = \
            db_steps.bs().get_product_by_external_id(service_product_id, service).get('id')

        created_terminals = db_steps.bs().get_terminal_by(service_product_id=product_id,
                                                          partner_id=partner_id)

        created_terminals_details = [remove_empty({'pri_id': created_terminal['pri_id'],
                                                   'sec_id': created_terminal['sec_id'],
                                                   'aux_id': created_terminal['aux_id'],
                                                   'processing_id': created_terminal['processing_id'],
                                                   'payment_method_id': created_terminal['payment_method_id']})
                                     for created_terminal in created_terminals]

        check.check_that(created_terminals_details, empty(),
                         step=u'Проверяем, что в базе не создались терминалы',
                         error=u'В базе есть терминал')


if __name__ == '__main__':
    pytest.main()
