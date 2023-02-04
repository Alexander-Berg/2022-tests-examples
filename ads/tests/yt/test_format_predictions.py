import base64
import struct
import numpy as np
import pytest
import torch
from collections import OrderedDict

from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.tools.cpp_async_threaded import cpp_threaded_worker
from ads_pytorch.yt.prediction import PredictionsFormatter, PredictionUnit


@pytest.fixture(scope='module')
def array():
    return torch.FloatTensor(list(x + 0.5 for x in range(100)))


@pytest.fixture(scope='module')
def keys():
    return torch.from_numpy(np.random.randint(0, 100000, size=100, dtype=np.int64))


@pytest.fixture(params=['threaded', 'usual'])
def run_formatter(request):
    if request.param == 'usual':
        async def _run(formatter):
            formatter()
    else:
        async def _run(formatter):
            thread_worker = cpp_threaded_worker()
            await thread_worker(formatter)
    return _run


@pytest.fixture(scope='module')
def reference(array, keys):
    # Unfortunately, pybind11 does not support OrderedDict and we cannot predict the ordering of the fields
    return ('\n'.join(['{}\t{}'.format(p, i) for i, p in zip(array, keys)]) + "\n").encode('utf-8')


def compare_results(res, reference):
    assert sorted(bytes(res).split(b'\n')) == sorted(reference.split(b'\n'))


@pytest.mark.parametrize('num_threads', [1, 3, 5])
@pytest.mark.asyncio
async def test_format_predictions(num_threads, reference, array, keys, run_formatter):
    formatter = libcpp_lib.PredictionFormatter(
        ["uint64_join_subkey", "predictions"],
        [False, False],
        num_threads
    )
    formatter.push_value({
        "uint64_join_subkey": keys,
        "predictions": array
    })
    await run_formatter(formatter)
    res = formatter.get_result()
    compare_results(res, reference)


@pytest.mark.parametrize(
    'dtype,reference',
    [
        (torch.int64, (1 << 64) - 1),
        (torch.int32, (1 << 32) - 1),
        (torch.int16, (1 << 16) - 1)
    ],
    ids=['Int64', 'Int32', 'Int16']
)
@pytest.mark.asyncio
async def test_format_predictions_with_type_cast(dtype, reference, run_formatter):
    formatter = libcpp_lib.PredictionFormatter(
        ["x", "predictions"],
        [True, False],
        1
    )
    formatter.push_value({
        "x": torch.tensor([-1], dtype=dtype),
        "predictions": torch.tensor([-3.2])
    })
    await run_formatter(formatter)
    res = formatter.get_result()
    compare_results(res, f"{reference}\t-3.2\n".encode('utf-8'))


@pytest.mark.parametrize(
    'dtype',
    [
        torch.int8,
        torch.uint8,
        torch.float32,
        torch.float64
    ],
    ids=['Int8', 'Uint8', 'Float32', 'Float64']
)
@pytest.mark.asyncio
async def test_no_available_cast(dtype, run_formatter):
    formatter = libcpp_lib.PredictionFormatter(
        ["x", "predictions"],
        [True, False],
        1
    )
    formatter.push_value({
        "x": torch.tensor([-1], dtype=dtype),
        "predictions": torch.tensor([-3.2])
    })
    with pytest.raises(Exception):
        await run_formatter(formatter)


################################################
#               TENSOR SERIALIZATION           #
################################################


@pytest.mark.asyncio
async def test_serialize_tensor(run_formatter):
    torch.manual_seed(12345)
    formatter = libcpp_lib.PredictionFormatter(["x"], [True], 1)
    tensor = torch.rand(3, 4, 5)
    formatter.push_value({"x": tensor})
    await run_formatter(formatter)
    res = formatter.get_result()
    deserialized_tensors = []
    for cur_res in bytes(res).split(b'\n'):
        if not cur_res:  # last empty string
            continue
        cur_res = base64.b64decode(cur_res)

        length = struct.unpack("=Q", cur_res[:8])[0]
        assert length == 2
        sizes = struct.unpack("=QQ", cur_res[8:24])
        assert sizes == (4, 5)
        typeid = struct.unpack('=B', cur_res[24:25])[0]
        assert typeid == 0  # for float32
        deserialized = torch.from_numpy(np.fromstring(cur_res[25:], dtype=np.float32)).view(*sizes)
        deserialized_tensors.append(deserialized)
    assert torch.allclose(tensor, torch.stack(deserialized_tensors))


@pytest.mark.parametrize('dim', [10, 1], ids=['column[10, 1]', 'column[1,1]'])
@pytest.mark.asyncio
async def test_serialize_column_vector_as_usual_type(run_formatter, dim):
    torch.manual_seed(12345)
    formatter = libcpp_lib.PredictionFormatter(["x"], [False], 1)
    tensor = torch.rand(dim, 1)
    formatter.push_value({"x": tensor})
    await run_formatter(formatter)
    res = formatter.get_result()
    result = [round(float(x.decode('utf-8')), 5) for x in bytes(res).split(b'\n') if x]
    reference = [round(float(x), 5) for x in tensor.squeeze(1)]
    assert result == reference


