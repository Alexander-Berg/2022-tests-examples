import subprocess

import six
import yaml
import yp.data_model as data_model

from infra.dctl.tests.helpers import helpers


def test_get_and_put_stage(dctl_binary, yp_env):
    addr = yp_env['addr']

    local_yp_client = yp_env['client']
    project = helpers.make_project()
    local_yp_client.create_object(data_model.OT_PROJECT, attributes=project)
    helpers.create_stage(local_yp_client, helpers.make_stage())

    result = subprocess.check_output(
        [dctl_binary.strpath, 'get', 'stage', helpers.DEFAULT_STAGE_ID, '--address', addr, '--disable-ssl'],
    )
    new_stage_id = 'new-test-stage'
    token_file = dctl_binary.dirpath('test_token.txt')
    token_file.write('token-for-dctl')

    p = subprocess.Popen(
        [
            dctl_binary.strpath, '--token-path', token_file.strpath, 'put', 'stage', '/dev/stdin', '--address', addr,
            '--disable-ssl',
        ],
        stdin=subprocess.PIPE, stderr=subprocess.PIPE)

    # replace stage_id in get stage output to put new stage as copied
    loaded = yaml.load(result)
    loaded['meta']['id'] = new_stage_id
    new_spec = yaml.dump(loaded)
    if not isinstance(new_spec, six.binary_type):
        new_spec = six.binary_type(new_spec, 'utf-8')
    p.communicate(new_spec)
    assert p.returncode == 0

    # select old and copied stages from yp and assert equal
    old_stage = local_yp_client.get_object(object_type=data_model.OT_STAGE,
                                           object_identity=helpers.DEFAULT_STAGE_ID,
                                           selectors=[''])[0]
    new_stage = local_yp_client.get_object(object_type=data_model.OT_STAGE,
                                           object_identity=new_stage_id,
                                           selectors=[''])[0]

    assert new_stage['meta']['id'] != old_stage['meta']['id']
    assert new_stage['meta']['project_id'] == old_stage['meta']['project_id']
    assert new_stage['spec']['account_id'] == old_stage['spec']['account_id']
    assert new_stage['spec']['dynamic_resources'] == old_stage['spec']['dynamic_resources']


def test_stage_sidecar_update(dctl_binary, yp_env):
    addr = yp_env['addr']

    local_yp_client = yp_env['client']
    project = helpers.make_project()
    local_yp_client.create_object(data_model.OT_PROJECT, attributes=project)
    stage = helpers.make_stage()
    stage.spec.deploy_units[helpers.DEFAULT_DEPLOY_UNIT_ID].pod_agent_sandbox_info.revision = 123
    stage.spec.deploy_units[helpers.DEFAULT_DEPLOY_UNIT_ID].patchers_revision = 1

    helpers.create_stage(local_yp_client, stage)

    local_yp_client.update_object(object_type=data_model.OT_STAGE,
                                  object_identity=helpers.DEFAULT_STAGE_ID,
                                  set_updates=[
                                      {
                                          "path": "/labels",
                                          "value": {
                                              'du_sidecar_target_revision': {
                                                  helpers.DEFAULT_DEPLOY_UNIT_ID: {
                                                      "podBin": 456,
                                                  }
                                              },
                                              'du_patchers_target_revision': {
                                                  helpers.DEFAULT_DEPLOY_UNIT_ID: 2
                                              },
                                          }
                                      }
                                  ], )

    token_file = dctl_binary.dirpath('test_token.txt')
    token_file.write('token-for-dctl')

    subprocess.check_output(
        [dctl_binary.strpath, 'sidecar', 'update', helpers.DEFAULT_STAGE_ID, '--address', addr, '--disable-ssl'],
    )

    updated_stage = local_yp_client.get_object(object_type=data_model.OT_STAGE,
                                               object_identity=helpers.DEFAULT_STAGE_ID,
                                               selectors=[''])[0]

    assert helpers.DEFAULT_STAGE_ID == updated_stage['meta']['id']
    du_spec = updated_stage['spec']['deploy_units'][helpers.DEFAULT_DEPLOY_UNIT_ID]
    assert du_spec['pod_agent_sandbox_info']['revision'] == 456
    assert du_spec['patchers_revision'] == 2
