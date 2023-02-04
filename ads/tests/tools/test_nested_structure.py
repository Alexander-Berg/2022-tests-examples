import pytest
import dataclasses
import inspect
from copy import deepcopy
from typing import Dict, Tuple
import torch

from ads_pytorch.tools.nested_structure import (
    apply, apply_async,
    flatten_iterator,
    DifferentNestedStructuresError
)


###################################################################
#                                APPLY                            #
###################################################################


@pytest.fixture(params=[0, 1, 2], ids=['AsyncFnAsync', 'Async', 'Usual'])
def do_async(request):
    return request.param


async def apply_helper(*objs, fn, do_async):
    if do_async == 0:
        assert not inspect.iscoroutinefunction(fn)

        async def _wrapped_fn(*args):
            return fn(*args)

        return await apply_async(*objs, fn=_wrapped_fn)
    elif do_async == 1:
        return await apply_async(*objs, fn=fn)
    elif do_async == 2:
        return apply(*objs, fn=fn)
    else:
        raise ValueError("Unknown mode?...")


# test apply on single collection

@dataclasses.dataclass(frozen=True)
class TensorHandle:
    it1: Tuple[torch.Tensor, ...]
    it2: Dict[str, torch.Tensor]


@dataclasses.dataclass(frozen=True)
class NestedTensorHandle:
    it1: Dict[str, TensorHandle]


_TEST_VALUES = [
    torch.IntTensor([1]),
    [torch.IntTensor([1]), torch.IntTensor([2])],
    tuple((torch.IntTensor([1]), torch.IntTensor([2]))),
    {1: torch.IntTensor([1]), "2": torch.IntTensor([2])},
    TensorHandle(
        it1=tuple((torch.IntTensor([1]), torch.IntTensor([2]))),
        it2={1: torch.IntTensor([3]), "2": torch.IntTensor([4])},
    ),
    NestedTensorHandle(
        it1=dict(
            x=TensorHandle(
                it1=tuple((torch.IntTensor([1]), torch.IntTensor([2]))),
                it2={1: torch.IntTensor([3]), "2": torch.IntTensor([4])},
            ),
            y=TensorHandle(
                it1=tuple((torch.IntTensor([5]), torch.IntTensor([6]))),
                it2={1: torch.IntTensor([7]), "2": torch.IntTensor([8])},
            )
        )
    )
]


_TEST_REFERENCES = [
    torch.IntTensor([2]),
    [torch.IntTensor([2]), torch.IntTensor([3])],
    tuple((torch.IntTensor([2]), torch.IntTensor([3]))),
    {1: torch.IntTensor([2]), "2": torch.IntTensor([3])},
    TensorHandle(
        it1=tuple((torch.IntTensor([2]), torch.IntTensor([3]))),
        it2={1: torch.IntTensor([4]), "2": torch.IntTensor([5])},
    ),
    NestedTensorHandle(
        it1=dict(
            x=TensorHandle(
                it1=tuple((torch.IntTensor([2]), torch.IntTensor([3]))),
                it2={1: torch.IntTensor([4]), "2": torch.IntTensor([5])},
            ),
            y=TensorHandle(
                it1=tuple((torch.IntTensor([6]), torch.IntTensor([7]))),
                it2={1: torch.IntTensor([8]), "2": torch.IntTensor([9])},
            )
        )
    )
]


_TEST_IDS = [
    'Tensor',
    'List',
    'Tuple',
    'Dict',
    'Dataclass',
    'NestedDataclass'
]


@pytest.mark.parametrize('value,reference', list(zip(_TEST_VALUES, _TEST_REFERENCES)), ids=_TEST_IDS)
@pytest.mark.asyncio
async def test_apply(value, reference, do_async):
    result = await apply_helper(value, fn=lambda tensor: tensor + torch.IntTensor([1]), do_async=do_async)
    # to allow comparison by value, convert sets and frozensets to tuples
    assert type(value) == type(result) == type(reference)
    if type(value) in {set, frozenset}:
        result = list(result)
        reference = list(reference)
    assert result == reference


_TEST_ZIP_REFERENCES = [
    torch.IntTensor([10]),
    [torch.IntTensor([10]), torch.IntTensor([20])],
    tuple((torch.IntTensor([10]), torch.IntTensor([20]))),
    {1: torch.IntTensor([10]), "2": torch.IntTensor([20])},
    TensorHandle(
        it1=tuple((torch.IntTensor([10]), torch.IntTensor([20]))),
        it2={1: torch.IntTensor([30]), "2": torch.IntTensor([40])},
    ),
    NestedTensorHandle(
        it1=dict(
            x=TensorHandle(
                it1=tuple((torch.IntTensor([10]), torch.IntTensor([20]))),
                it2={1: torch.IntTensor([30]), "2": torch.IntTensor([40])},
            ),
            y=TensorHandle(
                it1=tuple((torch.IntTensor([50]), torch.IntTensor([60]))),
                it2={1: torch.IntTensor([70]), "2": torch.IntTensor([80])},
            )
        )
    )
]


# now test the zip application


@pytest.mark.parametrize('value,reference', list(zip(_TEST_VALUES, _TEST_ZIP_REFERENCES)), ids=_TEST_IDS)
@pytest.mark.asyncio
async def test_apply_multiple(value, reference, do_async):
    def _foo(*tensors):
        return sum(tensors)

    result = await apply_helper(*[deepcopy(value) for _ in range(10)], fn=_foo, do_async=do_async)

    # to allow comparison by value, convert sets and frozensets to tuples
    assert type(value) == type(result) == type(reference)
    if type(value) in {set, frozenset}:
        result = list(result)
        reference = list(reference)
    assert result == reference


