from datetime import datetime
import typing as tp
from urllib import parse

from billing.hot.tests.clients import base
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.polling import poll


class Client:
    def __init__(self, base_client: base.BaseClient, cfg: config.PayoutConfig) -> None:
        self.payout_by_client_url = parse.urljoin(cfg.url, cfg.handlers.payout_by_client)
        self.payout_info_url = parse.urljoin(cfg.url, cfg.handlers.payout_info)
        self.base_client = base_client
        self.tvm_id = cfg.tvm_id
        self.current_state: tp.Optional[state.PipelineState] = None

    def payout_by_client(self, st: state.PipelineState, namespace: str = 'taxi') -> tp.AsyncContextManager:
        return self.base_client.post(
            self.payout_by_client_url,
            st,
            dst_tvm_id=self.tvm_id,
            body={"client_id": st.client_id,
                  "external_id": st.external_id,
                  "namespace": namespace,
                  }
        )

    def payout_info(self, st: state.PipelineState, from_date: datetime, statuses: list[str]) -> tp.AsyncContextManager:
        params = [("client_id", str(st.client_id)), ("from", from_date.strftime("%Y-%m-%d"))]
        params.extend(("statuses", str(status)) for status in statuses)
        return self.base_client.get(
            self.payout_info_url,
            st,
            dst_tvm_id=self.tvm_id,
            params=params,
        )

    async def poll_payout_info(
        self, st: state.PipelineState, from_date: datetime, statuses: list[str], expected_records: int,
        interval_seconds: float = 5, timeout_seconds: float = 60,
    ) -> list:
        async def poll_body():
            async with self.payout_info(st, from_date, statuses) as resp:
                resp_json = await resp.json()
                cnt = len(resp_json.get("data", []))
                if cnt == expected_records:
                    return resp_json["data"]
                elif cnt > expected_records:
                    raise ValueError(f"There are more records than expected: got {cnt}, expected {expected_records}")
            raise poll.RetryError

        return await poll.poll(poll_body, interval_seconds=interval_seconds, timeout_seconds=timeout_seconds)
