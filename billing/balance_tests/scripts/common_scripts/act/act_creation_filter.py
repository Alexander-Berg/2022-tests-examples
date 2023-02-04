# -*- coding: utf-8 -*-

__author__ = 'torvald'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import *

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Products, Services, Firms, Paysyses
from temp.igogor.balance_objects import Contexts

RABOTA_context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(product=Products.RABOTA, service=Services.RABOTA,
                                                      paysys=Paysyses.BANK_UR_RUB_VERTICAL)
REALTY_context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(product=Products.REALTY_COMM, service=Services.RABOTA,
                                                      paysys=Paysyses.BANK_UR_RUB_VERTICAL)

DIRECT_MONEY_RUB_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new()

QTY = D('100')
NDS = D('1.2')
FICTIVE_PERSONAL_ACCOUNT_COLLATERAL_ID = 1033

BASE_DT = datetime.datetime.now()
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT),
    pytest.mark.tickets('BALANCE-21543'),
]


def prepare_invoice_with_full_completions(context):
    product = context.product

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    # Выставляем и оплачиваем целевой счёт
    campaigns_list = [
        {'service_id': product.service.id, 'product_id': product.id, 'qty': QTY, 'begin_dt': BASE_DT}]
    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=context.paysys.id,
                                                                            invoice_dt=BASE_DT)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(product.service.id, orders_list[0]['ServiceOrderID'],
                                      {product.type.code: QTY}, 0, BASE_DT)
    return client_id, invoice_id


def change_act_filter(filters_list):
    # query = "insert into bo.t_config (items, 'DESC', value_json) values ('ACT_CREATION_FILTER', 'if any of filters in Act dont do it', :filters_list)"
    query = "update bo.t_config set value_json = :filters_list where item = 'ACT_CREATION_FILTER'"
    query_params = {'filters_list': filters_list}
    db.balance().execute(query, query_params)


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_null_daily(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": null, "act_type": "daily"}, {"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (
            context.service.id, Services.TEST_SERVICE.id))

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    print db.balance().execute('''select * from t_config where item = \'ACT_CREATION_FILTER\'''')
    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_null_monthly(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": null, "act_type": "monthly"}, {"service_id": %s, "firm_id": null, "act_type": "monthly"}]' % (
            context.service.id, Services.TEST_SERVICE.id))

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_all_services_to_12_firm_daily(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter('[{"service_id": null, "firm_id": 12, "act_type": "daily"}]')

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))

    invoice_owner, invoice_id = prepare_invoice_with_full_completions(REALTY_context)
    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY))

    invoice_owner, invoice_id = prepare_invoice_with_full_completions(DIRECT_MONEY_RUB_CONTEXT)
    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY))

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_all_services_to_12_firm_monthly(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter('[{"service_id": null, "firm_id": 12, "act_type": "monthly"}]')

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))

    invoice_owner, invoice_id = prepare_invoice_with_full_completions(REALTY_context)
    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY))

    invoice_owner, invoice_id = prepare_invoice_with_full_completions(DIRECT_MONEY_RUB_CONTEXT)
    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY))

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY))



@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_12_daily(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": 12, "act_type": "daily"}, {"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (
            context.service.id, Services.TEST_SERVICE.id))

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_12_monthly(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": 12, "act_type": "monthly"}, {"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (
            context.service.id, Services.TEST_SERVICE.id))

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id), empty())

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_1_daily(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": 1, "act_type": "daily"}, {"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (
            context.service.id, Services.TEST_SERVICE.id))
    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_1_daily_force(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": 1, "act_type": "daily"}, {"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (
            context.service.id, Services.TEST_SERVICE.id))

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_without_filter_row(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter('[{"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (Services.TEST_SERVICE.id))

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_empty_filter(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter('[]')

    steps.ActsSteps.generate(invoice_owner, force=0, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_empty_filter_force(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter('[]')

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    utils.check_that(db.get_acts_by_invoice(invoice_id)[0]['amount'], equal_to(QTY * NDS))


@pytest.mark.no_parallel
@pytest.mark.slow
@pytest.mark.smoke
@pytest.mark.parametrize('context', [RABOTA_context])
def test_RABOTA_null_monthly_fair(context):
    invoice_owner, invoice_id = prepare_invoice_with_full_completions(context)
    change_act_filter(
        '[{"service_id": %s, "firm_id": null, "act_type": "monthly"}, {"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (
            context.service.id, Services.TEST_SERVICE.id))

    steps.ActsSteps.enqueue([invoice_owner], date=BASE_DT, force=1)
    steps.CommonSteps.export('MONTH_PROC', 'Client', invoice_owner)
    utils.check_that(db.get_acts_by_client(invoice_owner), empty())

    change_act_filter('[{"service_id": %s, "firm_id": null, "act_type": "daily"}]' % (Services.TEST_SERVICE.id))
    steps.ActsSteps.enqueue([invoice_owner], date=BASE_DT, force=1)
    steps.CommonSteps.export('MONTH_PROC', 'Client', invoice_owner)
    utils.check_that(db.get_acts_by_client(invoice_owner)[0]['amount'], equal_to(QTY * NDS))


if __name__ == "__main__":
    pass
