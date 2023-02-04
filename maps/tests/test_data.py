import json
from jsonschema import validate, ValidationError
import logging
import os

import yatest.common

logger = logging.getLogger("test_data_logger")

schema = {
    "type": "object",
    "properties": {
        "lat": {"type": "number"},
        "lon": {"type": "number"},
        "radiostations": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "frequency": {"type": "number"},
                    "group_id": {"type": "string"},
                    "name": {"type": "string"},
                    "radius": {"type": "number"}
                },
                "required": ["frequency", "group_id", "name"]
            }
        }
    },
    "required": ["lat", "lon", "radiostations"]
}


def validate_file(path):
    with open(path) as in_file:
        try:
            validate(json.load(in_file), schema)
        except (ValidationError, ValueError) as err:
            logger.error("{}: {}".format(path, err.message))
            assert False


def validate_dir(path):
    for entity in os.listdir(path):
        if os.path.isdir(os.path.join(path, entity)):
            validate_dir(os.path.join(path, entity))
        elif entity.endswith('.json'):
            validate_file(os.path.join(path, entity))


def test_data():
    data_dir = yatest.common.source_path('maps/automotive/radio/data/broadcasts')
    validate_dir(data_dir)
