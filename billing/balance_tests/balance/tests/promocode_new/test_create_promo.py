# -*- coding: utf-8 -*-
import datetime
import pytest
import json
from xmlrpclib import Fault

from promocode_commons import fill_calc_params_fixed_discount, import_promocode, generate_code
import btestlib.reporter as reporter
from balance.features import Features
from balance import balance_steps as steps
from balance import balance_db as db
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Services, Currencies, PromocodeClass, Firms, Regions, Products

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_FISH,
                                                               region=Regions.RU, currency=Currencies.RUB)
NOW = datetime.datetime.now()

pytestmark = [reporter.feature(Features.PROMOCODE)]


def check_promo(promocode_id):
    pass


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('discount_pct_value', [0, 12.4, 103])
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_create_promo_fixed_discount(context, discount_pct_value):
    code = generate_code()
    calc_params = fill_calc_params_fixed_discount(discount_pct=discount_pct_value)
    promocode_id, promocode_code = import_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                                    end_dt=NOW,
                                                    promocodes=[code],
                                                    start_dt=NOW,
                                                    calc_params=calc_params,
                                                    minimal_amounts={},
                                                    firm_id=context.firm.id,
                                                    service_ids=[context.service.id])[0]
    calc_params_db = db.get_promocode_group_by_promocode_id(promocode_id)[0]['calc_params']
    assert json.loads(calc_params_db) == {'discount_pct': discount_pct_value}


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_create_promo_fixed_discount_none_discount(context):
    code = generate_code()
    calc_params = fill_calc_params_fixed_discount(discount_pct=None)
    with pytest.raises(Fault) as exc:
        import_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                         end_dt=NOW,
                         promocodes=[{'code': code, 'client_id': None}],
                         start_dt=NOW,
                         calc_params=calc_params,
                         minimal_amounts={},
                         firm_id=context.firm.id,
                         service_ids=[context.service.id])
    assert steps.CommonSteps.get_exception_code(exc.value) == 'INVALID_PROMOCODE_PARAMS'


@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_create_promo_fixed_qty(context):
    promocode_type = PromocodeClass.FIXED_QTY
    code = steps.PromocodeSteps.generate_code()
    calc_params = steps.PromocodeSteps.fill_calc_params(promocode_type=promocode_type,
                                                        services_list=[context.service.id], middle_dt=NOW,
                                                        discount_pct=0,
                                                        bonus1=10, bonus2=10, scale_points=[(1, 4)], currency='KZT',
                                                        convert_currency=1)
    import_promocode(calc_class_name=promocode_type,
                     end_dt=NOW,
                     promocodes=[code],
                     start_dt=NOW,
                     calc_params=calc_params,
                     minimal_amounts={"FISH": 143,
                                      "KZT": 15000},
                     firm_id=context.firm.id)


@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_create_promo_scale(context):
    promocode_type = PromocodeClass.SCALE
    code = steps.PromocodeSteps.generate_code()
    calc_params = steps.PromocodeSteps.fill_calc_params(promocode_type=promocode_type,
                                                        services_list=[context.service.id], middle_dt=NOW,
                                                        discount_pct=0,
                                                        bonus1=10, bonus2=10, scale_points=[(1, 4)],
                                                        convert_currency=1, minimal_qty=10)
    import_promocode(calc_class_name=promocode_type,
                     end_dt=NOW,
                     promocodes=[code],
                     start_dt=NOW,
                     calc_params=calc_params,
                     minimal_amounts={},
                     firm_id=context.firm.id)


@pytest.mark.parametrize('context', [DIRECT_FISH_RUB_CONTEXT])
def test_create_promo_legacy(context):
    promocode_type = PromocodeClass.LEGACY_PROMO
    code = steps.PromocodeSteps.generate_code()
    calc_params = steps.PromocodeSteps.fill_calc_params(promocode_type=promocode_type,
                                                        services_list=[context.service.id],
                                                        middle_dt=NOW + datetime.timedelta(seconds=1),
                                                        discount_pct=0,
                                                        bonus1=10, bonus2=10, minimal_qty=10)
    import_promocode(calc_class_name=promocode_type,
                     end_dt=NOW,
                     promocodes=[code],
                     start_dt=NOW,
                     calc_params=calc_params,
                     minimal_amounts={"FISH": 143,
                                      "KZT": 15000},
                     firm_id=context.firm.id)
