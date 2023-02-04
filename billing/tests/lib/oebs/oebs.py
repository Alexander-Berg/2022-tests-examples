import json
import datetime

from billing.hot.tests.clients.logbroker import logbroker
from billing.hot.tests.clients.payout import client as pc
from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.templates import loader, oebs


class OEBS:
    def __init__(self, lb: logbroker.LogBrokerAPI, renderer: oebs.OEBSRenderer) -> None:
        self.lb = lb
        self.renderer = renderer
        self.messages_cache = []
        self.messages_cache_dry_run = []

    def read_payouts(self, st: state.PipelineState, from_dt_ms: int = 0, dry_run: bool = False) -> list:
        self._read_to_cache(dry_run)

        result = []
        messages = self.messages_cache
        if dry_run:
            messages = self.messages_cache_dry_run
        for message in messages:
            if message.meta.write_time_ms < from_dt_ms:
                continue
            data_json = json.loads(message.data)
            if data_json['billing_contract_id'] not in map(lambda v: v.id, st.contracts.values()):
                continue
            result.append(data_json)
        return result

    def write_ard(self, response: loader.RenderedTemplateOrTemplatePath = 'basic.json', dry_run: bool = False) -> int:
        producer = self.lb.producers['ard']
        if dry_run:
            producer = self.lb.producers['ard-dry']
        response = self.renderer.render_ard_response(response)
        producer.write(response)
        return response['payment_batch_id']

    def write_oebs(self, response: loader.RenderedTemplate, dry_run: bool = False) -> int:
        producer = self.lb.producers['oebs']
        if dry_run:
            producer = self.lb.producers['oebs-dry']
        response = self.renderer.enrich_oebs_response(response)
        producer.write(response)
        return response['payment_id']

    def _read_to_cache(self, dry_run: bool = False) -> None:
        consumer = self.lb.consumers['new-payout']
        if dry_run:
            consumer = self.lb.consumers['new-payout-dry']
        while True:
            messages, found_any = consumer.read()
            if not found_any:
                break
            if dry_run:
                self.messages_cache_dry_run.extend(messages)
            else:
                self.messages_cache.extend(messages)


def request_with_status(response: loader.RenderedTemplate, status: str) -> loader.RenderedTemplate:
    result = response.copy()
    result['status_type'] = status
    return result


class OEBSPipeline:
    def __init__(self, o: OEBS) -> None:
        self.oebs = o
        self.loader = o.renderer.loader
        self.message_id_to_payment_batch_id = {}

    def read_payouts(self, st: state.PipelineState, from_dt_ms: int = 0, dry_run: bool = False) -> list:
        return self.oebs.read_payouts(st, from_dt_ms, dry_run)

    def write_ard_answer(self, ard_responses: dict[str, list[loader.RenderedTemplateOrTemplatePath]],
                         dry_run: bool = False) -> None:
        """Accepts ard_requests in response to messages from read_payouts. Keys are message_ids"""

        for message_id, ard_response_batch in ard_responses.items():
            assert ard_response_batch, "requests must be non-empty"

            payment_batch_id = None
            for ard_response in ard_response_batch:
                if isinstance(ard_response, str):
                    ard_response = self.loader.load_ard_response(ard_response)
                ard_response['message_id'] = message_id
                if payment_batch_id is not None:
                    ard_response['payment_batch_id'] = payment_batch_id
                payment_batch_id = self.oebs.write_ard(ard_response, dry_run)

            assert payment_batch_id is not None
            self.message_id_to_payment_batch_id[message_id] = payment_batch_id

    def write_oebs_answer(
        self, oebs_responses: dict[tuple[str, ...], list[loader.RenderedTemplate]], dry_run: bool = False
    ) -> dict[tuple[str, ...], int]:
        msg_ids_to_payment_id = {}
        """Accepts oebs_requests in response to messages from read_payouts. Keys are tuples of message_ids"""
        for message_ids, oebs_response_batch in oebs_responses.items():
            assert oebs_response_batch, "requests must be non-empty"

            payment_id = None
            for oebs_response in oebs_response_batch:
                oebs_response['payment_batch_id'] = [
                    self.message_id_to_payment_batch_id[msg_id] for msg_id in message_ids
                ]
                if payment_id is not None:
                    oebs_response['payment_id'] = payment_id
                payment_id = self.oebs.write_oebs(oebs_response, dry_run)

            assert payment_id is not None
            msg_ids_to_payment_id[message_ids] = payment_id
        return msg_ids_to_payment_id

    async def run_ok_pipeline(
        self, st: state.PipelineState, payout_client: pc.Client = None, expected_payouts_count: int = None,
        from_dt_ms: int = 0, dry_run: bool = False,
    ) -> (int, dict[str, int]):
        messages = self.read_payouts(st, from_dt_ms, dry_run)

        if expected_payouts_count is not None:
            assert len(messages) == expected_payouts_count, \
                f'got {len(messages)} messages expected {expected_payouts_count}'

        amount = sum(message['amount'] for message in messages)

        # BILLING-702
        for message in messages:
            assert message['billing_client_id'] == st.client_id

        ard_responses = {message['message_id']: ['basic.json'] for message in messages}
        self.write_ard_answer(ard_responses, dry_run)

        if payout_client is not None:
            now = datetime.datetime.now()
            await payout_client.poll_payout_info(
                st, from_date=now - datetime.timedelta(days=1), statuses=['confirmed'],
                expected_records=expected_payouts_count,
            )
        oebs_requests = {
            tuple(ard_responses): [self.oebs.renderer.render_oebs_response(amount, response_path='created.json')]}
        payment_id_dict = self.write_oebs_answer(oebs_requests, dry_run)
        assert len(payment_id_dict) == 1

        oebs_requests = {tuple(ard_responses): [self.oebs.renderer.render_oebs_response(amount)]}
        payment_id_dict = self.write_oebs_answer(oebs_requests)
        assert len(payment_id_dict) == 1

        return list(payment_id_dict.values())[0], self.message_id_to_payment_batch_id
