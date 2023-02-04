from collections import defaultdict
import json


def sort_json(data):
    data_list = list(data)
    for d in data_list:
        d['data'] = json.loads(d['data'])
    return sorted(data_list, key=lambda r: r['data']['id'])


def sort_yson(data):
    data_list = list(data)
    return sorted(data_list, key=lambda r: (r['original_data']['id'], r['mark_data']['id']))


class Client1:
    common = {'client_id': '1', 'contract_id': '1', 'currency': 'RUB', 'namespace': 'ns'}
    payout = common | {'account_id': 10, 'account_type': 'payout'}
    cashless = common | {'account_id': 11, 'account_type': 'cashless'}
    commissions_with_vat = common | {'account_id': 12, 'account_type': 'commissions_with_vat'}
    compensations = common | {'account_id': 13, 'account_type': 'compensations'}
    agent_rewards = common | {'account_id': 14, 'account_type': 'agent_rewards'}
    agent_rewards_payable = common | {'account_id': 15, 'account_type': 'agent_rewards_payable'}
    payout_sent = common | {'account_id': 19, 'account_type': 'payout_sent'}


def dt(dt):
    return {'dt': dt}


def credit(amount):
    return {'type': 'credit', 'amount': str(amount)}


def debit(amount):
    return {'type': 'debit', 'amount': str(amount)}


def info(txt=None, **kw):
    if txt:
        txt = str(txt)
    return {'batch_info': json.dumps({'_comment': txt, **kw})}


def batch(id_=None, ext_id=None, type_=None, count=None):
    return \
        ({'batch_id': int(id_)} if id_ else {}) | \
        ({'batch_ext_id': str(ext_id)} if ext_id else {}) | \
        ({'batch_type': str(type_)} if type_ else {}) | \
        ({'batch_count': int(count)} if count else {}) | \
        {}


def tariffer_payload(dry_run=False, product_mdh_id=None, amount_wo_vat=None, service_id=111, firm_id=13, tax_policy_id=1):
    return {
        'tariffer_payload':
            ({'dry_run': bool(dry_run)} if dry_run is not None else {}) |
            ({'product_mdh_id': str(product_mdh_id)} if product_mdh_id is not None else {}) |
            ({'amount_wo_vat': str(amount_wo_vat)} if amount_wo_vat is not None else {}) |
            ({'service_id': str(service_id)} if service_id is not None else {}) |
            ({'firm_id': str(firm_id)} if firm_id is not None else {}) |
            ({'tax_policy_id': str(tax_policy_id)} if tax_policy_id is not None else {}) |
            {}
    }


class EventBatch:
    def __init__(self, common):
        self.common = common or {}
        self.events = []

    def __call__(self, *args) -> 'EventBatch':
        self.events = args
        return self

    def __getitem__(self, key):
        return self.common | self.events[key]

    def __len__(self):
        return len(self.events)


class DataCollector:
    schema = [
        {'name': 'data', 'type': 'string'},
        {'name': 'already_marked_amount', 'type': 'string'},
    ]

    def __init__(self, init_dt=1627465336):
        self.init_dt = init_dt
        self._groups = defaultdict(list)
        self._event_id = 0
        self._batch_id = 0

    def add(self, batch, group='step-01'):
        self._groups[group].append(batch)
        return self

    def groups(self):
        return sorted(self._groups.keys())

    def get_group_data(self, group):
        return self._groups[group]

    def make_table(self, yt_client, table_path, lbexport_formater, group='step-01'):
        yt_client.create('table', table_path, recursive=True, attributes={'schema': self.schema})
        ready_data = []
        for _batch in self.get_group_data(group):
            self._batch_id += 10
            common_data = {
                'dt': self.init_dt + self._batch_id,
            } | batch(id_=self._batch_id, ext_id=self._batch_id, count=len(_batch))
            for _event in _batch:
                self._event_id += 10
                ready_data.append({
                    'already_marked_amount': _event.pop('already_marked_amount', None),
                    'data': lbexport_formater({'id': self._event_id, 'seq_id': self._event_id} | common_data | _event),
                })
        yt_client.write_table(
            table_path,
            ready_data,
        )
        return table_path
