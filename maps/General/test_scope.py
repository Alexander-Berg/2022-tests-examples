import typing as tp

from maps.pylibs.fixtures.mock_session import ApiMethodParams
from maps.pylibs.fixtures.fixture import FixtureInitializationError, calling_scope_variable


def get_test_scope_fixture(fixture_type: tp.Type) -> tp.Any:
    maybe_fixture_factory = calling_scope_variable('fixture_factory')
    if maybe_fixture_factory is None:
        raise FixtureInitializationError(
            'Unable to find "fixture_factory" in test scope'
        )
    fixture_factory = maybe_fixture_factory
    return fixture_factory(fixture_type)


def get_test_scope_api_params() -> ApiMethodParams:
    maybe_api_params: tp.Optional[ApiMethodParams] = calling_scope_variable('api_params')
    if maybe_api_params is None:
        raise FixtureInitializationError('No ApiParams has been found in test scope')
    assert isinstance(maybe_api_params, ApiMethodParams)
    api_params = maybe_api_params
    return api_params
