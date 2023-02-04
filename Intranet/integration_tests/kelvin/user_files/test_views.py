from builtins import object
import pytest
from django.utils import timezone

from django.core.urlresolvers import reverse

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment


@pytest.fixture
def course_models(lesson_models, subject_model, teacher):
    """
    Модель курса и связанные модели
    """
    lesson, problem1, problem2, link1, link2 = lesson_models
    course = Course.objects.create(
        name=u'Тестовый курс',
        subject=subject_model,
        owner=teacher,
    )
    clesson = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson,
        order=1,
        date_assignment=timezone.now(),
    )
    return course, clesson


@pytest.fixture
def course_models_assignment(course_models, student):
    course, clesson = course_models
    problem_links_ids = [link.id for link in
                         clesson.lesson.lessonproblemlink_set.all()]
    LessonAssignment.objects.create(
        clesson=clesson, student=student, problems=problem_links_ids[:1],
    )
    return course, clesson


@pytest.mark.django_db
class TestUserFilesCheckUploadViewSet(object):
    """Тесты ручки check_upload"""

    def test_check_upload_unauthorized(self, jclient, student,
                                       course_models_assignment):
        """
        Проверяет, что неавторизованного пользователя не пускают
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.logout()
        url = reverse('v2:user_files-check-upload', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_upload_student(self, jclient, student,
                                  course_models_assignment):
        """
        Проверяет, что Пользователь может загрузить в свою попытку
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.login(user=student)
        url = reverse('v2:user_files-check-upload', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': student.id,
        }, u'Неправильный ответ'

    def test_check_upload_student2(self, jclient, student, student2,
                                   course_models_assignment):
        """
        Проверяет, что другой пользователь не сможет воспользоваться ручкой
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.login(user=student2)
        url = reverse('v2:user_files-check-upload', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_upload_content_manager(self, jclient, student,
                                          content_manager,
                                          course_models_assignment):
        """
        Проверяет, что контент-менеджер проходит проверку
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.login(user=content_manager)
        url = reverse('v2:user_files-check-upload', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_upload_teacher_positive(self, jclient, student, teacher,
                                           course_models_assignment):
        """
        Проверяет, что учитель, владеющий курсом сможет грузить файлы
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.login(user=teacher)
        url = reverse('v2:user_files-check-upload', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_upload_teacher_negative(self, jclient, student,
                                           second_teacher,
                                           course_models_assignment):
        """
        Проверяет, что  произвольный учитель, не сможет грузить файлы
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.login(user=second_teacher)
        url = reverse('v2:user_files-check-upload', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))


