import pytest

from maps.infra.apiteka.server.spec.client.api import provider_api as apiteka

from maps.infra.apiteka.server.spec.tests.apikeys.api import model_api as apikeys
from maps.infra.apiteka.server.spec.tests.apikeys.model.project_service_info_keys import ProjectServiceInfoKeys

from maps.infra.apiteka.proto import apiteka_pb2 as proto
from unittest.mock import create_autospec, DEFAULT
from .misc import to_string

from .samples import apiteka_call_dispatch

import typing as tp


SERVICE_LINK = [
    apikeys.ProjectServiceInfo(
        tariff='myproject@demo',
        hidden=False,
        keys=[
            ProjectServiceInfoKeys(
                key='deadbeef',
                active=True,
                custom_params={}
            ),
            ProjectServiceInfoKeys(
                key='topsecret',
                active=False,
                custom_params={}
            )
        ]
    )
]


def apikeys_call_dispatch(resource_path, *args, **kwargs) -> tp.Any:
    if resource_path == '/v2/project_service_link_export':
        return SERVICE_LINK

    return DEFAULT


@pytest.fixture
def apikeys_api():
    with create_autospec(apikeys.ApiClient)() as stub:
        stub.call_api.side_effect = apikeys_call_dispatch
        yield apikeys.ModelApi(stub)


@pytest.fixture
def apiteka_client(apiteka_client: apiteka.ApiClient):
    apiteka_client.call_api.side_effect = apiteka_call_dispatch

    return apiteka_client


def test_apikeys_snapshot(provider_api: apiteka.ProviderApi, apikeys_api: apikeys.ModelApi):
    providers = proto.ProviderList.FromString(
        provider_api.get_provider_list().encode()
    )

    snapshot = proto.Snapshot()
    for provider in providers.providers:
        links: list[apikeys.ProjectServiceInfo]
        links = apikeys_api.get_project_service_info(provider.id)

        assignments = snapshot.provider_keys_assignments[provider.id].assignment
        assignment_view: dict[str, proto.Snapshot.PlanKeysAssignment] = {}
        for link in links:
            assignment = assignment_view.get(link.tariff)
            if assignment is None:
                assignment = assignment_view.setdefault(
                    link.tariff,
                    assignments.add(id=link.tariff)
                )

            key: ProjectServiceInfoKeys
            for key in link.keys:
                snapshot.key_specs.append(
                    proto.ApiKeySpec(
                        key=key.key,
                        is_active=key.active
                    )
                )
                assignment.api_keys.append(key.key)

    provider_api.provider_sync(body=to_string(snapshot))

    assert len(snapshot.key_specs) == len(SERVICE_LINK[0].keys)
    assert len(snapshot.provider_keys_assignments) == len(SERVICE_LINK)
