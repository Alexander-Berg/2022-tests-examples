import pytest
import json
import random

@pytest.fixture(scope="function")
def staff_resource():
    staff = json.loads(open('source/tests/test_vpnrobot_datacollect', 'r').read())['chef']
    return staff[random.randint(0, len(staff)-1)]

@pytest.fixture(scope="function")
def group_resource():
    groups = json.loads(open('source/tests/test_vpnrobot_datacollect', 'r').read())['vpngroups']
    return groups[random.randint(0, len(groups)-1)]

@pytest.fixture(scope="function", params=[
    [1, ['a','b'], 'shshsh'],
    ['av', 'va', 'trtr'],
    [0.12, 4, [2,4]],
    ['bark', 'foo', {'department': [1, 2], 'ancestors': {1: 'bla'}}],
    [1, 'foo', {'department': [1, 2], 'ancestors': {1: 'bla'}}]
])
def typeerror_resource(request):
    return request.param


@pytest.fixture(scope="function", params=[
    'foo', 1, 1.01, -1, (0, 1), [0, 1], {'0': 0, '1': 1}
])
def ret_resource(request):
    return request.param

@pytest.fixture(scope="function", params=[
    'foo', 1, 1.01, -1, (0, 1), [0, 1], {'0': 0, '1': 1},
    {'result': []}, {'result': 0}, {'result': {}}
])
def ret_resource1(request):
    return request.param