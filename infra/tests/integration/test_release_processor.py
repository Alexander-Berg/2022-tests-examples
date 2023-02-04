from __future__ import unicode_literals

import gevent

import yp.client
import yp.data_model
from infra.release_controller.tests.helpers import helpers


def test_processor(yp_env):
    yp_env.create_object(yp.data_model.OT_PROJECT,
                         attributes=helpers.make_project_dict())
    yp_env.create_object(yp.data_model.OT_STAGE,
                         attributes=helpers.make_stage_dict())

    yp_env.create_object(yp.data_model.OT_RELEASE_RULE,
                         attributes=helpers.make_sandbox_release_rule_dict('test-sandbox-releas-rule'))
    yp_env.create_object(yp.data_model.OT_RELEASE_RULE,
                         attributes=helpers.make_docker_release_rule_dict('test-docker-release-rule'))

    yp_env.create_object(yp.data_model.OT_RELEASE,
                         attributes=helpers.make_sandbox_release_dict('test-sandbox-release-1'))
    yp_env.create_object(yp.data_model.OT_RELEASE,
                         attributes=helpers.make_sandbox_release_dict('test-sandbox-release-2'))
    yp_env.create_object(yp.data_model.OT_RELEASE,
                         attributes=helpers.make_docker_release_dict('test-docker-release-1'))

    proc = helpers.make_processor(yp_env,
                                  release_rule_cache_ttl=0.1,
                                  select_release_batch_size=2,
                                  iteration_sleep=0.1)
    proc.start()
    gevent.sleep(3)
    processed_releases = yp_env.select_objects(yp.data_model.OT_RELEASE,
                                               filter='[/status/processing/finished/status] = "true"',
                                               selectors=['/meta/id'])
    tickets = yp_env.select_objects(yp.data_model.OT_DEPLOY_TICKET,
                                    limit=100,
                                    selectors=['/meta/id'])
    assert len(processed_releases) == 3
    assert len(tickets) == 3
