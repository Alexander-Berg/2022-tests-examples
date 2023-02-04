from copy import deepcopy


def _head(role, login):
    return {
        'role': role,
        'person': {
            'login': login,
        }
    }


def response_without_last_head(*args, **kwargs):
    dep1 = {
        'id': 1001,
        'heads': [
            _head('chief', 'chief1'),
        ],
    }
    dep2 = {
        'id': 1002,
        'heads': [
            _head('chief', 'chief2'),
            _head('hr_partner', 'hr1'),
            _head('hr_partner', 'hr2'),
        ],
    }
    dep3 = {
        'id': 1003,
        'heads': [],
    }

    return [
        {
            'department': deepcopy(dep1),
            'ancestors': [],
        },
        {
            'department': deepcopy(dep2),
            'ancestors': [
                {'department': deepcopy(dep1)},
            ],
        },
        {
            'department': deepcopy(dep3),
            'ancestors': [
                {'department': deepcopy(dep1)},
                {'department': deepcopy(dep2)},
            ],
        },
    ]


def response_with_last_head(*args, **kwargs):
    data = response_without_last_head()
    data[-1]['department']['heads'] = [_head('chief', 'chief3')]
    return data
