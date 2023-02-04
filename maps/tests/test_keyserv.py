import pytest

from maps.infra.apiteka.server.spec.client.api import provider_api as apiteka

from maps.infra.apiteka.server.spec.tests.keyserv.api import keyserv_api as keyserv
from maps.infra.apiteka.server.spec.tests.keyserv.models import (
    Keylist, Keystate, Restrictions, Stoplist, Stopitem
)

from maps.infra.apiteka.proto import apiteka_pb2 as proto
from unittest.mock import create_autospec
from .samples import apiteka_call_dispatch

import json


UNRESTRICTED_PLAN = 'unrestricted'


KEYSERV_KEYLIST = Keylist(
    [
        Keystate(
            key="deadbeef==",
            valid=True,
            broken=False,
            issued=1234,
            uri='stuff.yandex.ru',
            note="Fake keyserv key",
            stoplist=Stoplist([
                Stopitem(
                    blocked=0,
                    description="Still working",
                    modified="2000-01-01 12:00:00:12345"
                )
            ]),
            restrictions=Restrictions(
                allowStuff="1",
                maxStuff="1000"
            )
        ),
        Keystate(
            key="abba==",
            valid=True,
            broken=False,
            issued=1235,
            uri='music.not-yandex.ru',
            note="Another fake keyserv key",
            restrictions=Restrictions(
                allowSomething="0"
            )
        )
    ]
)


def keyserv_dispatch(*args, **kwargs):
    return KEYSERV_KEYLIST


def is_key_active(key: Keystate) -> bool:
    if not key.valid or key.broken:
        return False

    if hasattr(key, 'stoplist') and key.stoplist.value:
        return key.stoplist.value[0].blocked == '0'

    return True


@pytest.fixture
def keyserv_api():
    with create_autospec(keyserv.ApiClient)() as client:
        client.call_api.side_effect = keyserv_dispatch
        yield keyserv.KeyservApi(client)


@pytest.fixture
def apiteka_client(apiteka_client: apiteka.ApiClient) -> apiteka.ApiClient:
    apiteka_client.call_api.side_effect = apiteka_call_dispatch
    return apiteka_client


def test_keyserv(provider_api: apiteka.ProviderApi, keyserv_api: keyserv.KeyservApi):
    keys: Keylist
    # Empty comment matches and returns all keys
    keys = keyserv_api.find_keys_by_comment()

    snapshot = proto.Snapshot()

    keyserv_key: Keystate
    assignments = proto.Snapshot.ProviderKeysAssignment()
    assignments_view: dict[str, proto.Snapshot.PlanKeysAssignment] = {
        UNRESTRICTED_PLAN: assignments.assignment.add(id=UNRESTRICTED_PLAN)
    }

    for keyserv_key in keys.value:
        key = snapshot.key_specs.add()
        key.key = keyserv_key.key
        key.is_active = is_key_active(keyserv_key)
        key.origin = proto.Keyserv

        referer = key.restrictions.add()
        referer.http_referer = keyserv_key.uri

        if restrictions := keyserv_key.restrictions.to_dict():
            # A distinct plan is created for each key with non-empty restrictions.
            # TODO: unify plans with same restrictions (how to choose ids?)
            assignment = assignments_view.get(keyserv_key.key)
            if assignment is None:
                assignment = assignments_view.setdefault(
                    keyserv_key.key,
                    assignments.assignment.add(
                        id=keyserv_key.key,
                        features=json.dumps(restrictions)
                    )
                )
        else:
            assignment = assignments_view.get(UNRESTRICTED_PLAN)

        assignment.api_keys.append(keyserv_key.key)

    providers = proto.ProviderList.FromString(
        provider_api.get_provider_list().encode()
    )

    for provider in providers.providers:
        snapshot.provider_keys_assignments[provider.id].MergeFrom(assignments)

    provider_api.provider_sync(body=snapshot.SerializeToString())
