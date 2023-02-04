import pytest
import typing as tp

from contextlib import AbstractContextManager, nullcontext
from dataclasses import dataclass

from maps.infra.baseimage.template_generator.lib.merge import deep_join


@pytest.fixture
def case(request):
    return request.param


@dataclass
class MergeTestCase:
    name: str
    base: tp.Any
    update: tp.Any
    result: tp.Any = None
    raise_expectation: AbstractContextManager = nullcontext()

    def __str__(self) -> str:
        return self.name


MERGE_TESTS = [
    MergeTestCase(
        name='empty dicts',
        base={},
        update={},
        result={}
    ),
    MergeTestCase(
        name='nonempty dict',
        base={'a': 'foo'},
        update={},
        result={'a': 'foo'}
    ),
    MergeTestCase(
        name='nonempty dicts',
        base={'a': 'foo'},
        update={'b': 'bar'},
        result={'a': 'foo', 'b': 'bar'}
    ),
    MergeTestCase(
        name='empty lists',
        base=[],
        update=[],
        raise_expectation=pytest.raises(ValueError)
    ),
    MergeTestCase(
        name='nonempty lists',
        base=['a'],
        update=['b'],
        raise_expectation=pytest.raises(ValueError)
    ),
    MergeTestCase(
        name='two lists inside dict',
        base={'a': ['a']},
        update={'a': ['b']},
        result={'a': ['a', 'b']},
    ),
    MergeTestCase(
        name='two lists inside dict intersected',
        base={'a': ['a', 'b']},
        update={'a': ['a', 'c']},
        result={'a': ['a', 'b', 'c']},
    ),
    MergeTestCase(
        name='dict and list',
        base={},
        update=[],
        raise_expectation=pytest.raises(ValueError)
    ),
    MergeTestCase(
        name='dict and scalar',
        base={},
        update='a',
        raise_expectation=pytest.raises(ValueError)
    ),
    MergeTestCase(
        name='list and scalar',
        base=[],
        update='a',
        raise_expectation=pytest.raises(ValueError)
    ),
    MergeTestCase(
        name='dict and None',
        base={},
        update=None,
        result={}
    ),
    MergeTestCase(
        name='list and None',
        base={'a': []},
        update={'a': None},
        result={'a': []},
    ),
    MergeTestCase(
        name='scalar and None',
        base={'a': 'a'},
        update={'a': None},
        result={'a': 'a'},
    ),
    MergeTestCase(
        name='None and None',
        base={'a': None},
        update={'a': None},
        result={'a': None},
    ),
    MergeTestCase(
        name='scalar and scalar',
        base={'a': 'a'},
        update={'a': 'b'},
        result={'a': 'b'},
    ),
]


@pytest.mark.parametrize('case', MERGE_TESTS, ids=str, indirect=True)
def test_merge(case: MergeTestCase):
    with case.raise_expectation:
        assert deep_join(case.base, case.update) == case.result
