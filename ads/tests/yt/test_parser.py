import base64
import json
import dataclasses
import numpy as np
import pytest
import torch
import tqdm
import itertools
from typing import List
from ads_pytorch.yt.data_loader.parser import async_yabs_parser_v2, ParserPythonDescriptor
from ads_pytorch.cpp_lib import libcpp_lib


def get_started_parser(
    realvalue_features,
    categorical_features,
    weighted_categorical_features,
    num_parsers: int = 10,
    num_threads: int = 3
):
    interaction_features = [
        list(x)
        for x in itertools.chain(categorical_features)
        if not isinstance(x, str)
    ]
    categorical_features = [x for x in categorical_features if isinstance(x, str)]
    parser = libcpp_lib.TDictionaryLikeRecordParser(
        libcpp_lib.ThreadPoolHandle(num_threads),
        libcpp_lib.ThreadPoolHandle(num_parsers),
        30,  # maxYieldQueueSize
        libcpp_lib.TParserFeatures(
            categorical_features,
            weighted_categorical_features,
            realvalue_features,
            interaction_features
        ),
        None
    )
    parser.start()
    return parser


@pytest.fixture(scope="module")
def yabs_categorical_data_descr():
    categorical_factors = ["BannerID", "PageID", "UniqID"]
    multi_categorical_factors = ["BannerTitleLemmaH", "BannerBMCategoryID"]
    target_field = "IsClick"
    weight_field = "HitWeight"
    join_key = "my_ahaha_join_key"

    return categorical_factors, multi_categorical_factors, target_field, weight_field, join_key


@pytest.fixture(scope="module")
def parser_description():
    return json.dumps({
        "format": "json",
        "compression_algo": "identity"
    })


def array_to_string(array: np.ndarray, encoding: str = 'utf-8'):
    return json.dumps({
        "dtype": str(array.dtype),
        "size": list(array.shape),
        "data": base64.b64encode(array.tostring()).decode(encoding)
    })


def data_name(name):
    return name + "_pytorch_cat_data"


def data_len_name(name):
    return name + "_pytorch_cat_data_len"


def data_weight_name(name):
    return name + "_pytorch_cat_data_weight"


def realvalue_name(name):
    return name + "_pytorch_dense_data"


class AsyncIterator:
    def __init__(self, iterable):
        self.iterable = iter(iterable)

    def __aiter__(self):
        return self

    async def __anext__(self):
        try:
            return next(self.iterable)
        except StopIteration:
            raise StopAsyncIteration


def calc_interaction_hash(*feaids):
    if len(feaids) < 2:
        raise RuntimeError
    if len(feaids) == 2:
        return feaids[0] * 27942141 + feaids[1]
    return calc_interaction_hash(*feaids[:-1]) * 27942141 + feaids[-1]


def create_yabs_categorical_data(recs_count, multicat_size, categorical_data_descr):
    values = []
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = categorical_data_descr

    for rec_id in range(recs_count):
        dct = {}
        for cat in categorical_factors:
            feaid = hash(cat + str(rec_id))
            dct[data_name(cat)] = array_to_string(np.array([feaid], dtype=np.int64))
            dct[data_len_name(cat)] = array_to_string(np.array([1], dtype=np.int32))

        for cat in multi_categorical_factors:
            feaids = [hash(cat + str(rec_id) + str(i)) for i in range(multicat_size)]
            dct[data_name(cat)] = array_to_string(np.array(feaids, dtype=np.int64))
            dct[data_len_name(cat)] = array_to_string(np.array([multicat_size], dtype=np.int32))

        dct[realvalue_name(target_field)] = array_to_string(np.array([float(rec_id % 2)], dtype=np.float32))
        dct[realvalue_name(weight_field)] = array_to_string(np.array([1.0], dtype=np.float32))
        dct[data_name(join_key)] = array_to_string(np.array([1900], dtype=np.int64))
        values.append(dct)

    res = "\n".join([json.dumps(dct) for dct in values])
    return (res + "\n").encode('utf-8')


