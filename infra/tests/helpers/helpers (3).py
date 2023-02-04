from __future__ import unicode_literals

import yp.data_model

from infra.release_status_controller.src import controller
from infra.release_status_controller.src import indexers
from infra.release_status_controller.src import deploy_progress_maker
from infra.release_status_controller.src import patch_applied_cache
from infra.release_status_controller.src import patch_applied_checker
from infra.release_status_controller.src import patch_progress_maker
from infra.release_status_controller.src import release_status_updater
from infra.release_status_controller.src import ticket_status_maker
from infra.release_status_controller.src.lib import storage
from infra.release_status_controller.src.lib import yp_client


DEFAULT_DEPLOY_TICKET_ID = 'test-deploy-ticket'

DEFAULT_RESOURCE_ID = 'test-resource-id'
DEFAULT_LAYER_ID = 'test-layer-id'

DEFAULT_BOX_ID = 'test-box'
DEFAULT_DEPLOY_UNIT_ID = 'test-deploy-unit'

DEFAULT_DYNAMIC_RESOURCE_ID = 'test-dyn-res'
DEFAULT_DYNAMIC_RESOURCE_GROUP_MARK = 'test-group-mark'

DEFAULT_RELEASE_ID = 'test-release'
DEFAULT_RELEASE_RULE_ID = 'test-release-rule'
DEFAULT_RELEASE_TYPE = 'testing'
DEFAULT_SKYNET_ID = 'test-skynet-id'
DEFAULT_STAGE_ID = 'test-stage'
DEFAULT_PROJECT_ID = 'test-project'

DEFAULT_DOCKER_IMAGE_HASH = 'test-image-hash'
DEFAULT_DOCKER_IMAGE_NAME = 'test-image-name'
DEFAULT_DOCKER_IMAGE_TAG = 'test-image-tag'

DEFAULT_SANDBOX_RESOURCE_TYPE = 'test-resource-type'
DEFAULT_SANDBOX_TASK_ID = 'test-task-id'
DEFAULT_SANDBOX_TASK_TYPE = 'test-task-type'
DEFAULT_SANDBOX_RESOURCE_ATTRS = {
    'test-resource-attr-key1': 'test-resource-attr-val1',
    'test-resource-attr-key2': 'test-resource-attr-val2',
}


def make_default_acl():
    entry = yp.data_model.TAccessControlEntry()
    entry.action = yp.data_model.ACA_ALLOW
    entry.subjects.extend(['test'])
    entry.permissions.extend([yp.data_model.ACA_WRITE])
    return [entry]


DEFAULT_ACL = make_default_acl()


def make_release_rule():
    rule = yp.data_model.TReleaseRule()
    rule.meta.id = DEFAULT_RELEASE_RULE_ID
    rule.meta.acl.MergeFrom(DEFAULT_ACL)
    rule.meta.stage_id = DEFAULT_STAGE_ID
    rule.spec.sandbox.task_type = DEFAULT_SANDBOX_TASK_TYPE
    rule.spec.patches['my-patch'].CopyFrom(make_static_resource_sandbox_patch())
    return rule


def make_sandbox_release(release_id=DEFAULT_RELEASE_ID,
                         skynet_id=DEFAULT_SKYNET_ID,
                         attrs=None):
    release = yp.data_model.TRelease()
    release.meta.id = release_id
    release.meta.acl.MergeFrom(DEFAULT_ACL)
    sb = release.spec.sandbox
    sb.task_type = DEFAULT_SANDBOX_TASK_TYPE
    sb.task_id = DEFAULT_SANDBOX_TASK_ID
    sb.release_type = DEFAULT_RELEASE_TYPE
    res = sb.resources.add()
    res.type = DEFAULT_SANDBOX_RESOURCE_TYPE
    res.skynet_id = skynet_id
    if attrs:
        for k, v in attrs.items():
            res.attributes[k] = v
    return release


def make_multi_docker_release(release_id=DEFAULT_RELEASE_ID):
    release = yp.data_model.TRelease()
    release.meta.id = release_id
    release.meta.acl.MergeFrom(DEFAULT_ACL)
    d = release.spec.docker
    img = d.images.add()
    img.name = DEFAULT_DOCKER_IMAGE_NAME
    img.tag = DEFAULT_DOCKER_IMAGE_TAG
    img.digest = DEFAULT_DOCKER_IMAGE_HASH
    d.release_type = DEFAULT_RELEASE_TYPE
    return release


def make_docker_release(release_id=DEFAULT_RELEASE_ID):
    release = yp.data_model.TRelease()
    release.meta.id = release_id
    release.meta.acl.MergeFrom(DEFAULT_ACL)
    d = release.spec.docker
    d.image_name = DEFAULT_DOCKER_IMAGE_NAME
    d.image_tag = DEFAULT_DOCKER_IMAGE_TAG
    d.image_hash = DEFAULT_DOCKER_IMAGE_HASH
    d.release_type = DEFAULT_RELEASE_TYPE
    return release


