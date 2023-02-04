# coding=utf-8
from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta

from btestlib.constants import ContractSubtype, Currencies, PersonTypes, OfferConfirmationType, \
    OEBSOperationType, Services
import balance.balance_db as db
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenDefParams
import balance.balance_api as api
from balance import balance_steps as steps
from btestlib import reporter, utils
from btestlib.data import defaults
from btestlib.constants import Users, Nds, Currencies, Managers, Services, Firms, Regions, PersonTypes, Paysyses

MANAGER = Managers.SOME_MANAGER

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
DEFAULT_TIMEOUT = 5
DEFAULT_TERM = 10
DEFAULT_AMOUNT = {
    Currencies.RUB: 100,
    Currencies.EUR: 5,
    Currencies.USD: 5,
    Currencies.BYN: 100
}

def create_params_for_create_common_contract(client_id, person_id, params):
    params.update({
        'client_id': client_id,
        'manager_uid': MANAGER.uid,
        'person_id': person_id,
    })
    params['currency'] = 'RUB'
    return params

def create_person_and_offer_with_additional_params(client_id, contract_data, is_spendable=0, is_postpay=1,
                                                       remove_params=None, additional_params=None, is_offer=True,
                                                       person_id=None, ctype=None):

    params = contract_data['CONTRACT_PARAMS'].copy()
    if not is_spendable:
        params = {k.lower(): v for k, v in params.items()}
        params['firm_id'] = params.pop('firm')

    if params.has_key('partner_commission_pct'):
        params['commission_pct'] = params.pop('partner_commission_pct')
    params.update({'client_id': client_id, 'person_id': person_id, 'manager_uid': Managers.SOME_MANAGER.uid})
    # params['currency'] = params['currency'].char_code
    if not is_spendable and is_postpay:
        params.update({'payment_term': 10, 'payment_type': 3})
    elif not is_spendable and not is_postpay:
        params.update({'payment_type': 2})

    if remove_params:
        for key in remove_params:
            params.pop(key, None)

    if additional_params:
        params.update(additional_params)

    params['currency'] = params['currency'].char_code
    if params.has_key('country'):
        params['country'] = params['country'].id

    if ctype:
        params.update({'ctype': ctype})

    if is_offer:
        contract_id, contract_eid = steps.ContractSteps.create_offer(params)
    else:
        contract_id, contract_eid = steps.ContractSteps.create_common_contract(params)
    return contract_id, contract_eid, person_id

#Создаем подписанный оффер
def create_offer(contract_data=GenDefParams.YANDEX_TAXI,
                 services=None,
                 offer_confirmation_type=OfferConfirmationType.NO,
                 client_id=None, person_id=None,
                 signed = 0, is_deactivated = 0, is_faxed = 0, is_suspended = 0):
    with reporter.step(u'Подготавливаем договор такси с помощью CreateOffer'):
        if not client_id:
            client_id = steps.ClientSteps.create()
        if not person_id:
            person_id = steps.PersonSteps.create(client_id, contract_data['PERSON_TYPE'])
        additional_params = {'start_dt': CONTRACT_START_DT}
        additional_params.update({'offer_confirmation_type': offer_confirmation_type.value})
        if services:
            additional_params.update({'services': services})

        if Services.TAXI_CORP.id in contract_data['CONTRACT_PARAMS']['SERVICES']:
            additional_params.update({'ctype': 'GENERAL'})

        if is_deactivated == 1:
            additional_params.update({'is_deactivated': 1})

        if is_suspended == 1:
            client_id = steps.ClientSteps.create()
            person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
            contract_id, _ = steps.ContractSteps.create_contract('taxi_pre',
                                                         {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                          'DT': CONTRACT_START_DT.strftime('%Y-%m-%dT%H:%M:%S'),
                                                          'IS_SIGNED': CONTRACT_START_DT.strftime('%Y-%m-%dT%H:%M:%S'),
                                                          'IS_SUSPENDED': CONTRACT_START_DT.strftime(
                                                                  '%Y-%m-%dT%H:%M:%S'),
                                                          'OFFER_CONFIRMATION_TYPE': 'no',
                                                          'COMMISSION': 9})
            return client_id, person_id, contract_id

        if signed == 1:
            contract_id, _, _ = create_person_and_offer_with_additional_params(client_id,
                                                                                               contract_data,
                                                                                               # remove_params=remove_params,
                                                                                               additional_params=additional_params,
                                                                                               is_offer=1,
                                                                                               person_id=person_id)
        if is_faxed == 1:
            dt = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
            additional_params.update({'is_faxed': 1, 'is_faxed_dt': dt, 'commission': 9})
            #additional_params.update({'comission': 9})
            contract_id, _, _ = create_person_and_offer_with_additional_params(client_id,
                                                                                               contract_data,
                                                                                               # remove_params=remove_params,
                                                                                               additional_params=additional_params,
                                                                                               is_offer=0,
                                                                                               person_id=person_id)

        if is_faxed == 0 and signed == 0:
            additional_params.update({'commission': 9})
            contract_id, _, _ = create_person_and_offer_with_additional_params(client_id,
                                                                               contract_data,
                                                                               # remove_params=remove_params,
                                                                               additional_params=additional_params,
                                                                               is_offer=0,
                                                                               person_id=person_id)

        return client_id, person_id, contract_id




