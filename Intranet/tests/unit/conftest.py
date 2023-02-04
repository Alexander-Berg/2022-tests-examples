import pytest

from django_yauth.authentication_mechanisms.tvm.request import TvmServiceRequest
from django_yauth.user import YandexTestUserDescriptor
from django_yauth.authentication_mechanisms import tvm

from tvm2.ticket import ServiceTicket
from tvm2 import TVM2

from common import factories
from plan import settings
from plan.services.models import ServiceTag


@pytest.fixture
def review_required_tags() -> [ServiceTag]:
    return [
        factories.ServiceTagFactory(slug=slug)
        for slug in settings.REVIEW_REQUIRED_TAG_SLUGS
    ]


@pytest.fixture
def patch_tvm(monkeypatch):
    def fake_apply(self, service_ticket, user_ip, user_ticket=None):
        return self.get_service_user(
            service_ticket=service_ticket,
            raw_service_ticket='raw-service-ticket',
            user_ip=user_ip,
        )
    monkeypatch.setattr(TVM2, '_init_context', lambda *args, **kwargs: None)
    monkeypatch.setattr(tvm.Mechanism, 'apply', fake_apply)


@pytest.fixture
def mock_tvm_service_ticket(monkeypatch, request, patch_tvm):
    service_ticket = ServiceTicket({
        'dst': 'xxx',
        'debug_string': 'yyy',
        'logging_string': 'zzz',
        'scopes': '123',
        'src': request.param,
    })
    def fake_get_user(*args, **kwargs):
        return TvmServiceRequest(
            service_ticket=service_ticket,
            uid=None,
            mechanism=tvm.Mechanism(),
        )
    monkeypatch.setattr(YandexTestUserDescriptor, '_get_yandex_user', fake_get_user)
    return request.param
