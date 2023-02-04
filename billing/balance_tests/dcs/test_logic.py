# coding: utf-8

import functools
import datetime as dt

import mock
import pytest

from nirvana_api.json_rpc import RPCException

from balance.actions.dcs.logic import DCSLogic


@pytest.fixture(autouse=True)
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as m:
        yield m


@pytest.fixture
def prepare_contract_attributes_mock():
    patch_path = 'balance.actions.dcs.compare.caob.prepare_contract_attributes'
    with mock.patch(patch_path) as mock_:
        yield mock_


@pytest.fixture
def min_finish_dt():
    return dt.datetime(2000, 1, 1)


@pytest.fixture
def test_func():
    logic = DCSLogic(nirvana_block=mock.MagicMock())
    return functools.partial(logic.nirvana_prepare_data_for_caob, 'hahn', '/table')


def test_objects_not_supplied(test_func, min_finish_dt, prepare_contract_attributes_mock):
    test_func(min_finish_dt, objects=None)
    prepare_contract_attributes_mock.assert_called_with(min_finish_dt, [])


def test_objects_converted(test_func, min_finish_dt, prepare_contract_attributes_mock):
    test_func(min_finish_dt, objects=['1', '2', '3'])
    prepare_contract_attributes_mock.assert_called_with(min_finish_dt, [1, 2, 3])


def test_objects_catch_invalid(test_func, min_finish_dt):
    with pytest.raises(ValueError, match='invalid literal for int'):
        test_func(min_finish_dt, objects=['); select 1 from dual; --'])


def test_min_finish_dt_from_str(test_func, min_finish_dt, prepare_contract_attributes_mock):
    test_func(min_finish_dt.strftime('%Y-%m-%d'))
    prepare_contract_attributes_mock.assert_called_with(min_finish_dt, [])


def test_invalid_min_finish_dt(test_func):
    with pytest.raises(ValueError, match='Unknown date format'):
        test_func('')

    with pytest.raises(ValueError, match='Unknown date format'):
        test_func('In the year 2000')

    with pytest.raises(ValueError, match='Unknown min_finish_dt type'):
        test_func(946684800)


def test_run_download_diffs():
    clone_and_run_instance_patch_path = 'balance.actions.dcs.logic.clone_and_run_instance'
    get_application_patch_path = 'balance.actions.dcs.logic.getApplication'

    expected_exception = RPCException(70, 'quota exceeded', [])
    unexpected_exception = RPCException(20, 'required options are not set', [])

    args = (1, ['key', ], 'hahn', '/result')

    with mock.patch(clone_and_run_instance_patch_path,
                    side_effect=[expected_exception, None]) as m, \
            mock.patch(get_application_patch_path), \
            mock.patch('time.sleep'):
        DCSLogic().run_download_diffs(*args)
        assert m.call_count == 2

    with mock.patch(clone_and_run_instance_patch_path,
                    side_effect=[unexpected_exception, None]) as m, \
            mock.patch(get_application_patch_path):
        with pytest.raises(RPCException):
            DCSLogic().run_download_diffs(*args)
        assert m.call_count == 1
