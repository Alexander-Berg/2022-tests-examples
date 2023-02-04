import json
import logging
import re

import pytest  # noqa

from ads.emily.viewer.backend.app.schema import validate
from ads.emily.viewer.backend.app.config import SANDBOX_RESOURCE

logger = logging.getLogger(__name__)

YA_MAKE_STABLE_JSON_PATTERN = re.compile(r"SET\(STABLE_JSON (?P<resource_id>\d+)\)")


def test_validate_marshmallow_stable_json(stable_json):
    logger.info("Validating marshmallow stable.json")
    data = json.loads(stable_json)
    errors = validate.validate_marshmallow(data, validate.FullDumpSchema(), force=True)
    assert not errors, "Stable | Validation Errors:\n{}".format(json.dumps(errors, sort_keys=True, indent=3))


def test_stable_json_resource_id_equal_to_package(ml_backend_pkg, test_ya_make):

    def get_id_from_ya_make(data):
        m = None
        for line in data.splitlines():
            m = YA_MAKE_STABLE_JSON_PATTERN.match(line)
            if m:
                break
        assert m, "No STABLE_JSON resource_id found in tests/ya.make\nAdd: \n- SET(STABLE_JSON <resource_id>)"
        return int(m.group("resource_id"))

    def get_id_from_pkg(data):
        ids = [x["source"]["id"] for x in data["data"] if x["source"]["type"] == "SANDBOX_RESOURCE"]
        assert ids, "No SANDBOX_RESOURCE source found in ml-backend-pkg.json"
        assert len(ids) == 1, "More than one SANDBOX_RESOURCE source found in ml-backend-pkg.json"
        return ids[0]

    def get_id_from_config():
        return SANDBOX_RESOURCE

    ya_make_id = get_id_from_ya_make(test_ya_make)
    pkg_id = get_id_from_pkg(ml_backend_pkg)
    config_id = get_id_from_config()

    assert ya_make_id == pkg_id, f"ya.make STABLE_JSON id != ml-backend-pkg.json id | {ya_make_id} != {pkg_id}"
    assert pkg_id == config_id, f"ml-backend-pkg.json id != app.config.SANDBOX_RESOURCE (defailt) | {pkg_id} != {config_id}"
