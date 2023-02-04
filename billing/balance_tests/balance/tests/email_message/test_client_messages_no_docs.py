# coding: utf-8

import datetime as d
from dateutil.relativedelta import relativedelta
import pytest
import hamcrest
import balance.balance_api as api
from btestlib import utils, reporter
from balance import balance_db as db
from btestlib.data.defaults import Date
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import ContractCommissionType, ContractPaymentType, Currencies, Services, Firms, Collateral

pytestmark = [reporter.feature(Features.EMAIL), pytest.mark.tickets('BALANCE-28449,BALANCE-29036')]
UNSIGNED = 'UNSIGNED'

contract_params = dict(service=Services.DIRECT, firm=Firms.YANDEX_1, contract_type=ContractCommissionType.NO_AGENCY)


@pytest.mark.parametrize('months', [2, 4], ids=lambda months: 'IS_FAXED contract after %s months' % months)
def test_contract(months):
    contract_state = 'IS_FAXED'
    letter_type = 'contract_{}_months'.format(months)

    dt = d.datetime.now() - relativedelta(months=months)
    contract_id, contract_external_id = prepare_contract(contract_params, dt, state=contract_state)
    utils.check_that(set(get_message_list(contract_id)),
                     hamcrest.equal_to(set(get_letter_list(dt, contract_external_id, [letter_type]))),
                     step=u'Проверяем текст писем')


@pytest.mark.parametrize('contract_state',
                         ['IS_FAXED', 'IS_BOOKED'],
                         ids=lambda contract_state: 'terminate {} contract'.format(contract_state))
def test_terminate(contract_state):
    letter_type = 'terminate_%s' % contract_state.lower().lstrip('is_')

    dt = d.datetime.now() - relativedelta(months=2)
    contract_id, contract_external_id = prepare_contract(contract_params, dt, state='IS_SIGNED')
    prepare_terminate_collateral(contract_id, dt, state=contract_state)
    utils.check_that(set(get_message_list(contract_id)),
                     hamcrest.equal_to(set(get_letter_list(dt, contract_external_id, [letter_type]))),
                     step=u'Проверяем текст писем')


@pytest.mark.parametrize('months', [2, 4], ids=lambda months: 'after %s months' % months)
@pytest.mark.parametrize('contract_state, collateral_state, letter_type_list',
                         [('IS_SIGNED', 'UNSIGNED', ['collateral_unsigned']),
                          ('IS_SIGNED', 'IS_BOOKED', ['collateral_booked']),
                          ('IS_SIGNED', 'IS_FAXED', ['collateral_faxed']),
                          ('IS_BOOKED', 'IS_FAXED', ['collateral_faxed']),
                          ('IS_FAXED', 'UNSIGNED', ['contract', 'collateral_unsigned']),
                          ('IS_FAXED', 'IS_BOOKED', ['contract', 'collateral_booked']),
                          ('IS_FAXED', 'IS_FAXED', ['contract', 'collateral_faxed'])],
                         ids=lambda contract_state, collateral_state, letter_type_list:
                         '{} collateral for {} contract'.format(collateral_state, contract_state))
def test_collateral(months, contract_state, collateral_state, letter_type_list):
    letter_type_list = ['%s_%s_months' % (letter_type, months) for letter_type in letter_type_list]

    dt = d.datetime.now() - relativedelta(months=months)
    contract_id, contract_external_id = prepare_contract(contract_params, dt, state=contract_state)
    prepare_collateral(contract_id, dt, state=collateral_state)
    utils.check_that(set(get_message_list(contract_id)),
                     hamcrest.equal_to(set(get_letter_list(dt, contract_external_id, letter_type_list))),
                     step=u'Проверяем текст писем')


def prepare_contract(contract_params, dt, state=UNSIGNED):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_type = contract_params['contract_type']
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [contract_params['service'].id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'CURRENCY': Currencies.RUB.num_code,
        'FIRM': contract_params['firm'].id,
    }

    if state == 'IS_BOOKED':
        contract_params.update({'IS_FAXED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)), 'IS_BOOKED': 1})
    elif state == 'IS_FAXED' or state == 'IS_SIGNED':
        contract_params.update({state: utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))})

    contract_id, contract_external_id = steps.ContractSteps.create_contract_new(contract_type, contract_params)

    return contract_id, contract_external_id


