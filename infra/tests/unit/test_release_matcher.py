from __future__ import unicode_literals

from infra.release_controller.src import release_matcher
from infra.release_controller.src import storage
from infra.release_controller.tests.helpers import helpers


def test_matcher_sandbox_release_type():
    rule1 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-1')
    rule2 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-2')
    del rule2.spec.sandbox.release_types[:]
    rule2.spec.sandbox.release_types.extend(['unmatched-release-type'])
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_sandbox_release_pb(release_id='release')
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


def test_matcher_sandbox_task_type():
    rule1 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-1')
    rule2 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-2')
    rule2.spec.sandbox.task_type = 'unmatched-task-type'
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_sandbox_release_pb(release_id='release')
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


def test_matcher_sandbox_resource_types():
    rule1 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-1')
    rule2 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-2')
    rule2.spec.sandbox.resource_types.extend(['unmatched-resource-type'])
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_sandbox_release_pb(release_id='release')
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


def test_matcher_sandbox_resource_attributes():
    rule1 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-1')
    rule1.spec.sandbox.attributes['foo'] = 'bar'
    rule2 = helpers.make_sandbox_release_rule_pb(release_rule_id='rule-2')
    rule2.spec.sandbox.attributes['foo'] = 'unmatched'
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_sandbox_release_pb(release_id='release')
    for r in release.spec.sandbox.resources:
        r.attributes['foo'] = 'bar'
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=True)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


def test_matcher_docker_release_type():
    rule1 = helpers.make_docker_release_rule_pb(release_rule_id='rule-1')
    rule2 = helpers.make_docker_release_rule_pb(release_rule_id='rule-2')
    del rule2.spec.sandbox.release_types[:]
    rule2.spec.sandbox.release_types.extend(['unmatched-release-type'])
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_docker_release_pb(release_id='release')
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


# Case 1: legacy release, legacy rule
def test_matcher_docker_image_name():
    rule1 = helpers.make_docker_release_rule_pb(release_rule_id='rule-1')
    del rule1.spec.docker.images[:]
    rule2 = helpers.make_docker_release_rule_pb(release_rule_id='rule-2')
    del rule2.spec.docker.images[:]
    rule2.spec.docker.image_name = 'unmatched-image-name'
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_docker_release_pb(release_id='release')
    del release.spec.docker.images[:]
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


# Case 2: new release, new rule
def test_matcher_docker_images():
    rule1 = helpers.make_docker_release_rule_pb(release_rule_id='rule-1')
    rule2 = helpers.make_docker_release_rule_pb(release_rule_id='rule-2')
    rule2.spec.docker.images.extend([helpers.make_docker_image_pb('unmatched-image-name')])
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_docker_release_pb(release_id='release')
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']


# Case 2: new release, legacy rule
def test_matcher_docker_images_legacy_rule():
    rule1 = helpers.make_docker_release_rule_pb(release_rule_id='rule-1')
    del rule1.spec.docker.images[:]
    rule2 = helpers.make_docker_release_rule_pb(release_rule_id='rule-2')
    del rule2.spec.docker.images[:]
    rule2.spec.docker.image_name = 'unmatched-image-name'
    rule_storage = storage.make_storage(indexers=storage.RELEASE_RULE_INDEXERS)
    for rule in (rule1, rule2):
        rule_storage.add(rule)
    release = helpers.make_docker_release_pb(release_id='release')
    matcher = release_matcher.ReleaseMatcher(release_rule_storage=rule_storage,
                                             stage_storage=None,
                                             match_sandbox_resource_attributes=False)
    matched_rules = matcher.match(release)
    assert [r.meta.id for r in matched_rules] == ['rule-1']
