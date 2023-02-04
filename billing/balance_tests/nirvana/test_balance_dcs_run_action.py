# coding: utf-8

import json

import mock
import pytest

from butils import exc

from balance.actions.nirvana.operations import run_balance_dcs_action

from tests import object_builder as ob

DEFAULT_METHOD_NAME = 'nirvana_sample_method'


@pytest.fixture
def logic_cls():
    # spec ограничивает методы, которые присутствуют у подменяемого объекта
    # __call__ добавляется для того, чтобы мы могли создать объект класса DCSLogic
    patch_path = 'balance.actions.nirvana.operations.run_balance_dcs_action.DCSLogic'
    with mock.patch(patch_path, spec=['__call__', DEFAULT_METHOD_NAME]) as mock_:
        yield mock_


def fake_nirvana_block(session, method=DEFAULT_METHOD_NAME, params=''):
    return ob.NirvanaBlockBuilder.construct(
        session,
        request={
            'data': {
                'options': {
                    'method': method,
                    'params': params,
                },
            },
        },
    )


@pytest.fixture
def nirvana_block(session):
    return fake_nirvana_block(session)


@pytest.mark.usefixtures('logic_cls')
def test_success_run(nirvana_block):
    status = run_balance_dcs_action.process(nirvana_block)
    assert status.is_finished()


def test_success_run_with_params(session, logic_cls):
    params = {'param': 'value'}

    nirvana_block = fake_nirvana_block(session, params=json.dumps(params))
    status = run_balance_dcs_action.process(nirvana_block)
    assert status.is_finished()

    called_method = getattr(logic_cls(), DEFAULT_METHOD_NAME)
    called_method.assert_called_with(**params)


def test_invalid_method_name(session):
    nirvana_block = fake_nirvana_block(session, method='unknown_method')
    with pytest.raises(exc.INVALID_PARAM, match='only nirvana_'):
        run_balance_dcs_action.process(nirvana_block)


@pytest.mark.usefixtures('logic_cls')
def test_unexpected_method(session):
    unexpected_method = 'nirvana_unexpected_method'
    nirvana_block = fake_nirvana_block(session, method=unexpected_method)

    exc_message = 'method "%s" not found' % unexpected_method
    with pytest.raises(exc.INVALID_PARAM, match=exc_message):
        run_balance_dcs_action.process(nirvana_block)


@pytest.mark.usefixtures('logic_cls')
def test_invalid_params(session):
    nirvana_block = fake_nirvana_block(session, params='hello world')
    with pytest.raises(ValueError):
        run_balance_dcs_action.process(nirvana_block)


@pytest.mark.usefixtures('logic_cls')
def test_invalid_json_in_params(session):
    nirvana_block = fake_nirvana_block(session, params='[1, 2, 3]')
    with pytest.raises(exc.INVALID_PARAM, match='params should be only'):
        run_balance_dcs_action.process(nirvana_block)
