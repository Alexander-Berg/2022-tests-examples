# -*- coding: utf-8 -*-

from balance import balance_steps as steps
from btestlib.constants import Export

# Перечень типовых объектов для выгрузки:
#  OEBS_API:
#    CLIENT = 'Client'
#    PERSON = 'Person'
#    CONTRACT = 'Contract'
#    CONTRACT_COLLATERAL = 'ContractCollateral'
#    INVOICE = 'Invoice'
#    ACT = 'Act'
#  OEBS:
#    PRODUCT = 'Product'
#    TRANSACTION = 'ThirdPartyTransaction'
#    CORRECTION = 'ThirdPartyCorrection'
#    PAYMENT = 'Payment'
#    MANAGER = 'Manager'
#    ZEN_PAYMENT = 'ZenPayment'
#    OEBS_CPF = 'OebsCashPaymentFact'
#    BALALAYKA_PAYMENT = 'BalalaykaPayment'
#    SIDE_PAYMENT = 'SidePayment'
#    PARTNER_COMPLETIONS_RESOURCE = 'PartnerCompletionsResource'
#    INVOICE_REFUND = 'InvoiceRefund'

# Выгружаем объект
steps.CommonSteps.export('OEBS', 'Client', 1324232)

steps.CommonSteps.export('OEBS_API', 'Client', 1324232)
