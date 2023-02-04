# coding: utf-8

# __author__ = 'a-shumak'
#
# import balalayka.balalayka_api as api
#
#
# def a():
#     parameters = {
#         'registry_number': '002',
#         'registry_guid': '485665c7-67a9-46ca-83f0-2b6f68121493',
#         'contract_number': '38142973'
#     }
#
#     q = api.balalayka().ReceiveCardRegistryResponse(parameters)
#     print(q)
#
#
# a()


import balalayka.balalayka_api as api
from datetime import datetime


def a():
    s = api.balalayka().GetStatement('40702810800001005378', datetime(2018, 8, 14), False)
    a=1

a()