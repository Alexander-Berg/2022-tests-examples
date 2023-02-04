from __future__ import unicode_literals

import yp.client
import yp.data_model
from infra.release_status_controller.tests.helpers import helpers


def test_committed_ticket_success(yp_env):
    rule = helpers.make_release_rule()
    release = helpers.make_sandbox_release()
    project = helpers.make_project()
    stage = helpers.make_stage()
    helpers.set_deploy_unit_ready(stage=stage, deploy_unit_id=helpers.DEFAULT_DEPLOY_UNIT_ID)
    patch1 = helpers.make_static_resource_sandbox_patch()
    patch2 = helpers.make_static_layer_sandbox_patch()
    ticket = helpers.make_deploy_ticket(patches={'patch-1': patch1, 'patch-2': patch2})
    helpers.set_deploy_ticket_committed(ticket)

    yp_env.create_object(yp.data_model.OT_PROJECT, attributes=project)
    release_id = yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release)
    helpers.create_stage(yp_env, stage)
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE, attributes=rule)
    ticket_id = yp_env.create_object(yp.data_model.OT_DEPLOY_TICKET, attributes=ticket)

    ctl = helpers.make_controller(yp_env)
    ctl.resync_storages()
    ctl.process(release)

    release = yp_env.get_object(object_type=yp.data_model.OT_RELEASE,
                                object_identity=release_id,
                                selectors=[''])[0]
    ticket = yp_env.get_object(object_type=yp.data_model.OT_DEPLOY_TICKET,
                               object_identity=ticket_id,
                               selectors=[''])[0]
    assert release['status']['progress']['closed']['status'] == 'true'
    assert ticket['status']['progress']['closed']['status'] == 'true'
    for p in ticket['status']['patches'].values():
        assert p['progress']['success']['status'] == 'true'


def test_partially_committed_ticket_success(yp_env):
    rule = helpers.make_release_rule()
    release = helpers.make_sandbox_release()
    project = helpers.make_project()
    stage = helpers.make_stage()
    helpers.set_deploy_unit_ready(stage=stage, deploy_unit_id=helpers.DEFAULT_DEPLOY_UNIT_ID)
    patch1 = helpers.make_static_resource_sandbox_patch()
    patch2 = helpers.make_static_layer_sandbox_patch()
    ticket = helpers.make_deploy_ticket(patches={'patch-1': patch1, 'patch-2': patch2})
    helpers.set_deploy_ticket_partially_committed(ticket, ['patch-1'])

    yp_env.create_object(yp.data_model.OT_PROJECT, attributes=project)
    release_id = yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release)
    helpers.create_stage(yp_env, stage)
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE, attributes=rule)
    ticket_id = yp_env.create_object(yp.data_model.OT_DEPLOY_TICKET, attributes=ticket)
    ctl = helpers.make_controller(yp_env)
    ctl.resync_storages()
    ctl.process(release)

    release = yp_env.get_object(object_type=yp.data_model.OT_RELEASE,
                                object_identity=release_id,
                                selectors=[''])[0]
    ticket = yp_env.get_object(object_type=yp.data_model.OT_DEPLOY_TICKET,
                               object_identity=ticket_id,
                               selectors=[''])[0]
    assert release['status']['progress']['in_progress']['status'] == 'true'
    assert ticket['status']['progress']['in_progress']['status'] == 'true'
    assert ticket['status']['patches']['patch-1']['progress']['success']['status'] == 'true'
    assert not ticket['status']['patches']['patch-2']['progress']


