# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('default_conf',
'{"contracts":[{"partner_contract":{"attributes":"common"},"ctype":"GENERAL","tag":"agent_reward","commission":0,"firm":111,"services":{"mandatory":[655]},"currency":"RUB","unilateral":[0,1],"_params":{"enable_setting_attributes":1,"enable_validating_attributes":1}}],"thirdparty_processing":[{"service_ids":[655],"pipeline":[{"chain_alias":"BaseFilters"},"PayoutReadyDtUnit","PartnerUnit","ContractUnit","AmountUnit",{"unit":"OrderRewardUnit","params":{"use_min_reward":false}},"OEBSOrgID"]}],"close_month":[{"contract_tag":"agent_reward","month_close_generator":"RevPartnerGenerator"}]}'
)