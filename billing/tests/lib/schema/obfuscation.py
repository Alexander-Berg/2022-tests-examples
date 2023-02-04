import json
import typing as tp

import copy


class Obfuscator:
    def __init__(
        self,
        obfuscate_term: str,
        processor_result_paths: tp.Sequence[str] = None,
        accounts_balance_paths: tp.Sequence[str] = None,
        accounts_event_paths: tp.Sequence[str] = None,
        act_rows_paths: tp.Sequence[str] = None,
        acted_events_paths: tp.Sequence[str] = None,
    ):
        self.obfuscate_term = obfuscate_term
        self.processor_result_paths = processor_result_paths or []
        self.accounts_balance_paths = accounts_balance_paths or []
        self.accounts_event_paths = accounts_event_paths or []
        self.act_rows_paths = act_rows_paths or []
        self.acted_events_paths = acted_events_paths or []

    def obfuscate_processor_response(
        self,
        processor_response: dict[str, tp.Any],
    ) -> dict[str, tp.Any]:
        processor_response_copy = copy.deepcopy(processor_response)

        result = processor_response_copy['data']['result']
        self._obfuscate(result, self.processor_result_paths, 'processor result')

        return processor_response_copy

    def obfuscate_accounts_balances(
        self,
        accounts_balances: dict[str, tp.Any],
    ) -> dict[str, tp.Any]:
        accounts_balances_copy = copy.deepcopy(accounts_balances)

        for balance in accounts_balances_copy['data']['balances']:
            self._obfuscate(balance, self.accounts_balance_paths, 'accounts balance')

        return accounts_balances_copy

    def obfuscate_accounts_events(
        self,
        accounts_events: list[dict[str, tp.Any]]
    ) -> list[dict[str, tp.Any]]:
        accounts_events_copy = copy.deepcopy(accounts_events)

        for account_event in accounts_events_copy:
            self._obfuscate(account_event, self.accounts_event_paths, 'accounts event')

        return accounts_events_copy

    def obfuscate_agent_acts(
        self,
        agent_acts: list[dict[str, tp.Any]],
    ) -> list[dict[str, tp.Any]]:
        agent_acts_copy = copy.deepcopy(agent_acts)

        for agent_act in agent_acts_copy:
            loaded_transaction = json.loads(agent_act['transaction'])
            self._obfuscate(loaded_transaction, self.accounts_event_paths, 'accounts event')
            agent_act['transaction'] = json.dumps(loaded_transaction)

        return agent_acts_copy

    def obfuscate_act_rows(self, act_rows: list[dict[str, tp.Any]]) -> list[dict[str, tp.Any]]:
        act_rows_copy = copy.deepcopy(act_rows)

        for act_row in act_rows_copy:
            self._obfuscate(act_row, self.act_rows_paths, 'act row')

        return act_rows_copy

    def obfuscate_acted_events(self, acted_events: list[dict[str, tp.Any]]) -> list[dict[str, tp.Any]]:
        acted_events_copy = copy.deepcopy(acted_events)

        for acted_event in acted_events_copy:
            loaded_event_data = json.loads(acted_event['original_event_data'])
            self._obfuscate(loaded_event_data, self.accounts_event_paths, 'accounts event')
            acted_event['original_event_data'] = json.dumps(loaded_event_data)

            self._obfuscate(acted_event, self.acted_events_paths, 'acted event')

        return acted_events_copy

    def _obfuscate(self, data: tp.Any, paths: list[str], name: str) -> None:
        try:
            self._mask_by_paths(data, paths)
        except ValueError as cause_exc:
            raise ValueError(f'cannot obfuscate {name}') from cause_exc

    def mask(self, *args: tp.Any) -> str:
        return f'<{"".join([self.obfuscate_term, *[str(a) for a in args]])}>'

    def _mask_by_paths(self, data: tp.Any, paths: tp.Sequence[str]):
        for path in paths:
            try:
                self._mask_by_path(data, path.split('.'))
            except (ValueError, KeyError) as cause_exc:
                raise ValueError(f'cannot mask data by path {path}') from cause_exc

    def _mask_by_path(self, data: tp.Any, path: list[str]):
        if data is None:
            raise ValueError('cannot mask none data')

        key = path[0]
        keys = [path[0]]

        if isinstance(data, list) or isinstance(data, tuple):
            if key == '*':
                keys = range(len(data))
            else:
                if not key.isdigit():
                    raise ValueError(f'cannot index array {data} with not numeric key {key}')

                keys = [int(key)]

        for key in keys:
            if len(path) == 1:
                data[key] = self.mask()
            else:
                self._mask_by_path(data[key], path[1:])