@pytest.fixture(scope='module')
def yabs_categorical_data(yabs_categorical_data_descr):
    RECS_COUNT = 10
    multicat_size = 10

    return create_yabs_categorical_data(RECS_COUNT, multicat_size, yabs_categorical_data_descr)


@pytest.mark.asyncio
async def test_parse_categorical_and_additional(yabs_categorical_data, yabs_categorical_data_descr, parser_description):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + multi_categorical_factors + [join_key],
        weighted_categorical_features=[]
    )

    iterator = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([yabs_categorical_data]),
                parser_description=parser_description
            )
        ]
    )
    records = [r async for r in iterator]
    assert len(records) == 10
    for r in records:
        # determine record id - parser is asynchronous
        for rec_id in range(10):
            cat = next(iter(categorical_factors))
            feaid = hash(cat + str(rec_id))
            if r[data_name(cat)] == torch.LongTensor([feaid]):
                break
        else:
            raise RuntimeError

        for cat in categorical_factors:
            feaid = hash(cat + str(rec_id))
            assert r[data_name(cat)] == torch.LongTensor([feaid])
            assert r[data_len_name(cat)] == torch.IntTensor([1])

        for cat in multi_categorical_factors:
            feaids = [hash(cat + str(rec_id) + str(i)) for i in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([10])))

        assert r[realvalue_name(target_field)] == torch.FloatTensor([float(rec_id % 2)])
        assert r[realvalue_name(weight_field)] == torch.FloatTensor([1.0])
        assert r[data_name(join_key)] == torch.LongTensor([1900])


@pytest.mark.asyncio
async def test_parse_deep_interactions(parser_description, yabs_categorical_data, yabs_categorical_data_descr):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    interactions = [
        (categorical_factors[0], categorical_factors[1], categorical_factors[2]),
        (categorical_factors[0], multi_categorical_factors[0], categorical_factors[1], multi_categorical_factors[1])
    ]
    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + interactions + multi_categorical_factors + [join_key],
        weighted_categorical_features=[]
    )
    iterator = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([yabs_categorical_data]),
                parser_description=parser_description,
            )
        ]

    )

    counter = 0
    async for r in iterator:
        # Cubic categorical
        cat = interactions[0]
        name = ','.join([cat[0], cat[1], cat[2]])
        assert r[data_name(name)] == calc_interaction_hash(r[data_name(cat[0])], r[data_name(cat[1])], r[data_name(cat[2])])
        assert r[data_len_name(name)] == 1

        # multicategorical
        cat = interactions[1]
        name = ','.join([cat[0], cat[1], cat[2], cat[3]])
        assert r[data_len_name(name)] == 100
        feaids = list(calc_interaction_hash(r[data_name(cat[0])], x, r[data_name(cat[2])], y) for x, y in itertools.product(r[data_name(cat[1])], r[data_name(cat[3])]))
        assert torch.all(torch.eq(r[data_name(name)], torch.LongTensor(feaids)))
        counter += 1
    assert counter == 10


@pytest.mark.asyncio
async def test_parse_quadratic_interactions(parser_description, yabs_categorical_data, yabs_categorical_data_descr):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    interactions = [
        (categorical_factors[0], categorical_factors[1]),
        (categorical_factors[0], multi_categorical_factors[1]),
        (multi_categorical_factors[0], multi_categorical_factors[1])
    ]
    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + interactions + multi_categorical_factors + [join_key],
        weighted_categorical_features=[]
    )
    parser = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([yabs_categorical_data]),
                parser_description=parser_description,
            )
        ]

    )

    counter = 0
    async for r in parser:
        # Categorical-categorical
        cat = interactions[0]
        name = ','.join([cat[0], cat[1]])
        assert r[data_name(name)] == calc_interaction_hash(r[data_name(cat[0])], r[data_name(cat[1])])
        assert r[data_len_name(name)] == 1

        # categorical - multicategotical
        cat = interactions[1]
        name = ','.join([cat[0], cat[1]])
        assert r[data_len_name(name)] == 10
        feaids = [calc_interaction_hash(r[data_name(cat[0])], x) for x in r[data_name(cat[1])]]
        assert torch.all(torch.eq(r[data_name(name)], torch.LongTensor(feaids)))

        # multicategorical - multicategorical
        cat = interactions[2]
        name = ','.join([cat[0], cat[1]])
        assert r[data_len_name(name)] == 100
        feaids = list(calc_interaction_hash(x, y) for x, y in itertools.product(r[data_name(cat[0])], r[data_name(cat[1])]))
        assert torch.all(torch.eq(r[data_name(name)], torch.LongTensor(feaids)))
        counter += 1
    assert counter == 10


