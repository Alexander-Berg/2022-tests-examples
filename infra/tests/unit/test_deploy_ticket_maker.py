from __future__ import unicode_literals

import yp.data_model

from infra.release_controller.src import deploy_ticket_maker
from infra.release_controller.tests.helpers import helpers


def test_deploy_ticket_maker():
    maker = deploy_ticket_maker.DeployTicketMaker()
    empty_status_patch = yp.data_model.TDeployPatchStatus()

    # Case 1: sandbox release and sandbox rule
    release = helpers.make_sandbox_release_pb(release_id='release')
    rule = helpers.make_sandbox_release_rule_pb(release_rule_id='rule')
    stage = yp.data_model.TStage()
    ticket = maker.make_deploy_ticket(release=release,
                                      release_rule=rule,
                                      stage=stage)

    assert ticket.spec.release_id == release.meta.id
    assert ticket.spec.release_rule_id == rule.meta.id
    assert ticket.meta.stage_id == rule.meta.stage_id

    assert len(ticket.spec.patches) == len(rule.spec.patches)
    assert len(ticket.status.patches) == len(rule.spec.patches)
    for name, p in rule.spec.patches.iteritems():
        spec_patch = ticket.spec.patches[name]
        status_patch = ticket.status.patches[name]
        assert spec_patch == p
        assert status_patch == empty_status_patch

    # Case 2: legacy docker release, legacy docker rule
    rule = helpers.make_docker_release_rule_pb(release_rule_id='rule')
    del rule.spec.docker.images[:]
    release = helpers.make_docker_release_pb(release_id='release')
    del release.spec.docker.images[:]
    stage = yp.data_model.TStage()
    ticket = maker.make_deploy_ticket(release=release,
                                      release_rule=rule,
                                      stage=stage)

    assert ticket.spec.release_id == release.meta.id
    assert ticket.spec.release_rule_id == rule.meta.id
    assert ticket.meta.stage_id == rule.meta.stage_id

    assert len(ticket.spec.patches) == len(rule.spec.patches)
    assert len(ticket.status.patches) == len(rule.spec.patches)
    for name, p in rule.spec.patches.iteritems():
        spec_patch = ticket.spec.patches[name]
        status_patch = ticket.status.patches[name]
        p.docker.image_name = rule.spec.docker.image_name
        assert spec_patch == p
        assert status_patch == empty_status_patch

    # Case 3: new docker release, new docker rule
    rule = helpers.make_docker_release_rule_pb(release_rule_id='rule')
    release = helpers.make_docker_release_pb(release_id='release')
    stage = yp.data_model.TStage()
    ticket = maker.make_deploy_ticket(release=release,
                                      release_rule=rule,
                                      stage=stage)

    assert ticket.spec.release_id == release.meta.id
    assert ticket.spec.release_rule_id == rule.meta.id
    assert ticket.meta.stage_id == rule.meta.stage_id

    assert len(ticket.spec.patches) == len(rule.spec.patches)
    assert len(ticket.status.patches) == len(rule.spec.patches)
    for name, p in rule.spec.patches.iteritems():
        spec_patch = ticket.spec.patches[name]
        status_patch = ticket.status.patches[name]
        assert spec_patch == p
        assert status_patch == empty_status_patch

    # Case 4: new docker release, legacy docker rule
    rule = helpers.make_docker_release_rule_pb(release_rule_id='rule')
    del rule.spec.docker.images[:]
    release = helpers.make_docker_release_pb(release_id='release')
    stage = yp.data_model.TStage()
    ticket = maker.make_deploy_ticket(release=release,
                                      release_rule=rule,
                                      stage=stage)

    assert ticket.spec.release_id == release.meta.id
    assert ticket.spec.release_rule_id == rule.meta.id
    assert ticket.meta.stage_id == rule.meta.stage_id

    assert len(ticket.spec.patches) == len(rule.spec.patches)
    assert len(ticket.status.patches) == len(rule.spec.patches)
    for name, p in rule.spec.patches.iteritems():
        spec_patch = ticket.spec.patches[name]
        status_patch = ticket.status.patches[name]
        p.docker.image_name = rule.spec.docker.image_name
        assert spec_patch == p
        assert status_patch == empty_status_patch
