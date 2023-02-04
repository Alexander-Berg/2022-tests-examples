from contextlib import contextmanager

import pytest

from .tables import TestTable


@contextmanager
def does_not_raise():
    yield


@pytest.mark.parametrize(
    'init',
    [
        pytest.param(lambda kwargs: TestTable(**kwargs), id='init'),
        pytest.param(lambda kwargs: TestTable.deserialize(**kwargs), id='deserialize'),
    ],
)
@pytest.mark.parametrize(
    'kwargs,values,expect',
    [
        ({}, {'id': None, 'non_id': None}, lambda: does_not_raise()),
        ({'id': 1}, {'id': 1, 'non_id': None}, lambda: does_not_raise()),
        ({'id': 2, 'non_id': 'string'}, {'id': 2, 'non_id': 'string'}, lambda: does_not_raise()),
        ({'rogue': 'Anna Marie'}, {'id': None, 'non_id': None}, lambda: does_not_raise()),
        ({'wolverine': 'Logan'}, {'id': None, 'non_id': None}, lambda: pytest.raises(ValueError)),
    ],
)
def test_init(kwargs, values, expect, init):
    with expect():
        t = init(kwargs)
        assert t.id == values['id']
        assert t.non_id == values['non_id']
