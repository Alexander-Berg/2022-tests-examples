# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('default_conf',
    '{"contracts":[{"firm":34,"ctype":"SPENDABLE","tag":"practicum","services":{"mandatory":[1041]},"nds":0,"selfemployed":[0,1],"_params":{"enable_setting_attributes":1,"enable_validating_attributes":1}}],"thirdparty_processing":[{"service_ids":[1041],"pipeline":[{"unit":"GetId","params":{"shift":1}},"PartnerUnit",{"params":{"contract_filters":[{"filter":{"name":"service"}},{"filter":{"name":"partner"}},{"filter":{"name":"transaction_dt"}},{"filter":{"params":{"contract_type":"SPENDABLE"},"name":"contract_type"}}]},"unit":"ContractUnit"},"ExtractSideTaxiPromoIDs","AmountUnit","SetInternal","OEBSOrgID"]}],"close_month":[{"month_close_generator":"SpendablePartnerProductGenerator","contract_tag":"practicum"}]}'
)
