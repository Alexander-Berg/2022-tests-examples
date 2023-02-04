import mock
import pytest

from balance.scheme import dcsaap_diffs
from balance.actions.dcs.compare.common.diffs import download_ext_diffs_from_yt, run_change_diffs


@pytest.fixture
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as m:
        yield m.return_value


def test_download_ext_diffs_from_yt(yt_client_mock, session):
    yt_diffs = [
        {
            'key': 'key_value',
            't1_value': 'left',
            't2_value': 'right',
            'column_name': 'column',
            'diff_type': 3,
            'issue_key': '123',
            'status': 1,
        },
        {
            'key': 'key_value',
            't1_value': 'one',
            't2_value': 'two',
            'column_name': 'column1',
            'diff_type': 3,
        },

    ]
    yt_client_mock.read_table.return_value = yt_diffs

    run_id = 1
    keys = 'key'
    cluster = 'hahn'
    result = '//home/table'

    download_ext_diffs_from_yt(run_id, keys, cluster, result, session=session)

    rows = session.execute(dcsaap_diffs.select()).fetchall()
    assert len(rows) == 2
    assert rows[0].dt
    assert rows[1].dt

    expected_rows = [
        {
            'run_id': run_id,
            'key1_name': 'key',
            'key1_value': 'key_value',
            'column_name': 'column',
            'column_value1': 'left',
            'column_value2': 'right',
            'type': 3,
            'issue_key': '123',
            'status': 1,
        }, {
            'run_id': run_id,
            'key1_name': 'key',
            'key1_value': 'key_value',
            'column_name': 'column1',
            'column_value1': 'one',
            'column_value2': 'two',
            'type': 3,
            'issue_key': None,
            'status': None,
        }
    ]

    rows = [
        {
            'run_id': row.run_id,
            'key1_name': row.key1_name,
            'key1_value': row.key1_value,
            'column_name': row.column_name,
            'column_value1': row.column_value1,
            'column_value2': row.column_value2,
            'type': row.type,
            'issue_key': row.issue_key,
            'status': row.status,
        } for row in rows
    ]
    assert rows == expected_rows


def test_run_change_diffs(session):
    diffs = [
        {'run_id': 1,
         'key1_name': 'key',
         'key1_value': 'key_value1',
         'column_name': 'column',
         'column_value1': '1',
         'column_value2': '2',
         'type': 3},

        {'run_id': 1,
         'key1_name': 'key',
         'key1_value': 'key_value1',
         'column_name': 'column2',
         'column_value1': '3',
         'column_value2': '4',
         'type': 3},

        {'run_id': 1,
         'key1_name': 'key',
         'key1_value': 'key_value2',
         'column_name': 'column',
         'column_value1': '1',
         'column_value2': '2',
         'type': 3},

        {'run_id': 2,
         'key1_name': 'key',
         'key1_value': 'key_value2',
         'column_name': 'column',
         'column_value1': '3',
         'column_value2': '4',
         'type': 3},
    ]

    session.execute(dcsaap_diffs.insert(), diffs)
    changes = [
        {
            'column_name': 'column',
            'keys': ["key_value1"],
            'changes': {
                'issue_key': 'razdvatri',
                'status': 1,
            }
        },
        {
            'column_name': 'column2',
            'keys': ["key_value1"],
            'changes': {
                'issue_key': 'razdvatri',
                'status': 1,
            }
        },
    ]

    run_change_diffs(1, changes, session)

    rows = session.execute(dcsaap_diffs.select().where(dcsaap_diffs.c.issue_key != None)).fetchall()

    assert len(rows) == 2
    assert all(map(
        lambda row: row.issue_key == 'razdvatri' and row.status == 1 and row.run_id == 1,
        rows,
    ))
