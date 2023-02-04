import os

import arrow
import pytest
import requests


def _generate_accounts_request(client_id: str):
    return {'balances': [{
        'loc': {
            'namespace': 'plus',
            'type': 'act_income',
            'client_id': client_id,
            'country': None,
            'currency': None,
            'contract_id': None,
            'product_mdh_id': None,
        },
        'dt': 99999999999,
    }, {
        'loc': {
            'namespace': 'plus',
            'type': 'act_expense',
            'client_id': client_id,
            'country': None,
            'currency': None,
            'contract_id': None,
            'page_id': None,
        },
        'dt': 99999999999,
    }, {
        'loc': {
            'namespace': 'plus',
            'type': 'bonus_spendable',
            'client_id': client_id,
            'country': None,
            'currency': None,
            'original_act_sender_id': None,
            'original_act_receiver_id': None,
            'cashback_service': None,
            'topup_cashback_service': None,
            'cashback_type': None,
            'has_plus': None,
            'operator': None,
        },
        'dt': 99999999999,
    }, {
        'loc': {
            'namespace': 'plus',
            'type': 'bonus_general',
            'client_id': client_id,
            'country': None,
            'currency': None,
            'original_act_sender_id': None,
            'original_act_receiver_id': None,
            'cashback_service': None,
            'topup_cashback_service': None,
            'cashback_type': None,
            'has_plus': None,
            'operator': None,
        },
        'dt': 99999999999,
    }]}


@pytest.fixture()
def contract_4836456_requests(firm_yandex, contract_4836456, rub_to_usd, kzt_to_rub):
    """
    Этот кейс не может быть проверен, так как закрывается раз в квартал,
    а аккаунтер не поддерживает транзакции в дадёкое прошлое
    """
    processor_request_tpl = """
        {{"namespace":"plus","endpoint":"{endpoint}","event":{{
            "act_finish_timestamp":{act_finish_timestamp},
            "act_start_timestamp":{act_start_timestamp},
            "bonus_sum_accumulated":"{bonus_sum_accumulated}",
            "act_receiver_id":"31","act_receiver_name":"ТОО «Яндекс.Такси Корп»",
            "act_sender_id":"1","act_sender_name":"ООО «Яндекс»",
            "cashback_service":"eda","cashback_type":"transaction","country":"KZ","currency":"KZT",
            "dry_run":false,"has_plus":"false","act_type":"general",
            "original_act_receiver_id":"31","original_act_receiver_name":"ТОО «Яндекс.Такси Корп»",
            "original_act_sender_id":"24","original_act_sender_name":"ТОО «Яндекс.Еда Казахстан»",
            "recalculate_attempt_number":0,"topup_cashback_service":"yataxi"
        }}}}
    """
    first = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-61).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        bonus_sum_accumulated='30',
    )
    second = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-1).timestamp()),
        bonus_sum_accumulated='39',
    )
    yield {
        'first-check': processor_request_tpl.format(
            endpoint='act-row-check', **first
        ),
        'first': processor_request_tpl.format(
            endpoint='act-row', **first
        ),
        'second-check': processor_request_tpl.format(
            endpoint='act-row-check', **second
        ),
        'second': processor_request_tpl.format(
            endpoint='act-row', **second
        ),
        'accounts': _generate_accounts_request(str(int(contract_4836456['obj']['client_id']))),
    }