def make_static_resource_sandbox_patch(sandbox_resource_type=DEFAULT_SANDBOX_RESOURCE_TYPE,
                                       deploy_unit_id=DEFAULT_DEPLOY_UNIT_ID,
                                       static_resource_ref=DEFAULT_RESOURCE_ID,
                                       attrs=None):
    patch = yp.data_model.TDeployPatchSpec()
    sb = patch.sandbox
    sb.sandbox_resource_type = sandbox_resource_type
    sb.static.deploy_unit_id = deploy_unit_id
    sb.static.static_resource_ref = static_resource_ref
    if attrs:
        for k, v in attrs.items():
            sb.attributes[k] = v
    return patch


def make_static_layer_sandbox_patch(sandbox_resource_type=DEFAULT_SANDBOX_RESOURCE_TYPE,
                                    deploy_unit_id=DEFAULT_DEPLOY_UNIT_ID,
                                    layer_ref=DEFAULT_LAYER_ID):
    patch = yp.data_model.TDeployPatchSpec()
    sb = patch.sandbox
    sb.sandbox_resource_type = sandbox_resource_type
    sb.static.deploy_unit_id = deploy_unit_id
    sb.static.layer_ref = layer_ref
    return patch


def make_dynamic_resource_sandbox_patch(sandbox_resource_type=DEFAULT_SANDBOX_RESOURCE_TYPE,
                                        dynamic_resource_id=DEFAULT_DYNAMIC_RESOURCE_ID):
    patch = yp.data_model.TDeployPatchSpec()
    sb = patch.sandbox
    sb.sandbox_resource_type = sandbox_resource_type
    sb.dynamic.dynamic_resource_id = dynamic_resource_id
    sb.dynamic.deploy_group_mark = DEFAULT_DYNAMIC_RESOURCE_GROUP_MARK
    return patch


def make_docker_patch(deploy_unit_id=DEFAULT_DEPLOY_UNIT_ID,
                      box_id=DEFAULT_BOX_ID):
    patch = yp.data_model.TDeployPatchSpec()
    d = patch.docker.docker_image_ref
    d.deploy_unit_id = deploy_unit_id
    d.box_id = box_id
    return patch


def make_committed_patch_status():
    s = yp.data_model.TDeployPatchStatus()
    s.action.type = yp.data_model.DPAT_COMMIT
    return s


def make_skipped_patch_status():
    s = yp.data_model.TDeployPatchStatus()
    s.action.type = yp.data_model.DPAT_SKIP
    return s


def make_deploy_ticket(patches,
                       ticket_id=DEFAULT_DEPLOY_TICKET_ID,
                       release_id=DEFAULT_RELEASE_ID):
    ticket = yp.data_model.TDeployTicket()
    ticket.meta.id = ticket_id
    ticket.meta.acl.MergeFrom(DEFAULT_ACL)
    ticket.meta.stage_id = DEFAULT_STAGE_ID
    ticket.spec.release_id = release_id
    ticket.spec.release_rule_id = DEFAULT_RELEASE_RULE_ID
    empty_patch_status = yp.data_model.TDeployPatchStatus()
    for patch_name, patch in patches.iteritems():
        ticket.spec.patches[patch_name].CopyFrom(patch)
        ticket.status.patches[patch_name].CopyFrom(empty_patch_status)
    return ticket


def make_deploy_unit(resource_id=DEFAULT_RESOURCE_ID,
                     resource_url=DEFAULT_SKYNET_ID,
                     layer_id=DEFAULT_LAYER_ID,
                     layer_url=DEFAULT_SKYNET_ID,
                     docker_image_name=DEFAULT_DOCKER_IMAGE_NAME,
                     docker_image_tag=DEFAULT_DOCKER_IMAGE_TAG):
    unit = yp.data_model.TDeployUnitSpec()
    unit.images_for_boxes[DEFAULT_BOX_ID].name = docker_image_name
    unit.images_for_boxes[DEFAULT_BOX_ID].tag = docker_image_tag
    box = unit.replica_set.replica_set_template.pod_template_spec.spec.pod_agent_payload.spec.boxes.add()
    box.id = DEFAULT_BOX_ID
    resources = unit.replica_set.replica_set_template.pod_template_spec.spec.pod_agent_payload.spec.resources
    sr = resources.static_resources.add()
    sr.id = resource_id
    sr.url = resource_url
    layer = resources.layers.add()
    layer.id = layer_id
    layer.url = layer_url
    return unit


def make_dynamic_resource(revision=1,
                          group_mark=DEFAULT_DYNAMIC_RESOURCE_GROUP_MARK,
                          url=DEFAULT_SKYNET_ID):
    rv = yp.data_model.TStageSpec.TStageDynamicResourceSpec()
    rv.deploy_unit_ref = DEFAULT_DEPLOY_UNIT_ID
    r = rv.dynamic_resource
    r.revision = revision
    g = r.deploy_groups.add()
    g.mark = group_mark
    g.urls.extend([url])
    return rv


