from mock import MagicMock, patch
from staff.lib.tvm2 import (
    get_calling_service_name,
    get_tvm_ticket_by_deploy,
)


def test_get_tvm_ticket_by_deploy():
    service_name = 'test'
    data = {
        service_name: {
            'ticket': 'ticket',
            'tvm_id': 2000401
        }
    }
    response = MagicMock(**{'json.return_value': data})
    session = MagicMock(**{'get.return_value': response})
    with patch('staff.lib.tvm2.tvm2_deploy_session', session):
        res = get_tvm_ticket_by_deploy(service_name, 2000401)
        assert res == 'ticket'


def test_get_calling_service_name():
    tvm_id = 1
    expected_to_find = 'found'

    tvm_settings = {
        expected_to_find: tvm_id,
        'not_found': tvm_id + 1,
    }

    request = MagicMock(**{'yauser.service_ticket.src': tvm_id})

    with patch('staff.lib.tvm2.settings.TVM_APPLICATIONS', tvm_settings):
        assert get_calling_service_name(request) == expected_to_find
