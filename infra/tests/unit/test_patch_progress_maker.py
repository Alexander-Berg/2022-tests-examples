from __future__ import unicode_literals
import mock

import yp.data_model
from infra.release_status_controller.src import patch_progress_maker
from infra.release_status_controller.tests.helpers import helpers


PROGRESS_CONDITION_NAMES = {'pending', 'in_progress', 'success', 'failed', 'cancelled'}


def _assert_progress_conditions(p, true_condition_name):
    if true_condition_name is not None:
        assert true_condition_name in PROGRESS_CONDITION_NAMES
    for n in PROGRESS_CONDITION_NAMES:
        s = getattr(p, n).status
        if true_condition_name is not None and n == true_condition_name:
            assert s == yp.data_model.CS_TRUE
        else:
            assert s == yp.data_model.CS_UNKNOWN


def _get_progress_maker(applied_checker):
    applied_cache = mock.Mock()
    applied_cache.get.return_value = (False, False)
    progress_maker = patch_progress_maker.PatchProgressMaker(applied_checker=applied_checker,
                                                             applied_cache=applied_cache)
    return progress_maker


def _test_patch_progress_maker(patch,
                               stage,
                               is_deploy_ready_condition,
                               is_deploy_in_progress_condition,
                               is_deploy_failed_condition):
    applied_checker = mock.Mock()
    applied_checker.is_patch_applied.return_value = True
    progress_maker = _get_progress_maker(applied_checker)

    stage_spec_ts = 1
    stage.status.spec_timestamp = stage_spec_ts
    release = helpers.make_sandbox_release()
    committed_status = helpers.make_committed_patch_status()
    skipped_status = helpers.make_skipped_patch_status()

    # Case 1: patch is skipped
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=skipped_status,
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts)
    _assert_progress_conditions(p, true_condition_name='cancelled')

    # Case 2: stage_spec_timestamp != stage.status.spec_timestamp
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=committed_status,
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts + 1)
    _assert_progress_conditions(p, true_condition_name=None)

    # Case 3: patch is not committed
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=yp.data_model.TDeployPatchStatus(),
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts)
    _assert_progress_conditions(p, true_condition_name=None)

    # Case 4: patch is not applied (it was rewrite by other stage spec update)
    applied_checker.is_patch_applied.return_value = False
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=committed_status,
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts)
    _assert_progress_conditions(p, true_condition_name='cancelled')
    applied_checker.is_patch_applied.return_value = True

    # Case 5: deploy is failed
    is_deploy_failed_condition.status = yp.data_model.CS_TRUE
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=committed_status,
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts)
    _assert_progress_conditions(p, true_condition_name='failed')
    is_deploy_failed_condition.status = yp.data_model.CS_UNKNOWN

    # Case 6: deploy is in progress
    is_deploy_in_progress_condition.status = yp.data_model.CS_TRUE
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=committed_status,
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts)
    _assert_progress_conditions(p, true_condition_name='in_progress')
    is_deploy_in_progress_condition.status = yp.data_model.CS_UNKNOWN

    # Case 7: deploy is ready
    is_deploy_ready_condition.status = yp.data_model.CS_TRUE
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=committed_status,
                                           release=release,
                                           stage=stage,
                                           stage_spec_ts=stage_spec_ts)
    _assert_progress_conditions(p, true_condition_name='success')
    is_deploy_ready_condition.status = yp.data_model.CS_UNKNOWN


def test_dynamic_resource_patch_progress_maker():
    stage = helpers.make_stage()
    patch = helpers.make_dynamic_resource_sandbox_patch()
    dyn_resource_status = stage.status.dynamic_resources[patch.sandbox.dynamic.dynamic_resource_id].status
    _test_patch_progress_maker(patch=patch,
                               stage=stage,
                               is_deploy_ready_condition=dyn_resource_status.ready.condition,
                               is_deploy_in_progress_condition=dyn_resource_status.in_progress.condition,
                               is_deploy_failed_condition=dyn_resource_status.error.condition)

    # Special case for dyn resources: deploy is in progress and failed at the
    # same time
    dyn_resource_status.ready.condition.status = yp.data_model.CS_FALSE
    dyn_resource_status.in_progress.condition.status = yp.data_model.CS_TRUE
    dyn_resource_status.error.condition.status = yp.data_model.CS_TRUE

    applied_checker = mock.Mock()
    applied_checker.is_patch_applied.return_value = True
    progress_maker = _get_progress_maker(applied_checker)
    p = progress_maker.make_patch_progress(patch_name='test-patch',
                                           patch_spec=patch,
                                           patch_status=helpers.make_committed_patch_status(),
                                           release=helpers.make_sandbox_release(),
                                           stage=stage,
                                           stage_spec_ts=stage.status.spec_timestamp)
    assert p.in_progress.status == yp.data_model.CS_TRUE
    assert p.failed.status == yp.data_model.CS_TRUE
    assert p.pending.status == yp.data_model.CS_UNKNOWN
    assert p.success.status == yp.data_model.CS_UNKNOWN
    assert p.cancelled.status == yp.data_model.CS_UNKNOWN


def test_deploy_unit_patch_progress_maker_sandbox():
    stage = helpers.make_stage()
    patch = helpers.make_static_resource_sandbox_patch()
    unit_status = stage.status.deploy_units[patch.sandbox.static.deploy_unit_id]
    _test_patch_progress_maker(patch=patch,
                               stage=stage,
                               is_deploy_ready_condition=unit_status.ready,
                               is_deploy_in_progress_condition=unit_status.in_progress,
                               is_deploy_failed_condition=unit_status.failed)


def test_deploy_unit_patch_progress_maker_docker():
    stage = helpers.make_stage()
    patch = helpers.make_docker_patch()
    unit_status = stage.status.deploy_units[patch.docker.docker_image_ref.deploy_unit_id]
    _test_patch_progress_maker(patch=patch,
                               stage=stage,
                               is_deploy_ready_condition=unit_status.ready,
                               is_deploy_in_progress_condition=unit_status.in_progress,
                               is_deploy_failed_condition=unit_status.failed)