@pytest.fixture(scope='module')
def yabs_categorical_data_big_record(yabs_categorical_data_descr):
    values = []
    obj_count = 10
    multicat_size = 10

    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr

    dct = {}
    for object_id in range(obj_count):
        for cat in categorical_factors:
            feaid = hash(cat + str(object_id))
            dct.setdefault(data_name(cat), []).append(feaid)
            dct.setdefault(data_len_name(cat), []).append(1)

        for cat in multi_categorical_factors:
            feaids = [hash(cat + str(object_id) + str(i)) for i in range(multicat_size)]
            dct.setdefault(data_name(cat), []).extend(feaids)
            dct.setdefault(data_len_name(cat), []).append(multicat_size)

    dct[realvalue_name(target_field)] = array_to_string(np.array([float(object_id % 2) for object_id in range(obj_count)], dtype=np.float32))
    dct[realvalue_name(weight_field)] = array_to_string(np.array([1.0] * obj_count, dtype=np.float32))
    dct[data_name(join_key)] = array_to_string(np.array([1900] * obj_count, dtype=np.int64))

    for cat in categorical_factors + multi_categorical_factors:
        dct[data_name(cat)] = array_to_string(np.array(dct[data_name(cat)], dtype=np.int64))
        dct[data_len_name(cat)] = array_to_string(np.array(dct[data_len_name(cat)], dtype=np.int32))

    values.append(dct)

    res = "\n".join([json.dumps(dct) for dct in values])
    return (res + "\n").encode('utf-8')


@pytest.mark.asyncio
async def test_parse_categorical_and_additional_big_record(parser_description, yabs_categorical_data_big_record, yabs_categorical_data_descr):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr

    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + multi_categorical_factors + [join_key],
        weighted_categorical_features=[]
    )

    iterator = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([yabs_categorical_data_big_record]),
                parser_description=parser_description,
            )
        ]
    )
    count = 0
    async for r in iterator:
        for cat in categorical_factors:
            feaids = [hash(cat + str(x)) for x in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([1] * 10)))

        for cat in multi_categorical_factors:
            feaids = [hash(cat + str(object_id) + str(i)) for object_id in range(10) for i in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([10] * 10)))

        assert torch.all(torch.eq(r[realvalue_name(target_field)], torch.FloatTensor([float(object_id % 2) for object_id in range(10)])))
        assert torch.all(torch.eq(r[realvalue_name(weight_field)], torch.FloatTensor([1.0] * 10)))
        assert torch.all(torch.eq(r[data_name(join_key)], torch.LongTensor([1900] * 10)))
        count += 1
    assert count == 1


@pytest.mark.asyncio
async def test_interactions_big_record(parser_description, yabs_categorical_data_big_record, yabs_categorical_data_descr):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + multi_categorical_factors + [join_key],
        weighted_categorical_features=[]
    )

    iterator = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([yabs_categorical_data_big_record]),
                parser_description=parser_description
            )
        ]
    )
    count = 0
    async for r in iterator:
        for cat in categorical_factors:
            feaids = [hash(cat + str(x)) for x in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([1] * 10)))

        for cat in multi_categorical_factors:
            feaids = [hash(cat + str(object_id) + str(i)) for object_id in range(10) for i in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([10] * 10)))

        assert torch.all(torch.eq(r[realvalue_name(target_field)], torch.FloatTensor([float(object_id % 2) for object_id in range(10)])))
        assert torch.all(torch.eq(r[realvalue_name(weight_field)], torch.FloatTensor([1.0] * 10)))
        assert torch.all(torch.eq(r[data_name(join_key)], torch.LongTensor([1900] * 10)))
        count += 1
    assert count == 1


