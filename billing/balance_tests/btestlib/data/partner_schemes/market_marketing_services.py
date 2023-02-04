# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register(
    'market_marketing_services_default_conf',
    '{"contracts":[{"firm":111,"ctype":"GENERAL","tag":"marketing_services","services":{"mandatory":[1126]},"partner_contract":{"attributes":"common"},"person":{"type":"UR","ownership_type_ui":{"forbidden":"SELFEMPLOYED"}},"_params":{"enable_setting_attributes":1,"enable_validating_attributes":1}}],"close_month":[{"month_close_generator":{"name":"RevPartnerGenerator"},"contract_tag":"marketing_services"}]}'
)
