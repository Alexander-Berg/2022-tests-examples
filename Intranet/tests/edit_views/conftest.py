import json

import pytest

from django.core.urlresolvers import reverse


@pytest.fixture()
def person_profile_request_factory(rf):
    class Factory:
        def post(self, person, handler, data):
            request = rf.post(
                reverse(handler, kwargs={'login': person.login}),
                json.dumps(data),
                content_type='application/json'
            )
            request.service_is_readonly = False
            request.real_user_ip = '0.0.0.0'
            request.user = person.user
            return request

    return Factory()