@pytest.fixture(scope='module')
def yabs_categorical_data_large(yabs_categorical_data_descr):
    RECS_COUNT = 100
    multicat_size = 10

    return create_yabs_categorical_data(RECS_COUNT, multicat_size, yabs_categorical_data_descr)


# This test checks whether parser yields all records even with deep interactions and high parallelism
# Even with overcommit of the threads
# 1, 3, 10, 100,
@pytest.mark.parametrize('num_threads', [1, 3, 10, 100])
@pytest.mark.parametrize('num_parsers', [1, 7])
@pytest.mark.asyncio
async def test_parser_yield_all_records(parser_description, num_threads, num_parsers, yabs_categorical_data_large, yabs_categorical_data_descr):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    interactions = [
        (categorical_factors[0], categorical_factors[1], categorical_factors[2]),
        (categorical_factors[0], multi_categorical_factors[0], categorical_factors[1], multi_categorical_factors[1])
    ]

    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + interactions + multi_categorical_factors + [join_key],
        weighted_categorical_features=[],
        num_parsers=num_parsers,
        num_threads=num_threads
    )

    iterator = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([yabs_categorical_data_large]),
                parser_description=parser_description
            )
        ]
    )

    records = [r async for r in iterator]
    assert len(records) == 100


@pytest.fixture(scope='module')
def yabs_categorical_data_very_large(yabs_categorical_data_descr):
    RECS_COUNT = 100
    multicat_size = 100

    return create_yabs_categorical_data(RECS_COUNT, multicat_size, yabs_categorical_data_descr)


@pytest.mark.asyncio
async def test_parser_yield_consistency(parser_description, yabs_categorical_data_very_large, yabs_categorical_data_descr):
    num_threads = 2
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    interactions = [
        (categorical_factors[0], categorical_factors[1], categorical_factors[2], multi_categorical_factors[0], multi_categorical_factors[1]),
        (categorical_factors[0], multi_categorical_factors[0], categorical_factors[1], multi_categorical_factors[1]),
        (categorical_factors[0], multi_categorical_factors[0], categorical_factors[1], multi_categorical_factors[1], multi_categorical_factors[0])
    ]

    for _ in tqdm.trange(10):
        count = 0

        parser = get_started_parser(
            realvalue_features=[target_field, weight_field],
            categorical_features=categorical_factors + interactions + multi_categorical_factors + [join_key],
            weighted_categorical_features=[],
            num_parsers=3,
            num_threads=num_threads
        )

        async for _ in async_yabs_parser_v2(
            parser=parser,
            parser_descriptors=[
                ParserPythonDescriptor(
                    string_iterator=AsyncIterator([yabs_categorical_data_very_large]),
                    parser_description=parser_description
                )
            ]
        ):
            count += 1
        assert count == 100


@pytest.mark.asyncio
async def test_parser_ordered(
    parser_description,
    yabs_categorical_data_very_large,
    yabs_categorical_data_descr
):
    categorical_factors, multi_categorical_factors, target_field, weight_field, join_key = yabs_categorical_data_descr
    interactions = [
        (categorical_factors[0], categorical_factors[1], categorical_factors[2], multi_categorical_factors[0], multi_categorical_factors[1]),
        (categorical_factors[0], multi_categorical_factors[0], categorical_factors[1], multi_categorical_factors[1]),
        (categorical_factors[0], multi_categorical_factors[0], categorical_factors[1], multi_categorical_factors[1], multi_categorical_factors[0])
    ]

    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + interactions + multi_categorical_factors + [join_key],
        weighted_categorical_features=[],
        num_threads=1,
        num_parsers=1
    )

    data = [
        (x + "\n").encode("utf-8")
        for x in yabs_categorical_data_very_large.decode("utf-8").split("\n")
        if x
    ]

    count = 0
    async for rec in async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator(data),
                parser_description=parser_description
            )
        ]
    ):
        tensor: torch.Tensor = rec[data_name("BannerID")]
        feaid = hash("BannerID" + str(count))
        assert torch.allclose(tensor, torch.tensor([feaid], dtype=torch.int64))
        count += 1
    assert count == 100


