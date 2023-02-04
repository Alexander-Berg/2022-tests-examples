import re
from itertools import chain
from typing import Set, Type

import pytest

from hamcrest import anything, assert_that, equal_to, has_entries, has_entry, has_key

from billing.yandex_pay.yandex_pay.api.handlers.base import BaseHandler
from billing.yandex_pay.yandex_pay.api.handlers.jwk import JsonWebKeysHandler
from billing.yandex_pay.yandex_pay.api.handlers.utility import PingDBHandler, PingHandler

SWAGGER_EXCLUDED_HANDLERS = (JsonWebKeysHandler, PingHandler, PingDBHandler)

enable_swagger_setting = pytest.mark.parametrize(
    'settings_to_overwrite', [{'API_SWAGGER_ENABLED': True}], indirect=True
)


@pytest.fixture
def swagger_included_handlers(api_handlers) -> Set[Type[BaseHandler]]:
    return {
        handler
        for handler in api_handlers
        if not issubclass(handler, SWAGGER_EXCLUDED_HANDLERS)
    }


def _strip_path_regex(path: str):
    # Strip regex constraints for path parameters to match swagger expressions,
    # e.g. '/events/mastercard/{tail:.*}' -> '/events/mastercard/{tail}'
    return re.sub(r'\{(.+?)\:(.+?)\}', r'{\1}', path)


@pytest.fixture
async def swagger_expected_paths(app, swagger_included_handlers) -> Set[str]:
    urls = chain.from_iterable(app.app.URLS)
    expected_paths = {
        _strip_path_regex(url.path)
        for url in urls
        if url.handler in swagger_included_handlers
    }
    return expected_paths


@pytest.mark.asyncio
@enable_swagger_setting
async def test_swagger_json_accessible(app, swagger_expected_paths):
    response = await app.get('api/doc/swagger.json')
    assert_that(response.status, equal_to(200))

    json_body = await response.json()
    assert_that(json_body, has_key('paths'))
    assert sorted(json_body['paths'].keys()) == sorted(swagger_expected_paths)

    expected_info_part = has_entries({'title': 'Yandex.Pay API', 'version': anything()})
    expected_definitions_part = has_entries({
        'SuccessResponseSchema': has_entries({
            'properties': has_entries({'status': {'type': 'string', 'default': 'success'}}),
        }),
    })
    assert_that(json_body, has_entry('info', expected_info_part))
    assert_that(json_body, has_entry('definitions', expected_definitions_part))


@pytest.mark.asyncio
@enable_swagger_setting
async def test_swagger_ui_accessible(app):
    response = await app.get('api/doc')
    assert_that(response.status, equal_to(200))


@pytest.mark.asyncio
@pytest.mark.parametrize('url', ['api/doc/swagger.json', 'api/doc'])
async def test_can_turn_off_swagger(app, url):
    response = await app.get(url)
    assert_that(response.status, equal_to(404))
