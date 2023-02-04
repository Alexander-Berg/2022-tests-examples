import pytest
from mock.mock import MagicMock, call

from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse
from django.http import Http404
from django.template.response import TemplateResponse

from kelvin.mail.views import CONFIRMATION_PAGE_TEMPLATE, SUCCESS_PAGE_TEMPLATE, unsubscribe_view

User = get_user_model()


@pytest.mark.xfail
def test_unsubscribe_view(rf, mocker):
    """
    Тесты страниц отписки от рассылки
    """
    code = '111'
    view_url = reverse(
        'scheduled_email_unsubscribe', kwargs={'user_code': code})
    mocked_get_object_or_404 = mocker.patch(
        'kelvin.mail.views.get_object_or_404')

    # Пользователь не найден
    mocked_get_object_or_404.side_effect = Http404
    request = rf.get(view_url)
    request.yauser = MagicMock()
    request.user = MagicMock()

    response = unsubscribe_view(request, code)

    assert response.status_code == 404, u'Неправильный статус ответа'
    assert mocked_get_object_or_404.mock_calls == [
        call(User, parent_code='111'),
    ], u'Нужно найти пользователя по коду'

    mocked_get_object_or_404.reset_mock()
    mocked_get_object_or_404.side_effect = None

    # Гет-запрос, отписанный пользователь
    mocked_get_object_or_404.return_value = MagicMock(id=1, unsubscribed=True)
    request = rf.get(view_url)
    request.yauser = MagicMock()
    request.user = MagicMock()

    response = unsubscribe_view(request, code)

    assert mocked_get_object_or_404.mock_calls == [
        call(User, parent_code='111'),
    ], u'Нужно найти пользователя по коду'
    assert isinstance(response, TemplateResponse), u'Нужно ответить шаблоном'
    assert response.template_name == SUCCESS_PAGE_TEMPLATE, (
        u'Нужно ответить шаблоном успешной операции')
    assert mocked_get_object_or_404.return_value.mock_calls == [], (
        u'Не должно быть действий с пользователем')

    mocked_get_object_or_404.reset_mock()

    # Гет-запрос, неотписанный пользователь
    mocked_get_object_or_404.return_value = MagicMock(id=1, unsubscribed=False)
    request = rf.get(view_url)
    request.yauser = MagicMock()
    request.user = MagicMock()

    response = unsubscribe_view(request, code)

    assert mocked_get_object_or_404.mock_calls == [
        call(User, parent_code='111'),
    ], u'Нужно найти пользователя по коду'
    assert isinstance(response, TemplateResponse), u'Нужно ответить шаблоном'
    assert response.template_name == CONFIRMATION_PAGE_TEMPLATE, (
        u'Нужно ответить шаблоном подтверждения операции')
    assert mocked_get_object_or_404.return_value.mock_calls == [], (
        u'Не должно быть действий с пользователем')

    mocked_get_object_or_404.reset_mock()

    # Пост-запрос, неотписанный пользователь
    request = rf.post(view_url)
    request.yauser = MagicMock()
    request.user = MagicMock()
    response = unsubscribe_view(request, code)

    assert mocked_get_object_or_404.mock_calls == [
        call(User, parent_code='111'),
        call().save(),
    ], u'Нужно найти пользователя по коду и изменить его состояние'
    assert isinstance(response, TemplateResponse), u'Нужно ответить шаблоном'
    assert response.template_name == SUCCESS_PAGE_TEMPLATE, (
        u'Нужно ответить шаблоном успешной операции')
    assert mocked_get_object_or_404.return_value.unsubscribed is True, (
        u'Пользователь должен стать отписанным')

    mocked_get_object_or_404.reset_mock()

    # Патч-запрос
    request = rf.patch(view_url)
    request.yauser = MagicMock()
    request.user = MagicMock()
    response = unsubscribe_view(request, code)

    assert response.status_code == 405, u'Неправильный статус ответа'
    assert mocked_get_object_or_404.mock_calls == [], (
        u'Запросы кроме GET и POST не должны обрабатываться')
