from builtins import object

import pytest
from mock import call
from mock.mock import MagicMock

from django.conf import settings
from django.contrib.auth import get_user_model

from kelvin.courses.models import Course
from kelvin.mail.models import EmailTemplate
from kelvin.staff_notifications.models import ActivationCode, NewTeacherAction, NotificationEmailManager
from kelvin.subjects.models import Subject

User = get_user_model()


class TestNotificationEmailManager(object):
    """
    Тесты для менеджера рассылки `NotificationEmailManager`
    """
    # TODO Тесты не работают, нужно переписывать

    @pytest.mark.xfail()
    def test_save(self, mocker):
        """Проверка генерирования кода"""
        mocked_save = mocker.patch('django.db.models.Model.save')
        mocked_generate_code = mocker.patch(
            'kelvin.staff_notifications.models.generate_code')
        mocked_generate_code.return_value = 'QWERTY'

        # сохранение без кода
        action = NewTeacherAction(name='some action')
        action.save()
        assert mocked_save.mock_calls == [call()], (
            u'Должны вызвать родительский метод')
        assert mocked_generate_code.mock_calls == [call(6)], (
            u'Должны сгенерировать новый код')
        assert action.code == 'QWERTY', u'Неправильно сгенерирован код'

        # сохранение с кодом
        mocked_generate_code.reset_mock()
        mocked_save.reset_mock()
        action = NewTeacherAction(name='some action', code='qwaqwa')
        action.save()
        assert mocked_save.mock_calls == [call()], (
            u'Должны вызвать родительский метод')
        assert mocked_generate_code.mock_calls == [], (
            u'Код не должен генерироваться')
        assert action.code == 'qwaqwa', u'Код не должен измениться'

    @pytest.mark.xfail()
    def test_do_with(self, mocker):
        """
        Проверяет работу действия
        """
        # неподтвержденный учитель с несуществующим курсом
        course = Course(name=u'Оригинал')
        teacher = User(is_teacher=False, first_name=u'Иван',
                       last_name=u'Иванов', email='ivanov@1.ru')
        subject = Subject(id=10, name=u'Математика')
        email_template = EmailTemplate(
            name=u'Письмо',
            template=(
                '{{ user.first_name }} {{ course.name }} {{ subject.name }}'),
        )
        action = NewTeacherAction(
            name='some action',
            course=course,
            subject=subject,
            email_template=email_template,
        )
        mocked_teacher_save = mocker.patch.object(teacher, 'save')
        mocked_teacher_profile = mocker.patch.object(User, 'teacher_profile')
        mocked_courses = mocker.patch.object(Course, 'objects')
        mocked_courses.filter.return_value.first.return_value = None
        mocked_courses.get.return_value = course
        mocked_course_copy = mocker.patch.object(course, 'copy')
        mocked_course_save = mocker.patch.object(course, 'save')
        mocked_clessons = mocker.patch.object(
            Course, 'courselessonlink_set')
        first_clesson = MagicMock()
        second_clesson = MagicMock()
        mocked_clessons.order_by.return_value = [first_clesson, second_clesson]
        mocked_send_mail = mocker.patch(
            'kelvin.staff_notifications.models.send_mail')
        mocked_render = mocker.patch.object(email_template, 'render')
        mocked_render.return_value = '<rendered>'
        mocked_timezone = mocker.patch(
            'kelvin.staff_notifications.models.timezone')
        mocked_timezone.now.return_value = 'now datetime'

        created_course, messages = action.do_with(teacher)
        assert messages == {
            'info': [
                u'Учитель подтвержден',
                u'Учителю проставлен предмет Математика',
                u'Учитель добавлен в группу Учителя',
                u'Учителю скопирован курс: Оригинал',
                u'Учителю было отправлено письмо',
            ],
        }
        assert created_course == course
        assert teacher.is_teacher is True
        assert mocked_teacher_save.mock_calls == [call()]
        assert mocked_teacher_profile.mock_calls == [call.save()]
        assert mocked_students.mock_calls == [call.add(teacher)]
        assert mocked_courses.mock_calls == [
            call.filter(owner=teacher, copy_of=course),
            call.filter().first(),
            call.get(id=None),
        ]
        assert mocked_course_copy.mock_calls == [call(teacher)]
        assert mocked_clessons.mock_calls == [
            call.update(accessible_to_teacher='now datetime'),
            call.order_by('order'),
        ]
        assert first_clesson.date_assignment == 'now datetime'
        assert first_clesson.mock_calls == [
            call.save(),
        ]
        assert second_clesson.mock_calls == []
        assert mocked_course_save.mock_calls == []
        assert mocked_send_mail.mock_calls == [call(
            u'Письмо',
            None,
            settings.DEFAULT_FROM_EMAIL,
            ['ivanov@1.ru'],
            html_message='<rendered>',
        )]
        assert mocked_render.mock_calls == [call({
            'user': teacher,
            'course': course,
            'subject': subject,
        })]

        # подтвержденный учитель, у которого уже есть курс
        mocked_teacher_save.reset_mock()
        mocked_teacher_profile.reset_mock()
        mocked_students.reset_mock()
        mocked_courses.reset_mock()
        mocked_course_copy.reset_mock()
        mocked_clessons.reset_mock()
        first_clesson.reset_mock()
        mocked_course_save.reset_mock()
        mocked_send_mail.reset_mock()
        mocked_render.reset_mock()

        existed_course = Course()
        mocked_courses.filter.return_value.first.return_value = existed_course
        action.open_lessons_count = -1

        created_course, messages = action.do_with(teacher)
        assert messages == {
            'info': [
                u'Учителю проставлен предмет Математика',
                u'Учитель добавлен в группу Учителя',
                u'Учителю было отправлено письмо',
            ],
            'warning': [
                u'Учитель не требует подтверждения',
                u'Учитель уже имеет курс',
            ],
        }
        assert created_course == existed_course
        assert teacher.is_teacher is True
        assert mocked_teacher_save.mock_calls == []
        assert mocked_teacher_profile.mock_calls == [call.save()]
        assert mocked_students.mock_calls == [call.add(teacher)]
        assert mocked_courses.mock_calls == [
            call.filter(owner=teacher, copy_of=course),
            call.filter().first(),
        ]
        assert mocked_course_copy.mock_calls == []
        assert mocked_clessons.mock_calls == [
            call.update(accessible_to_teacher='now datetime'),
            call.update(date_assignment='now datetime'),
        ]
        assert first_clesson.mock_calls == []
        assert mocked_course_save.mock_calls == []
        assert mocked_send_mail.mock_calls == [call(
            u'Письмо',
            None,
            settings.DEFAULT_FROM_EMAIL,
            ['ivanov@1.ru'],
            html_message='<rendered>',
        )]
        assert mocked_render.mock_calls == [call({
            'user': teacher,
            'course': created_course,
            'subject': subject,
        })]


class TestActivationCodeMocked(object):
    """
    Тесты модели кода активации
    """
    def test_batch_create(self, mocker):
        """
        Проверка на то, что batch_create работает в транзакции
        """
        get_connection_mock = mocker.patch(
            'django.db.transaction.get_connection')
        ActivationCode.batch_create(0, [])

        assert len(get_connection_mock.mock_calls) > 0


@pytest.mark.django_db
class TestActivationCode(object):
    """
    Тесты модели кода активации
    """
    def test_batch_create(self, action1, action2):
        """
        Проверка генерации набора кодов
        """
        created_ids = ActivationCode.batch_create(3, [action1, action2])

        codes = (
            ActivationCode.objects.all()
            .order_by('id')
            .prefetch_related('actions')
        )

        assert [code.pk for code in codes] == created_ids
        for code in codes:
            assert list(code.actions.all()) == [action1, action2]
