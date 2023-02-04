from maps.infra.apiteka.proto import apiteka_pb2 as proto
from unittest.mock import DEFAULT

from .misc import to_string

import typing as tp


SOME_PROVIDER = proto.ProviderList.ProviderSummary(
    id='some-provider',
    abc_slug='abc-slug'
)


PROVIDER_LIST = proto.ProviderList(providers=[SOME_PROVIDER])


def apiteka_call_dispatch(resource_path, *args, **kwargs) -> tp.Any:
    if resource_path == '/provider/list':
        return to_string(PROVIDER_LIST)

    return DEFAULT
