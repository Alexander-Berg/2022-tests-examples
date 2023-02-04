import mock
import yaml

from infra.dctl.src.lib import cliutil, yp_client
from infra.dctl.src.lib import stage
from infra.dctl.src import consts
from infra.dctl.tests.helpers import helpers
import yp.data_model as data_model


def test_dump_stage(yp_env, capsys):
    project = helpers.make_project()
    yp_env.create_object(data_model.OT_PROJECT, attributes=project)
    helpers.create_stage(yp_env, helpers.make_stage())

    with mock.patch('yp.client.YpClient.create_grpc_object_stub', return_value=yp_env.create_grpc_object_stub()):
        ctx = mock.Mock()
        ctx.get_client.return_value = yp_client.YpClient('test_url', 'token', 'test_user')
        cliutil.get_and_dump_object(ctx=ctx,
                                    cluster='test',
                                    object_type=data_model.OT_STAGE,
                                    object_id=helpers.DEFAULT_STAGE_ID,
                                    skip_ro_fields=True,
                                    fmt=consts.DEFAULT_FORMAT)
        out, _ = capsys.readouterr()
        stage_dump = yaml.load(out)
        assert stage_dump['meta']['id'] == helpers.DEFAULT_STAGE_ID
        assert stage_dump['meta']['project_id'] == helpers.DEFAULT_PROJECT_ID
        assert stage_dump['spec']


def test_put_stage(yp_env):
    project = helpers.make_project()
    yp_env.create_object(data_model.OT_PROJECT, attributes=project)
    stage_object = helpers.make_stage()

    with mock.patch('yp.client.YpClient.create_grpc_object_stub', return_value=yp_env.create_grpc_object_stub()):
        ctx = mock.Mock()
        ctx.get_client.return_value = yp_client.YpClient('test_url', 'token', 'test_user')

        with mock.patch('infra.dctl.src.lib.helpers.patch_pod_spec_secrets', return_value=None):
            stage.put(stage=stage_object,
                      cluster='test cluster',
                      rewrite_delegation_tokens=True,
                      vault_client=ctx.get_vault_client(),
                      vault_client_rsa_fallback=ctx.get_vault_client(),
                      client=ctx.get_client()
                      )

            old_stage = yp_env.get_object(object_type=data_model.OT_STAGE,
                                          object_identity=helpers.DEFAULT_STAGE_ID,
                                          selectors=[''])[0]
            assert old_stage['meta']['project_id']
            assert old_stage['spec']['account_id']
            assert old_stage['spec']['dynamic_resources']


def test_dump_and_put_stage(yp_env, capsys):
    # create stage in local yp
    project = helpers.make_project()
    yp_env.create_object(data_model.OT_PROJECT, attributes=project)
    helpers.create_stage(yp_env, helpers.make_stage())

    # dump created stage to dict
    with mock.patch('yp.client.YpClient.create_grpc_object_stub', return_value=yp_env.create_grpc_object_stub()):
        ctx = mock.Mock()
        ctx.get_client.return_value = yp_client.YpClient('test_url', 'token', 'test_user')
        cliutil.get_and_dump_object(ctx=ctx,
                                    cluster='test',
                                    object_type=data_model.OT_STAGE,
                                    object_id=helpers.DEFAULT_STAGE_ID,
                                    skip_ro_fields=True,
                                    fmt=consts.DEFAULT_FORMAT)
        out, _ = capsys.readouterr()
        stage_dump = yaml.load(out)

        # put dumped stage after clearing read only fields
        new_id = 'new_stage_id'
        stage_copy = stage.cast_yaml_dict_to_yp_object(stage_dump)
        stage_copy.meta.id = new_id
        with mock.patch('infra.dctl.src.lib.helpers.patch_pod_spec_secrets', return_value=None):
            stage.put(stage=stage_copy,
                      cluster='test cluster',
                      rewrite_delegation_tokens=False,
                      vault_client=ctx.get_vault_client(),
                      vault_client_rsa_fallback=ctx.get_vault_client(),
                      client=ctx.get_client()
                      )

            # select from local yp and compare stages
            old_stage = yp_env.get_object(object_type=data_model.OT_STAGE,
                                          object_identity=helpers.DEFAULT_STAGE_ID,
                                          selectors=[''])[0]
            new_stage = yp_env.get_object(object_type=data_model.OT_STAGE,
                                          object_identity=new_id,
                                          selectors=[''])[0]

            assert new_stage['meta']['id'] != old_stage['meta']['id']
            assert new_stage['meta']['project_id'] == old_stage['meta']['project_id']
            assert new_stage['spec']['account_id'] == old_stage['spec']['account_id']
            assert new_stage['spec']['dynamic_resources'] == old_stage['spec']['dynamic_resources']
