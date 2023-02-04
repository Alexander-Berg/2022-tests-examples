from unittest import mock
import pytest
from django.http import HttpRequest

from intranet.audit.src.users.models import User
from intranet.audit.src.users.middleware import AuthMiddleware, auth_middleware_exempt
from intranet.audit.src.api_v1.views.view import APIView


@pytest.fixture
def exempt_all_view():
    @auth_middleware_exempt()
    class DummyView(APIView):
        pass

    return DummyView


@pytest.fixture
def exempt_user_view():
    @auth_middleware_exempt(need_yauser=True)
    class DummyView(APIView):
        pass

    return DummyView


@pytest.fixture
def test_request():
    return HttpRequest()


@pytest.fixture
def test_middleware():
    return AuthMiddleware()


def test_request_not_save(db, dummy_yauser, test_request, test_middleware, ):
    assert User.objects.count() == 0
    with mock.patch('intranet.audit.src.users.middleware.authenticate') as yauth_mock:
        yauth_mock.return_value = dummy_yauser
        test_middleware.process_view(test_request, mock.Mock(), (), {})
    assert User.objects.count() == 0
    yauth_mock.assert_called_with(request=test_request)

    user = test_request.user
    yauser = test_request.yauser
    assert user.uid == yauser.uid
    assert user.login == yauser.login
    assert user.first_name == yauser.first_name
    assert user.last_name == yauser.last_name


def test_request_auth_middleware_exempt_all_success(transactional_db, test_request, test_middleware,
                                                    django_assert_num_queries, exempt_all_view,
                                                    ):
    assert User.objects.count() == 0
    view_func = exempt_all_view.as_view()
    with mock.patch('intranet.audit.src.users.middleware.authenticate') as yauth_mock:
        with django_assert_num_queries(0):
            test_middleware.process_view(test_request, view_func, (), {})
    assert yauth_mock.called is False
    assert User.objects.count() == 0


def test_request_auth_middleware_exempt_user_success(transactional_db, test_request, test_middleware,
                                                     django_assert_num_queries, exempt_user_view, dummy_yauser,
                                                     ):
    assert User.objects.count() == 0
    view_func = exempt_user_view.as_view()
    with mock.patch('intranet.audit.src.users.middleware.authenticate') as yauth_mock:
        yauth_mock.return_value = dummy_yauser
        with django_assert_num_queries(0):
            test_middleware.process_view(test_request, view_func, (), {})
    yauth_mock.assert_called_with(request=test_request)
    assert User.objects.count() == 0


def test_request_auth_middleware_no_yauser_success(transactional_db, test_request, test_middleware,
                                                   django_assert_num_queries, exempt_user_view, dummy_yauser,
                                                   ):
    assert User.objects.count() == 0
    view_func = exempt_user_view.as_view()
    test_request.META['HTTP_HOST'] = 'test.yandex-team.ru'
    with mock.patch('intranet.audit.src.users.middleware.authenticate') as yauth_mock:
        yauth_mock.return_value = None
        with django_assert_num_queries(0):
            response = test_middleware.process_view(test_request, view_func, (), {})
    yauth_mock.assert_called_with(request=test_request)
    assert response.url == 'https://passport.yandex-team.ru/auth?retpath=http%3A//test.yandex-team.ru'
    assert User.objects.count() == 0


def test_request_auth_middleware_no_yauser_json_success(transactional_db, test_request, test_middleware,
                                                        django_assert_num_queries, exempt_user_view, dummy_yauser,
                                                        ):
    assert User.objects.count() == 0
    view_func = exempt_user_view.as_view()
    test_request.content_type = 'application/json'
    with mock.patch('intranet.audit.src.users.middleware.authenticate') as yauth_mock:
        yauth_mock.return_value = None
        with django_assert_num_queries(0):
            response = test_middleware.process_view(test_request, view_func, (), {})
    yauth_mock.assert_called_with(request=test_request)
    assert response.data['message'] == ['Invalid credentials']
    assert User.objects.count() == 0