def make_project(project_id=DEFAULT_PROJECT_ID):
    project = yp.data_model.TProject()
    project.meta.id = project_id
    project.spec.account_id = 'tmp'
    return project


def make_stage(stage_id=DEFAULT_STAGE_ID,
               project_id=DEFAULT_PROJECT_ID,
               deploy_unit_id=DEFAULT_DEPLOY_UNIT_ID,
               dynamic_resource_id=DEFAULT_DYNAMIC_RESOURCE_ID,
               dynamic_resource=None,
               deploy_unit=None):
    stage = yp.data_model.TStage()
    stage.meta.id = stage_id
    stage.meta.project_id = project_id
    stage.spec.account_id = 'tmp'
    if deploy_unit is None:
        deploy_unit = make_deploy_unit()
    stage.spec.deploy_units[deploy_unit_id].CopyFrom(deploy_unit)
    if dynamic_resource is None:
        dynamic_resource = make_dynamic_resource()
    stage.spec.dynamic_resources[dynamic_resource_id].CopyFrom(dynamic_resource)
    return stage


def create_stage(yp_env, stage):
    stage_id = yp_env.create_object(yp.data_model.OT_STAGE, attributes=stage)
    resp = yp_env.get_object(object_type=yp.data_model.OT_STAGE,
                             object_identity=stage_id,
                             selectors=['/spec'],
                             options={'fetch_timestamps': True},
                             enable_structured_response=True)
    spec_ts = resp['result'][0]['timestamp']
    yp_env.update_object(object_type=yp.data_model.OT_STAGE,
                         object_identity=stage_id,
                         set_updates=[{'path': '/status/spec_timestamp', 'value': spec_ts}])


def set_deploy_unit_ready(stage, deploy_unit_id):
    stage.status.deploy_units[deploy_unit_id].ready.status = yp.data_model.CS_TRUE


def set_deploy_unit_in_progress(stage, deploy_unit_id):
    stage.status.deploy_units[deploy_unit_id].in_progress.status = yp.data_model.CS_TRUE


def set_deploy_unit_cluster_ready(stage, deploy_unit_id, cluster, pod_count=100):
    status = stage.status.deploy_units[deploy_unit_id].replica_set.cluster_statuses[cluster].status
    status.ready_condition.status = yp.data_model.CS_TRUE
    progress = status.deploy_status.details.current_revision_progress
    progress.pods_ready = pod_count
    progress.pods_total = progress.pods_in_progress + progress.pods_ready + progress.pods_failed


def set_deploy_unit_cluster_in_progress(stage, deploy_unit_id, cluster, pod_count=100):
    status = stage.status.deploy_units[deploy_unit_id].replica_set.cluster_statuses[cluster].status
    status.in_progress_condition.status = yp.data_model.CS_TRUE
    progress = status.deploy_status.details.current_revision_progress
    progress.pods_in_progress = pod_count
    progress.pods_total = progress.pods_in_progress + progress.pods_ready + progress.pods_failed


def set_deploy_ticket_committed(ticket):
    ticket.status.action.type = yp.data_model.DPAT_COMMIT
    for name in ticket.spec.patches:
        ticket.status.patches[name].action.type = yp.data_model.DPAT_COMMIT


def set_deploy_ticket_partially_committed(ticket, patch_names):
    for name in patch_names:
        ticket.status.patches[name].action.type = yp.data_model.DPAT_COMMIT


def set_deploy_patch_cancelled(status, patch):
    status.patches[patch].progress.cancelled.status = yp.data_model.CS_TRUE


def make_yp_client(yp_env):
    class DummyThreadPool(object):
        def apply(self, func, args=None, kwargs=None):
            args = args or ()
            kwargs = kwargs or {}
            return func(*args, **kwargs)

    stub = yp_env.create_grpc_object_stub()
    c = yp_client.YpClient(stub=stub)
    c._tp = DummyThreadPool()
    return c


def make_controller(yp_env, release_rule_cache_ttl=None):
    ticket_storage = storage.make_storage(indexers.TICKET_INDEXERS)
    release_storage = storage.make_storage()
    stage_storage = storage.make_storage()

    applied_checker = patch_applied_checker.PatchAppliedChecker()
    applied_cache = patch_applied_cache.PatchAppliedCache()
    progress_maker = patch_progress_maker.PatchProgressMaker(
        applied_checker=applied_checker,
        applied_cache=applied_cache
    )
    status_maker = ticket_status_maker.TicketStatusMaker(
        patch_progress_maker=progress_maker,
        deploy_progress_maker=deploy_progress_maker.DeployProgressMaker(),
        stage_storage=stage_storage
    )
    client = make_yp_client(yp_env)
    status_updater = release_status_updater.ReleaseStatusUpdater(
        yp_client=client,
        ticket_status_maker=status_maker,
        ticket_storage=ticket_storage
    )
    return controller.UpdateStatusController(
        yp_client=client,
        release_status_updater=status_updater,
        ticket_storage=ticket_storage,
        release_storage=release_storage,
        stage_storage=stage_storage
    )