#Активный, подписанный
#Превратить в тест
def signed_offer_active():
    client_id, person_id, contract_id = create_offer(signed = 1)
    contracts_info = get_client_contracts(client_id)


#Активный, подписанный по факсу
def faxed_offer_active():
    client_id, person_id, contract_id = create_offer(is_faxed = 1)
    contracts_info = get_client_contracts(client_id)
    # contract_data=GenDefParams.YANDEX_TAXI
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, contract_data['PERSON_TYPE'])
    # dt = utils.Date.first_day_of_month(datetime.now())
    # params = create_params_for_create_common_contract(client_id, person_id,
    #                                                   contract_data['CONTRACT_PARAMS'].copy())
    #
    # params.update({'comission': 9, 'is_faxed': 1, 'is_faxed_dt': dt})

    # contract_id, contract_eid = create_common_contract(params)

    #with reporter.step(u'Меняем IS_SIGNED на IS_FAXED'):
    #    db.balance().execute("UPDATE t_contract_collateral SET is_faxed = :dt WHERE contract2_id = :contract_id",
    #                         {'dt': dt, 'contract_id': contract_id})
    #    db.balance().execute("UPDATE t_contract_collateral SET is_signed = null WHERE contract2_id = :contract_id",
    #                         {'dt': dt, 'contract_id': contract_id})
    # contracts_info = get_client_contracts(client_id)

#Деактивированный, подписанный - автоматически проставляется 'IS_SUSPENDED': 1
def signed_offer_deactivated():
    client_id, person_id, contract_id = create_offer(signed = 1, is_deactivated = 1)
    contracts_info = get_client_contracts(client_id)

#Деактивированный, подписанный по факсу - автоматически проставляется 'IS_SUSPENDED': 1
def faxed_offer_deactivated():
    client_id, person_id, contract_id = create_offer(is_faxed = 1, is_deactivated = 1)
    contracts_info = get_client_contracts(client_id)

#Неподписанный
def not_signed_offer():
    client_id, person_id, contract_id = create_offer(signed = 0)
    contracts_info = get_client_contracts(client_id)

#Аннулированный, подписанный
def signed_offer_cancelled():
    client_id, person_id, contract_id = create_offer(signed = 1)
    dt = datetime.now()
    db.balance().execute("UPDATE t_contract_collateral SET is_cancelled = :dt WHERE contract2_id = :contract_id",
                             {'dt': dt, 'contract_id': contract_id})
    contracts_info = get_client_contracts(client_id)

#Аннулированный, неподписанный
def not_signed_offer_cancelled():
    client_id, person_id, contract_id = create_offer(signed = 0)
    dt = datetime.now()
    db.balance().execute("UPDATE t_contract_collateral SET is_cancelled = :dt WHERE contract2_id = :contract_id",
                             {'dt': dt, 'contract_id': contract_id})
    contracts_info = get_client_contracts(client_id)
    # for i in range(len(contracts_info[0]['SERVICES'])):
    #     partner_balance = api.medium().GetPartnerBalance(contracts_info[0]['SERVICES'][i], [contract_id])


#Приостановленный, подписанный
def signed_offer_suspended():
    client_id, person_id, contract_id = create_offer(signed = 1, is_suspended = 1)
    contracts_info = get_client_contracts(client_id)
    taxi_balance = api.medium().GetTaxiBalance([contract_id])

def create_common_contract(params, passport_uid=defaults.PASSPORT_UID):
    with reporter.step(u'Создаем договор через CreateCommonContract'):
        result = api.medium().CreateCommonContract(passport_uid, params)
        contract_id, contract_eid = result['ID'], result['EXTERNAL_ID']

    steps.ContractSteps.report_url(contract_id)

    return contract_id, contract_eid

#Получаем инфу о договорах клиента по client_id
def get_client_contracts(client_id):
    with reporter.step(u"Получаем договоры для клиента с id: {}".format(client_id)):
        contracts_info = api.medium().GetClientContracts({'ClientID': client_id, 'Signed': 0})
        reporter.attach(u"Информация о договорах", utils.Presenter.pretty(contracts_info))
        return contracts_info

#signed_offer_active()

# parnter_balance = api.medium().GetPartnerBalance(111, [849493])
#
# parnter_balance = api.medium().GetTaxiBalance([849493])
# #    YANDEX_TAXI = {'CONTRACT_PARAMS': {'FIRM': Firms.TAXI_13.id, 'COUNTRY': Regions.RU, 'REGION': '77000000000',
#                                       'CURRENCY': Currencies.RUB, 'SERVICES': [111, 124, 128, 125, 605, 626],
#                                       'NDS_FOR_RECEIPT': Nds.DEFAULT,
#                                       'PERSONAL_ACCOUNT': 1, 'SERVICE_MIN_COST': Decimal('0')},
#                   'PERSON_TYPE': 'ur'}

signed_offer_active()


