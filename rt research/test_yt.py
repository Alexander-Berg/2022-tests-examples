import pytest
import platform
import uuid
import irt.utils

try:
    from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
except ImportError as e:
    if platform.system() == 'Linux':
        raise e


def write_data(client, tbl):
    data = [
        {
            "col1": "val1"
        }
    ]

    client.write_table(tbl, data)


@pytest.mark.linux
def test_yt():
    config = YtConfig(wait_tablet_cell_initialization=True)
    yt_stuff = YtStuff(config)
    yt_stuff.start_local_yt()
    client = yt_stuff.get_yt_client()

    src = '//home/' + str(uuid.uuid4())
    dst = src + '_out'

    write_data(client, src)

    assert not irt.utils.is_inside_yt_job()
    assert not irt.utils.is_inside_local_yt_job()
    assert irt.utils.get_current_yt_job_id() is None
    assert irt.utils.get_current_yt_operation_id() is None
    assert irt.utils.get_current_yt_cluster() is None

    def mapper(row):
        import irt.utils
        yield {
            'is_in_yt_job': irt.utils.is_inside_yt_job(),
            'job_id': irt.utils.get_current_yt_job_id(),
            'operation_id': irt.utils.get_current_yt_operation_id(),
            'yt_cluster': irt.utils.get_current_yt_cluster(),
            'local_yt': irt.utils.is_inside_local_yt_job()
        }

    client.run_map(mapper, src, dst)
    data = list(client.read_table(dst))

    assert len(data) == 1
    data = data[0]
    assert len(data) == 5
    assert data['is_in_yt_job']
    assert data['job_id']
    assert data['operation_id']
    assert data['yt_cluster'] is None  # local YT
    assert data['local_yt']
