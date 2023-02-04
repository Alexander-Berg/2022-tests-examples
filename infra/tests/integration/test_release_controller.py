from __future__ import unicode_literals

import yp.common
import yp.data_model
from infra.release_controller.tests.helpers import helpers


def test_release_controller(yp_env):
    yp_env.create_object(yp.data_model.OT_PROJECT, attributes=helpers.make_project_dict())
    yp_env.create_object(yp.data_model.OT_STAGE, attributes=helpers.make_stage_dict())
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE, attributes=helpers.make_sandbox_release_rule_dict())
    release_dict = helpers.make_sandbox_release_dict()
    yp_env.create_object(yp.data_model.OT_RELEASE, attributes=release_dict)
    release = yp.common.dict_to_protobuf(release_dict, yp.data_model.TRelease)

    ctl = helpers.make_controller(yp_env)
    ctl.resync_storages_if_needed()
    ctl.process(release)

    tickets = yp_env.select_objects(object_type=yp.data_model.OT_DEPLOY_TICKET,
                                    limit=100)
    assert len(tickets) == 1