@pytest.mark.asyncio
async def test_serialize_column_vector_as_usual_type_with_cast(run_formatter):
    torch.manual_seed(12345)
    formatter = libcpp_lib.PredictionFormatter(["x"], [True], 1)
    tensor = torch.LongTensor(list(range(-5, 5)))
    formatter.push_value({"x": tensor})
    await run_formatter(formatter)
    res = formatter.get_result()
    result = [int(x.decode('utf-8')) for x in bytes(res).split(b'\n') if x]
    reference = [
        18446744073709551611,
        18446744073709551612,
        18446744073709551613,
        18446744073709551614,
        18446744073709551615,
        0,
        1,
        2,
        3,
        4
    ]

    assert result == reference


@pytest.mark.parametrize('dim', [10, 1], ids=['column[10, 1]', 'column[1,1]'])
@pytest.mark.asyncio
async def test_multidim_column_as_tensor(run_formatter, dim):
    torch.manual_seed(12345)
    formatter = libcpp_lib.PredictionFormatter(["x"], [False], 1)
    tensor = torch.rand(dim, 1, 1)
    formatter.push_value({"x": tensor})
    await run_formatter(formatter)
    res = formatter.get_result()

    for reference, cur_res in zip(tensor.squeeze(1).squeeze(1), bytes(res).split(b'\n')):
        if not cur_res:  # last empty string
            continue
        cur_res = base64.b64decode(cur_res)

        length = struct.unpack("=Q", cur_res[:8])[0]
        assert length == 2
        sizes = struct.unpack("=QQ", cur_res[8:24])
        assert sizes == (1, 1)
        typeid = struct.unpack('=B', cur_res[24:25])[0]
        assert typeid == 0  # for float32
        deserialized = torch.from_numpy(np.fromstring(cur_res[25:], dtype=np.float32)).view(*sizes)
        assert round(float(reference), 5) == round(float(deserialized.squeeze()), 5)


###############################################
#              TEST YT FORMATTER              #
###############################################


@pytest.mark.parametrize('num_threads', [1, 3])
@pytest.mark.asyncio
async def test_empty_predictions(num_threads):
    formatter = PredictionsFormatter(num_threads)
    data, schema = await formatter(
        all_predictions=[],
        cast_to_unsigned={"key": False, "key2": False}
    )
    assert data == b''
    assert schema == OrderedDict()


@pytest.mark.parametrize('num_threads', [1, 3])
@pytest.mark.asyncio
async def test_key_clash(num_threads):
    torch.manual_seed(12345)
    batch_size = 3
    predictions_count = 4
    predictions = [
        PredictionUnit(
            keys={
                "key": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.uint8),
            },
            predictions={
                "key": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.float32) * 0.1,
                "double": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.float64) * 0.01,
            }
        )
        for i in range(predictions_count)
    ]

    formatter = PredictionsFormatter(num_threads)
    with pytest.raises(ValueError):
        await formatter(
            all_predictions=predictions,
            cast_to_unsigned={"key": False}
        )


