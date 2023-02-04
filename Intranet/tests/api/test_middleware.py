# coding: utf-8
import random
from typing import Type

import mock
import pytest
from django.contrib.auth.models import AnonymousUser
from django_yauth.authentication_mechanisms import cookie, oauth
from django_yauth.authentication_mechanisms.base import BaseMechanism
from django_yauth.authentication_mechanisms.tvm import TvmServiceRequest, TvmImpersonatedRequest
from django_yauth.user import YandexTestUser, AnonymousYandexUser, YandexUser

from idm.framework.authentication import IDMUserDescriptor, Mechanism as TVMMechanism
from idm.framework.middleware import IDMYandexAuthMiddleware
from idm.tests.utils import get_mocked_tvm_client, FakeRequest, create_user, random_slug

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def request_cls() -> Type[FakeRequest]:
    class _FakeRequest(FakeRequest):
        """Чтобы избежать установки дескриптора другим тестом"""
    return _FakeRequest


def test_idm_yandex_auth_middleware(request_cls: Type[FakeRequest]):
    request = request_cls(method='POST', yauser=YandexTestUser())

    assert not hasattr(request_cls, 'user')
    middleware = IDMYandexAuthMiddleware()
    middleware.process_request(request)

    assert isinstance(getattr(request_cls, 'user'), IDMUserDescriptor)


def test_idm_user_descriptor__user_cached(request_cls: Type[FakeRequest]):
    request = request_cls()
    IDMYandexAuthMiddleware().assign_yauser(request)
    IDMYandexAuthMiddleware().assign_user(request)
    request._user = user = create_user()

    assert request.user is user


def test_idm_user_descriptor__yauser_not_authenticated(request_cls: Type[FakeRequest]):
    request = request_cls(yauser=AnonymousYandexUser())
    IDMYandexAuthMiddleware().assign_user(request)

    assert isinstance(request.user, AnonymousUser)


@pytest.mark.parametrize('real_user', (True, False))
def test_idm_user_descriptor__yauser_by_tvm_service(request_cls: Type[FakeRequest], real_user: bool):
    class ServiceTicket:
        def __init__(self, src: str):
            self.src = src

    user = None
    username = random_slug()
    if real_user:
        user = create_user()
        username = user.username

    with mock.patch('idm.framework.authentication.get_tvm_client', wraps=get_mocked_tvm_client):
        tvm_mechanism = TVMMechanism()
    request = request_cls(
        yauser=TvmServiceRequest(uid=None, service_ticket=ServiceTicket(src=username), mechanism=tvm_mechanism)
    )
    IDMYandexAuthMiddleware().assign_user(request)

    if real_user:
        assert request.user == user
    else:
        assert isinstance(request.user, AnonymousUser)


@pytest.mark.parametrize('real_user', (True, False))
@pytest.mark.parametrize(('yauser_cls', 'mechanism_cls'), (
        (TvmImpersonatedRequest, TVMMechanism),
        (oauth.OauthYandexUser, oauth.Mechanism),
        (YandexUser, cookie.Mechanism),
))
def test_idm_user_descriptor__yauser_by_oid(
        request_cls: Type[FakeRequest],
        real_user: bool,
        yauser_cls: Type[YandexUser],
        mechanism_cls: Type[BaseMechanism],
):
    user = None
    uid = random.randint(1, 10**8)
    if real_user:
        user = create_user(uid=uid)
        uid = user.uid

    with mock.patch('idm.framework.authentication.get_tvm_client', wraps=get_mocked_tvm_client):
        tvm_mechanism = mechanism_cls()
    request = request_cls(
        yauser=yauser_cls(uid=uid, mechanism=tvm_mechanism)
    )
    IDMYandexAuthMiddleware().assign_user(request)

    if real_user:
        assert request.user == user
    else:
        assert isinstance(request.user, AnonymousUser)
