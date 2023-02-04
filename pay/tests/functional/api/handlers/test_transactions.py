import re
from uuid import uuid4

import pytest
from aioresponses import CallbackResult

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, match_equality, matches_regexp

from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus


@pytest.fixture
async def bill(user, storage, rands, document):
    return await storage.bill.create(
        Bill(
            document_id=document.document_id,
            uid=user.uid,
            supplier_bill_id=rands(),
            status=BillStatus.NEW,
            amount=100,
            amount_to_pay=100,
            bill_date=utcnow(),
        )
    )


@pytest.fixture
async def document(user, storage, rands):
    return await storage.document.create(
        Document(document_id=uuid4(), uid=user.uid, value=rands(), code=DocumentCode.DRIVER_LICENSE)
    )


@pytest.fixture(autouse=True)
def mock_kazna(aioresponses_mocker, bill_payments_settings):
    def callback(url, *, headers, **kwargs):
        assert headers.get('X-Ya-Dest-Url') == bill_payments_settings.KAZNA_API_URL.rstrip('/') + '/pay'

        return CallbackResult(
            200,
            payload={
                '3ds': {
                    'acsUrl': 'https://acs.test',
                    'md': '789',
                    'paReq': 'pareq',
                    'termUrl': 'https://term.test',
                },
                'paymentID': 17568,
            },
        )

    base_url = bill_payments_settings.ZORA_URL.rstrip('/')
    return aioresponses_mocker.post(re.compile(f'{base_url}.*'), callback=callback)


@pytest.mark.usefixtures('mock_app_authentication')
@pytest.mark.asyncio
async def test_post_orders(app, user, storage, rands, bill):
    resp = await app.post('/api/v1/orders', json={'bill_ids': [str(bill.bill_id)]}, raise_for_status=True)
    order_id = (await resp.json())['data']['order']['order_id']

    resp = await app.post(
        f'/api/v1/orders/{order_id}/transactions',
        json={
            'return_url': 'https://return.test',
            'payment_method': 'YANDEX_PAY',
            'payment_token': 'TOKEN',
            'payer_full_name': 'Ivanov Ivan Ivanovich',
            'mpi_3ds_info': {
                'browser_language': 'ru',
                'browser_tz': '-180',
                'browser_accept_header': 'ACCEPT_HEADER',
                'browser_ip': '192.0.2.1',
                'browser_screen_width': 1920,
                'browser_screen_height': 1080,
                'window_width': 640,
                'window_height': 480,
                'browser_color_depth': 24,
                'browser_user_agent': 'USER_AGENT',
                'browser_javascript_enabled': True,
            },
        },
    )
    json_body = await resp.json()

    assert_that(resp.status, equal_to(201))
    assert_that(
        json_body,
        equal_to(
            {
                'status': 'success',
                'code': 201,
                'data': {
                    'transaction': match_equality(
                        has_entries(
                            {
                                'status': 'NEW',
                                'acs_url': matches_regexp(re.compile('^https://acs.test.*')),
                            }
                        )
                    ),
                },
            }
        ),
    )
