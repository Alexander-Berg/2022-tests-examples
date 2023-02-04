# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('zaxi_selfemployed_tips_conf',
         '{"contracts":[{"firm":124,"ctype":"GENERAL","tag":"rub_resident","services":{"mandatory":[1121]},"unilateral":1,"currency":"RUB","_params":{"enable_setting_attributes":1,"enable_validating_attributes":1}}],"thirdparty_processing":[{"service_ids":[1121],"pipeline":[{"chain_alias":"BaseFilters"},"PartnerUnit","AmountUnit","SetInternal","ContractUnit",{"unit":"OrderRewardUnit","params":{"use_min_reward":false}},"OEBSOrgID"]}],"close_month":[{"month_close_generator":"RevPartnerGenerator","contract_tag":"rub_resident"}]}')
register('selfemployed_spendable',
         """{
         "contracts":[
             {
                 "firm":124,
                 "ctype":"SPENDABLE",
                 "tag":"zaxi_selfemployed_spendable",
                 "services":{
                     "mandatory":[
                         1120
                     ]
                 },
                 "nds":0,
                 "selfemployed":1,
                 "_params":{
                     "enable_setting_attributes":1,
                     "enable_validating_attributes":1
                 }
             }
         ],
         "thirdparty_processing":[
             {
                 "service_ids":[
                     1120
                 ],
                 "pipeline":[
                     {
                         "unit":"GetId",
                         "params":{
                             "shift":1
                         }
                     },
                     "PartnerUnit",
                     {
                         "params":{
                             "contract_filters":[
                                 {
                                     "filter":{
                                         "name":"service"
                                     }
                                 },
                                 {
                                     "filter":{
                                         "name":"partner"
                                     }
                                 },
                                 {
                                     "filter":{
                                         "name":"transaction_dt"
                                     }
                                 },
                                 {
                                     "filter":{
                                         "params":{
                                             "contract_type":"SPENDABLE"
                                         },
                                         "name":"contract_type"
                                     }
                                 }
                             ]
                         },
                         "unit":"ContractUnit"
                     },
                     "ExtractSideTaxiPromoIDs",
                     "AmountUnit",
                     "OEBSOrgID"
                 ]
             }
         ],
         "close_month":[
             {
                 "month_close_generator":"SpendablePartnerProductGenerator",
                 "contract_tag":"zaxi_selfemployed_spendable"
             }
         ]
     }"""
         )