def test_committed_ticket_cancel(yp_env):
    rule = helpers.make_release_rule()
    release = helpers.make_sandbox_release()
    unit = helpers.make_deploy_unit(resource_url='updated-skynet-id')
    project = helpers.make_project()
    stage = helpers.make_stage(deploy_unit=unit)
    helpers.set_deploy_unit_ready(stage=stage,
                                  deploy_unit_id=helpers.DEFAULT_DEPLOY_UNIT_ID)
    patch = helpers.make_static_resource_sandbox_patch()
    ticket = helpers.make_deploy_ticket(patches={'patch': patch})
    helpers.set_deploy_ticket_committed(ticket)

    yp_env.create_object(yp.data_model.OT_PROJECT, attributes=project)
    release_id = yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release)
    helpers.create_stage(yp_env, stage)
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE, attributes=rule)
    ticket_id = yp_env.create_object(yp.data_model.OT_DEPLOY_TICKET, attributes=ticket)

    ctl = helpers.make_controller(yp_env)
    ctl.resync_storages()
    ctl.process(release)

    release = yp_env.get_object(object_type=yp.data_model.OT_RELEASE,
                                object_identity=release_id,
                                selectors=[''])[0]
    ticket = yp_env.get_object(object_type=yp.data_model.OT_DEPLOY_TICKET,
                               object_identity=ticket_id,
                               selectors=[''])[0]
    assert release['status']['progress']['closed']['status'] == 'true'
    assert ticket['status']['progress']['closed']['status'] == 'true'
    assert ticket['status']['patches']['patch']['progress']['cancelled']['status'] == 'true'


def test_committed_ticket_with_attr_patches_fail(yp_env):
    rule = helpers.make_release_rule()
    release = helpers.make_sandbox_release()
    project = helpers.make_project()
    stage = helpers.make_stage()
    helpers.set_deploy_unit_ready(stage=stage, deploy_unit_id=helpers.DEFAULT_DEPLOY_UNIT_ID)
    patch1 = helpers.make_static_resource_sandbox_patch(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)
    patch2 = helpers.make_static_resource_sandbox_patch(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)
    ticket = helpers.make_deploy_ticket(patches={'patch-1': patch1, 'patch-2': patch2})
    helpers.set_deploy_ticket_committed(ticket)

    yp_env.create_object(yp.data_model.OT_PROJECT, attributes=project)
    release_id = yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release)
    helpers.create_stage(yp_env, stage)
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE, attributes=rule)
    ticket_id = yp_env.create_object(yp.data_model.OT_DEPLOY_TICKET, attributes=ticket)

    ctl = helpers.make_controller(yp_env)
    ctl.resync_storages()
    ctl.process(release)

    release = yp_env.get_object(object_type=yp.data_model.OT_RELEASE,
                                object_identity=release_id,
                                selectors=[''])[0]
    ticket = yp_env.get_object(object_type=yp.data_model.OT_DEPLOY_TICKET,
                               object_identity=ticket_id,
                               selectors=[''])[0]
    assert release['status']['progress']['closed']['status'] == 'true'
    assert ticket['status']['progress']['closed']['status'] == 'true'
    for p in ticket['status']['patches'].values():
        assert p['progress']['cancelled']['status'] == 'true'


def test_committed_ticket_with_attr_patches_success(yp_env):
    rule = helpers.make_release_rule()
    release = helpers.make_sandbox_release(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)
    project = helpers.make_project()
    stage = helpers.make_stage()
    helpers.set_deploy_unit_ready(stage=stage, deploy_unit_id=helpers.DEFAULT_DEPLOY_UNIT_ID)
    patch1 = helpers.make_static_resource_sandbox_patch(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)
    patch2 = helpers.make_static_resource_sandbox_patch(attrs=helpers.DEFAULT_SANDBOX_RESOURCE_ATTRS)
    ticket = helpers.make_deploy_ticket(patches={'patch-1': patch1, 'patch-2': patch2})
    helpers.set_deploy_ticket_committed(ticket)

    yp_env.create_object(yp.data_model.OT_PROJECT, attributes=project)
    release_id = yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release)
    helpers.create_stage(yp_env, stage)
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE, attributes=rule)
    ticket_id = yp_env.create_object(yp.data_model.OT_DEPLOY_TICKET, attributes=ticket)

    ctl = helpers.make_controller(yp_env)
    ctl.resync_storages()
    ctl.process(release)

    release = yp_env.get_object(object_type=yp.data_model.OT_RELEASE,
                                object_identity=release_id,
                                selectors=[''])[0]
    ticket = yp_env.get_object(object_type=yp.data_model.OT_DEPLOY_TICKET,
                               object_identity=ticket_id,
                               selectors=[''])[0]
    assert release['status']['progress']['closed']['status'] == 'true'
    assert ticket['status']['progress']['closed']['status'] == 'true'
    for p in ticket['status']['patches'].values():
        assert p['progress']['success']['status'] == 'true'
