# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from datetime import datetime

from btestlib.constants import Products, PersonTypes, Services, Paysyses, Firms
from btestlib import utils as utils


class CommonDefaults(object):
    PERSON_TYPE = PersonTypes.UR.code
    SERVICE = Services.DIRECT.id
    PRODUCT = Products.DIRECT_FISH.id
    PAYSYS = Paysyses.BANK_UR_RUB.id
    FIRM = Firms.YANDEX_1.id
    DT = datetime.now()
    QTY = 100
    SHIPMENT_QTY = 30


class ContractDefaults(object):
    CONTRACT_TYPE = 'opt_agency_prem'
    CONTRACT_DT = utils.Date.date_to_iso_format(CommonDefaults.DT)
    CONTRACT_PAYMENT_TYPE = 3
    CONTRACT_CURRENCY = 810
    COLLATERAL_TYPE = 1048