@pytest.mark.django_db
class TestUserFilesCheckMetaViewSet(object):
    """Тесты ручки check_meta"""

    def test_check_meta_unauthorized(self, jclient, student,
                                     course_models_assignment):
        """
        Проверяет, что неавторизованного пользователя не пускают
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.logout()
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_meta_student(self, jclient, student,
                                user_custom_answer_with_files,
                                course_models_assignment):
        """
        Проверяет, что пользователь может получить информацию по своим файлам
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?check_keys=1'
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': student.id,
            'public_keys': ['123', '456', '789'],
        }, u'Неправильный ответ'

    def test_check_meta_student2(self, jclient, student, student2,
                                 user_custom_answer_with_files,
                                 course_models_assignment):
        """
        Проверяет, что другой пользователь не может получить информацию
        по файлам
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?check_keys=1'
        jclient.login(user=student2)
        response = jclient.get(url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_meta_student_filters(self, jclient, student,
                                        user_custom_answer_with_files,
                                        course_models_assignment):
        """
        Проверяет, что пользователь может получить информацию если у него
        фильтры вложены в существующие файлы.
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат ()
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?public_keys=123,456&check_keys=1'
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': student.id,
            'public_keys': ['123', '456'],
        }, u'Неправильный ответ'

    def test_check_meta_student_filters2(self, jclient, student,
                                         user_custom_answer_with_files,
                                         course_models_assignment):
        """
        Проверяет, что пользователь не может получить информацию если он
        запрашивает идентификатор файла, которого нет в результате
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат ()
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?public_keys=123,456,79&check_keys=1'
        response = jclient.get(url)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_meta_content_manager(self, jclient, content_manager,
                                        user_custom_answer_with_files,
                                        course_models_assignment, student):
        """
        Проверяет, что контент-менеджер может получить информацию по файлам и
        фильтр по ключам пускает
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?public_keys=123,456,789&check_keys=1'
        jclient.login(user=content_manager)
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': content_manager.id,
            'public_keys': ['123', '456', '789'],
        }, u'Неправильный ответ'

    def test_check_meta_teacher_positive(self, jclient, student, teacher,
                                         user_custom_answer_with_files,
                                         course_models_assignment):
        """
        Проверяет, что учитель может получить информацию
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?check_keys=1'
        jclient.login(user=teacher)
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': teacher.id,
            'public_keys': ['123', '456', '789'],
        }, u'Неправильный ответ'

    def test_check_meta_teacher_negative(self, jclient, second_teacher,
                                         user_custom_answer_with_files,
                                         course_models_assignment, student):
        """
        Проверяет, что учитель не может получить информацию по попытке, где
        он не учитель
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?check_keys=1'
        jclient.login(user=second_teacher)
        response = jclient.get(url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_meta_keys_processing(self, jclient, content_manager,
                                        user_custom_answer_with_files,
                                        course_models_assignment, student):
        """
        Проверяет обработку флага check_keys и его дефолтное значение (0)
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        url += '?check_keys=0'
        jclient.login(user=content_manager)
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': content_manager.id,
        }, u'Неправильный ответ'

        # Проверяем, что по дефолту check_keys == False
        url = reverse('v2:user_files-check-meta', args=(answer['id'],))
        jclient.login(user=content_manager)
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': content_manager.id,
        }, u'Неправильный ответ'


@pytest.mark.django_db
class TestUserFilesCheckDeleteViewSet(object):
    """Тесты ручки check_delete"""

    def test_check_delete_unauthorized(self, jclient, student,
                                       course_models_assignment):
        """
        Проверяет, что неавторизованного пользователя не пускают
        """
        course, clesson = course_models_assignment
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        jclient.logout()
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_delete_student(self, jclient, student,
                                  user_custom_answer_with_files,
                                  course_models_assignment):
        """
        Проверяет, что пользователь может удалять свои файлы
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': student.id,
            'public_keys': ['123', '456', '789'],
        }, u'Неправильный ответ'

    def test_check_delete_student2(self, jclient, student, student2,
                                   user_custom_answer_with_files,
                                   course_models_assignment):
        """
        Проверяет, что другой пользователь не может удалять файлы
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        jclient.login(user=student2)
        response = jclient.get(url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_delete_student_filters(self, jclient, student,
                                          user_custom_answer_with_files,
                                          course_models_assignment):
        """
        Проверяет, что пользователь может удалять файлы если
        фильтры вложены в существующие файлы.
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат ()
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        url += '?public_keys=123,456'
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': student.id,
            'public_keys': ['123', '456'],
        }, u'Неправильный ответ'

    def test_check_delete_student_filters2(self, jclient, student,
                                           user_custom_answer_with_files,
                                           course_models_assignment):
        """
        Проверяет, что пользователь не может удалять файлы если он
        запрашивает идентификатор файла, которого нет в результате
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат ()
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        url += '?public_keys=123,456,79'
        response = jclient.get(url)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_check_delete_content_manager(self, jclient, content_manager,
                                          user_custom_answer_with_files,
                                          course_models_assignment, student):
        """
        Проверяет, что контент-менеджер может удалять файлы и
        фильтр по ключам пускает
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        url += '?public_keys=123,456,789'
        jclient.login(user=content_manager)
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': content_manager.id,
            'public_keys': ['123', '456', '789'],
        }, u'Неправильный ответ'

    def test_check_delete_teacher_positive(self, jclient, student, teacher,
                                           user_custom_answer_with_files,
                                           course_models_assignment):
        """
        Проверяет, что учитель может удалять файлы
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        jclient.login(user=teacher)
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'user_id': teacher.id,
            'public_keys': ['123', '456', '789'],
        }, u'Неправильный ответ'

    def test_check_delete_teacher_negative(self, jclient, second_teacher,
                                           user_custom_answer_with_files,
                                           course_models_assignment, student):
        """
        Проверяет, что учитель не может удалять файлы попытки, где
        он не учитель
        """

        course, clesson = course_models_assignment
        create_data = {
            'answers': user_custom_answer_with_files,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Пробуем получить результат
        url = reverse('v2:user_files-check-delete', args=(answer['id'],))
        jclient.login(user=second_teacher)
        response = jclient.get(url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
