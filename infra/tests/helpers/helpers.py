from __future__ import unicode_literals

import yp.data_model


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
