import pytest
import typing as tp

from maps.infra.sedem.cli.lib.service import Service
from maps.pylibs.fixtures.api_fixture import ApiFixture
from nanny_repo import repo_pb2


ServiceFactory = tp.Callable[[...], Service]


SPEC_CASES = [
    None,  # no spec
    repo_pb2.CleanupPolicySpec(),
    repo_pb2.CleanupPolicySpec(
        type=repo_pb2.CleanupPolicySpec.PolicyType.SIMPLE_COUNT_LIMIT,
        simple_count_limit=repo_pb2.CleanupPolicySimpleCountLimit(
            snapshots_count=3,
        ),
    ),
    repo_pb2.CleanupPolicySpec(
        type=repo_pb2.CleanupPolicySpec.PolicyType.RESOURCE_TYPE_COUNT,
        resource_type_count=repo_pb2.CleanupPolicyResourceTypeCount(
            same_resource_count=5,
        ),
    ),
]


@pytest.mark.parametrize('spec', SPEC_CASES)
def test_setup_cleanup_policy(service_factory: ServiceFactory,
                              api_mock: ApiFixture,
                              spec: tp.Optional[repo_pb2.CleanupPolicySpec]) -> None:
    service = service_factory('fake_srv')
    service_name = 'maps_core_fake_srv_testing'

    if spec:
        api_mock.nanny.set_cleanup_policy(repo_pb2.CleanupPolicy(
            meta=repo_pb2.CleanupPolicyMeta(id=service_name),
            spec=spec,
        ))

    service.api.rtc.setup_cleanup_policy(staging='testing')

    policy_spec = api_mock.nanny.cleanup_policy(service_name).spec
    assert policy_spec.type == repo_pb2.CleanupPolicySpec.PolicyType.SIMPLE_COUNT_LIMIT
    assert policy_spec.simple_count_limit.snapshots_count == 2
    assert policy_spec.simple_count_limit.disposable_count == 0
    assert policy_spec.simple_count_limit.stalled_ttl == 'PT24H'
