import pytest
from django.conf import settings

from bcl.core.models import Service
from bcl.exceptions import TvmNoTicket, TvmServiceNotAllowed
from bcl.toolbox.tvm import authorize_with_tvm


def test_authorize_with_tvm_no_ticket(request_factory):
    with pytest.raises(TvmNoTicket):
        authorize_with_tvm(request_factory.get('/'))


invalid_tvm_id = 1253451423451324


def test_authorize_with_tvm_service_not_allowed(patch_tvm_auth, request_factory):
    patch_tvm_auth(invalid_tvm_id)
    with pytest.raises(TvmServiceNotAllowed) as exc_info:
        authorize_with_tvm(request_factory.get('/'))
    assert exc_info.value.msg == TvmServiceNotAllowed.default_message % invalid_tvm_id


@pytest.fixture()
def valid_tvm_id():
    return settings.BCL_TVM_ID_TEST


@pytest.fixture()
def valid_service(valid_tvm_id):
    return Service.get_by_tmv_id(valid_tvm_id)


def test_authorize_with_ok(valid_tvm_id, valid_service, patch_tvm_auth, request_factory):
    patch_tvm_auth(valid_tvm_id)
    res = authorize_with_tvm(request_factory.get('/'))
    assert res.service.id == valid_service.id
    assert res.service_id == valid_service.id
    assert res.service.alias == valid_service.alias
    assert res.client_alias == valid_tvm_id


def test_authorize_with_ok_and_service_alias(
    valid_tvm_id, valid_service, patch_tvm_auth,
    request_factory
):
    patch_tvm_auth(valid_tvm_id)
    res = authorize_with_tvm(
        request_factory.get('/', **{'HTTP_X-Bcl-Service-Alias': 'test_service'})
    )
    assert res.service.alias == 'test_service'
