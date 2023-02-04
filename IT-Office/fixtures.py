import pytest
from source.monitoring.monitoring import SLACollector
from source.utils import (OtherUtils,
                   StartrekUtils)

class FakeCallFunction():
    def __init__(self, return_dict):
        self.return_dict = return_dict

    def __call__(self, *args, **kwargs):
        return self.return_dict

class FakeResponse():
    def __init__(self, return_dict):
        self.return_dict = return_dict

    def __call__(self, *args, **kwargs):
        return self

    def json(self):
        return self.return_dict

class MockIssues():
    def __init__(self):
        self.fake_issues = []
        self.created_dict = {}

    def add_fake_issues(self, **kwargs):
        self.fake_issues = kwargs.get('issues_list', [])

    def create(self, **kwargs):
        self.created_dict.update(kwargs)
        return self

    @property
    def transitions(*args, **kwargs):
        return FakeFabric(treated=FakeFabric())

    def find(self, queue=None, **kwargs):
        return self.fake_issues

class TrancieversFinderReponse(FakeResponse):

    def __call__(self, *args, **kwargs):
        if args[0] == 'fqdn':
            return self.return_dict['fqdn']
        if args[0] == 'sn':
            return self.return_dict['sn']

class FakeFabric():
    """Class for representing any complex Startrek-library likes object"""
    def __init__(self, **kwargs):
        self._fake_dict = kwargs
        self.created_dict = {}

    def __getattr__(self, name):
        try:
            return self._fake_dict[name]
        except KeyError:
            msg = "'{0}' object has no attribute '{1}'"
            raise AttributeError(msg.format(type(self).__name__, name))

    def __getitem__(self, item):
        try:
            return self._fake_dict[item]
        except KeyError:
            msg = "'{0}' object has no key '{1}'"
            raise AttributeError(msg.format(type(self).__name__, item))

    def execute(self, **kwargs):
        if self._fake_dict.get('transition'):
            self.created_dict[self._fake_dict.get('transition')] = True

class FakeFabricIterator(FakeFabric):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.iteration = kwargs.get('comment_list') \
                         or kwargs.get('transition_list') \
                         or kwargs.get('abc_services') \
                         or kwargs.get("fix_versions")

    def create(self, **kwargs):
        self.created_dict = kwargs

    def __iter__(self):
        return iter(self.iteration)

    def __getitem__(self, item):
        return self.iteration[item]

@pytest.fixture()
def mock_get_st_macros(monkeypatch):

    def mockreturn(self, id, queue="HDRFS"):
        return {"Result":{}}

    monkeypatch.setattr(StartrekUtils, 'get_st_macros_actions', mockreturn)

@pytest.fixture()
def mock_noc_export(monkeypatch):
    def mockreturn(self):
        return {
            "admiral-u1.yndx.net": {
                "inventory": [
                    {
                        "description": "Switch 1 - Power Supply A",
                        "hwtype": "card",
                        "sn": "DCB2208B4GT"
                    },
                    {
                        "description": "c36xx Stack",
                        "hwtype": "card",
                        "sn": "FDO2211E16R"
                    },
                    {
                        "description": "WS-C3650-48PS-L",
                        "hwtype": "card",
                        "sn": "FDO2211E16R"
                    },
                    {
                        "description": "1000BaseBX-10D SFP",
                        "hwtype": "trans",
                        "sn": "W18083104235"
                    }
                ],
                "object_id": "9998",
                "sn": "210235351610CC000006",
                "timestamp": 1552271587
            },
            "avex-101a1.yndx.net": {
                "inventory": [
                    {
                        "description": "WS-C2960X-24PD-L",
                        "hwtype": "card",
                        "sn": "FCW2032A155"
                    },
                    {
                        "description": "SFP-10GBase-LR",
                        "hwtype": "trans",
                        "sn": "SB3A690031"
                    },
                    {
                        "description": "SFP-10GBase-LR",
                        "hwtype": "trans",
                        "sn": "PX40F7Z"
                    }
                ],
                "object_id": "9998",
                "sn": "210235351610CC000006",
                "timestamp": 1552357998
            }
        }

    monkeypatch.setattr(OtherUtils, 'fetch_trancievers_info', mockreturn)


@pytest.fixture()
def mock_sla_fetcher(monkeypatch):
    def mockreturn(self, issue):
        dict_to_return = {
            "HDRFS-1": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
            "HDRFS-2": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
            "HDRFS-3": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
            "HDRFS-4": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
            "HDRFS-5": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
            "HDRFS-6": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
            "HDRFS-7": {
                'reaction': {'spent': 100, 'status': 'ok'},
                'solve': {'spent': 300, 'status': 'ok'}},
        }
        return dict_to_return.get(issue.key)

    monkeypatch.setattr(SLACollector, '_get_sla_time_by_issue', mockreturn)