import pytest

from .data import get_table_data, create_table


@pytest.fixture(scope="module")
def tensor_table(local_yt):
    yt_client = local_yt.get_client()
    yt_client.create("map_node", '//home/bs', recursive=True, force=True)
    tensor_table_path = '//home/bs/tensor_table'
    create_table(tensor_table_path, get_table_data(), yt_client)

    return tensor_table_path
