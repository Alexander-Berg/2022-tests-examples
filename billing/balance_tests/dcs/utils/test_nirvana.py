from balance.actions.dcs.utils import nirvana as nirvana_utils


def test_get_nirvana_io_by_type():
    input_example = {
        'key': {
            'name': 'key',
            'type': 'INPUT',
            'items': [{'dataType': 'json'}],
        }
    }

    assert nirvana_utils.get_io_by_type({}, 'text') is None
    assert nirvana_utils.get_io_by_type(input_example, 'text') is None
    assert nirvana_utils.get_io_by_type(input_example, 'json') == 'key'
