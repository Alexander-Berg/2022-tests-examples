from __future__ import unicode_literals

import yp.common
import yp.client
import yp.data_model
from yp_proto.yp.client.api.proto import deploy_pb2
from infra.swatlib import metrics
from infra.release_controller.src import controller
from infra.release_controller.src import deploy_ticket_maker
from infra.release_controller.src import processor
from infra.release_controller.src import release_matcher
from infra.release_controller.src import release_selector
from infra.release_controller.src import storage
from infra.release_controller.src.lib import yp_client


DEFAULT_RELEASE_ID = 'test-release'
DEFAULT_RELEASE_RULE_ID = 'test-release-rule'
DEFAULT_STAGE_ID = 'test-stage'
DEFAULT_PROJECT_ID = 'test-project'

DEFAULT_DEPLOY_UNIT_ID = 'test-deploy-unit'
DEFAULT_BOX_ID = 'test-box'

DEFAULT_ACL = [{
    'action': 'allow',
    'subjects': ['test'],
    'permissions': ['write'],
}]

DEFAULT_RELEASE_TYPE = 'testing'

DEFAULT_SANDBOX_TASK_TYPE = 'test-task-type'
DEFAULT_SANDBOX_TASK_ID = 'test-task-id'
DEFAULT_SANDBOX_RESOURCE_TYPE = 'test-resource-type'

DEFAULT_DOCKER_IMAGE_NAME = 'test-image-name'
DEFAULT_DOCKER_IMAGE_TAG = 'test-image-tag'
DEFAULT_DOCKER_IMAGE_HASH = 'test-image-hash'


def make_sandbox_release_dict(release_id=None):
    release_id = release_id or DEFAULT_RELEASE_ID
    return {
        'meta': {
            'id': release_id,
            'acl': DEFAULT_ACL,
        },
        'spec': {
            'sandbox': {
                'task_type': DEFAULT_SANDBOX_TASK_TYPE,
                'task_id': DEFAULT_SANDBOX_TASK_ID,
                'release_type': DEFAULT_RELEASE_TYPE,
                'resources': [{'type': DEFAULT_SANDBOX_RESOURCE_TYPE, 'skynet_id': 'test-skynet-id'}]
            }
        }
    }


def make_sandbox_release_pb(release_id=None):
    return yp.common.dict_to_protobuf(make_sandbox_release_dict(release_id),
                                      proto_class=yp.data_model.TRelease)


def make_docker_image_dict(name=None):
    return {
        'name': name or DEFAULT_DOCKER_IMAGE_NAME,
        'tag': DEFAULT_DOCKER_IMAGE_TAG,
        'digest': DEFAULT_DOCKER_IMAGE_HASH
    }


def make_docker_image_pb(name=None):
    return yp.common.dict_to_protobuf(make_docker_image_dict(name),
                                      proto_class=deploy_pb2.TDockerImageDescription)


def make_docker_release_dict(release_id=None):
    release_id = release_id or DEFAULT_RELEASE_ID
    return {
        'meta': {
            'id': release_id,
            'acl': DEFAULT_ACL,
        },
        'spec': {
            'docker': {
                'image_name': DEFAULT_DOCKER_IMAGE_NAME,
                'images': [make_docker_image_dict()],
                'image_tag': DEFAULT_DOCKER_IMAGE_TAG,
                'image_hash': DEFAULT_DOCKER_IMAGE_HASH,
                'release_type': DEFAULT_RELEASE_TYPE,
            }
        }
    }


def make_docker_release_pb(release_id=None):
    return yp.common.dict_to_protobuf(make_docker_release_dict(release_id),
                                      proto_class=yp.data_model.TRelease)


