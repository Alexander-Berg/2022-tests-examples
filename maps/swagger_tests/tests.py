import json
import logging
import os
from jsonschema import RefResolver
from swagger_spec_validator import validator20
from yatest.common import source_path

logger = logging.getLogger("test_logger")


def resolve_absolute_path(path):
    prefix, infix, suffix = path.partition('#/')
    absolute_path = os.path.join(
        source_path('maps/wikimap/feedback/api/schemas/v1'),
        prefix)
    if path.endswith(".json#"):
        return 'file://' + absolute_path
    if not prefix or not infix:
        return path
    return 'file://' + absolute_path + infix + suffix


def patch_refs(schema_fragment):
    """modifies relative file refs, replaces them with file uri with absolute path

       e.g. fragment "$ref": "definitions.json#/Pet" will be replaced with
        "$ref": "file:///${ARCADIA_ROOT}/PATH_IN_REPO/definitions.json#/Pet"
    """
    if isinstance(schema_fragment, dict):
        for key in schema_fragment:
            if key == '$ref':
                schema_fragment[key] = resolve_absolute_path(schema_fragment[key])
            else:
                patch_refs(schema_fragment[key])
    elif isinstance(schema_fragment, list):
        for item in schema_fragment:
            patch_refs(item)


def test_that_swagger_schema_is_valid():
    swagger_config = json.loads(open('feedback.api.json').read())
    patch_refs(swagger_config)
    assert isinstance(validator20.validate_spec(swagger_config), RefResolver)
