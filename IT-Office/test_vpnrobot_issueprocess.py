from source.vpnrobot import process_issue, DataCollect, UserInfo, templ_withoutvpn, templ_withvpn, templ_resolved
import time
import pytest


class FakeFabric:
    def __init__(self, **kwargs):
        self._fake_dict = kwargs
        self.created_dict = {}

    def __getattr__(self, name):
        try:
            return self._fake_dict[name]
        except KeyError:
            msg = "'{0}' object has no attribute '{1}'"
            raise AttributeError(msg.format(type(self).__name__, name))

    def execute(self):
        if self._fake_dict.get('transition'):
            self.created_dict[self._fake_dict.get('transition')] = True


class FakeFabricIterator(FakeFabric):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.iteration = kwargs.get('comment_list') or kwargs.get('transition_list')

    def create(self, **kwargs):
        if self.created_dict:
            self.created_dict.append(kwargs)
        else:
            self.created_dict = [kwargs]

    def __iter__(self):
        return iter(self.iteration)

    def __getitem__(self, item):
        return self.iteration[item]


@pytest.fixture()
def mock_sleep(monkeypatch):
    def mockreturn(self):
        return

    monkeypatch.setattr(time, 'sleep', mockreturn)


@pytest.fixture()
def mock_get_staff(monkeypatch):
    def mockreturn(self, creator):
        return {'department_group': []}

    monkeypatch.setattr(UserInfo, 'get_staff', mockreturn)


@pytest.fixture()
def mock_get_chef(monkeypatch):
    def mockreturn(self, creator, staff):
        return 'fakechief'

    monkeypatch.setattr(DataCollect, 'get_chef', mockreturn)


@pytest.fixture()
def mock_get_groups(monkeypatch):
    def mockreturn(self, creator):
        if creator == 'artemav':
            return ['FakeGroup', 'anotherFakeGroupwithoutVPN']
        elif creator == 'fakecreator':
            return ['sameshit', 'Office.VPN.NG_6', 'someshit']

    monkeypatch.setattr(UserInfo, 'get_user_groups', mockreturn)


test_data = [
    (FakeFabric(key='HDRFS-1',
                createdBy=FakeFabric(id='artemav'),
                transitions=FakeFabricIterator(transition_list={'inProgress': FakeFabric(transition='inProgress'),
                                                                'resolved': FakeFabric(transition='resolved')}),
                comments=FakeFabricIterator(comment_list=[])),
     {
         'createdBy': 'artemav',
         'summonee': 'fakechief',
         'inProgress': True,
         'resolved': None,
         'comments': [templ_withoutvpn]
     }),
    (FakeFabric(key='HDRFS-11',
                createdBy=FakeFabric(id='fakecreator'),
                transitions=FakeFabricIterator(transition_list={'inProgress': FakeFabric(transition='inProgress'),
                                                                'resolved': FakeFabric(transition='resolved')}),
                comments=FakeFabricIterator(comment_list=[])),
     {
         'createdBy': 'fakecreator',
         'summonee': None,
         'inProgress': True,
         'resolved': True,
         'comments': [templ_withvpn, templ_resolved]
     })

]


@pytest.mark.parametrize('input_data, ex_result', test_data)
def test_test(mock_get_staff, mock_get_chef, mock_get_groups, mock_sleep, input_data, ex_result):
    issue = input_data
    process_issue(issue)
    if len(issue.comments.created_dict) > 1:
        assert issue.comments.created_dict[0]['text'] == ex_result['comments'][0]
        assert issue.comments.created_dict[1]['text'] == ex_result['comments'][1]
    else:
        assert issue.comments.created_dict[0]['text'] == ex_result['comments'][0]
    if ex_result['summonee']:
        assert issue.comments.created_dict[0]['summonee'] == ex_result['summonee']
    if ex_result['inProgress']:
        assert issue.transitions.transition_list['inProgress'].created_dict['inProgress'] == ex_result['inProgress']
    if ex_result['inProgress'] and ex_result['resolved']:
        assert issue.transitions.transition_list['inProgress'].created_dict['inProgress'] == ex_result['inProgress']
        assert issue.transitions.transition_list['resolved'].created_dict['resolved'] == ex_result['resolved']