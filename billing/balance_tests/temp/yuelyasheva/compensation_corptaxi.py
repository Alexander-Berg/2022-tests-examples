
import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data.partner_contexts import *
from btestlib.matchers import has_entries_casted

@pytest.mark.parametrize('context, uid, login', [
                        (CORP_TAXI_RU_CONTEXT_SPENDABLE, 436363578, 'yb-atst-user-5'),
                        (CORP_TAXI_KZ_CONTEXT_SPENDABLE, 675282951, 'yb-atst-user-32')
                        ],
                         ids=[
                             'CORP_TAXI_RU',
                             'CORP_TAXI_KZ'
                         ])
def test_corp_taxi_compensation(context, uid, login, switch_to_trust):
    switch_to_trust(service=context.service)
    user = User(uid, login, secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context.service)
    service_product_id_fee = steps.SimpleApi.create_service_product(context.service, taxi_client_id, service_fee=1)
    taxi_person_id = steps.PersonSteps.create(taxi_client_id, context.person_type.code)
    taxi_person_partner_id = steps.PersonSteps.create(taxi_client_id, context.person_type.code, {'is-partner': '1'})

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()
    corp_person_id = steps.PersonSteps.create(corp_client_id, context.person_type.code)

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    # расходный с таксопарком
    _, _, taxi_contract_spendable_id, _ = steps.ContractSteps.create_partner_contract(
            context,
            client_id=taxi_client_id, person_id=taxi_person_partner_id)

    # создаем платеж компенсацию
    _, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_compensation(context.service, service_product_id, user=user,
                                            order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # формируем шаблон для сравнения
    expected_template = steps.SimpleApi.create_expected_tpt_row_compensation(context, taxi_client_id, taxi_contract_spendable_id,
                                                                             taxi_person_partner_id, trust_payment_id, payment_id)

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж компенсацию с шаблоном')