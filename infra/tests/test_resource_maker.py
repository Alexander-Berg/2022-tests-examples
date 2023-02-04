import os
import time

from unittest import mock

import yatest

from infra.rtc_sla_tentacles.backend.lib.harvesters.resource_maker import ResourceMaker
from infra.rtc_sla_tentacles.backend.lib.tentacle_agent import const as agent_const


def test_resource_preparation(config_interface, fake_snapshot_manager):
    storage_dir = yatest.common.runtime.work_path('resources')
    h = ResourceMaker(
        "test_maker",
        arguments={
            'storage_dir': storage_dir,
            'keep_copies': 0,
        },
        common_parameters=None,
        common_settings={"update_interval_sec": 300},
        snapshot_manager=fake_snapshot_manager,
        config_interface=config_interface,
        several_harvesters=True,
    )

    ts = int(time.time())
    with mock.patch('subprocess.Popen') as mock_popen:
        proc = mock_popen.return_value
        proc.returncode = 0
        proc.communicate.return_value = (b'rbtorrent:testvalue', b'')

        # Effectively running "sky", "share", '-d' inside tests.
        torrent_id, resource_timestamp = h.extract(ts)

    assert resource_timestamp > ts - h._get_resource_period()
    assert torrent_id == 'rbtorrent:testvalue'

    with open(os.path.join(storage_dir, str(resource_timestamp), agent_const.AGENT_RESOURCE_NAME)) as resource:
        assert resource.read() == str(resource_timestamp)