@pytest.fixture(scope="module")
def weighted_categorical_data_descr():
    categorical_factors = ["BannerID", "PageID", "UniqID"]
    weighted_categorical_factors = ["Token", "OntoID"]
    target_field = "IsClick"
    weight_field = "HitWeight"
    join_key = "my_ahaha_join_key"

    return categorical_factors, weighted_categorical_factors, target_field, weight_field, join_key


def create_weighted_categorical_data(recs_count, weighted_cat_size, categorical_data_descr):
    values = []
    categorical_factors, weighted_categorical_factors, target_field, weight_field, join_key = categorical_data_descr

    for rec_id in range(recs_count):
        dct = {}
        for cat in categorical_factors:
            feaid = hash(cat + str(rec_id))
            dct[data_name(cat)] = array_to_string(np.array([feaid], dtype=np.int64))
            dct[data_len_name(cat)] = array_to_string(np.array([1], dtype=np.int32))

        for cat in weighted_categorical_factors:
            feaids = [hash(cat + str(rec_id) + str(i)) for i in range(weighted_cat_size)]
            feaweights = [float(rec_id) / (float(i) + 1.0) for i in range(weighted_cat_size)]
            dct[data_name(cat)] = array_to_string(np.array(feaids, dtype=np.int64))
            dct[data_len_name(cat)] = array_to_string(np.array([weighted_cat_size], dtype=np.int32))
            dct[data_weight_name(cat)] = array_to_string(np.array(feaweights, dtype=np.float32))

        dct[realvalue_name(target_field)] = array_to_string(np.array([float(rec_id % 2)], dtype=np.float32))
        dct[realvalue_name(weight_field)] = array_to_string(np.array([1.0], dtype=np.float32))
        dct[data_name(join_key)] = array_to_string(np.array([1900], dtype=np.int64))
        values.append(dct)

    res = "\n".join([json.dumps(dct) for dct in values])
    return (res + "\n").encode('utf-8')


@pytest.fixture(scope='module')
def weighted_categorical_data(weighted_categorical_data_descr):
    RECS_COUNT = 10
    weighted_cat_size = 10

    return create_weighted_categorical_data(RECS_COUNT, weighted_cat_size, weighted_categorical_data_descr)


@pytest.mark.asyncio
async def test_parse_weighted_categorical(parser_description, weighted_categorical_data, weighted_categorical_data_descr):
    categorical_factors, weighted_categorical_factors, target_field, weight_field, join_key = weighted_categorical_data_descr
    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + [join_key],
        weighted_categorical_features=weighted_categorical_factors,
    )
    iterator = async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([weighted_categorical_data]),
                parser_description=parser_description
            )
        ]
    )
    records = [r async for r in iterator]
    assert len(records) == 10
    for r in records:
        # determine record id - parser is asynchronous
        for rec_id in range(10):
            cat = next(iter(categorical_factors))
            feaid = hash(cat + str(rec_id))
            if r[data_name(cat)] == torch.LongTensor([feaid]):
                break
        else:
            raise RuntimeError

        for cat in categorical_factors:
            feaid = hash(cat + str(rec_id))
            assert r[data_name(cat)] == torch.LongTensor([feaid])
            assert r[data_len_name(cat)] == torch.IntTensor([1])

        for cat in weighted_categorical_factors:
            feaids = [hash(cat + str(rec_id) + str(i)) for i in range(10)]
            feaweights = [float(rec_id) / (float(i) + 1.0) for i in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([10])))
            assert torch.allclose(r[data_weight_name(cat)], torch.FloatTensor(feaweights))

        assert r[realvalue_name(target_field)] == torch.FloatTensor([float(rec_id % 2)])
        assert r[realvalue_name(weight_field)] == torch.FloatTensor([1.0])
        assert r[data_name(join_key)] == torch.LongTensor([1900])


