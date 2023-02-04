# coding: utf-8

import datetime
from decimal import Decimal as D
import mock
import pytest

import balance.constants as cst
from cluster_tools.yt_import_overdraft_limits import import_overdraft_limits
from tests import object_builder as ob
from balance import mapper

FIRM_ID = 1
SERVICE_ID_1 = 1
SERVICE_ID_2 = 2
DEFAULT_SERVICE_ID = cst.ServiceId.DIRECT


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(autouse=True)
def yt_client_mock(client):
    mock_path = 'yt.wrapper.YtClient'

    def mocked_yt_client_list(dir_path, *args, **kwargs):
        if dir_path == '//dir1':
            return ['2019-01-01', '2020-02-02']
        if dir_path == '//dir2':
            return ['2020-01-02']
        if dir_path == '//dir_exception':
            return ['table_exception']
        if dir_path == '//exception':
            raise Exception('error in list dir')
        return []

    def mocked_read_table(path, *args, **kwargs):
        path = str(path)
        if 'dir1' in path:
            return iter(
                [
                    {'client_id': client.id, 'service_id': SERVICE_ID_1, 'work_currency': 'RUB', 'overdraft_lim': 100},
                    {'client_id': client.id, 'work_currency': 'USD', 'overdraft_lim': 123.4567, 'valid_until': '2020-04-12', 'merge_policy': cst.ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE},
                    {'client_id': client.id, 'service_id': SERVICE_ID_2, 'work_currency': 'USD', 'overdraft_lim': 123.4567, 'valid_until': None, 'merge_policy': cst.ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE}
                ]
            )
        if 'dir2' in path:
            return iter(
                [
                    {'client_id': client.id, 'work_currency': 'RUB', 'overdraft_lim': 200},
                ]
            )
        if 'dir_exception' in path:
            raise Exception('error in table read')
        return iter([])

    def mocked_get(path):
        if '2020-02-02' in path:
            return "{ 'billing_import_start': 'qwer', 'billing_import_failed': 'qwer', '...': '...' }"
        return "{ 'billing_import_start': 'qwer', '...': '...' }"

    def mocked_set(path, value):
        pass

    def mocked_remove(path):
        pass

    with mock.patch(mock_path) as m:
        m.return_value.list.side_effect = mocked_yt_client_list
        m.return_value.read_table.side_effect = mocked_read_table
        m.return_value.get.side_effect = mocked_get
        m.return_value.set.side_effect = mocked_set
        m.return_value.remove.side_effect = mocked_remove
        yield m


@pytest.fixture(autouse=True)
def yt_helpers_mock():
    mock_path = 'balance.utils.yt_helpers.get_token'
    with mock.patch(mock_path) as m:
        yield m


def test_import_into_table(session, client):
    session.add(mapper.ExternalOverdraftImports(
        dir_path='//dir1',
        last_imported='2019-01-01',
        default_service_id=DEFAULT_SERVICE_ID,
    ))
    session.flush()

    import_overdraft_limits(session)

    session.expire_all()

    external_overdraft_1 = client.external_overdraft.get(SERVICE_ID_1)
    assert external_overdraft_1.client_id == client.id
    assert external_overdraft_1.service_id == SERVICE_ID_1
    assert external_overdraft_1.iso_currency == 'RUB'
    assert external_overdraft_1.overdraft_limit == 100
    assert external_overdraft_1.valid_until is None
    assert external_overdraft_1.merge_policy is None

    external_overdraft_2 = client.external_overdraft.get(DEFAULT_SERVICE_ID)
    assert external_overdraft_2.client_id == client.id
    assert external_overdraft_2.service_id == DEFAULT_SERVICE_ID
    assert external_overdraft_2.iso_currency == 'USD'
    assert external_overdraft_2.overdraft_limit == D('123.45')
    assert external_overdraft_2.valid_until == datetime.date(2020, 4, 12)
    assert external_overdraft_2.merge_policy == cst.ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE

    external_overdraft_3 = client.external_overdraft.get(SERVICE_ID_2)
    assert external_overdraft_3.client_id == client.id
    assert external_overdraft_3.service_id == SERVICE_ID_2
    assert external_overdraft_3.iso_currency == 'USD'
    assert external_overdraft_3.overdraft_limit == D('123.45')
    assert external_overdraft_3.valid_until is None
    assert external_overdraft_3.merge_policy == cst.ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE

    import_dir = session.query(
        mapper.ExternalOverdraftImports
    ).filter(
        mapper.ExternalOverdraftImports.dir_path == "//dir1"
    ).first()
    assert import_dir.last_imported == '2020-02-02'

    assert client.exports['OVERDRAFT'].state == cst.ExportState.enqueued


