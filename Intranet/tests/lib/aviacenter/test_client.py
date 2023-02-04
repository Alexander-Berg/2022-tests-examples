from intranet.trip.src.lib.aviacenter.api import aviacenter


def test_params():
    params = {
        'true': True,
        'false': False,
        'str': 'str',
        'int': 123,
        'array': [1, 2, 3],
        'empty_array': [],
        'object': {
            '1': {
                'sub_obj': {
                    '2': {
                        'field': 'value1',
                    },
                },
            },
            '2': {
                'sub_obj': {
                    '1': {
                        'field': 'value2',
                    },
                    '2': {
                        'field': 'value3',
                    },
                },
            },
        },
        'list_of_objects': [
            {
                'kek': 1,
            },
            {
                'lol': 2,
            },
        ],
        'none': None,
    }
    prepared_params = aviacenter._prepare_params(params)
    assert prepared_params == {
        'true': 1,
        'false': 0,
        'str': 'str',
        'int': '123',
        'array[]': ['1', '2', '3'],
        'object[1][sub_obj][2][field]': 'value1',
        'object[2][sub_obj][1][field]': 'value2',
        'object[2][sub_obj][2][field]': 'value3',
        'list_of_objects[0][kek]': '1',
        'list_of_objects[1][lol]': '2',
        'auth_key': '',
    }
