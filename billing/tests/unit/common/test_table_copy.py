# -*- coding: utf-8 -*-

from unittest import mock
import pytest

from billing.log_tariffication.py.jobs.common import table_copy


@pytest.fixture
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as yt_client:
        yield yt_client.return_value


def yt_client_defaults(yt_client_mock, new_table_dt):
    yt_client_mock.get.return_value = yt_client_mock
    yt_client_mock.attributes = {'dml_dt': new_table_dt}
    yt_client_mock.list.return_value = ['2000-01-01T00:00:00', '2010-01-01T00:00:00', '2020-01-01T00:00:00']


def test_new_table(yt_client_mock):
    new_table_dml_dt = '2020-02-01T00:00:00.000000Z'
    new_table_name = new_table_dml_dt.split('.')[0]
    yt_client_defaults(yt_client_mock, new_table_dml_dt)

    dml_dt, start_copy = table_copy.run_job(
        ['2020-01-01T00:00:00', '2020-01-01T00:00:00', '2020-01-01T00:00:00'],
        'fake_cluster',
        'fake_table',
        '//home/fake_dst_folder'
    )
    assert dml_dt == {'dml_dt': new_table_name}
    assert start_copy == {
        'table': '//home/fake_dst_folder/' + new_table_name,
        'dst-path': '//home/fake_dst_folder/' + new_table_name
    }


def test_old_table(yt_client_mock):
    new_table_dml_dt = '2020-01-01T00:00:00.000000Z'
    yt_client_defaults(yt_client_mock, new_table_dml_dt)

    dml_dt, start_copy = table_copy.run_job(
        ['2020-01-01T00:00:00', '2020-01-01T00:00:00', '2020-01-01T00:00:00'],
        'fake_cluster',
        'fake_table',
        '//home/fake_dst_folder'
    )
    assert dml_dt is None
    assert start_copy is None


def test_different_tax_artifacts():
    dml_dt, start_copy = table_copy.run_job(
        ['2020-01-01T00:00:00', '2020-02-01T00:00:00', '2020-03-01T00:00:00'],
        'fake_cluster',
        'fake_table',
        '//home/fake_dst_folder'
    )
    assert dml_dt is None
    assert start_copy is None


def test_table_already_exists(yt_client_mock):
    yt_client_defaults(yt_client_mock, '2020-01-01T00:00:00.000000Z')

    with pytest.raises(Exception) as exc_info:
        table_copy.run_job(
            ['2019-01-01T00:00:00', '2019-01-01T00:00:00', '2019-01-01T00:00:00'],
            'fake_cluster',
            'fake_table',
            '//home/fake_dst_folder'
        )

    assert exc_info.value.args[0] == 'Table //home/fake_dst_folder already exists at 2020-01-01T00:00:00'
