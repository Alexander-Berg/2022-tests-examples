import re
from unittest import mock

import pytest
import yarl
from aiohttp import hdrs
from aioresponses import CallbackResult
from aioresponses import aioresponses as base_aioresponses

from billing.yandex_pay_plus.yandex_pay_plus.interactions.base import create_connector


class aioresponses(base_aioresponses):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._mocks = {}

    def add(self, url, method, *args, **kwargs):
        super().add(url, method, *args, **kwargs)
        matcher = list(self._matches.values())[-1]
        mock_ = self._mocks[matcher] = mock.Mock()
        return mock_

    def head(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_HEAD, **kwargs)

    def get(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_GET, **kwargs)

    def post(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_POST, **kwargs)

    def put(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PUT, **kwargs)

    def patch(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PATCH, **kwargs)

    def delete(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_DELETE, **kwargs)

    def options(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_OPTIONS, **kwargs)

    async def match(self, method, url, **kwargs):
        response = await super().match(method, url, **kwargs)
        # DETECT_ZORA_BEGIN
        # Моки aioresponses_mocker(settings.ZORA_URL) трудно понимать.
        # Потому что если тест сложный (напр. функциональный), моки зоры мешают друг другу
        # Хочется aioresponses_mocker.post("http://my-url.test"), а не aioresponses_mocker.post(ZORA_URL)
        # Ну так давайте так и мокать. И воспользуемся тем, что настоящий url передаётся в x-ya-dest-url.
        if response is None:
            headers = kwargs.get('headers')
            if headers:
                for key in headers:
                    if key.lower() == 'x-ya-dest-url':
                        url = yarl.URL(headers[key])
                        break
            response = await super().match(method, url, **kwargs)
        # DETECT_ZORA_END
        if response is None:
            print('Failed to match:', method, url)  # noqa: T001
        else:
            for matcher, mock_ in self._mocks.items():
                if matcher.match(method, url):
                    mock_(**kwargs)
                    break
        return response


@pytest.fixture(autouse=True)
def aioresponses_mocker():
    with aioresponses(passthrough=['http://127.0.0.1:']) as m:
        yield m


@pytest.fixture(autouse=True)
def aioresponses_zora_mocker(aioresponses_mocker):
    return aioresponses_mocker


@pytest.fixture(autouse=True)
def setup_tvm(yandex_pay_plus_settings, rands, aioresponses_mocker):
    # Since we allow passthrough for localhost, setting up custom host for tvm
    from billing.yandex_pay_plus.yandex_pay_plus.interactions.base import TVM_CONFIG
    TVM_CONFIG['host'] = 'tvm'
    TVM_CONFIG['port'] = 80
    yandex_pay_plus_settings.TVM_URL = 'http://tvm:80'

    def tvm_callback(url, **kwargs):
        dst = kwargs['params']['dsts']
        return CallbackResult(
            status=200,
            payload={
                yandex_pay_plus_settings.TVM_CLIENT: {
                    'tvm_id': dst,
                    'ticket': f'service-ticket-f{rands()}',
                },
            },
        )

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_plus_settings.TVM_URL}/tvm/tickets.*$'),
        callback=tvm_callback,
        repeat=True,
    )


@pytest.fixture
def create_client(dummy_logger, request_id):
    TVM_NOT_SET = object()

    def _inner(client_cls, tvm_id=TVM_NOT_SET):
        if not hasattr(client_cls, 'CONNECTOR'):
            client_cls.CONNECTOR = create_connector()

        params = {
            'logger': dummy_logger,
            'request_id': request_id,
        }
        if tvm_id is not TVM_NOT_SET:
            params['tvm_id'] = tvm_id
        return client_cls(**params)

    return _inner
