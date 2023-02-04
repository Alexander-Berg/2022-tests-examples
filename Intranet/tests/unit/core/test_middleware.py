import pytest
from django.conf import settings
from unittest.mock import Mock

from intranet.femida.src.core.middleware import MagicLinksMiddleware

from intranet.femida.tests import factories as f


@pytest.mark.parametrize('model_name', settings.MAGICLINKS_SUPPORTED_MODELS)
@pytest.mark.parametrize('prefix', ('api', '_api/magiclinks'))
def test_disabled_magiclinks_middleware_request_processing(model_name, prefix):
    request = Mock()
    request.yauser.authenticated_by.mechanism_name = 'tvm'
    request.yauser.service_ticket.src = settings.TVM_MAGICLINKS_CLIENT_ID
    request.path = f'/{prefix}/{model_name}/1234'
    middleware = MagicLinksMiddleware()
    response = middleware.process_request(request)
    assert response.status_code == 404


@pytest.mark.parametrize('model_name', settings.MAGICLINKS_SUPPORTED_MODELS)
@pytest.mark.parametrize('prefix', ('api', '_api/magiclinks'))
def test_enabled_magiclinks_middleware_request_processing(model_name, prefix):
    f.create_waffle_switch('enable_magiclinks_for_{}'.format(model_name))
    request = Mock()
    request.yauser.authenticated_by.mechanism_name = 'tvm'
    request.yauser.service_ticket.src = settings.TVM_MAGICLINKS_CLIENT_ID
    request.path = f'/{prefix}/{model_name}/1234'
    middleware = MagicLinksMiddleware()
    response = middleware.process_request(request)
    assert not response
