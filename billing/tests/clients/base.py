import json
import logging
import typing as tp
import allure
from asyncio import exceptions
import aiohttp
import tvm2
from aiohttp_retry import RetryClient, ExponentialRetry

from billing.hot.tests.lib.state import state

logger = logging.getLogger(__name__)


class BaseClient:
    def __init__(self, tvm_client: tp.Optional[tvm2.TVM2], timeout: int = 15) -> None:
        self.tvm_client = tvm_client

        timeout = aiohttp.ClientTimeout(total=timeout)
        retry_options = ExponentialRetry(attempts=3, max_timeout=5,
                                         exceptions={exceptions.TimeoutError})
        self.session = RetryClient(timeout=timeout, retry_options=retry_options)

    async def close(self) -> None:
        await self.session.close()

    def get(
        self, url: str, st: state.PipelineState, headers: dict[str, str] = None,
        params=None, dst_tvm_id: int = None,
    ):
        headers = headers or {}
        self._add_headers(headers, dst_tvm_id, st)
        with allure.step(f'GET-запрос {url}, request_id={headers["X-Request-ID"]}'):
            allure.attach(json.dumps(params), 'Параметры запроса', allure.constants.AttachmentType.JSON)
            return _RequestLogging(self.session.get(url, headers=headers, params=params), url, params=params)

    def post(
        self, url: str, st: state.PipelineState, body: dict[str, tp.Any],
        headers: dict[str, str] = None, dst_tvm_id: int = None
    ):
        headers = headers or {}
        self._add_headers(headers, dst_tvm_id, st)
        body = json.dumps(body)
        with allure.step(f'POST-запрос {url}, request_id={headers["X-Request-ID"]}'):
            allure.attach(body, 'Тело запроса', allure.constants.AttachmentType.JSON)
            return _RequestLogging(self.session.post(url, headers=headers, data=body), url, body=body)

    def _add_headers(self, headers: dict[str, str], tvm_id: int, st: state.PipelineState) -> None:
        headers['X-Request-ID'] = st.request_id
        self._add_tvm_ticket(headers, tvm_id)

    def _add_tvm_ticket(self, headers: dict[str, str], tvm_id: int) -> None:
        if tvm_id is None or self.tvm_client is None:
            return

        service_ticket = self.tvm_client.get_service_ticket(tvm_id)
        if service_ticket is None:
            raise LookupError(f'cannot get tvm ticket for tvm_id {tvm_id}')

        headers['X-Ya-Service-Ticket'] = service_ticket


class _RequestLogging:
    def __init__(self, response_coro, url: str, params: list = None, body: str = None) -> None:
        self.url = url
        self.body = body
        self.params = params
        self.response_coro = response_coro

    async def __aenter__(self):
        logger.info('request to %s: params: %s, body: %s', self.url, str(self.params), self.body)
        response = await self.response_coro.__aenter__()
        try:
            resp = json.dumps(await response.json())
        except Exception:
            try:
                resp = await response.text()
            except Exception as e:
                resp = str(e)
                raise
        finally:
            logger.info('response from %s: %s', self.url, resp)
        return response

    async def __aexit__(self, *exc) -> None:
        await self.response_coro.__aexit__(*exc)
