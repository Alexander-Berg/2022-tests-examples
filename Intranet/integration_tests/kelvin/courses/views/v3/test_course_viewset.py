from builtins import object
import pytest

from django.core.urlresolvers import reverse

from kelvin.courses.models import Course, CourseStudent


@pytest.fixture
def test_data(some_owner, subject_model):
    """
    Модели для тестов
    """
    course = Course.objects.create(owner=some_owner, name=u'Курс',
                                   subject=subject_model)
    return course

@pytest.mark.exact
@pytest.mark.django_db
class TestCourseViewSet(object):
    """
    Тесты доступа к методам курса
    """
    def test_list(self, jclient, teacher, student, content_manager, parent,
                  test_data):
        """
        Тест получения списка курсов, создания курса
        """
        list_url = reverse('v3:course-list')

        # неавторизованный пользователь не имеет доступа
        response = jclient.get(list_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        # ученик не имеет доступ
        jclient.login(user=student)
        response = jclient.get(list_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        # учитель, родитель и контент-менеджер имеют доступ
        for user in (teacher, content_manager, parent):
            jclient.login(user=user)
            response = jclient.get(list_url)
            assert response.status_code == 200, (
                u'Неправильный статус ответа: {0}'.format(response.content))

        # никто не может создавать курсы
        jclient.logout()
        response = jclient.post(list_url, {})
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        jclient.login(user=student)
        response = jclient.get(list_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        for user in (teacher, content_manager, parent):
            jclient.login(user=user)
            response = jclient.get(list_url, {})
            assert response.status_code == 200, (
                u'Неправильный статус ответа: {0}'.format(response.content))
