from __future__ import unicode_literals

import yp.client
import yp.data_model
from infra.release_controller.src import release_selector
from infra.release_controller.src.lib import pbutil
from infra.release_controller.tests.helpers import helpers


def _process_release(yp_client, release_id):
    processing = yp.data_model.TReleaseProcessing()
    pbutil.set_condition_success(processing.finished, 'OK')
    yp_client.update_release_status_processing(release_id, processing)


def test_release_selector(yp_env):
    release_count = 5
    batch_size = 2
    expected_release_ids = []
    for i in xrange(release_count):
        release_id = 'test-release-{}'.format(i)
        release = helpers.make_sandbox_release_dict(release_id=release_id)
        yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release)
        expected_release_ids.append(release_id)
    yp_client = helpers.make_yp_client(yp_env)
    selector = release_selector.NotProcessedReleaseBatchSelector(
        yp_client=yp_client,
        batch_size=batch_size
    )
    batch_lens = []
    release_ids = []
    while True:
        _, _, b = selector.select_next_batch()
        if not b:
            break
        for r in b:
            _process_release(yp_client, r.meta.id)
            release_ids.append(r.meta.id)
        batch_lens.append(len(b))
    assert [2, 2, 1] == batch_lens
    assert expected_release_ids == release_ids