def make_sandbox_release_rule_dict(release_rule_id=None, stage_id=None):
    release_rule_id = release_rule_id or DEFAULT_RELEASE_RULE_ID
    stage_id = stage_id or DEFAULT_STAGE_ID
    return {
        'meta': {
            'id': release_rule_id,
            'stage_id': stage_id,
            'acl': DEFAULT_ACL,
        },
        'spec': {
            'sandbox': {
                'task_type': DEFAULT_SANDBOX_TASK_TYPE,
                'resource_types': [DEFAULT_SANDBOX_RESOURCE_TYPE],
                'release_types': [DEFAULT_RELEASE_TYPE],
            },
            'patches': {
                'test-patch': {
                    'sandbox': {
                        'sandbox_resource_type': DEFAULT_SANDBOX_RESOURCE_TYPE
                    }
                }
            },
            'selector_source': yp.data_model.TReleaseRuleSpec.ESelectorSource.CUSTOM
        }
    }


def make_sandbox_release_rule_pb(release_rule_id=None, stage_id=None):
    return yp.common.dict_to_protobuf(make_sandbox_release_rule_dict(release_rule_id, stage_id),
                                      proto_class=yp.data_model.TReleaseRule)


def make_docker_release_rule_dict(release_rule_id=None, stage_id=None):
    release_rule_id = release_rule_id or DEFAULT_RELEASE_RULE_ID
    stage_id = stage_id or DEFAULT_STAGE_ID
    return {
        'meta': {
            'id': release_rule_id,
            'stage_id': stage_id,
            'acl': DEFAULT_ACL,
        },
        'spec': {
            'docker': {
                'image_name': DEFAULT_DOCKER_IMAGE_NAME,
                'release_types': [DEFAULT_RELEASE_TYPE],
                'images': [make_docker_image_dict()],
            },
            'patches': {
                'test-patch': {
                    'docker': {
                        "docker_image_ref": {
                            'deploy_unit_id': DEFAULT_DEPLOY_UNIT_ID,
                            'box_id': DEFAULT_BOX_ID
                        }
                    }
                }
            },
            'selector_source': yp.data_model.TReleaseRuleSpec.ESelectorSource.CUSTOM
        }
    }


def make_docker_release_rule_pb(release_rule_id=None, stage_id=None):
    return yp.common.dict_to_protobuf(make_docker_release_rule_dict(release_rule_id, stage_id),
                                      proto_class=yp.data_model.TReleaseRule)


def make_project_dict(project_id=None):
    project_id = project_id or DEFAULT_PROJECT_ID
    return {
        'meta': {
            'id': project_id
        },
        'spec': {
            'account_id': 'tmp'
        }
    }


def make_stage_dict(stage_id=None):
    stage_id = stage_id or DEFAULT_STAGE_ID
    return {
        'meta': {
            'id': stage_id,
            'project_id': DEFAULT_PROJECT_ID
        },
        'spec': {
            'account_id': 'tmp'
        }
    }


def make_stage_pb(stage_id=None):
    return yp.common.dict_to_protobuf(make_stage_dict(stage_id),
                                      proto_class=yp.data_model.TStage)


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
    ticket_maker = deploy_ticket_maker.DeployTicketMaker()
    release_rule_storage = storage.make_storage(storage.RELEASE_RULE_INDEXERS)
    stage_storage = storage.make_storage(storage.STAGE_INDEXERS)
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=release_rule_storage,
                                             stage_storage=stage_storage,
                                             match_sandbox_resource_attributes=False)
    return controller.ReleaseController(yp_client=make_yp_client(yp_env),
                                        release_matcher=matcher,
                                        deploy_ticket_maker=ticket_maker,
                                        release_rule_storage=release_rule_storage,
                                        stage_storage=stage_storage,
                                        cache_ttl=release_rule_cache_ttl)


def make_processor(yp_env,
                   release_rule_cache_ttl=None,
                   select_release_batch_size=None,
                   iteration_sleep=None,
                   metrics_registry=None):
    metrics_registry = metrics_registry or metrics.Registry()
    yp_client = make_yp_client(yp_env)
    ctl = make_controller(
        yp_env,
        release_rule_cache_ttl=release_rule_cache_ttl,
    )
    selector = release_selector.NotProcessedReleaseBatchSelector(
        yp_client=yp_client,
        batch_size=select_release_batch_size,
    )
    return processor.ReleaseProcessor(release_selector=selector,
                                      release_controller=ctl,
                                      iteration_sleep=iteration_sleep,
                                      metrics_registry=metrics_registry)
