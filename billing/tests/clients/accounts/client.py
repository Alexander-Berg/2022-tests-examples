import datetime
import time
import typing as tp
from urllib import parse

from billing.hot.tests.clients import base
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.state.state import PipelineState
from billing.hot.tests.lib.templates.accounts import AccountsReadBatchRenderer, AccountsWriteBatchRenderer


class Client:
    def __init__(
        self,
        base_client: base.BaseClient,
        cfg: config.AccountsConfig,
        readbatch_renderer: AccountsReadBatchRenderer,
        writebatch_renderer: AccountsWriteBatchRenderer

    ) -> None:
        self.cfg = cfg
        self.base_client = base_client
        self.tvm_id = cfg.tvm_id
        self.readbatch_renderer = readbatch_renderer
        self.writebatch_renderer = writebatch_renderer

    def post(self, url, st, request: dict) -> tp.AsyncContextManager:
        return self.base_client.post(
            url,
            st,
            dst_tvm_id=self.tvm_id,
            body=request,
        )

    def get(self, url: str, st: state.PipelineState, params: dict) -> tp.AsyncContextManager:
        return self.base_client.get(url, st, params=params)

    def read_batch(self, st, request: dict) -> tp.AsyncContextManager:
        return self.post(
            parse.urljoin(self.cfg.url, self.cfg.handlers.read_batch),
            st,
            request,
        )

    def write_batch(self, st: state.PipelineState, request: dict) -> tp.AsyncContextManager:
        return self.post(
            parse.urljoin(self.cfg.url, self.cfg.handlers.write_batch),
            st,
            request,
        )

    def read_balances(self, st, ts: int, accounts: list, namespace: str) -> tp.AsyncContextManager:
        return self.post(
            parse.urljoin(self.cfg.url, self.cfg.handlers.read_batch),
            st,
            request=self.readbatch_renderer.render_read_balances_request(st, ts, accounts, namespace)
        )

    def get_account_detailed_turnover(self, st: PipelineState, client_id: int,
                                      dt_from: datetime.datetime, dt_to: datetime.datetime,
                                      namespace: str = 'taxi', type_: str = 'payout',
                                      add_params: tp.Optional[dict[str, tp.Any]] = None,
                                      ) -> tp.AsyncContextManager:
        url = parse.urljoin(self.cfg.url, self.cfg.handlers.turnover_detailed)

        add_params = add_params or {}
        if "service_id" in add_params and not add_params["service_id"]:
            add_params.pop("service_id")
            add_params["service_id__empty"] = "1"

        if "contract_id" in add_params and not add_params["contract_id"]:
            add_params.pop("contract_id")
            add_params["contract_id__empty"] = "1"

        if "currency" in add_params and not add_params["currency"]:
            add_params.pop("currency")
            add_params["currency__empty"] = "1"

        if "product" in add_params and not add_params["product"]:
            add_params.pop("product")
            add_params["product__empty"] = "1"

        add_params = {key: str(value) for key, value in add_params.items()}

        params = {
            "client_id": str(client_id),
            "dt_from": str(round(time.mktime(dt_from.timetuple()))),
            "dt_to": str(round(time.mktime(dt_to.timetuple()))),
            "namespace": namespace,
            "type": type_,
            **add_params,
        }

        return self.get(
            url, st, params
        )

    def get_exported_events(
        self,
        st: PipelineState,
        *,
        external_ids: list[str],
        namespace: str,
        account: str,
        client_id: int,
        contract_id: int,
        **kwargs,
    ):
        url = parse.urljoin(self.cfg.url, self.cfg.handlers.exported_events)
        params = {
            'external_id': external_ids,
            'namespace': namespace,
            'type': account,
            'client_id': client_id,
            'contract_id': contract_id,
        }

        for k, v in kwargs.items():
            if v is None:
                params[f'{k}__empty'] = "1"
            else:
                params[k] = v

        return self.get(url, st, params)
