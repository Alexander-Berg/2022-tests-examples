# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

true, false = True, False

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('default_conf', '{"contracts":[{"firm":1,"ctype":"SPENDABLE","tag":"news_payment","services":{"mandatory":[1127]},"person":{"type":{"mandatory":["ur","ph"]}},"_params":{"enable_setting_attributes":1,"enable_validating_attributes":1}}],"thirdparty_processing":[{"service_ids":[1127],"pipeline":[{"unit":"GetId","params":{"shift":1}},"PartnerUnit",{"params":{"contract_filters":[{"filter":{"name":"service"}},{"filter":{"name":"partner"}},{"filter":{"name":"transaction_dt"}},{"filter":{"params":{"contract_type":"SPENDABLE"},"name":"contract_type"}}]},"unit":"ContractUnit"},"AmountUnit","OEBSOrgID"]}],"close_month":[{"month_close_generator":"SpendablePartnerProductGenerator","contract_tag":"news_payment"}]}')