@pytest.fixture(scope='module')
def weighted_categorical_data_big_record(weighted_categorical_data_descr):
    values = []
    obj_count = 10
    weighted_cat_size = 10

    categorical_factors, weighted_categorical_factors, target_field, weight_field, join_key = weighted_categorical_data_descr

    dct = {}
    for object_id in range(obj_count):
        for cat in categorical_factors:
            feaid = hash(cat + str(object_id))
            dct.setdefault(data_name(cat), []).append(feaid)
            dct.setdefault(data_len_name(cat), []).append(1)

        for cat in weighted_categorical_factors:
            feaids = [hash(cat + str(object_id) + str(i)) for i in range(weighted_cat_size)]
            feaweights = [float(object_id) / (float(i) + 1.0) for i in range(weighted_cat_size)]
            dct.setdefault(data_name(cat), []).extend(feaids)
            dct.setdefault(data_len_name(cat), []).append(weighted_cat_size)
            dct.setdefault(data_weight_name(cat), []).extend(feaweights)

    dct[realvalue_name(target_field)] = array_to_string(np.array([float(object_id % 2) for object_id in range(obj_count)], dtype=np.float32))
    dct[realvalue_name(weight_field)] = array_to_string(np.array([1.0] * obj_count, dtype=np.float32))
    dct[data_name(join_key)] = array_to_string(np.array([1900] * obj_count, dtype=np.int64))

    for cat in categorical_factors + weighted_categorical_factors:
        dct[data_name(cat)] = array_to_string(np.array(dct[data_name(cat)], dtype=np.int64))
        dct[data_len_name(cat)] = array_to_string(np.array(dct[data_len_name(cat)], dtype=np.int32))
        if cat in weighted_categorical_factors:
            dct[data_weight_name(cat)] = array_to_string(np.array(dct[data_weight_name(cat)], dtype=np.float32))

    values.append(dct)

    res = "\n".join([json.dumps(dct) for dct in values])
    return (res + "\n").encode('utf-8')


@pytest.mark.asyncio
async def test_parse_weighted_categorical_big_record(parser_description, weighted_categorical_data_big_record, weighted_categorical_data_descr):
    categorical_factors, weighted_categorical_factors, target_field, weight_field, join_key = weighted_categorical_data_descr
    parser = get_started_parser(
        realvalue_features=[target_field, weight_field],
        categorical_features=categorical_factors + [join_key],
        weighted_categorical_features=weighted_categorical_factors,
    )
    count = 0
    async for r in async_yabs_parser_v2(
        parser=parser,
        parser_descriptors=[
            ParserPythonDescriptor(
                string_iterator=AsyncIterator([weighted_categorical_data_big_record]),
                parser_description=parser_description
            )
        ]
    ):
        for cat in categorical_factors:
            feaids = [hash(cat + str(x)) for x in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([1] * 10)))

        for cat in weighted_categorical_factors:
            feaids = [hash(cat + str(object_id) + str(i)) for object_id in range(10) for i in range(10)]
            feaweights = [float(object_id) / (float(i) + 1.0) for object_id in range(10) for i in range(10)]
            assert torch.all(torch.eq(r[data_name(cat)], torch.LongTensor(feaids)))
            assert torch.all(torch.eq(r[data_len_name(cat)], torch.IntTensor([10] * 10)))
            assert torch.allclose(r[data_weight_name(cat)], torch.FloatTensor(feaweights))

        assert torch.all(torch.eq(r[realvalue_name(target_field)], torch.FloatTensor([float(object_id % 2) for object_id in range(10)])))
        assert torch.all(torch.eq(r[realvalue_name(weight_field)], torch.FloatTensor([1.0] * 10)))
        assert torch.all(torch.eq(r[data_name(join_key)], torch.LongTensor([1900] * 10)))
        count += 1
    assert count == 1


@pytest.mark.parametrize("string_count", [1, 35])
@pytest.mark.asyncio
async def test_malformed_string(parser_description, weighted_categorical_data_big_record, weighted_categorical_data_descr, string_count):
    categorical_factors, weighted_categorical_factors, target_field, weight_field, join_key = weighted_categorical_data_descr
    weighted_categorical_data_big_record *= string_count
    weighted_categorical_data_big_record = weighted_categorical_data_big_record[:-35]

    with pytest.raises(RuntimeError):
        parser = get_started_parser(
            realvalue_features=[target_field, weight_field],
            categorical_features=categorical_factors + [join_key],
            weighted_categorical_features=weighted_categorical_factors,
        )
        async for _ in async_yabs_parser_v2(
            parser=parser,
            parser_descriptors=[
                ParserPythonDescriptor(
                    string_iterator=AsyncIterator([weighted_categorical_data_big_record]),
                    parser_description=parser_description
                )
            ]
        ):
            pass


