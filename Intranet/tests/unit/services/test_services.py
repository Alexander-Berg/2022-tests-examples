import pytest

from django.core.exceptions import ValidationError

from common import factories
from plan.services.models import Service


@pytest.mark.parametrize('request_state', ('requested', 'partially_approved', 'approved', 'processing_idm', 'processing_abc'))
def test_delete_service_with_moving_descendants(request_state):
    parent = factories.ServiceFactory(state=Service.states.IN_DEVELOP)
    child = factories.ServiceFactory(parent=parent)
    factories.ServiceMoveRequestFactory(service=child, state=request_state)
    with pytest.raises(ValidationError):
        parent.state = Service.states.DELETED
        parent.save(update_fields=['state'])