@pytest.mark.parametrize('num_threads', [1])
@pytest.mark.asyncio
async def test_format_scalar_predictions(num_threads):
    torch.manual_seed(12345)
    batch_size = 3
    predictions_count = 4
    predictions = [
        PredictionUnit(
            keys={
                "key_uint8": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.uint8),
                "key_int8": torch.tensor([-x for x in range(i * batch_size, (i + 1) * batch_size)], dtype=torch.int8),

                "key_int16": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int16) * (-10),
                "key_int32": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int32) * (-100),
                "key_int64": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int64) * (-1000),

                "key_int16_casted": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int16) * (-10),
                "key_int32_casted": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int32) * (-100),
                "key_int64_casted": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int64) * (-1000),
            },
            predictions={
                "float": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.float32) * 0.1,
                "double": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.float64) * 0.01,

                "int8": torch.tensor([-x for x in range(i * batch_size, (i + 1) * batch_size)], dtype=torch.int8),
                "uint8": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.uint8),

                "int16": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int16) * 10,
                "int32": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int32) * 100,
                "int64": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int64) * 1000,
            }
        )
        for i in range(predictions_count)
    ]

    def _abscast(value, pow):
        if value == 0:
            return value
        return 2 ** pow - int(value)

    reference_rows = {
        "float": [round(x * 0.1, 5) for x in range(predictions_count * batch_size)],
        "double": [round(x * 0.01, 5) for x in range(predictions_count * batch_size)],
        "int8": [-x for x in range(predictions_count * batch_size)],
        "uint8": list(range(predictions_count * batch_size)),
        "int16": [int(x * 10) for x in range(predictions_count * batch_size)],
        "int32": [int(x * 100) for x in range(predictions_count * batch_size)],
        "int64": [int(x * 1000) for x in range(predictions_count * batch_size)],

        "key_int8": [-x for x in range(predictions_count * batch_size)],
        "key_uint8": list(range(predictions_count * batch_size)),
        "key_int16": [-int(x * 10) for x in range(predictions_count * batch_size)],
        "key_int32": [-int(x * 100) for x in range(predictions_count * batch_size)],
        "key_int64": [-int(x * 1000) for x in range(predictions_count * batch_size)],
        "key_int16_casted": [_abscast(x * 10, pow=16) for x in range(predictions_count * batch_size)],
        "key_int32_casted": [_abscast(x * 100, pow=32) for x in range(predictions_count * batch_size)],
        "key_int64_casted": [_abscast(x * 1000, pow=64) for x in range(predictions_count * batch_size)],
    }

    formatter = PredictionsFormatter(num_threads)
    res = await formatter(
        all_predictions=predictions,
        cast_to_unsigned={
            "key_int8": False,
            "key_uint8": False,
            "key_int16": False,
            "key_int32": False,
            "key_int64": False,
            "key_int16_casted": True,
            "key_int32_casted": True,
            "key_int64_casted": True,
        }
    )
    # check yt types
    assert dict(res[1]) == {
        # for floats, we have only double at yt
        "float": "double",
        "double": "double",
        # For all ints, we have corresponding yt types
        "int8": "int8",
        "uint8": "uint8",
        "int16": "int16",
        "int32": "int32",
        "int64": "int64",

        "key_int8": "int8",
        "key_uint8": "uint8",

        "key_int16": "int16",
        "key_int32": "int32",
        "key_int64": "int64",

        # for casted types, we might have unsigned yt types
        "key_int16_casted": "uint16",
        "key_int32_casted": "uint32",
        "key_int64_casted": "uint64",
    }

    prediction_data = bytes(res[0]).decode('utf-8')
    lines = [x for x in prediction_data.split("\n") if len(x)]
    res_data = {}
    for line in lines:
        for key, value in zip(res[1].keys(), line.split("\t")):
            value = value.strip()
            if key in {"float", "double"}:
                value = round(float(value), 5)
            else:
                value = int(value)
            res_data.setdefault(key, []).append(value)

    assert res_data == reference_rows


@pytest.mark.parametrize('num_threads', [1])
@pytest.mark.asyncio
async def test_format_single_tensor_prediction(num_threads):
    # This is test on user api: user are allowed to output not a dict, but a tensor
    # in a simplest case

    torch.manual_seed(12345)
    batch_size = 3
    predictions_count = 4
    predictions = [
        PredictionUnit(
            keys={
                "key": torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.int64) * (-1000),
            },
            predictions=torch.tensor(list(range(i * batch_size, (i + 1) * batch_size)), dtype=torch.float64) * 0.01,
        )
        for i in range(predictions_count)
    ]

    reference_rows = {
        "prediction": [round(x * 0.01, 5) for x in range(predictions_count * batch_size)],
        "key": [-int(x * 1000) for x in range(predictions_count * batch_size)],
    }

    formatter = PredictionsFormatter(num_threads)
    res = await formatter(
        all_predictions=predictions,
        cast_to_unsigned={"key": False}
    )

    assert dict(res[1]) == {
        "prediction": "double",
        "key": "int64",
    }

    prediction_data = bytes(res[0]).decode('utf-8')
    lines = [x for x in prediction_data.split("\n") if len(x)]
    res_data = {}
    for line in lines:
        for key, value in zip(res[1].keys(), line.split("\t")):
            value = value.strip()
            value = round(float(value), 5) if key == "prediction" else int(value)
            res_data.setdefault(key, []).append(value)

    assert res_data == reference_rows


def test_yt_formatter_name():
    formatter = PredictionsFormatter(1)
    assert formatter.name == "Base64RawFormatter"


@pytest.mark.parametrize(
    "prediction",
    [
        PredictionUnit(
            keys={"key": torch.tensor(1, dtype=torch.int64)},
            predictions=torch.tensor(1, dtype=torch.float64) * 0.01,
        ),
        PredictionUnit(
            keys={"key": torch.tensor([1], dtype=torch.int64)},
            predictions=torch.tensor(1, dtype=torch.float64) * 0.01,
        ),
        PredictionUnit(
            keys={"key": torch.tensor(1, dtype=torch.int64)},
            predictions=torch.tensor([1], dtype=torch.float64) * 0.01,
        ),
        PredictionUnit(
            keys={"key": torch.tensor([1], dtype=torch.int64)},
            predictions=torch.tensor([1], dtype=torch.float64) * 0.01,
        )
    ]
)
@pytest.mark.parametrize('num_threads', [1])
@pytest.mark.asyncio
async def test_format_predictions_with_zero_sized_tensors(num_threads, prediction):
    torch.manual_seed(12345)
    formatter = PredictionsFormatter(num_threads)
    predictions = [prediction]
    await formatter(
        all_predictions=predictions,
        cast_to_unsigned={"key": False}
    )
