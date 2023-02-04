import json
import requests

from btestlib.utils import call_http
from btestlib import passport_steps

URL = 'https://ws-{}.balance.os.yandex.net:{}/'
HOST = 'dev1e'
PORT = 8080

FULL_URL = URL.format(HOST, PORT)


def ping():
    session = passport_steps.auth_session()
    headers = {'X-Requested-With': 'XMLHttpRequest'}
    r = call_http(session, '{}v1/ping'.format(FULL_URL), {}, headers, method='GET')
    pass


def cashmachines():
    session = passport_steps.auth_session()
    headers = {'X-Requested-With': 'XMLHttpRequest'}
    r = call_http(session, '{}v1/cashmachines'.format(FULL_URL), {}, headers, method='GET')
    pass


def receipts():
    session = passport_steps.auth_session()
    headers = {'X-Requested-With': 'XMLHttpRequest'}
    params = {
        "receipt_content": {
            "firm_inn": "7736207543",
            "receipt_type": "income",
            "taxation_type": "OSN",
            "agent_type": "none_agent",
            "client_email_or_phone": "isupov@yandex-team.ru",
            "rows": [
                {
                    "price": "10.0",
                    "qty": "10.0",
                    "tax_type": "nds_18",
                    "payment_type_type": "prepayment",
                    "text": "666"
                }
            ],
            "payments": [
                {
                    "amount": "100",
                    "payment_type": "card"
                }
            ]
        }
    }
    r = call_http(session, '{}v1/receipts'.format(FULL_URL), params, headers, method='POST')
    pass


if __name__ == "__main__":
    cashmachines()
    # receipts()
