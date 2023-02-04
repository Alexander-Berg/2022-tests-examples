from __future__ import unicode_literals

import yp.data_model
from infra.release_status_controller.src import deploy_progress_maker
from infra.release_status_controller.src.lib import pbutil
from infra.release_status_controller.tests.helpers import helpers


def test_deploy_progress_maker():
    du_id1 = 'du1'
    du_id2 = 'du2'
    du_id3 = 'du3'

    stage = helpers.make_stage(deploy_unit_id=du_id1)
    helpers.set_deploy_unit_in_progress(stage, du_id1)
    helpers.set_deploy_unit_cluster_ready(stage, du_id1, 'sas')
    helpers.set_deploy_unit_cluster_in_progress(stage, du_id1, 'vla')

    stage.spec.deploy_units[du_id2].CopyFrom(helpers.make_deploy_unit())
    helpers.set_deploy_unit_ready(stage, du_id2)
    helpers.set_deploy_unit_cluster_ready(stage, du_id2, 'vla')
    helpers.set_deploy_unit_cluster_ready(stage, du_id2, 'man')

    stage.spec.deploy_units[du_id3].CopyFrom(helpers.make_deploy_unit())
    helpers.set_deploy_unit_ready(stage, du_id3)
    helpers.set_deploy_unit_cluster_ready(stage, du_id3, 'vla')
    helpers.set_deploy_unit_cluster_ready(stage, du_id3, 'man')

    ticket = helpers.make_deploy_ticket(patches={
        'patch-1': helpers.make_static_resource_sandbox_patch(deploy_unit_id=du_id1),
        'patch-2': helpers.make_static_resource_sandbox_patch(deploy_unit_id=du_id2),
    })

    maker = deploy_progress_maker.DeployProgressMaker()

    status = yp.data_model.TDeployTicketStatus()
    maker.make_deploy_progress(stage, ticket, status)

    assert set(status.progress.cluster_statuses.iterkeys()) == {'sas', 'man', 'vla'}
    assert pbutil.is_condition_true(status.progress.cluster_statuses['sas'].in_progress) is False
    assert pbutil.is_condition_true(status.progress.cluster_statuses['sas'].ready) is True
    assert status.progress.cluster_statuses['sas'].progress.pods_ready == 100
    assert status.progress.cluster_statuses['sas'].progress.pods_total == 100

    assert pbutil.is_condition_true(status.progress.cluster_statuses['man'].in_progress) is False
    assert pbutil.is_condition_true(status.progress.cluster_statuses['man'].ready) is True
    assert status.progress.cluster_statuses['man'].progress.pods_ready == 100
    assert status.progress.cluster_statuses['man'].progress.pods_total == 100

    assert pbutil.is_condition_true(status.progress.cluster_statuses['vla'].in_progress) is True
    assert pbutil.is_condition_true(status.progress.cluster_statuses['vla'].ready) is False
    assert status.progress.cluster_statuses['vla'].progress.pods_ready == 100
    assert status.progress.cluster_statuses['vla'].progress.pods_in_progress == 100
    assert status.progress.cluster_statuses['vla'].progress.pods_total == 200

    status = yp.data_model.TDeployTicketStatus()
    helpers.set_deploy_patch_cancelled(status, 'patch-1')
    maker.make_deploy_progress(stage, ticket, status)
    assert set(status.progress.cluster_statuses.iterkeys()) == set()
