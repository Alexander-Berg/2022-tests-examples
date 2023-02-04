from builtins import object
from datetime import timedelta

import pytest

from django.core.urlresolvers import reverse
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink, CourseStudent, CoursePermission
from kelvin.lessons.models import Lesson


@pytest.fixture
def clesson_data(some_owner, subject_model):
    """
    Модели для тестов
    """
    lesson = Lesson.objects.create(owner=some_owner, name=u'Занятие')
    course = Course.objects.create(owner=some_owner, name=u'Курс',
                                   subject=subject_model)
    clesson = CourseLessonLink.objects.create(
        course=course, lesson=lesson, order=1,
    )
    return clesson


@pytest.fixture
def clesson_with_book(some_owner, subject_model):
    """
    Модели для тестов
    """
    lesson = Lesson.objects.create(owner=some_owner, name=u'Занятие')
    course1 = Course.objects.create(owner=some_owner, name=u'Книга1',
                                    subject=subject_model)
    course2 = Course.objects.create(owner=some_owner, name=u'Книга2',
                                    subject=subject_model)
    course_with_book = Course.objects.create(owner=some_owner, name=u'Курс',
                                             subject=subject_model)
    course_with_book.source_courses = [course1, course2]
    book_clesson = CourseLessonLink.objects.create(
        course=course1, lesson=lesson, order=1,
    )
    return book_clesson, course_with_book


