from __future__ import unicode_literals

from infra.release_status_controller.src import patch_applied_checker
from infra.release_status_controller.tests.helpers import helpers


def test_static_sandbox():
    c = patch_applied_checker.PatchAppliedChecker()
    stage = helpers.make_stage()
    release = helpers.make_sandbox_release()

    patch = helpers.make_static_resource_sandbox_patch()
    assert c.is_patch_applied(patch, release, stage) is True

    unmatched_patch = helpers.make_static_resource_sandbox_patch(static_resource_ref='missing')
    assert c.is_patch_applied(unmatched_patch, release, stage) is False

    patch = helpers.make_static_layer_sandbox_patch()
    assert c.is_patch_applied(patch, release, stage) is True

    unmatched_patch = helpers.make_static_layer_sandbox_patch(layer_ref='missing')
    assert c.is_patch_applied(unmatched_patch, release, stage) is False


def test_dyn_res_sandbox():
    c = patch_applied_checker.PatchAppliedChecker()
    stage = helpers.make_stage()
    release = helpers.make_sandbox_release()

    patch = helpers.make_dynamic_resource_sandbox_patch()
    assert c.is_patch_applied(patch, release, stage) is True


def test_docker():
    c = patch_applied_checker.PatchAppliedChecker()
    stage = helpers.make_stage()
    release = helpers.make_docker_release()

    patch = helpers.make_docker_patch()
    assert c.is_patch_applied(patch, release, stage) is True

    unmatched_patch = helpers.make_docker_patch(box_id='missing')
    assert c.is_patch_applied(unmatched_patch, release, stage) is False

    release = helpers.make_multi_docker_release()

    patch = helpers.make_docker_patch()
    assert c.is_patch_applied(patch, release, stage) is True

    unmatched_patch = helpers.make_docker_patch(box_id='missing')
    assert c.is_patch_applied(unmatched_patch, release, stage) is False


def test_sandbox_res_with_attrs():
    c = patch_applied_checker.PatchAppliedChecker()
    stage = helpers.make_stage()
    release = helpers.make_sandbox_release(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)

    patch = helpers.make_static_resource_sandbox_patch(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)
    assert c.is_patch_applied(patch, release, stage) is True

    patch_with_partial_attrs = helpers.make_static_resource_sandbox_patch(
        attrs={'test-resource-attr-key1': 'test-resource-attr-val1'}
    )
    assert c.is_patch_applied(patch_with_partial_attrs, release, stage) is True

    patch_with_non_matching_attrs = helpers.make_static_resource_sandbox_patch(
        attrs={'test-resource-attr-non-existing-rand-key': 'test-resource-attr-val'}
    )
    assert c.is_patch_applied(patch_with_non_matching_attrs, release, stage) is False
