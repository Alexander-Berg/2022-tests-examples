import typing as tp

from billing.hot.tests.clients import base
from billing.hot.tests.clients.processor.client import Client as ProcessorClient
from billing.hot.tests.config import config
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.templates import processor as renderer


class Client(ProcessorClient):
    def __init__(self, base_client: base.BaseClient, cfg: config.ProcessorConfig):
        super(Client, self).__init__(base_client, cfg)
        self.renderer = renderer.ProcessorActotronActRowsRenderer(self.loader)

    def acts(
        self,
        st: state.PipelineState,
        contract_type: type,
        body: tp.Union[str, dict[str, tp.Any]] = 'acts.json',
        namespace: str = 'bnpl',
        act_sum_positive: float = 0,
        act_sum_negative: float = 0,
        act_sum_wo_vat_positive: float = 0,
        act_sum_wo_vat_negative: float = 0,
    ) -> tp.AsyncContextManager:
        request = self.renderer.render_acts_request(
            st=st,
            contract_type=contract_type,
            request=body,
            namespace=namespace,
            act_sum_positive=act_sum_positive,
            act_sum_negative=act_sum_negative,
            act_sum_wo_vat_positive=act_sum_wo_vat_positive,
            act_sum_wo_vat_negative=act_sum_wo_vat_negative,
        )

        return self.process(st, request)
