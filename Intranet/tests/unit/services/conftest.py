from mock import patch
import pretend
import pytest

from unit.idm.conftest import patch_tvm  # noqa: F401


@pytest.fixture
def move_services_subtasks():
    with patch('plan.api.idm.actions.move_service') as idm_call:
        with patch('plan.api.idm.actions.assert_service_node_exists') as idm_exists_call:
            with patch('plan.services.tasks.rerequest_roles') as rerequest_roles:
                with patch('plan.services.tasks.notify_staff') as notify_staff:
                    yield pretend.stub(
                        idm_call=idm_call,
                        idm_exists_call=idm_exists_call,
                        rerequest_roles=rerequest_roles,
                        notify_staff=notify_staff,
                    )
