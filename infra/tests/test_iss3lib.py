import json
import os

import mock
from iss3lib import info


def test_get_instance_state():
    with mock.patch('iss3lib.info.load_instance_state', return_value={
        'properties': {1: 2},
        'other_properties': {3: 4}
    }):
        assert info.get_instance_properties() == {1: 2}
    with mock.patch('iss3lib.info.load_instance_state', return_value={
        'other_properties': {3: 4}
    }):
        assert info.get_instance_properties() == {}


def test_load_instance_state():
    with open(info.DUMP_JSON, 'w') as f:
        json.dump({'properties': [1, 2]}, f)
    assert info.load_instance_state() == {'properties': [1, 2]}
    os.remove(info.DUMP_JSON)