@pytest.fixture()
def contract_4834697_requests(firm_yandex, contract_4834697):
    processor_request_tpl = """
        {{"namespace":"plus","endpoint":"{endpoint}","event":{{
            "act_finish_timestamp":{act_finish_timestamp},
            "act_start_timestamp":{act_start_timestamp},
            "bonus_sum_accumulated":"{bonus_sum_accumulated}",
            "act_receiver_id":"130","act_receiver_name":"ООО «Яндекс.Доставка»",
            "act_sender_id":"1","act_sender_name":"ООО «Яндекс»",
            "cashback_service":"Нет аналитики","cashback_type":"agent","country":"RU","currency":"RUB",
            "dry_run":false,"has_plus":"false","act_type":"spendable",
            "original_act_receiver_id":"130","original_act_receiver_name":"ООО «Яндекс.Доставка»",
            "original_act_sender_id":"1","original_act_sender_name":"ООО «Яндекс»",
            "recalculate_attempt_number":0,"topup_cashback_service":"taxi_agent"
        }}}}
    """
    first = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-61).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        bonus_sum_accumulated='663',
    )
    second = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-1).timestamp()),
        bonus_sum_accumulated='606',
    )
    yield {
        'first-check': processor_request_tpl.format(
            endpoint='act-row-check', **first
        ),
        'first': processor_request_tpl.format(
            endpoint='act-row', **first
        ),
        'second-check': processor_request_tpl.format(
            endpoint='act-row-check', **second
        ),
        'second': processor_request_tpl.format(
            endpoint='act-row', **second
        ),
        'accounts': _generate_accounts_request(str(int(contract_4834697['obj']['client_id']))),
    }


@pytest.fixture()
def contract_2144410_requests(firm_yandex, contract_2144410):
    processor_request_tpl = """
        {{"namespace":"plus","endpoint":"{endpoint}","event":{{
            "act_finish_timestamp":{act_finish_timestamp},
            "act_start_timestamp":{act_start_timestamp},
            "bonus_sum_accumulated":"{bonus_sum_accumulated}",
            "act_receiver_id":"13","act_receiver_name":"ООО «Яндекс.Такси»",
            "act_sender_id":"1","act_sender_name":"ООО «Яндекс»",
            "cashback_service":"Нет аналитики","cashback_type":"agent","country":"RU","currency":"RUB",
            "dry_run":false,"has_plus":"false","act_type":"spendable",
            "original_act_receiver_id":"130","original_act_receiver_name":"ООО «Яндекс.Доставка»",
            "original_act_sender_id":"1","original_act_sender_name":"ООО «Яндекс»",
            "recalculate_attempt_number":0,"topup_cashback_service":"taxi_agent"
        }}}}
    """
    first = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-61).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        bonus_sum_accumulated='131087.27',
    )
    second = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-1).timestamp()),
        bonus_sum_accumulated='138221.07',
    )
    yield {
        'first-check': processor_request_tpl.format(
            endpoint='act-row-check', **first
        ),
        'first': processor_request_tpl.format(
            endpoint='act-row', **first
        ),
        'second-check': processor_request_tpl.format(
            endpoint='act-row-check', **second
        ),
        'second': processor_request_tpl.format(
            endpoint='act-row', **second
        ),
        'accounts': _generate_accounts_request(str(int(contract_2144410['obj']['client_id']))),
    }


@pytest.fixture()
def no_analitycs_requests():
    processor_request_tpl = """
        {{"namespace":"plus","endpoint":"{endpoint}","event":{{
            "act_finish_timestamp":{act_finish_timestamp},
            "act_start_timestamp":{act_start_timestamp},
            "bonus_sum_accumulated":"{bonus_sum_accumulated}",
            "act_receiver_id":"Нет аналитики","act_receiver_name":"",
            "act_sender_id":"1","act_sender_name":"ООО «Яндекс»",
            "cashback_service":"Нет аналитики","cashback_type":"agent","country":"RU","currency":"RUB",
            "dry_run":false,"has_plus":"false","act_type":"spendable",
            "original_act_receiver_id":"130","original_act_receiver_name":"ООО «Яндекс.Доставка»",
            "original_act_sender_id":"1","original_act_sender_name":"ООО «Яндекс»",
            "recalculate_attempt_number":0,"topup_cashback_service":"taxi_agent"
        }}}}
    """
    first = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-61).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        bonus_sum_accumulated='111',
    )
    second = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-1).timestamp()),
        bonus_sum_accumulated='222',
    )
    yield {
        'first-check': processor_request_tpl.format(
            endpoint='act-row-check', **first
        ),
        'first': processor_request_tpl.format(
            endpoint='act-row', **first
        ),
        'second-check': processor_request_tpl.format(
            endpoint='act-row-check', **second
        ),
        'second': processor_request_tpl.format(
            endpoint='act-row', **second
        ),
        'accounts': _generate_accounts_request("0"),
    }


