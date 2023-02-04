"""
Source: https://tech.yandex.ru/routing/distance_matrix/doc/concepts/structure-docpage/#structure__jsonschema
but with some fixes to fit current format:
* added traffic_type
* added behavior_info
* added "FAIL" into "status_failed"
"""

SCHEMA = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "definitions": {
        "status_ok": {
            "type": "string",
            "enum": ["OK"]
        },
        "status_failed": {
            "type": "string",
            "enum": ["FAIL", "FAILED"]
        },
        "duration_value": {
            "type": "object",
            "description": "Route duration in seconds",
            "properties": {
                "value": {
                    "type": "integer"
                }
            },
            "required": ["value"],
            "additionalProperties": False
        },
        "distance_value": {
            "type": "object",
            "description": "Route distance in meters",
            "properties": {
                "value": {
                    "type": "integer"
                }
            },
            "required": ["value"],
            "additionalProperties": False
        },
        "cell_ok": {
            "type": "object",
            "description": "Matrix cell, contains transit distance and duration of route for speficied mode of transport",
            "properties": {
                "status": {"$ref": "#/definitions/status_ok"},
                "duration": {"$ref": "#/definitions/duration_value"},
                "distance": {"$ref": "#/definitions/distance_value"}
            },
            "required": ["status", "duration", "distance"],
            "additionalProperties": False
        },
        "cell_failed": {
            "type": "object",
            "description": "Failed matrix cell, reported when route can not be built between specified points",
            "properties": {
                "status": {"$ref": "#/definitions/status_failed"}
            },
            "required": ["status"],
            "additionalProperties": False
        },
        "traffic_type": {
            "type": "string",
            "enum": ["realtime", "forecast"]
        },
        "behavior_info": {
            "type": "object",
            "properties": {
                "l0_time_model": {"type": "string"},
                "l1_time_model": {"type": "string"},
                "name": {"type": "string"},
                "realtime": {"type": "boolean"},
                "time_switch": {"type": "integer"},
            },
            "additionalProperties": False
        },
        "matrix_response": {
            "type": "object",
            "description": "Distance and duration matrix response",
            "properties": {
                "rows": {
                    "type": "array",
                    "description": "Matrix rows",
                    "items": {
                        "type": "object",
                        "properties": {
                            "elements": {
                                "type": "array",
                                "description": "Matrix row",
                                "items": {
                                    "oneOf": [
                                        {"$ref": "#/definitions/cell_ok"},
                                        {"$ref": "#/definitions/cell_failed"}
                                    ]
                                },
                                "additionalItems": False,
                                "minItems": 1
                            }
                        },
                        "required": ["elements"],
                        "additionalProperties": False
                    },
                    "additionalItems": False,
                    "minItems": 1
                },
                "traffic_type": {"$ref": "#/definitions/traffic_type"},
                "behavior_info": {"$ref": "#/definitions/behavior_info"}
            },
            "required": ["rows"],
            "additionalProperties": False
        },
        "error_response": {
            "type": "object",
            "description": "Error response",
            "properties": {
                "errors": {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 1,
                    "additionalItems": False
                }
            },
            "required": ["errors"],
            "additionalProperties": False
        }
    },
    "type": "object",
    "oneOf": [
        {"$ref": "#/definitions/matrix_response"},
        {"$ref": "#/definitions/error_response"}
    ]
}
