import numpy as np

from yabs.proto.tsar_pb2 import TTsarVector

from ads.bigkv.torch_v2.dump_getter import TorchV2DumpGetter

from ads.bigkv.tensor_transport.lib.sb_getter import get_new_tensors
from ads.bigkv.tensor_transport.lib.add_new_tensors import add_new_tensors
from ads.bigkv.tensor_transport.lib.utils import tensor_table_to_dict


class MockTorchV2DumpGetter(TorchV2DumpGetter):
    @staticmethod
    def get_sb_info(model_version=None):
        return {'http': {'proxy': 'fake url'}}

    @staticmethod
    def some_tensor():
        return np.arange(3 * 4 * 5, dtype=np.float32)

    @classmethod
    def download_and_unpack_tensor(cls, url=None, tensor_path=None):
        return cls.some_tensor().reshape(3, 4, 5)


def test_torch_v2_dump_getter(local_yt, tensor_table):
    dumps_path = '//home/bs/test_torch_v2_dump_getter_dumps'
    yt_client = local_yt.get_client()

    tmp_table = '//home/bs/test_torch_v2_dump_getter_tensor_table'
    yt_client.copy(tensor_table, tmp_table)
    tensor_table = tmp_table

    add_new_tensors(
        client=yt_client,
        table_path=tensor_table,
        dump_path=dumps_path,
        new_tensors=get_new_tensors([
            MockTorchV2DumpGetter(model_version=0, tensor_version=1234),
        ])
    )

    result = tensor_table_to_dict(yt_client, tensor_table)

    expected_tensor = MockTorchV2DumpGetter.some_tensor()
    real_tensor = TTsarVector()
    real_tensor.ParseFromString(result[1234])
    real_tensor = np.array([x for x in real_tensor.Factors]).reshape(*expected_tensor.shape)

    assert np.allclose(expected_tensor, real_tensor)