@pytest.mark.django_db
class TestClessonViewSet(object):
    """
    Тесты доступов к ручкам курсозанятия
    """
    def test_list(self, jclient, teacher, student, content_manager, parent):
        """
        Тест доступа ручки списка курсозанятий
        """
        list_url = reverse('v2:course_lesson-list')

        # неавторизованный пользователь не имеет доступа
        response = jclient.get(list_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # авторизованный пользователь имеет доступ
        for user in (teacher, student, content_manager, parent):
            jclient.login(user=user)
            response = jclient.get(list_url)
            assert response.status_code == 200, (
                u'Неправильный статус ответа: {0}'.format(response.content))

    def test_detail(self, jclient, student, teacher, parent, content_manager,
                    clesson_data, some_owner):
        """
        Тест доступа к ручке одного курсозанятия
        """
        clesson = clesson_data
        detail_url = reverse('v2:course_lesson-detail', args=(clesson.id,))

        # анонимам нет доступа
        response = jclient.get(detail_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # контент-менеджер имеет доступ
        jclient.login(user=content_manager)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # обычным пользователям нет доступа
        jclient.login(user=student)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # обычный пользователь не имеет доступ к зянятию, назначенному ему на
        # будущее
        CourseStudent.objects.create(course=clesson.course, student=student)
        clesson.date_assignment = timezone.now() + timedelta(days=1)
        clesson.save()
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # обычный пользователь получает доступ, если занятие ему назначили
        clesson.date_assignment = timezone.now()
        clesson.save()
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # учитель имеет доступ, если он владелец курса или курс свободный,
        # занятие доступно учителю
        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clesson.accessible_to_teacher = timezone.now()
        clesson.save()
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clesson.course.free = True
        clesson.course.save()
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clesson.course.free = False
        clesson.course.owner = teacher
        clesson.course.save()
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # родитель может смотреть занятие, которое назначено его ребенку
        jclient.login(user=parent)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        parent.parent_profile.children = [student]
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # родитель не имеет доступ к зянятию, назначенному его детям на
        # будущее
        clesson.date_assignment = timezone.now() + timedelta(days=1)
        clesson.save()
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # никто не может редактировать назначенное занятие
        for user in (student, teacher, parent, content_manager):
            jclient.login(user=user)
            response = jclient.patch(detail_url, {})
            assert response.status_code == 403, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

        # контент-менеджер и учитель-владелец могут редактировать
        # неназначенное занятие
        clesson.date_assignment = None
        clesson.save()
        for user in (content_manager, teacher):
            jclient.login(user=user)
            response = jclient.patch(detail_url, {})
            assert response.status_code == 200, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

        # ученик и родитель не могут редактировать неназначенное занятие
        for user in (student, parent):
            jclient.login(user=user)
            response = jclient.patch(detail_url, {})
            assert response.status_code == 403, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

        # учитель не может редактировать не свой курс, даже если он свободный
        clesson.course.owner = some_owner
        clesson.course.free = True
        clesson.course.save()
        jclient.login(user=teacher)
        response = jclient.patch(detail_url, {})
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    test_clesson_urls_data = (
        'v2:course_lesson-results',
    )

    @pytest.mark.parametrize('test_url', test_clesson_urls_data)
    def test_clesson_urls(self, jclient, teacher, student,
                          content_manager, parent, test_url, clesson_data):
        """
        Тест доступа к ручке результатов
        """
        clesson = clesson_data
        test_url = reverse(test_url, args=(clesson.id,))

        # анонимам нет доступа
        response = jclient.get(test_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # обычным пользователям нет доступа
        jclient.login(user=student)
        response = jclient.get(test_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # любые учителя имеют доступ, если занятие доступно
        jclient.login(user=teacher)
        response = jclient.get(test_url)
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clesson.accessible_to_teacher = timezone.now()
        clesson.save()
        response = jclient.get(test_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # контент-менеджеры имеют доступ
        clesson.accessible_to_teacher = None
        clesson.save()
        jclient.login(user=content_manager)
        response = jclient.get(test_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # родители имеют доступ при наличии ребенка в группе и назначенном
        # занятии
        jclient.login(user=parent)
        response = jclient.get(test_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        CourseStudent.objects.create(course=clesson.course, student=student)
        parent.parent_profile.children = [student]
        response = jclient.get(test_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clesson.date_assignment = timezone.now()
        clesson.save()
        response = jclient.get(test_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_complete(self, jclient, teacher, student, content_manager,
                      parent, clesson_data):
        """
        Тест к методу завершения занятия
        """
        clesson = clesson_data
        now = timezone.now()
        clesson.date_assignment = now
        clesson.accessible_to_teacher = now
        clesson.save()
        complete_url = reverse('v2:course_lesson-complete', args=(clesson.id,))

        # у неавторизованного нет доступа
        response = jclient.post(complete_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # ни у кого нет доступа, если он не владелец курса
        for user in (student, teacher, content_manager, parent):
            jclient.login(user=user)
            response = jclient.post(complete_url)
            assert response.status_code == 403, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

        # у владельца есть доступ
        jclient.login(user=clesson.course.owner)
        response = jclient.post(complete_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_finish(self, jclient, teacher, student, content_manager,
                    parent, clesson_data):
        """
        Тест к методу завершения прохождения занятия учеником
        """
        # TODO  надо проверять, что занятие назначено ученику
        clesson = clesson_data
        clesson.accessible_to_teacher = timezone.now()
        clesson.save()
        finish_url = reverse('v2:course_lesson-finish', args=(clesson.id,))

        # у неавторизованного нет доступа
        response = jclient.post(finish_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # нет прав завершить свое прохождение
        for user in (student, teacher, content_manager, parent):
            jclient.login(user=user)
            response = jclient.post(finish_url)
            assert response.status_code == 403, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

    def test_make_homework(self, jclient, teacher, student, content_manager,
                           parent, clesson_data):
        """
        Тест к методу создания домашнего задания
        """
        clesson = clesson_data
        now = timezone.now()
        clesson.accessible_to_teacher = now
        clesson.date_completed = now
        clesson.save()
        CourseStudent.objects.create(course=clesson.course, student=student)
        make_homework_url = reverse('v2:course_lesson-make-homework',
                                    args=(clesson.id,))

        # у неавторизованного нет доступа
        response = jclient.post(make_homework_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # у учителя и контент-менеджера есть доступ
        for user in (teacher, content_manager):
            jclient.login(user=user)
            response = jclient.post(make_homework_url)
            assert response.status_code == 201, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

        # у студента и родителя нет права
        for user in (student, parent):
            jclient.login(user=user)
            response = jclient.post(make_homework_url)
            assert response.status_code == 403, (
                u'Неправильный статус ответа, ответ: {0}'.format(
                    response.content))

    def test_web(self, jclient, teacher, student, content_manager, parent,
                 clesson_with_book):
        """
        Тест получения информации о курсозанятии для веба
        """
        clesson, course_with_book = clesson_with_book
        clesson.accessible_to_teacher = timezone.now()
        clesson.save()
        web_url = reverse('v2:course_lesson-web', args=(clesson.id,))

        # в занятие в курсе для анонимного пользователя доступ имеют все
        clesson.course.allow_anonymous = True
        clesson.course.save()
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        for user in (student, parent, teacher, content_manager):
            jclient.login(user=user)
            response = jclient.get(web_url)
            assert response.status_code == 200, (
                u'Неправильный статус ответа: {0}'.format(response.content))

        # неавторизованный пользователь не имеет доступ в обычный курс
        clesson.course.allow_anonymous = False
        clesson.course.save()
        jclient.logout()
        response = jclient.get(web_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # контент-менеджер имеет доступ
        jclient.login(user=content_manager)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа: {0}'.format(response.content))

        # учитель имеет доступ к курсозанятию, если этот курс используется
        # как книга в других его курсах
        old_owner = course_with_book.owner
        course_with_book.owner = teacher
        course_with_book.save()
        jclient.login(user=teacher)
        response = jclient.get(web_url)
        course_with_book.owner = old_owner
        course_with_book.save()
        assert response.status_code == 200, (
            u'Неправильный статус ответа: {0}'.format(response.content))

        # учитель должен быть владельцем курса (или предыдущий пункт про книги)
        clesson.course.owner = teacher
        clesson.course.save()
        jclient.login(user=teacher)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа: {0}'.format(response.content))

        # ученик и родитель по умолчанию не имеют доступ
        for user in (student, parent):
            jclient.login(user=user)
            response = jclient.get(web_url)
            assert response.status_code == 403, (
                u'Неправильный статус ответа: {0}'.format(response.content.encode('utf-8')))

        # ученик имеет доступ в назначенное занятие своего курса, родитель -
        # в занятие ребенка
        parent.parent_profile.children = [student]
        CourseStudent.objects.create(student=student, course=clesson.course)
        for user in (student, parent):
            jclient.login(user=user)
            response = jclient.get(web_url)
            assert response.status_code == 403, (
                u'Неправильный статус ответа: {0}'.format(response.content))

        clesson.date_assignment = timezone.now()
        clesson.save()
        for user in (student, parent):
            jclient.login(user=user)
            response = jclient.get(web_url)
            assert response.status_code == 200, (
                u'Неправильный статус ответа: {0}'.format(response.content))
