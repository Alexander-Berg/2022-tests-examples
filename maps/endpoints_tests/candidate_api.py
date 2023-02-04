import asyncio
import typing as tp
from unittest.mock import Mock

import grpc
import pytest
from aiohttp import web
from google.protobuf.message import Message as ProtoMessage

from maps.infra.sedem.client.machine_api import MachineApi, MachineNotFoundError, MachineBadRequestError, MachineConflictError
from maps.infra.sedem.client.sedem_api import SedemApi
from maps.infra.sedem.machine.lib.candidate_api import (
    ReleaseCandidateStatus,
)
from maps.infra.sedem.machine.lib.ci import (
    ConfigEntity,
    FlowLaunch,
    FlowProcessId,
    GetConfigHistoryRequest,
    GetConfigHistoryResponse,
    ProxyCiClient,
    StartFlowRequest,
    StartFlowResponse,
    TvmCredentials,
)
from maps.infra.sedem.proto import sedem_pb2
from maps.infra.sedem.common.release.nanny.release_spec import (
    DockerImage,
    NannyReleaseSpec,
)
from maps.infra.sedem.machine.tests.typing import (
    CommitFactory,
    ReleaseCandidateFactory,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.pycare.test_utils import MockUserTicket


class MockRpcError(grpc.RpcError, grpc.Call):
    def __init__(self, code: grpc.StatusCode, details: str) -> None:
        self._code = code
        self._details = details

    def code(self) -> grpc.StatusCode:
        return self._code

    def details(self) -> str:
        return self._details


class MockCiClient(ProxyCiClient):
    def __init__(self) -> None:
        super().__init__(tvm_client=Mock())
        self._responses = {}

    def _service_stub_method_mock(self,
                                  request: ProtoMessage,
                                  metadata: list[tuple[str, str]]) -> ProtoMessage:
        request_type = type(request)
        assert request_type in self._responses
        iter_responses = self._responses[type(request)]
        response = next(iter_responses)
        if isinstance(response, Exception):
            raise response
        return response

    def add_response(self,
                     request_type: tp.Type[ProtoMessage],
                     response: tp.Union[ProtoMessage, Exception, None] = None,
                     response_list: tp.Optional[list[tp.Union[ProtoMessage, Exception]]] = None) -> None:
        assert bool(response) != bool(response_list)
        if response:
            response_list = [response]
        self._responses[request_type] = iter(response_list)

    async def _create_grpc_channel(self) -> Mock:
        return Mock(
            __enter__=Mock(),
            __exit__=Mock(return_value=False),
            unary_unary=Mock(return_value=self._service_stub_method_mock),
        )

    def _get_tvm_credentials(self, user_ticket: str) -> TvmCredentials:
        return TvmCredentials(
            service_ticket='fake-ticket',
            user_ticket=user_ticket,
        )


@pytest.fixture(scope='function')
def ci_client() -> MockCiClient:
    return MockCiClient()


@pytest.fixture(scope='function')
async def test_application(test_application: web.Application,
                           ci_client: MockCiClient) -> web.Application:
    test_application[ProxyCiClient.APP_KEY] = ci_client
    return test_application


@pytest.mark.asyncio
async def test_register_for_unknown_service(sedem_api: SedemApi,
                                            commit_factory: CommitFactory) -> None:
    commit = commit_factory(arc_hash='hash1')

    with pytest.raises(Exception, match=r'No config for [-\w]+ found'):
        await asyncio.to_thread(
            sedem_api.candidate_register,
            service_name='maps-fake-service',
            task_id='12345',
            arc_commit_hash=commit.Oid,
        )


@pytest.mark.asyncio
async def test_register_unknown_commit(sedem_api: SedemApi,
                                       service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    with pytest.raises(Exception, match=r'Commit \w+ not found in arcadia'):
        await asyncio.to_thread(
            sedem_api.candidate_register,
            service_name=service_config.name,
            task_id='12345',
            arc_commit_hash='hash1',
        )


@pytest.mark.asyncio
async def test_register_correct(sedem_api: SedemApi,
                                machine_api: MachineApi,
                                service_config_factory: ServiceConfigFactory,
                                commit_factory: CommitFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    commit = commit_factory(arc_hash='hash1')

    await asyncio.to_thread(
        sedem_api.candidate_register,
        service_name=service_config.name,
        task_id='12345',
        arc_commit_hash=commit.Oid,
    )

    response = await asyncio.to_thread(
        machine_api.candidate_list,
        service_name=service_config.name,
    )
    candidates = response.candidates
    assert len(candidates) == 1
    candidate = candidates[0]
    assert candidate.task_id == '12345'
    assert candidate.commit.arc_commit_hash == commit.Oid
    assert candidate.WhichOneof('Completion') == 'building'


@pytest.mark.asyncio
async def test_list_candidates(machine_api: MachineApi,
                               service_config_factory: ServiceConfigFactory,
                               release_candidate_factory: ReleaseCandidateFactory,
                               release_factory: ReleaseFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    for i in range(1, 4):
        status = {
            1: ReleaseCandidateStatus.READY,
            2: ReleaseCandidateStatus.BROKEN,
            3: ReleaseCandidateStatus.BUILDING,
        }[i]
        revision = 100 + i
        release_spec = NannyReleaseSpec(
            docker=DockerImage(name='maps/core-teapot', tag=revision),
            environments=[],
        )
        await release_candidate_factory(
            service_config=service_config,
            task_id=str(1000 + i),
            arc_hash=f'hash{i}',
            revision=revision,
            release_spec=release_spec,
            status=status,
        )

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        origin_revision=101,
        release_arc_hash='hash1',
        release_revision=101,
    )

    response = await asyncio.to_thread(
        machine_api.candidate_list,
        service_name=service_config.name,
    )
    candidates = response.candidates
    assert len(candidates) == 2
    for i, candidate in zip((3, 2), candidates):
        assert candidate.service_name == service_config.name
        assert candidate.task_id == str(1000 + i)
        assert candidate.commit.arc_commit_hash == f'hash{i}'
        assert candidate.commit.revision == 100 + i
        assert candidate.created_at
        if i == 2:
            assert candidate.HasField('completed_at')
            assert candidate.WhichOneof('Completion') == 'broken'
        else:
            assert not candidate.HasField('completed_at')
            assert candidate.WhichOneof('Completion') == 'building'


@pytest.mark.asyncio
async def test_finalize_for_unknown_service(sedem_api: SedemApi,
                                            commit_factory: CommitFactory) -> None:
    with pytest.raises(Exception, match=r'No config for [-\w]+ found'):
        await asyncio.to_thread(
            sedem_api.candidate_finalize,
            service_name='maps-fake-service',
            task_id='12345',
            finalization_request=sedem_pb2.ReleaseCandidateFinalizationRequest(
                broken=sedem_pb2.ReleaseCandidateFinalizationRequest.Broken(),
            )
        )


@pytest.mark.asyncio
async def test_finalize_unknown_task(sedem_api: SedemApi,
                                     service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    with pytest.raises(Exception, match=r'No building candidate with task id #\d+ for [-\w]+ found'):
        await asyncio.to_thread(
            sedem_api.candidate_finalize,
            service_name=service_config.name,
            task_id='12345',
            finalization_request=sedem_pb2.ReleaseCandidateFinalizationRequest(
                broken=sedem_pb2.ReleaseCandidateFinalizationRequest.Broken(),
            )
        )


@pytest.mark.asyncio
async def test_finalize_ready(sedem_api: SedemApi,
                              machine_api: MachineApi,
                              service_config_factory: ServiceConfigFactory,
                              release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    await release_candidate_factory(
        service_config=service_config,
        task_id='12345',
        release_spec=None,
        status=ReleaseCandidateStatus.BUILDING,
    )

    NannyReleaseSpec = sedem_pb2.ReleaseSpec.NannyReleaseSpec
    proto_release_spec = sedem_pb2.ReleaseSpec(
        nanny=NannyReleaseSpec(
            docker=NannyReleaseSpec.DockerImage(
                name='maps/core-teapot',
                tag='latest'
            ),
        ),
    )

    response = await asyncio.to_thread(
        sedem_api.candidate_finalize,
        service_name=service_config.name,
        task_id='12345',
        finalization_request=sedem_pb2.ReleaseCandidateFinalizationRequest(
            release_spec=proto_release_spec,
            ready=sedem_pb2.ReleaseCandidateFinalizationRequest.Ready(),
        )
    )

    response = await asyncio.to_thread(
        machine_api.candidate_list,
        service_name=service_config.name,
    )
    candidates = response.candidates
    assert len(candidates) == 1
    candidate = candidates[0]
    assert candidate.task_id == '12345'
    assert candidate.WhichOneof('Completion') == 'ready'


@pytest.mark.asyncio
async def test_finalize_broken(sedem_api: SedemApi,
                               machine_api: MachineApi,
                               service_config_factory: ServiceConfigFactory,
                               release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    await release_candidate_factory(
        service_config=service_config,
        task_id='12345',
        release_spec=None,
        status=ReleaseCandidateStatus.BUILDING,
    )

    response = await asyncio.to_thread(
        sedem_api.candidate_finalize,
        service_name=service_config.name,
        task_id='12345',
        finalization_request=sedem_pb2.ReleaseCandidateFinalizationRequest(
            broken=sedem_pb2.ReleaseCandidateFinalizationRequest.Broken(),
        )
    )

    response = await asyncio.to_thread(
        machine_api.candidate_list,
        service_name=service_config.name,
    )
    candidates = response.candidates
    assert len(candidates) == 1
    candidate = candidates[0]
    assert candidate.task_id == '12345'
    assert candidate.WhichOneof('Completion') == 'broken'


@pytest.mark.asyncio
async def test_build_for_unknown_service(machine_api: MachineApi,
                                         commit_factory: CommitFactory,
                                         mock_user_ticket: MockUserTicket) -> None:
    commit = commit_factory(arc_hash='hash1')

    with pytest.raises(Exception, match=r'No config for [-\w]+ found'):
        with mock_user_ticket('fake-ticket'):
            await asyncio.to_thread(
                machine_api.candidate_build,
                service_name='maps-fake-service',
                arc_commit_hash=commit.Oid,
            )


@pytest.mark.asyncio
async def test_build_unknown_commit(machine_api: MachineApi,
                                    service_config_factory: ServiceConfigFactory,
                                    mock_user_ticket: MockUserTicket) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    with pytest.raises(Exception, match=r'Commit \w+ not found in arcadia'):
        with mock_user_ticket('fake-ticket'):
            await asyncio.to_thread(
                machine_api.candidate_build,
                service_name=service_config.name,
                arc_commit_hash='hash1',
            )


@pytest.mark.asyncio
async def test_build_non_trunk_commit(machine_api: MachineApi,
                                      service_config_factory: ServiceConfigFactory,
                                      commit_factory: CommitFactory,
                                      mock_user_ticket: MockUserTicket) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    commit = commit_factory(arc_hash='hash1', revision=None)

    with pytest.raises(Exception, match=r'Commit \w+ is not from trunk'):
        with mock_user_ticket('fake-ticket'):
            await asyncio.to_thread(
                machine_api.candidate_build,
                service_name=service_config.name,
                arc_commit_hash=commit.Oid,
            )


@pytest.mark.asyncio
async def test_build_missing_a_yaml(machine_api: MachineApi,
                                    service_config_factory: ServiceConfigFactory,
                                    commit_factory: CommitFactory,
                                    mock_user_ticket: MockUserTicket,
                                    ci_client: MockCiClient) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    commit = commit_factory(arc_hash='hash1')

    ci_client.add_response(
        request_type=GetConfigHistoryRequest,
        response=GetConfigHistoryResponse(),  # empty response: no a.yaml found
    )

    with pytest.raises(Exception, match=r'No valid CI config found at [/\w]+'):
        with mock_user_ticket('fake-ticket'):
            await asyncio.to_thread(
                machine_api.candidate_build,
                service_name=service_config.name,
                arc_commit_hash=commit.Oid,
            )


@pytest.mark.asyncio
async def test_build_without_delegation(machine_api: MachineApi,
                                        service_config_factory: ServiceConfigFactory,
                                        commit_factory: CommitFactory,
                                        mock_user_ticket: MockUserTicket,
                                        ci_client: MockCiClient) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    commit = commit_factory(arc_hash='hash1')

    ci_client.add_response(
        request_type=GetConfigHistoryRequest,
        response=GetConfigHistoryResponse(
            last_valid_entity=ConfigEntity(
                has_token=False,
            ),
        ),
    )

    with pytest.raises(Exception, match=r'You must delegate OAuth token to CI before launching builds'):
        with mock_user_ticket('fake-ticket'):
            await asyncio.to_thread(
                machine_api.candidate_build,
                service_name=service_config.name,
                arc_commit_hash=commit.Oid,
            )


@pytest.mark.asyncio
async def test_build_correct(machine_api: MachineApi,
                             service_config_factory: ServiceConfigFactory,
                             commit_factory: CommitFactory,
                             mock_user_ticket: MockUserTicket,
                             ci_client: MockCiClient) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    commit = commit_factory(arc_hash='hash1')

    ci_client.add_response(
        request_type=GetConfigHistoryRequest,
        response=GetConfigHistoryResponse(
            last_valid_entity=ConfigEntity(  # set valid entity
                has_token=True,
            ),
        ),
    )
    ci_client.add_response(
        request_type=StartFlowRequest,
        response=StartFlowResponse(
            launch=FlowLaunch(
                flow_process_id=FlowProcessId(
                    dir=service_config.path,
                    id='build',
                ),
                number=42,
            )
        ),
    )

    with mock_user_ticket('fake-ticket'):
        response = await asyncio.to_thread(
            machine_api.candidate_build,
            service_name=service_config.name,
            arc_commit_hash=commit.Oid,
        )
    assert response.launch_url == (
        f'https://a.yandex-team.ru/projects/{service_config.abc_slug}/'
        f'ci/actions/launch?dir={service_config.path}&id=build&number=42'
    )


@pytest.mark.asyncio
async def test_build_retries(machine_api: MachineApi,
                             service_config_factory: ServiceConfigFactory,
                             commit_factory: CommitFactory,
                             mock_user_ticket: MockUserTicket,
                             ci_client: MockCiClient) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    commit = commit_factory(arc_hash='hash1')

    ci_client.add_response(
        request_type=GetConfigHistoryRequest,
        response_list=[
            MockRpcError(code=grpc.StatusCode.UNAVAILABLE, details='reset by peer'),
            GetConfigHistoryResponse(
                last_valid_entity=ConfigEntity(  # set valid entity
                    has_token=True,
                ),
            ),
        ],
    )
    ci_client.add_response(
        request_type=StartFlowRequest,
        response_list=[
            MockRpcError(code=grpc.StatusCode.UNAVAILABLE, details='reset by peer'),
            StartFlowResponse(
                launch=FlowLaunch(
                    flow_process_id=FlowProcessId(
                        dir=service_config.path,
                        id='build',
                    ),
                    number=42,
                )
            ),
        ],
    )

    with mock_user_ticket('fake-ticket'):
        response = await asyncio.to_thread(
            machine_api.candidate_build,
            service_name=service_config.name,
            arc_commit_hash=commit.Oid,
        )
    assert response.launch_url


@pytest.mark.asyncio
async def test_lookup_candidate_svn_revision(sedem_api: SedemApi,
                                             machine_api: MachineApi,
                                             service_config_factory: ServiceConfigFactory,
                                             commit_factory: CommitFactory,
                                             release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    for i in range(1, 2):
        revision = 100 + i
        release_spec = NannyReleaseSpec(
            docker=DockerImage(name='maps/core-teapot', tag=revision),
            environments=[],
        )
        await release_candidate_factory(
            service_config=service_config,
            task_id=str(1000 + i),
            arc_hash=f'hash{i}',
            revision=revision,
            release_spec=release_spec,
            status=ReleaseCandidateStatus.READY,
        )

    response = await asyncio.to_thread(
        machine_api.lookup_candidate,
        service_name=service_config.name,
        svn_revision=101,
    )
    candidate = response.candidate

    assert candidate.task_id == '1001'
    assert candidate.commit.arc_commit_hash == 'hash1'
    assert candidate.commit.revision == 101
    assert candidate.WhichOneof('Completion') == 'ready'


@pytest.mark.asyncio
async def test_lookup_candidate_no_revision(sedem_api: SedemApi,
                                            machine_api: MachineApi,
                                            service_config_factory: ServiceConfigFactory,
                                            commit_factory: CommitFactory,
                                            release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    for i in range(1, 3):
        revision = 100 + i
        release_spec = NannyReleaseSpec(
            docker=DockerImage(name='maps/core-teapot', tag=revision),
            environments=[],
        )
        await release_candidate_factory(
            service_config=service_config,
            task_id=str(1000 + i),
            arc_hash=f'hash{i}',
            revision=revision,
            release_spec=release_spec,
            status=ReleaseCandidateStatus.READY,
        )

    response = await asyncio.to_thread(
        machine_api.lookup_candidate,
        service_name=service_config.name,
    )
    candidate = response.candidate

    assert candidate.commit.revision == 102
    assert candidate.WhichOneof('Completion') == 'ready'


@pytest.mark.asyncio
async def test_lookup_no_candidate(sedem_api: SedemApi,
                                   machine_api: MachineApi,
                                   service_config_factory: ServiceConfigFactory,
                                   commit_factory: CommitFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    with pytest.raises(MachineNotFoundError):
        await asyncio.to_thread(
            machine_api.lookup_candidate,
            service_name=service_config.name,
        )


@pytest.mark.asyncio
async def test_lookup_only_ready_candidate(sedem_api: SedemApi,
                                           machine_api: MachineApi,
                                           service_config_factory: ServiceConfigFactory,
                                           commit_factory: CommitFactory,
                                           release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    for i in range(1, 5):
        status = {
            1: ReleaseCandidateStatus.READY,
            2: ReleaseCandidateStatus.BROKEN,
            3: ReleaseCandidateStatus.READY,
            4: ReleaseCandidateStatus.BUILDING,
        }[i]
        revision = 100 + i
        release_spec = NannyReleaseSpec(
            docker=DockerImage(name='maps/core-teapot', tag=revision),
            environments=[],
        )
        await release_candidate_factory(
            service_config=service_config,
            task_id=str(1000 + i),
            arc_hash=f'hash{i}',
            revision=revision,
            release_spec=release_spec,
            status=status,
        )

    response = await asyncio.to_thread(
        machine_api.lookup_candidate,
        service_name=service_config.name,
    )
    candidate = response.candidate
    assert candidate.commit.revision == 103
    assert candidate.WhichOneof('Completion') == 'ready'


@pytest.mark.asyncio
async def test_lookup_candidate_building(sedem_api: SedemApi,
                                         machine_api: MachineApi,
                                         service_config_factory: ServiceConfigFactory,
                                         commit_factory: CommitFactory,
                                         release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    revision = 100
    release_spec = NannyReleaseSpec(
        docker=DockerImage(name='maps/core-teapot', tag=revision),
        environments=[],
    )
    await release_candidate_factory(
        service_config=service_config,
        task_id=str(1000),
        arc_hash=f'hash{1}',
        revision=revision,
        release_spec=release_spec,
        status=ReleaseCandidateStatus.BUILDING,
    )

    with pytest.raises(MachineBadRequestError, match=f'Commit r{revision} is not yet ready'):
        await asyncio.to_thread(
            machine_api.lookup_candidate,
            service_name=service_config.name,
            svn_revision=100,
        )


@pytest.mark.asyncio
async def test_lookup_candidate_broken(sedem_api: SedemApi,
                                       machine_api: MachineApi,
                                       service_config_factory: ServiceConfigFactory,
                                       commit_factory: CommitFactory,
                                       release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    revision = 100
    release_spec = NannyReleaseSpec(
        docker=DockerImage(name='maps/core-teapot', tag=revision),
        environments=[],
    )
    await release_candidate_factory(
        service_config=service_config,
        task_id='1000',
        arc_hash='hash1',
        revision=revision,
        release_spec=release_spec,
        status=ReleaseCandidateStatus.BROKEN,
    )

    with pytest.raises(MachineBadRequestError, match=f'Commit r{revision} is broken and can\'t be released'):
        await asyncio.to_thread(
            machine_api.lookup_candidate,
            service_name=service_config.name,
            svn_revision=100,
        )


@pytest.mark.asyncio
async def test_lookup_not_candidate(sedem_api: SedemApi,
                                    machine_api: MachineApi,
                                    service_config_factory: ServiceConfigFactory,
                                    commit_factory: CommitFactory,
                                    release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    revision = 100
    commit_factory(arc_hash='hash1', revision=revision)
    with pytest.raises(MachineNotFoundError, match='Commit r100 was never built'):
        await asyncio.to_thread(
            machine_api.lookup_candidate,
            service_name=service_config.name,
            svn_revision=100,
        )


@pytest.mark.asyncio
async def test_lookup_release_conflict(sedem_api: SedemApi,
                                                     machine_api: MachineApi,
                                                     service_config_factory: ServiceConfigFactory,
                                                     commit_factory: CommitFactory,
                                                     release_candidate_factory: ReleaseCandidateFactory,
                                                     release_factory: ReleaseFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    revision = 100
    release_spec = NannyReleaseSpec(
        docker=DockerImage(name='maps/core-teapot', tag=revision),
        environments=[],
    )
    await release_candidate_factory(
        service_config=service_config,
        task_id='1000',
        arc_hash='hash1',
        revision=revision,
        release_spec=release_spec,
        status=ReleaseCandidateStatus.READY,
    )

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        release_arc_hash='hash1'
    )

    with pytest.raises(MachineNotFoundError, match='No candidates available for release'):
        await asyncio.to_thread(
            machine_api.lookup_candidate,
            service_name=service_config.name
        )

    with pytest.raises(MachineConflictError, match='Commit r100 is already released as v1.1'):
        await asyncio.to_thread(
            machine_api.lookup_candidate,
            service_name=service_config.name,
            svn_revision=100,
        )
