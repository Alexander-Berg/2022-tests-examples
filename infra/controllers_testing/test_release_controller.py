import copy
import json
import pytest

import infra.callisto.controllers.release as releases


@pytest.mark.skip('Dirty test: goes to YP, uses knowledge of internal structure. Use it only locally.')
def test_current_behavior():
    ctrl = releases._make_test_stage_controller(readonly=True)  # noqa

    set_updates = ctrl.update_stage(RESOURCES)
    assert not set_updates  # Assume that nothing is changing right now.

    # Change static resource.
    RESOURCES.append(releases.ResourceSpec('models.archive', '12345', None))
    set_updates = ctrl.update_stage(RESOURCES)
    assert set_updates[0][1]['static_resources'][0]['url'] == '12345'

    # Change layer.
    RESOURCES.append(releases.ResourceSpec('generator.configs.tar.gz', '6789', None))
    set_updates = ctrl.update_stage(RESOURCES)
    assert set_updates[0][1]['static_resources'][0]['url'] == '12345'
    assert set_updates[0][1]['layers'][-1]['url'] == '6789'

    # Change non-existent resource.
    RESOURCES.append(releases.ResourceSpec('non-existent', 'whatever', None))
    set_updates_1 = ctrl.update_stage(RESOURCES)
    assert set_updates_1 == set_updates

    # Change existing but not synchronized resource.
    RESOURCES.append(releases.ResourceSpec('dru_layer', 'whatever', None))
    set_updates_1 = ctrl.update_stage(RESOURCES)
    assert set_updates_1 == set_updates


def test_parse_nanny_status():
    resources = releases.parse_nanny_status(json.dumps(NANNY_RESOURCES))
    assert set(NANNY_RESOURCES.keys()) == {resource.name for resource in resources}
    for resource in resources:
        assert resource.url == NANNY_RESOURCES[resource.name]['skynet_id']


def test_is_ready():
    deploy_policy, spec_revision, target_revision, ready_status, progress = [
        {'sas': {'pod_count': 162L, 'deployment_strategy': {'max_unavailable': 16L, 'max_tolerable_downtime_pods': 8L}}},
        187L,
        187L,
        {'status': 'true', 'last_transition_time': {'seconds': 1621853618L, 'nanos': 695388000L}},
        {'pods_in_progress': 1L, 'pods_total': 162L, 'pods_ready': 161L}
    ]
    assert releases.ReleaseController.is_ready(
        deploy_policy, spec_revision, target_revision, ready_status, progress
    )

    assert not releases.ReleaseController.is_ready(
        deploy_policy, spec_revision + 1, target_revision, ready_status, progress
    )

    assert not releases.ReleaseController.is_ready(
        deploy_policy, spec_revision, target_revision, {'status': 'false'}, progress
    )

    in_progress = {'pods_in_progress': 20L, 'pods_total': 162L, 'pods_ready': 142L}
    assert not releases.ReleaseController.is_ready(
        deploy_policy, spec_revision, target_revision, ready_status, in_progress
    )


def test_resource_patch_builder():
    spec = releases.ResourcesPatchBuilder()

    spec.fill({'platinum-base': [copy.deepcopy(PLATINUM)], 'tier1-base': [copy.deepcopy(TIER1)]})
    assert not list(spec.get_updates())

    # Change static resource.
    spec.update_resource('platinum-base', 'models.archive', '12345')
    assert spec.changeset == {'platinum-base': set(['models.archive'])}
    spec.update_resource('tier1-base', 'models.archive', '12345')
    assert spec.changeset == {'platinum-base': set(['models.archive']),
                              'tier1-base': set(['models.archive'])}

    # Change layer.
    spec.update_resource('platinum-base', 'generator.configs.tar.gz', '67890')
    assert spec.changeset == {'platinum-base': set(['models.archive', 'generator.configs.tar.gz']),
                              'tier1-base': set(['models.archive'])}

    # Change non-existent resource.
    spec.update_resource('platinum-base', 'non-existent', 'whatever')
    assert spec.changeset == {'platinum-base': set(['models.archive', 'generator.configs.tar.gz']),
                              'tier1-base': set(['models.archive'])}

    # How to test existing but not synchronized resource?

    PLATINUM['layers'][0]['url'] = '67890'
    PLATINUM['static_resources'][0]['url'] = '12345'
    TIER1['static_resources'][0]['url'] = '12345'
    assert any(PLATINUM == r[1] for r in spec.get_updates())
    assert any(TIER1 == r[1] for r in spec.get_updates())


NANNY_RESOURCES = {'models.archive': {'skynet_id': 'rbtorrent:80775cd533ee16ed7dfcdf8e080640dc988f56e9'},
                   'vars.conf': {'skynet_id': 'rbtorrent:4cc129c365d37ffc6029d08b88e50f912554b4ee'},
                   'generator.configs.tar.gz': {'skynet_id': 'rbtorrent:ebc3b061591ff76933da74d5f7995de725fc476c'},
                   'remote_storage_configs.tar.gz': {'skynet_id': 'rbtorrent:99025061b48f67578455fb7086a06d6a12da8f73'},
                   'httpsearch': {'skynet_id': 'rbtorrent:db2ad753f0352f4deedff08c543f586b6494062e'}}

RESOURCES = releases.parse_nanny_status(json.dumps(NANNY_RESOURCES))

PLATINUM = {
    'layers': [{'id': 'generator.configs.tar.gz', 'url': 'old_url_1'}],
    'static_resources': [{'id': 'models.archive', 'url': 'old_url_2'}]
}

TIER1 = {
    'layers': [],
    'static_resources': [{'id': 'models.archive', 'url': 'old_url_2'}]
}