def test_old_tables(session, client):
    session.add(mapper.ExternalOverdraftImports(
        dir_path='//dir1',
        last_imported='2020-03-03',
        default_service_id=DEFAULT_SERVICE_ID,
    ))
    session.flush()

    import_overdraft_limits(session)

    session.expire_all()

    external_overdraft_1 = client.external_overdraft.get(SERVICE_ID_1)
    assert external_overdraft_1 is None

    external_overdraft_2 = client.external_overdraft.get(DEFAULT_SERVICE_ID)
    assert external_overdraft_2 is None

    import_dir = session.query(
        mapper.ExternalOverdraftImports
    ).filter(
        mapper.ExternalOverdraftImports.dir_path == "//dir1"
    ).first()
    assert import_dir.last_imported == '2020-03-03'


def test_multiple_paths(session, client, yt_client_mock):
    for dir_path, service_id in [
        ('//dir1', DEFAULT_SERVICE_ID),
        ('//dir_exception', DEFAULT_SERVICE_ID),
        ('//empty_dir', DEFAULT_SERVICE_ID),
        ('//dir2', SERVICE_ID_1),
    ]:
        session.add(mapper.ExternalOverdraftImports(
            dir_path=dir_path,
            last_imported='2019-01-01',
            default_service_id=service_id,
        ))
    session.flush()

    import_overdraft_limits(session)

    session.expire_all()

    external_overdraft_1 = client.external_overdraft.get(SERVICE_ID_1)
    assert external_overdraft_1.client_id == client.id
    assert external_overdraft_1.service_id == SERVICE_ID_1
    assert external_overdraft_1.iso_currency == 'RUB'
    assert external_overdraft_1.overdraft_limit == 200
    assert external_overdraft_1.valid_until is None
    assert external_overdraft_1.merge_policy is None

    external_overdraft_2 = client.external_overdraft.get(DEFAULT_SERVICE_ID)
    assert external_overdraft_2.client_id == client.id
    assert external_overdraft_2.service_id == DEFAULT_SERVICE_ID
    assert external_overdraft_2.iso_currency == 'USD'
    assert external_overdraft_2.overdraft_limit == D('123.45')
    assert external_overdraft_2.valid_until == datetime.date(2020, 4, 12)
    assert external_overdraft_2.merge_policy == cst.ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE

    import_dir = session.query(
        mapper.ExternalOverdraftImports
    ).filter(
        mapper.ExternalOverdraftImports.dir_path == "//dir1"
    ).first()
    assert import_dir.last_imported == '2020-02-02'

    import_dir = session.query(
        mapper.ExternalOverdraftImports
    ).filter(
        mapper.ExternalOverdraftImports.dir_path == "//dir2"
    ).first()
    assert import_dir.last_imported == '2020-01-02'

    yt_client = yt_client_mock.return_value
    yt_client.get.assert_any_call(u'//dir1/2020-02-02/@')
    yt_client.get.assert_any_call(u'//dir2/2020-01-02/@')
    assert yt_client.get.call_count == 2
    yt_client.remove.assert_any_call(u'//dir1/2020-02-02/@billing_import_failed')
    assert yt_client.remove.call_count == 1
    assert yt_client.set.call_count == 6
