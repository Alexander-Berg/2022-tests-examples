from builtins import object
import pytest

from django.core.urlresolvers import reverse
from kelvin.courses.models import Course, CourseStudent


@pytest.mark.django_db
class TestActivationCodeViewSet(object):
    """
    Тесты апи работы с учительскими кодами
    """

    def test_anonymous_code(self, jclient, empty_activation_code):
        """
        Анонимный пользователь должен всегда получать ошибку 401
        """

        detail_url = reverse("v2:activation_code-activate")
        response = jclient.post(detail_url, {
            'activation_code': empty_activation_code.code,
        })
        assert response.status_code == 401, (
            u'Ручкой может пользоваться только авторизованный пользователь')

    def test_empty_code(self, jclient, second_teacher, empty_activation_code):
        """
        Если учитель ввел код, который существует, но к которому
        нет действий, то ничего не произойдет
        """

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=second_teacher)
        response = jclient.post(detail_url, {
            'activation_code': empty_activation_code.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        assert Course.objects.filter(owner=second_teacher).count() == 0, (
            u'Код пустой, так что курсы не должны были скопироваться')

    def test_code_copy_one_course(self, jclient, second_teacher,
                                  code_copy_one_course):
        """
        Тестируем код, который скопирует учителю один курс
        """

        # Изначально у учителя не должно быть курсов

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=second_teacher)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        assert Course.objects.filter(owner=second_teacher).count() == 1, (
            u'Должен был скопироваться один курс')

    def test_code_copy_one_course_already_have(self, jclient, teacher,
                                               code_copy_one_course):
        """
        Тестируем код, который скопирует учителю один курс который уже есть
        у этого учителя
        """

        # Изначально у учителя должен быть один курс

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=teacher)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        assert Course.objects.filter(owner=teacher).count() == 1, (
            u'Курс не должен быть скопирован')

    def test_code_copy_two_courses(self, jclient, second_teacher,
                                   code_copy_two_courses):
        """
        Тестируем код, который скопирует учителю два курса
        """

        # Изначально у учителя не должно быть курсов

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=second_teacher)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_two_courses.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        assert Course.objects.filter(owner=second_teacher).count() == 2, (
            u'Должны были скопироваться оба курса')

    def test_code_student(self, jclient, student, code_copy_one_course):
        """
        Тест проверяет, что ученик может воспользоваться кодом
        (и стать учителем)
        """

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=student)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')
        student.refresh_from_db()
        assert student.is_teacher, (
            u'Ученик должен был стать учителем')
        assert Course.objects.filter(owner=student).count() == 1, (
            u'Должен был скопироваться курс')

    def test_code_retry(self, jclient, student, student2,
                        code_copy_one_course):
        """
        Тест проверяет, что один код не может использоваться несколько раз
        """

        detail_url = reverse("v2:activation_code-activate")

        jclient.login(user=student)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        jclient.login(user=student2)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 404, (
            u'Код уже был использован')
        assert Course.objects.filter(owner=student2).count() == 0, (
            u'Ученику не должно было ничего скопироваться')

    def test_code_retry_same_user(self, jclient, student,
                                  code_copy_one_course):
        """
        Тест проверяет, что при успешной активации одиин и тот же пользователь
        может полчуть положительный ответ произвольнеое число раз.
        """

        detail_url = reverse("v2:activation_code-activate")

        jclient.login(user=student)
        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        response = jclient.post(detail_url, {
            'activation_code': code_copy_one_course.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

    def test_code_set_subject(self, jclient, student, code_with_subject,
                              subject_model):
        """
        Проверяем, что код установит учителю правильный предмет
        """

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=student)
        response = jclient.post(detail_url, {
            'activation_code': code_with_subject.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')
        student.refresh_from_db()
        assert student.teacher_profile.subject == subject_model, (
            u'Должен был проставиться предмет')

    def test_code_set_activated_by(self, jclient, student,
                                   empty_activation_code):
        """
        Проверяем, что после работы у кода правильно заполнится поле
        activated_by
        """

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=student)
        response = jclient.post(detail_url, {
            'activation_code': empty_activation_code.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')
        empty_activation_code.refresh_from_db()
        assert empty_activation_code.activated_by == student, (
            u'Должен был проставиться активирующий пользователь')

    def test_code_course_to_add(self, jclient, second_teacher, course,
                                code_course_to_add):
        """
        Тестируем код, который добавит учителя как ученика в курс
        """
        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=second_teacher)
        response = jclient.post(detail_url, {
            'activation_code': code_course_to_add.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        assert CourseStudent.objects.filter(
            student=second_teacher,
            course=course,
        ).count() == 1, u'Учитель должен стать учеником в этом курсе'

    def test_code_course_to_add_already_have(self, jclient, second_teacher,
                                             course, code_course_to_add):
        """
        Тестируем код, который добавит учителя как ученика в курс если он уже
        был учеником в этом курсе
        """

        CourseStudent.objects.create(
            student=second_teacher,
            course=course,
        )

        detail_url = reverse("v2:activation_code-activate")
        jclient.login(user=second_teacher)
        response = jclient.post(detail_url, {
            'activation_code': code_course_to_add.code,
        })
        assert response.status_code == 200, (
            u'Код должен обработаться без ошибок')

        assert CourseStudent.objects.filter(
            student=second_teacher,
            course=course,
        ).count() == 1, u'Учитель должен быть учеником в этом курсе'

        code_course_to_add.refresh_from_db()
        assert code_course_to_add.activated_by == second_teacher, (
            u'Код должен быть присвоен учителю')
