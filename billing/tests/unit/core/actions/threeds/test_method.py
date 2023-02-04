from dataclasses import replace
from uuid import UUID

import pytest

from sendr_auth import User

from hamcrest import assert_that, contains_inanyorder, contains_string

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.method import ThreeDSMethodAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import ThreeDSMethodHTMLDocumentID
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    ThreeDSMethodDataNotFoundError,
    ThreeDSMethodDocumentNotSupportedError,
    TransactionNotFoundError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage import Storage
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import ThreeDS2MethodData
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import beautiful_soup


@pytest.fixture
async def checkout_order(storage, stored_checkout_order):
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            uid=7777,
        )
    )


@pytest.fixture
async def transaction(storage, checkout_order, entity_transaction, entity_threeds_authentication_request):
    return await storage.transaction.create(
        replace(
            entity_transaction,
            transaction_id=UUID('56bdfb08-337b-4c3d-8727-76a1d11945a3'),
            checkout_order_id=checkout_order.checkout_order_id,
            data=replace(
                entity_transaction.data,
                threeds=replace(
                    entity_transaction.data.threeds,
                    threeds2_method_data=ThreeDS2MethodData(
                        threeds_method_notification_url='https://method-result.test',
                        threeds_server_transaction_id='trans-id',
                        threeds_server_method_url='https://3ds-server.test',
                        threeds_acs_method_url='https://acs.test',
                        threeds_acs_method_params={'foo': 'bar'},
                    ),
                ),
            ),
        )
    )


class TestRenderIndex:
    @pytest.mark.asyncio
    async def test_render(self, storage: Storage, transaction):
        method_html = await ThreeDSMethodAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id
        ).run()

        soup = beautiful_soup(method_html)
        body = soup.body
        iframes = list(body.find_all('iframe'))
        scripts = list(body.find_all('script'))

        # should have both iframes
        assert_that(
            [item['src'] for item in iframes],
            contains_inanyorder(
                'https://test.pay.yandex.ru/3ds/method/56bdfb08-337b-4c3d-8727-76a1d11945a3/3ds-server',
                'https://test.pay.yandex.ru/3ds/method/56bdfb08-337b-4c3d-8727-76a1d11945a3/acs',
            ),
        )
        # should have postmessage bubbler
        assert_that(
            scripts[0].string,
            contains_string('parent.postMessage'),
        )
        # should have tds waiter
        assert_that(
            scripts[1].string,
            contains_string('timeoutId'),
        )

    @pytest.mark.asyncio
    async def test_render_without_acs_url(self, storage: Storage, transaction):
        transaction.data.threeds.threeds2_method_data.threeds_acs_method_url = None
        transaction = await storage.transaction.save(transaction)

        method_html = await ThreeDSMethodAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id
        ).run()

        soup = beautiful_soup(method_html)
        body = soup.body
        iframes = list(body.find_all('iframe'))

        assert_that(
            [item['src'] for item in iframes],
            contains_inanyorder(
                'https://test.pay.yandex.ru/3ds/method/56bdfb08-337b-4c3d-8727-76a1d11945a3/3ds-server',
            )
        )


class TestRenderACS:
    @pytest.mark.asyncio
    async def test_render(self, storage: Storage, transaction):
        method_html = await ThreeDSMethodAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id,
            html_document_id=ThreeDSMethodHTMLDocumentID.ACS,
        ).run()

        soup = beautiful_soup(method_html)
        body = soup.body
        form = body.form
        inp = list(form.find_all('input'))

        assert body['onload'] == "setTimeout(function() { document.forms['form'].submit(); }, 10)"
        assert form['action'] == 'https://acs.test'

        assert len(inp) == 1
        assert inp[0]['name'] == 'foo'
        assert inp[0]['value'] == 'bar'


class TestRenderThreeDSServer:
    @pytest.mark.asyncio
    async def test_render(self, storage: Storage, transaction):
        method_html = await ThreeDSMethodAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id,
            html_document_id=ThreeDSMethodHTMLDocumentID.THREEDS_SERVER,
        ).run()

        soup = beautiful_soup(method_html)
        body = soup.body
        form = body.form
        inp = list(form.find_all('input'))

        assert body['onload'] == "setTimeout(function() { document.forms['form'].submit(); }, 10)"
        assert form['action'] == 'https://3ds-server.test'

        assert len(inp) == 0


@pytest.mark.asyncio
async def test_wrong_uid(storage: Storage, transaction):
    with pytest.raises(TransactionNotFoundError):
        await ThreeDSMethodAction(
            user=User(uid=8888),
            transaction_id=transaction.transaction_id
        ).run()


@pytest.mark.asyncio
async def test_no_method_data(storage: Storage, transaction):
    transaction.data.threeds.threeds2_method_data = None
    transaction = await storage.transaction.save(transaction)
    with pytest.raises(ThreeDSMethodDataNotFoundError):
        await ThreeDSMethodAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id
        ).run()


@pytest.mark.asyncio
async def test_unknown_html_document_id(storage: Storage, mocker, transaction):
    with pytest.raises(ThreeDSMethodDocumentNotSupportedError):
        await ThreeDSMethodAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id,
            html_document_id='i-am-a-dummy',
        ).run()
