# coding: utf-8
from __future__ import unicode_literals

schema = {
    'id': {'type': 'number'},
    'login': {'type': 'string'},
    'date': {"type": "string", "format": "date-time", "pattern": "^\\d{4}-\\d{2}-\\d{2}$"},
    'favourite_number': {'type': 'integer'},
    'is_dismissed': {'type': 'boolean'},
    "quit_at": {
        "type": [
            "null",
            "string",
        ]
    },
    'employment': {
        'enum': [
            'full',
            'partial',
            'secondary',
        ]
    },
    'tshirt': {
        'enum': [
            1,
            2,
            3,
        ]
    },
    'akward': {
        'type': [
            'integer',
            'string',
        ]
    },
    'more_akward': {
        'type': [
            'string',
            'integer',
        ]
    },
    'somestring': {},
}