@pytest.fixture()
def no_contract_requests(firm_yabank):
    processor_request_tpl = """
        {{"namespace":"plus","endpoint":"{endpoint}","event":{{
            "act_finish_timestamp":{act_finish_timestamp},
            "act_start_timestamp":{act_start_timestamp},
            "bonus_sum_accumulated":"{bonus_sum_accumulated}",
            "act_receiver_id":"1","act_receiver_name":"ООО «Яндекс»",
            "act_sender_id":"1098","act_sender_name":"АО «Яндекс Банк»",
            "cashback_service":"Нет аналитики","cashback_type":"agent","country":"RU","currency":"RUB",
            "dry_run":false,"has_plus":"false","act_type":"general",
            "original_act_receiver_id":"130","original_act_receiver_name":"ООО «Яндекс.Доставка»",
            "original_act_sender_id":"1","original_act_sender_name":"ООО «Яндекс»",
            "recalculate_attempt_number":0,"topup_cashback_service":"taxi_agent"
        }}}}
    """
    first = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-61).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        bonus_sum_accumulated='333',
    )
    second = dict(
        act_start_timestamp=int(arrow.utcnow().shift(days=-31).timestamp()),
        act_finish_timestamp=int(arrow.utcnow().shift(days=-1).timestamp()),
        bonus_sum_accumulated='444',
    )
    yield {
        'first-check': processor_request_tpl.format(
            endpoint='act-row-check', **first
        ),
        'first': processor_request_tpl.format(
            endpoint='act-row', **first
        ),
        'second-check': processor_request_tpl.format(
            endpoint='act-row-check', **second
        ),
        'second': processor_request_tpl.format(
            endpoint='act-row', **second
        ),
        'accounts': _generate_accounts_request("0"),
    }


def canonize_processor_response(resp):
    event = resp['data']['result']['event']
    act_finish_timestamp = event['act_finish_timestamp']
    event['tariffer_payload']['external_id'] = event['tariffer_payload']['external_id'].replace(
        str(act_finish_timestamp), '<canonized>')
    event['tariffer_payload']['common_ts'] = '<canonized>'
    event['act_start_timestamp'] = '<canonized>'
    event['act_finish_timestamp'] = '<canonized>'
    return resp


def test_plus_processing(
    firm_yandex, product_1_ru_rub,
    contract_2144410_requests, contract_4834697_requests,
    no_analitycs_requests, no_contract_requests
):
    contract_X_requests = {
        '2144410': contract_2144410_requests,
        '4834697': contract_4834697_requests,
        'no_analitycs': no_analitycs_requests,
        'no_contract': no_contract_requests,
    }

    processor_base_url = os.environ['PROCESSOR_BASE_URL']
    processor_process_url = f'{processor_base_url}/v1/process'
    accounts_base_url = os.environ['ACCOUNTS_BASE_URL']
    accounts_read_batch_url = f'{accounts_base_url}/v1/batch/read'

    responses = {}

    for contract_id, contract_requests in contract_X_requests.items():
        responses[contract_id] = {}
        resp = requests.post(processor_process_url,
                             data=contract_requests['first-check'].encode('utf-8')).json()
        responses[contract_id]['12_processor-check'] = canonize_processor_response(resp)
        resp = requests.post(processor_process_url,
                             data=contract_requests['first'].encode('utf-8')).json()
        responses[contract_id]['15_processor'] = canonize_processor_response(resp)
        resp = requests.post(accounts_read_batch_url,
                             json=contract_requests['accounts']).json()
        responses[contract_id]['18_account'] = resp

        resp = requests.post(processor_process_url,
                             data=contract_requests['second-check'].encode('utf-8')).json()
        responses[contract_id]['22_processor-check'] = canonize_processor_response(resp)
        resp = requests.post(processor_process_url,
                             data=contract_requests['second'].encode('utf-8')).json()
        responses[contract_id]['25_processor'] = canonize_processor_response(resp)
        resp = requests.post(accounts_read_batch_url,
                             json=contract_requests['accounts']).json()
        responses[contract_id]['28_account'] = resp

    return responses
