#!/usr/bin/env python
# coding: utf-8

# In[49]:


# import logging
# import xmlrpc.client
#
# LOG_FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
# logging.basicConfig(level='INFO', format=LOG_FORMAT)
# logger = logging.getLogger(__name__)
# logging.getLogger(__name__).setLevel('DEBUG')
#
# xmlhost = 'http://greed-ts.paysys.yandex.ru:8002/xmlrpc'
# OPERATOR = '0'
# CLIENT_ALREADY_ASSOCIATED_CODE = 4008
#
#
# # In[28]:
# def do_association(client_id, customer_uid):
#     def create_association(client_id, customer_uid):
#         try:
#             association = billing.Balance.CreateUserClientAssociation(
#                 OPERATOR, client_id, customer_uid
#             )
#             logger.debug('Association: {}'.format(association))
#         except Exception as err:
#             logger.warning(err)
#         return association
#
#     code, message = create_association(client_id, customer_uid)
#
#     if code == CLIENT_ALREADY_ASSOCIATED_CODE:
#         message_parts = message.split()
#         old_client_id = message_parts[-1]
#         try:
#             removed_association = billing.Balance.RemoveUserClientAssociation(
#                 OPERATOR, old_client_id, customer_uid
#             )
#             logger.debug('Removed Association: {}'.format(removed_association))
#             create_association(client_id, customer_uid)
#         except Exception as err:
#             logger.warning(err)
#
#
# def create_contract(login, payment='pre'):
#     billing = xmlrpc.client.ServerProxy(xmlhost)
#     billing_client_info = {
#         'name': 'Биллинг-клиент для создания клиента',
#         'email': 'ya@ya.ru',
#         'phone': '+79990002233',
#         'city': 'Москва',
#         'region_id': '225',
#         'currencY': 'RUB',
#     }
#     billing_client = billing.Balance.CreateClient(OPERATOR, billing_client_info)
#     client_id = billing_client[2]
#     logger.debug('Billing client id: {}'.format(client_id))
#
#     info = billing.Balance.GetPassportByLogin(OPERATOR, login)
#     logger.debug('Passport returned: {}'.format(info))
#     customer_uid = info['Uid']
#
#     do_association(client_id, customer_uid)
#
#     person_info = {
#         'client_id': client_id,
#         'person_id': 0,
#         'type': 'ph',
#         'lname': 'person_last_name',
#         'fname': 'person_first_name',
#         'mname': 'person_patr_name',
#         'phone': '+79990002233',
#         'email': 'ya@ya.ru',
#         # флаг партнерский ли плательщик. 1 для расходных договоров, 0 для доходных
#         'is-partner': 1
#     }
#     person = billing.Balance.CreatePerson(OPERATOR, person_info)
#     logger.debug('Person: {}'.format(person))
#
#     offer_info = {
#         'client_id': client_id,
#         'person_id': person,
#         # указываем тип договора. GENERAL - доходный (135, 650), SPENDABLE - расходный (135, 651)
#         'ctype': 'SPENDABLE',
#         'currency': 'RUB',
#         'firm_id': 13,
#         'manager_uid': '389886597',
#         # [135] - старая схема, [135, 651] - две схемы, [651] - только новая схема
#         'services': [135, 651],
#         'country': '225',
#         'region': '213',
#     }
#     if offer_info['ctype'] == 'GENERAL':
#         if payment == 'pre':
#             offer_info.update({
#                 'payment_type': 2,
#                 'offer_activation_due_term': 30,
#                 'offer_activation_payment_amount': '10000.0',
#                 'offer_confirmation_type': 'min-payment',
#             })
#         elif payment == 'post':
#             offer_info.update({
#                 'payment_type': 3,
#                 'payment_term': 180,
#                 'offer_confirmation_type': 'min-payment',
#             })
#         else:
#             raise ValueError('Unknown payment type: {}'.format(payment))
#     else:
#         offer_info.update({
#             'nds': 18,
#             'payment_type': 1,
#             'payment_term': 180,
#             'offer_confirmation_type': 'min-payment',
#
#             offer = billing.Balance.CreateOffer(OPERATOR, offer_info)
#         logger.debug('Offer: {}'.format(offer))
#
#
# return {
#     'contract_id': offer['EXTERNAL_ID'],
#     'contract_internal_id': offer['ID'],
#     'billing_id': str(client_id),
#     'yandex_uid': str(customer_uid)
# }
#
# # In[55]:
#
#
# login = 'amirovr'
# payment = 'pre'
# print(create_contract(login, payment))
#
# # In[ ]:


from balance import balance_api as api
def method():
    payment = 'pre'
    billing_client_info = {
        'name': 'Биллинг-клиент для создания клиента',
        'email': 'ya@ya.ru',
        'phone': '+79990002233',
        'city': 'Москва',
        'region_id': '225',
        'currencY': 'RUB',
    }
    billing_client = api.medium().CreateClient(16571028, billing_client_info)
    client_id = billing_client[2]

    person_info = {
        'client_id': client_id,
        'person_id': 0,
        # для таксопарка ur, для клиента ph
        'type': 'ur',
        'lname': 'person_last_name',
        'fname': 'person_first_name',
        'mname': 'person_patr_name',
        'phone': '+79990002233',
        'email': 'ya@ya.ru',
        # флаг партнерский ли плательщик. 1 для расходных договоров, 0 для доходных
        'is-partner': '1'
    }
    person = api.medium().CreatePerson(16571028, person_info)

    offer_info = {
        'client_id': client_id,
        'person_id': person,
        # указываем тип договора. GENERAL - доходный (135, 650), SPENDABLE - расходный (135, 651)
        'ctype': 'SPENDABLE',
        'currency': 'RUB',
        'firm_id': 13,
        'manager_uid': '389886597',
        # [135] - старая схема, [135, 651] - две схемы, [651] - только новая схема
        'services': [135, 651],
        'country': '225',
        'region': '213',
    }
    if offer_info['ctype'] == 'GENERAL':
        if payment == 'pre':
            offer_info.update({
                'payment_type': 2,
                'offer_activation_due_term': 30,
                'offer_activation_payment_amount': '10000.0',
                'offer_confirmation_type': 'min-payment',
            })
        elif payment == 'post':
            offer_info.update({
                'payment_type': 3,
                'payment_term': 180,
                'offer_confirmation_type': 'min-payment',
            })
        else:
            raise ValueError('Unknown payment type: {}'.format(payment))
    else:
        offer_info.update({
            'nds': 18,
            'payment_type': 1,
            'payment_term': 180,
            'offer_confirmation_type': 'min-payment'})

    offer = api.medium().CreateOffer(16571028, offer_info)

method()