# Now we test exceptions when our nested structures differ


# We have to check our implementation detail - KeyError from fn function
@pytest.mark.parametrize(
    'v1,v2',
    [
        ([1, 2, 3], [1, 2]),
        ([1, 2], [1, 2, 3]),
        ([1, 2, 3], (1, 2, 3)),
        (TensorHandle(tuple(), {}), {1: 2, 2: 3}),
        (TensorHandle(tuple(), {}), NestedTensorHandle(TensorHandle(tuple(), {}))),
        ({1: 2, 2: 3}, {1: 2, 2: 3, 3: 4}),
        ({1: 2, 2: 3, 3: 4}, {1: 2, 2: 3}),
        ({1: 2, 2: 3}, {1: 2, 3: 4}),
        # Now things get more complicated - we should make difference between value and structure.
        # In fact, these suites check that value is not allowed to be list, dict or arbitrary dataclass
        ([1, 2], [1, [1, 2]]),
        ({1: 2}, {1: [2, 3]}),
        ([1, 2], [1, TensorHandle(tuple(), {})]),
        # Now we test first suite, but in nested setting
        (TensorHandle((1, 2), {1: 2}), TensorHandle((1, 2), {2: 3})),
        (TensorHandle((1, 2), {1: 2}), TensorHandle((1, 2), {2: 3, 1: 2})),
        (TensorHandle((1, 2), {1: 2}), TensorHandle((1,), {1: 2})),
    ],
    ids=[
        "list_differ_len1",
        "list_differ_len2",
        "different_order_collection_type",
        "different_dataclass_and_dict",
        "different_dataclasses",
        "second_dict_extra_key",
        "first_dict_extra_key",
        "dict_equal_len_different_keys",
        "list_instead_of_value",
        "dict_instead_of_value",
        "dataclass_instead_of_value",
        "nested_dict_different_keys",
        "nested_dict_different_len",
        "nested_tuple_different_len",
    ]
)
@pytest.mark.asyncio
async def test_differing_nested_structures(v1, v2, do_async):
    with pytest.raises(DifferentNestedStructuresError):
        await apply_helper(v1, v2, fn=lambda x1, x2: x1 + x2, do_async=do_async)


# And now we want two check two corner cases connected with dicts
# First of all, dictionaries in python 3.7+ are ordered, but our code must not rely on this
# order since we may pass two equal dicts with different keys order and code must work


@pytest.mark.asyncio
async def test_equal_dicts_with_different_key_order(do_async):
    dct1 = {"1": 2, "2": 3}
    dct2 = {"2": 20, "1": 30}
    assert list(dct1.keys()) == ["1", "2"]
    assert list(dct2.keys()) == ["2", "1"]

    res = await apply_helper(dct1, dct2, fn=lambda x1, x2: x1 + x2, do_async=do_async)
    assert res == {"1": 32, "2": 23}


@pytest.mark.asyncio
async def test_key_error_from_fn(do_async):
    dct1 = {"1": 2, "2": 3}
    dct2 = {"2": 20, "1": 30}

    counter = 0

    def _fn(x1, x2):
        nonlocal counter
        counter += 1
        if counter == 2:
            raise KeyError

    with pytest.raises(KeyError):
        await apply_helper(dct1, dct2, fn=_fn, do_async=do_async)


###################################################################
#                          FLATTEN ITERATOR                       #
###################################################################

@dataclasses.dataclass
class AlreadyFlat:
    a: int
    b: int
    c: int


@dataclasses.dataclass
class X:
    x: Dict[str, int]
    y: Dict[str, int]


@dataclasses.dataclass
class Y:
    z: Dict[str, int]
    w: Dict[str, int]


@dataclasses.dataclass
class A:
    a1: X
    a2: Y


@pytest.mark.parametrize('value,reference', [
    # object itself - no nested keys
    (1, [((), 1)]),

    # already flat structures
    ((1, 2, 3), [((0, ), 1), ((1, ), 2), ((2, ), 3)]),
    ([1, 2, 3], [((0, ), 1), ((1, ), 2), ((2, ), 3)]),
    ({"a": 1, "b": 2, "c": 3}, [(("a", ), 1), (("b", ), 2), (("c", ), 3)]),
    (AlreadyFlat(1, 2, 3), [(("a", ), 1), (("b", ), 2), (("c", ), 3)]),

    # nested structures
    ([[1, 2], [3, 4]], [((0, 0), 1), ((0, 1), 2), ((1, 0), 3), ((1, 1), 4)]),
    (((1, 2), (3, 4)), [((0, 0), 1), ((0, 1), 2), ((1, 0), 3), ((1, 1), 4)]),
    (
        [{"a": 1}, (2, 3), [4], {"b": [5, 6, {"c": 7}]}],
        [
            ((0, "a"), 1),
            ((1, 0), 2),
            ((1, 1), 3),
            ((2, 0), 4),
            ((3, "b", 0), 5),
            ((3, "b", 1), 6),
            ((3, "b", 2, "c"), 7),
        ]
    ),
    (A(X({"a": 1}, {"b": 2}), Y({"c": 3}, {"d": 4})), [(('a1', 'x', 'a'), 1), (('a1', 'y', 'b'), 2), (('a2', 'z', 'c'), 3), (('a2', 'w', 'd'), 4)])
])
def test_flatten_iterator(value, reference):
    assert list(flatten_iterator(value)) == reference
