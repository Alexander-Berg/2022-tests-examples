import json
from swagger_spec_validator import validator20


def test_schema():
    swagger_json = json.loads(open('mrc-drive-updater.api.json').read())
    validator20.validate_spec(swagger_json)
