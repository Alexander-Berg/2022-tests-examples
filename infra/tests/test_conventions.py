"""
Ensure bundled checks follow conventions.
https://a.yandex-team.ru/arc/trunk/arcadia/infra/rtc/juggler/bundle#conventions

"""
# TODO: implement rest of the tests, see conventions url above.

import pytest
import re

from infra.rtc.juggler.bundle.checks import create_manifest


CHECK_NAME_REGEXP = '^[a-z0-9_]+$'
CHECK_NAME_REGEXP_COMPILED = re.compile(CHECK_NAME_REGEXP)

ALL_CHECKS_NAMES = []
ALL_SERVICES_NAMES = []

for check in create_manifest().to_dict()['checks']:
    ALL_CHECKS_NAMES.append(check['check_name'])
    for service in check['services']:
        ALL_SERVICES_NAMES.append(service)


@pytest.mark.parametrize("name", ALL_CHECKS_NAMES)
def test_checks_names_match_regexp(name):
    assert re.match(CHECK_NAME_REGEXP_COMPILED, name) is not None


@pytest.mark.parametrize("name", ALL_SERVICES_NAMES)
def test_services_names_match_regexp(name):
    assert re.match(CHECK_NAME_REGEXP_COMPILED, name) is not None


@pytest.mark.parametrize("name", ALL_CHECKS_NAMES)
def test_check_names_has_no_boilerplate_words(name):
    assert not name.startswith('check_')
    assert not name.endswith('_check')


@pytest.mark.parametrize("name", ALL_SERVICES_NAMES)
def test_service_names_has_no_boilerplate_words(name):
    assert not name.startswith('check_')
    assert not name.endswith('_check')
