# -*- coding: utf-8 -*-
__all__ = ['SCHEMES']

import importlib

from btestlib.utils import aDict

# NOTE: можно использовать для получения строки
# from btestlib.data.partner_schemes import SCHEMES
# print(SCHEMES.dzen_writer.ph)

MODULE_NAMES = ['dzen_writer', 'supercheck', 'cloud_referal', 'subagency_tickets',
                'health_payments', 'pvz', 'practicum', 'investments', 'uslugi', 'zaxi', 'k50',
                'market_marketing_services', 'news', 'logistics_lk', 'music']

scheme_modules = {name: importlib.import_module('.' + name, package='btestlib.data.partner_schemes')
                  for name in MODULE_NAMES}

SCHEMES = aDict({name: aDict(module.SCHEMES) for name, module in scheme_modules.items()})
