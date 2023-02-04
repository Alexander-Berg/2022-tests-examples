# coding: utf-8
import datetime
from decimal import Decimal as D

import pytest

from balance import balance_steps2 as steps
from balance.balance_objects import Contract, Context, Line
from balance.balance_templates import Contracts, Contexts
from btestlib import utils
from btestlib.constants import ContractPaymentType, ContractCommissionType

# можно просто использовать сами сервисы
# DIRECT = Services.DIRECT.id
# MARKET = Services.MARKET.id
# MEDIA = Services.MEDIA.id
# MEDIASELLING = Services.MEDIASELLING.id
# ADFOX = Services.ADFOX.id

# это должно быть в контекстах
# DIRECT_PRODUCT = Products.DIRECT_FISH
# MARKET_PRODUCT = Products.MARKET
# MEDIASELLING_PRODUCT = Products.MEDIA
# ADFOX_PRODUCT = Products.ADFOX

# это где должно быть?
# YANDEX_FIRM = Firms.YANDEX_1.id
# MARKET_FIRM = Firms.MARKET_111.id

# если это используется в одном месте то константы не нужны
TODAY = datetime.datetime.now()
TOMORROW = utils.Date.shift_date(TODAY, days=1)
YESTERDAY = utils.Date.shift_date(TODAY, days=-1)
HALF_YEAR_AFTER_NOW = utils.Date.shift_date(TODAY, days=180)
HALF_YEAR_BEFORE_NOW = utils.Date.shift_date(TODAY, days=-180)
YEAR_BEFORE_NOW = utils.Date.shift_date(TODAY, days=-365)

QTY = 100
CONTRACT_TYPE = 'NO_AGENCY'


BASE_CONTRACT = Contracts.DEFAULT.new(type=ContractCommissionType.NO_AGENCY, payment_type=ContractPaymentType.PREPAY,
                                      start_dt=YEAR_BEFORE_NOW, finish_dt=HALF_YEAR_AFTER_NOW,
                                      signed_dt=YEAR_BEFORE_NOW)  # type: Contract
DIRECT_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT


@pytest.mark.parametrize('descr, contract_template, request_dt', [
    ('request_dt_during_active_contract_dates',
     BASE_CONTRACT.new(start_dt=YESTERDAY, finish_dt=TOMORROW, signed_dt=YESTERDAY), TODAY)
], ids=lambda descr, contract_template, request_dt: descr)
@pytest.mark.parametrize('base_context', [DIRECT_CONTEXT], ids=lambda context: context.name)
def test_contract_available_on_paystep(descr, contract_template, request_dt, base_context):
    # type: (str, Contract, datetime, Context) -> None
    context = base_context.new(contract_template=contract_template)  # type: Context
    contract = steps.prepare(context.contract)
    request = steps.prepare(context.request.new(client=contract.client, InvoiceDesireDT=request_dt))
    pass


def test_create_invoice():
    context = DIRECT_CONTEXT.new(contract_template=BASE_CONTRACT)
    steps.prepare(
        context.invoice.new(total=D('10000.0'), paid=D('10000.0'), consumed=D('5000.0'), completed=D('2500.0')))
    steps.prepare(context.new(lines_templates=[Line().new(qty=D('100.0'), sum=D('3000.0'))]).invoice)


def test_create_act():
    context = DIRECT_CONTEXT.new(contract_template=BASE_CONTRACT)
    steps.prepare(context.act)