def prepare_terminate_collateral(contract_id, dt, state=UNSIGNED):
    params = {'CONTRACT2_ID': contract_id, 'FINISH_DT': dt,
              'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))}
    if state == 'IS_BOOKED':
        params.update({'IS_BOOKED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))})
    elif state == 'IS_FAXED':
        params.update({'IS_BOOKED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
                       'IS_FAXED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))})

    steps.ContractSteps.create_collateral(Collateral.TERMINATE, params)

    with reporter.step(u'Проставляем дату брони подписи и получения факса допника равной дате создания договора'):
        #  Подменяеем дату в T_CONTRACT_COLLATERAL на дату, указанную в допнике (при создании допника ставится текущая)
        query = "UPDATE T_CONTRACT_COLLATERAL " \
                "SET IS_FAXED = TO_DATE(:strdate, 'YYYY-MM-DD HH24:MI:SS') " \
                "WHERE CONTRACT2_ID =:item AND NUM = 1"
        db.balance().execute(query, {'strdate': dt.strftime("%Y-%m-%d %H:%M:%S"), 'item': contract_id})
        #  Подменяеем дату в T_CONTRACT_ATTRIBUTES на дату, указанную в допнике (при создании допника ставится текущая)
        query = "UPDATE T_CONTRACT_ATTRIBUTES SET VALUE_DT = TO_DATE(:strdate, 'YYYY-MM-DD HH24:MI:SS') " \
                "WHERE COLLATERAL_ID= " \
                "(SELECT ID FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID=:item AND NUM=1) AND CODE='IS_BOOKED_DT'"
        db.balance().execute(query, {'strdate': dt.strftime("%Y-%m-%d %H:%M:%S"), 'item': contract_id})

    if state == 'IS_FAXED':
        with reporter.step(u'Снимаем бронь подписи допника'):
            # Убираем галочку 'IS_BOOKED'. Без нее допник почему-то не создается.
            query = "UPDATE T_CONTRACT_ATTRIBUTES SET VALUE_NUM = 0 " \
                    "WHERE COLLATERAL_ID= " \
                    "(SELECT ID FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID=:item AND NUM=1) AND CODE='IS_BOOKED'"
            db.balance().execute(query, {'item': contract_id})
    steps.ContractSteps.refresh_contracts_cache(contract_id)


def prepare_collateral(contract_id, dt, state=UNSIGNED):
    params = {'CONTRACT2_ID': contract_id, 'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))}
    if state == 'IS_BOOKED':
        params.update({'IS_BOOKED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))})
    elif state == 'IS_FAXED':
        params.update({'IS_FAXED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt))})

    steps.ContractSteps.create_collateral(Collateral.OTHER, params)

    if state == 'IS_BOOKED':
        with reporter.step(u'Проставляем дату брони подписи допника равной дате создания договора'):
            # Подменяеем дату в T_CONTRACT_ATTRIBUTES на дату, указанную в допнике
            # (при создании допника ставится текущая)
            query = "UPDATE T_CONTRACT_ATTRIBUTES SET VALUE_DT = TO_DATE(:strdate, 'YYYY-MM-DD HH24:MI:SS') " \
                    "WHERE COLLATERAL_ID= " \
                    "(SELECT ID FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID=:item AND NUM=1) AND CODE='IS_BOOKED_DT'"
            db.balance().execute(query, {'strdate': dt.strftime("%Y-%m-%d %H:%M:%S"), 'item': contract_id})
            steps.ContractSteps.refresh_contracts_cache(contract_id)


def get_message_list(contract_id):
    steps.CommonSteps.export('CONTRACT_NOTIFY', 'Contract', contract_id)
    with reporter.step(u'Получаем список писем для клиентов по договору: ' + str(contract_id)):
        query = "SELECT ID FROM T_MESSAGE " \
                "WHERE OBJECT_ID =:item AND DT >= trunc(SYSDATE) - 1 AND RECEPIENT_ADDRESS = 'm-SC@qCWF.rKU'"
        # m-SC@qCWF.rKU - этот адрес используется для рассылок клиентам
        id_list = [recipient['id'] for recipient in db.balance().execute(query, {'item': contract_id})]
    return [api.test_balance().GetEmailMessageData(id_)['body'] for id_ in id_list]


def get_letter_list(dt, contract_external_id, letter_type_list):
    address = u'Адрес для отправки оригинала указан на странице - https://yandex.ru/support/balance/concepts/addresses.html.'
    mail = u'vip-invoice@yandex-team.ru'
    letter_formats_dict = {
        'contract_2_months':
            u'''
Добрый день!

{contract_date} вы прислали нам копию подписанного с вашей стороны договора № {contract_external_id} от {contract_date}.
Его оригинал важно прислать в Яндекс до {date_to_sign}, иначе возможность выставлять новые счета по этому договору будет приостановлена.

{address}

Игнорируйте это автоматическое напоминание, если Вы отправили документ менее двух недель назад.


--
С уважением,
ГК Яндекс
''',
        'contract_4_months':
        u'''
Добрый день!

Хотим обратить ваше внимание, что мы всё еще не получили от вас оригинал договора № {contract_external_id} от {contract_date}.
Из-за этого возможность выставлять новые счета по нему была приостановлена. Она станет доступна снова, как только вы пришлёте оригинал документа в Яндекс.

Напомним, что копию договора мы получили от вас еще {contract_date}.

{address}

Игнорируйте это автоматическое напоминание, если Вы отправили документ менее двух недель назад.


--
С уважением,
ГК Яндекс
''',
        'terminate_faxed':
        u'''
Добрый день!

Вы не прислали нам оригинал подписанного с вашей стороны соглашения № 01 от {contract_date} о расторжении договора № {contract_external_id} от {contract_date}.

Настоящим уведомляем вас о том, что оригинал данного дополнительного соглашения должен быть подписан и направлен в Яндекс. {address}

Игнорируйте это автоматическое напоминание, если Вы отправили документ менее двух недель назад.


--
С уважением,
ГК Яндекс
''',
        'terminate_booked':
        u'''
Добрый день!

{contract_date} было согласовано соглашение № 01 от {contract_date} о расторжении договора № {contract_external_id} от {contract_date}.

Его важно подписать и прислать в Яндекс по электронной почте {mail}.

Мы также ждем от вас оригинал документа. {address}


--
С уважением,
ГК Яндекс
''',
        'collateral_unsigned_2_months':
            u'''
Добрый день!

{contract_date} к договору № {contract_external_id} от {contract_date} было создано дополнительное соглашение № 01 от {contract_date}.

Его важно подписать и прислать в Яндекс по электронной почте {mail} до {date_to_sign}, иначе возможность выставлять новые счета по договору будет приостановлена.

Мы также ждем от вас оригинал документа. {address}


--
С уважением,
ГК Яндекс
''',
        'collateral_unsigned_4_months':
        u'''
Добрый день!

Хотим обратить ваше внимание, что мы всё еще не получили от вас подписанное дополнительное соглашение № 01 от {contract_date} к договору № {contract_external_id} от {contract_date}. Из-за этого возможность выставлять новые счета по договору была приостановлена.

Она станет доступна снова, как только вы пришлете документ, подписанный с вашей стороны. Его важно подписать и прислать в Яндекс по электронной почте {mail}.

Мы также ждем от вас оригинал документа. {address}


--
С уважением,
ГК Яндекс
''',
        'collateral_booked_2_months':
        u'''
Добрый день!

{contract_date} к договору № {contract_external_id} от {contract_date} было создано дополнительное соглашение № 01 от {contract_date}.

Его важно подписать и прислать в Яндекс по электронной почте {mail} до {date_to_sign}, иначе возможность выставлять новые счета по договору будет приостановлена.

Мы также ждем от вас оригинал документа. {address}


--
С уважением,
ГК Яндекс
''',
        'collateral_booked_4_months':
        u'''
Добрый день!

Хотим обратить ваше внимание, что мы всё еще не получили от вас подписанное дополнительное соглашение № 01 от {contract_date} к договору № {contract_external_id} от {contract_date}. Из-за этого возможность выставлять новые счета по договору была приостановлена.

Она станет доступна снова, как только вы пришлете документ, подписанный с вашей стороны. Его важно подписать и прислать в Яндекс по электронной почте {mail}.

Мы также ждем от вас оригинал документа. {address}


--
С уважением,
ГК Яндекс
''',
        'collateral_faxed_2_months':
        u'''
Добрый день!

{contract_date} вы прислали нам копию подписанного с вашей стороны дополнительного соглашения № 01 от {contract_date} к договору № {contract_external_id} от {contract_date}.
Его оригинал важно прислать в Яндекс до {date_to_sign}, иначе возможность выставлять новые счета по договору будет приостановлена.

{address}

Игнорируйте это автоматическое напоминание, если Вы отправили документ менее двух недель назад.


--
С уважением,
ГК Яндекс
''',
        'collateral_faxed_4_months':
        u'''
Добрый день!

Хотим обратить ваше внимание, что мы всё еще не получили от вас подписанное дополнительное соглашение № 01 от {contract_date} к договору № {contract_external_id} от {contract_date}. Из-за этого возможность выставлять новые счета по нему была приостановлена. Она станет доступна снова, как только вы пришлёте оригинал документа в Яндекс.

Напомним, что копию дополнительного соглашения мы получили от вас еще {contract_date}.

Его оригинанал необходимо отправить нам. {address}

Игнорируйте это автоматическое напоминание, если Вы отправили документ менее двух недель назад.


--
С уважением,
ГК Яндекс
'''
    }
    return [letter_formats_dict[letter_type].format(contract_date=dt.strftime("%d.%m.%Y"),
                                                    contract_external_id=contract_external_id,
                                                    address=address,
                                                    date_to_sign=(dt + relativedelta(months=3)).strftime("%d.%m.%Y"),
                                                    mail=mail
                                                    )
            for letter_type in letter_type_list]
