from __future__ import unicode_literals

import mock

from yp_proto.yp.client.hq.proto import types_pb2
from instancectl.hq.volumes import its
from sepelib.util import fs


def test_its_volume_create_symlink(tmpdir):
    controls_path = tmpdir.join('real-controls')
    work_dir = tmpdir.join('fake-work-dir')
    work_dir.mkdir()
    volume = types_pb2.Volume(name='controls')
    controls_work_dir_path = work_dir.join('controls')
    plugin = its.ItsVolumePlugin(
        service_id='fake-service',
        auto_tags=[],
        first_poll_timeout=0,
        shared_storage=None,
        controls_real_path=controls_path.strpath,
    )

    def _make_mock(*args, **kwargs):
        return mock.Mock()

    plugin._make_poller = _make_mock
    plugin.setup(work_dir=work_dir.strpath,
                 v=volume)
    assert controls_path.isdir()
    assert controls_work_dir_path.islink()
    assert controls_work_dir_path.readlink() == controls_path.strpath
    fs.atomic_write(file_path=controls_work_dir_path.join('fake-control').strpath,
                    contents='fake-contents')
    assert controls_path.join('fake-control').read() == 'fake-contents'

    # Run setup once again to ensure it's idempotent
    plugin.setup(work_dir=work_dir.strpath,
                 v=volume)
    assert controls_path.isdir()
    assert controls_work_dir_path.islink()
    assert controls_work_dir_path.readlink() == controls_path.strpath