#####################################################################
#                        MULTISTREAM PARSER                         #
#####################################################################


@dataclasses.dataclass
class MultistreamFixtureOutput:
    first_categorical_factors: List[str]
    first_realvalue_factors: List[str]
    second_categorical_factors: List[str]
    second_realvalue_factors: List[str]
    join_keys: List[str]
    first_stream: List[bytes]
    second_stream: List[bytes]


@pytest.fixture(scope="module")
def multistream_records():
    first_categorical_factors = ["Cat1", "Cat2"]
    first_realvalue_factors = ["Rv1", "Rv2"]

    second_categorical_factors = ["Cat3", "Cat4"]
    second_realvalue_factors = ["Rv3", "Rv4"]

    join_keys = [
        "HitLogID",
        "BannerID",
        "ShowTime"
    ]

    obj_count = 300

    def _build_string_stream(
        categorical_factors: List[str],
        realvalue_factors: List[str]
    ):
        values = []
        for rec_id in range(obj_count):
            dct = {}
            for cat in categorical_factors + join_keys:
                feaid = hash(cat) + rec_id
                dct[data_name(cat)] = array_to_string(np.array([feaid, 1000000 + feaid], dtype=np.int64))
                dct[data_len_name(cat)] = array_to_string(np.array([1, 1], dtype=np.int32))

            for factorId, rv in enumerate(realvalue_factors):
                dct[realvalue_name(rv)] = array_to_string(
                    np.array([[float(rec_id % 2) * 10 ** factorId]] * 2, dtype=np.float32)
                )
            values.append(dct)

        # test several records in stream
        stream = []
        for i in range(0, obj_count, 3):
            cur_value = "\n".join([json.dumps(dct) for dct in values[i: i + 3]])
            stream.append((cur_value + "\n").encode('utf-8'))
        return stream

    first_stream = _build_string_stream(
        categorical_factors=first_categorical_factors,
        realvalue_factors=first_realvalue_factors
    )

    second_stream = _build_string_stream(
        categorical_factors=second_categorical_factors,
        realvalue_factors=second_realvalue_factors
    )

    return MultistreamFixtureOutput(
        first_categorical_factors=first_categorical_factors,
        first_realvalue_factors=first_realvalue_factors,
        second_categorical_factors=second_categorical_factors,
        second_realvalue_factors=second_realvalue_factors,
        join_keys=join_keys,
        first_stream=first_stream,
        second_stream=second_stream,
    )


def _check_proper_record_join(rec, fixture_output: MultistreamFixtureOutput):
    first_categorical_factors = fixture_output.first_categorical_factors
    first_realvalue_factors = fixture_output.first_realvalue_factors
    second_categorical_factors = fixture_output.second_categorical_factors
    second_realvalue_factors = fixture_output.second_realvalue_factors
    join_keys = fixture_output.join_keys

    yielded_record_id = None
    # check we have merged record
    for name in first_categorical_factors + second_categorical_factors + join_keys:
        # see data generation code
        if yielded_record_id is None:
            yielded_record_id = int(rec[data_name(name)][0]) - hash(name)
        possible_values = [
            hash(name) + yielded_record_id,
            hash(name) + 1000000 + yielded_record_id
        ]
        if rec[data_name(name)].numel() == 2:
            assert rec[data_name(name)].tolist() == possible_values
        else:
            assert int(rec[data_name(name)][0]) in possible_values

    for rv_list in [first_realvalue_factors, second_realvalue_factors]:
        for factorId, rv in enumerate(rv_list):
            numel = rec[realvalue_name(rv)].numel()
            reference = np.array([[float(yielded_record_id % 2) * 10 ** factorId]] * numel, dtype=np.float32)
            assert np.allclose(rec[realvalue_name(rv)].numpy(), reference)
