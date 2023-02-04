from builtins import object
import pytest

from django.core.urlresolvers import reverse
from kelvin.courses.models import CoursePermission
from integration_tests.fixtures.courses import make_course_available_for_student


@pytest.mark.django_db
class TestSourceCourseViewSet(object):
    """
    Тесты доступа к API курсов-источников
    """

    def test_detail(self, jclient, content_manager, student, teacher,
                    source_course):
        """
        Тест получения одного курса-источника
        """
        detail_url = reverse('v2:source_course-detail',
                             args=(source_course.id,))

        # неавторизованный пользователь не имеет доступа
        response = jclient.get(detail_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # ученик не имеет доступа
        jclient.login(user=student)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # учитель и контент-менеджер имеют доступ
        for user in (teacher, content_manager):
            make_course_available_for_student(source_course, user)
            CoursePermission.objects.get_or_create(
                user=user, 
                course=source_course, 
                defaults={"permission": CoursePermission.OWNER | CoursePermission.CONTENT_MANAGER}
            )

            jclient.login(user=user)
            response = jclient.get(detail_url)
            assert response.status_code == 200, (
                u'Неправильный статус ответа: {0}'.format(response.content))
