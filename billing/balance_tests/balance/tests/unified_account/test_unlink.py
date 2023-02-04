# # coding=utf-8
#
# import balance.balance_db as db
# import pytest
# import datetime
# import pprint
# from balance import balance_steps as steps
# from balance import balance_api as api
# from btestlib.data import defaults
# from balance import balance_db as db
# from temp.igogor.balance_objects import Contexts
# from btestlib.constants import Permissions, PersonTypes, Paysyses, Services, Products, User, Regions, Currencies
#
# NOW = datetime.datetime.now()
# DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
# MARKET = Contexts.MARKET_RUB_CONTEXT.new()
# VZGLYAD = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VZGLYAD, product=Products.VZGLYAD)
# QTY = 100
#
#
# def create_order(context, client_id, agency_id=None, result={}, descr=''):
#     service_order_id = steps.OrderSteps.next_id(context.service.id)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
#                                        product_id=context.product.id, params={'AgencyID': agency_id,
#                                                                               'Text': descr})
#     result[descr] = '{}-{}'.format(str(context.service.id), service_order_id)
#     return order_id
#
# def test_unlink_orders():
#     user = User(436363467, 'yb-atst-user-17')
#
#     agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#     linked_client = steps.ClientSteps.create()
#     non_linked_client = steps.ClientSteps.create()
#
#     steps.ClientSteps.unlink_from_login(user.uid)
#     api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
#                                                    [linked_client])
#     # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
#     #                                                )
#     result = {}
#
#     create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
#                  descr='linked client order direct')
