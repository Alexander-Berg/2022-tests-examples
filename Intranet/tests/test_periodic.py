import os
import tempfile
from contextlib import ExitStack
from datetime import timedelta
from unittest.mock import MagicMock, patch

import factory
import pytest

from django.utils import timezone

from kelvin.common.mixer_tools import django_mixer as mixer
from kelvin.courses.models import NotifyDayOff, PeriodicNotification, PeriodicStudentNotification
from kelvin.courses.services import prepare_periodic_course_notifications

from ..services import create_periodic_tracker_issue
from ..tracker_templates.notification import NotificationTrackerTemplateBase


def make_student_in_past(**kwargs):
    date_created = kwargs.pop('date_created', None)
    cycle = kwargs.pop('_cycle', 1)
    students = []
    kwargs["student__username"] = mixer.sequence(lambda x: "test_{}".format(x))

    for n in range(cycle):
        student = mixer.blend('courses.CourseStudent', **kwargs)
        if date_created:
            student.date_created = date_created
            student.save()
        students.append(student)

    return students if len(students) > 1 else students[0]


@pytest.mark.django_db
class TestNotifyDayOff:
    def test_not_allowed(self):
        now = timezone.now()
        mixer.blend(
            'courses.NotifyDayOff',
            day=now.day,
            month=now.month,
            valid_from=now.date()
        )

        assert NotifyDayOff.objects.allowed(now) == False

    def test_allowed(self):
        now = timezone.now()
        mixer.blend(
            'courses.NotifyDayOff',
            day=now.day+1,
            month=now.month,
            valid_from=now.date()
        )

        assert NotifyDayOff.objects.allowed(now) == True

    def test_valid_from(self):
        now = timezone.now()
        mixer.blend(
            'courses.NotifyDayOff',
            day=now.day,
            month=now.month,
            valid_from=now.date() + timedelta(days=1)
        )

        assert NotifyDayOff.objects.allowed(now) == True

    def test_valid_to(self):
        now = timezone.now()
        mixer.blend(
            'courses.NotifyDayOff',
            day=now.day,
            month=now.month,
            valid_from=now.date() - timedelta(days=30),
            valid_to=now.date() -timedelta(days=15)
        )

        assert NotifyDayOff.objects.allowed(now) == True


@pytest.mark.django_db
class TestPeriodicNotifications:
    def test_create_no_notify(self, django_assert_num_queries):
        course = mixer.blend('courses.Course')
        periodic_course = mixer.blend('courses.PeriodicCourse', course=course)
        notification1 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=7, priority=50,
        )
        students = mixer.cycle(10).blend(
            'courses.CourseStudent',
            student__username=mixer.sequence(lambda x: "test_{}".format(x)),
            course=course,
        )

        with django_assert_num_queries(5):
            prepare_periodic_course_notifications()

        student_notifications = PeriodicStudentNotification.objects.all()
        assert not student_notifications.exists()

    def test_create_first_notify(self, django_assert_num_queries):
        now = timezone.now().date()
        days_ago_8 = now-timedelta(days=8)

        course = mixer.blend('courses.Course')
        periodic_course = mixer.blend('courses.PeriodicCourse', course=course)
        notification1 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=7, priority=50,
        )
        notification2 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=14, priority=40,
        )

        students = make_student_in_past(
            course=course,
            date_created=days_ago_8,
            _cycle=10,
        )

        with django_assert_num_queries(7):
            prepare_periodic_course_notifications()

        student_notifications = PeriodicStudentNotification.objects.filter(
            student_id=students[0].pk,
            notification=notification1
        )
        assert student_notifications.count() == 1

        student_notification = student_notifications.first()
        assert student_notification.course_id == course.pk

    def test_no_secode_notify_before_first(self, django_assert_num_queries):
        now = timezone.now().date()
        days_ago_15 = now - timedelta(days=15)

        course = mixer.blend('courses.Course')
        periodic_course = mixer.blend('courses.PeriodicCourse', course=course)
        notification1 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=7, priority=50,
        )
        notification2 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=14, priority=40,
        )

        students = make_student_in_past(
            course=course,
            date_created=days_ago_15,
            _cycle=10,
        )

        with django_assert_num_queries(7):
            prepare_periodic_course_notifications()

        student_notifications = PeriodicStudentNotification.objects.filter(
            student_id=students[0].pk,
            notification=notification2,
        )
        assert not student_notifications.exists()

    def test_create_second_notify(self, django_assert_num_queries):
        now = timezone.now().date()
        days_ago_15 = now - timedelta(days=15)

        course = mixer.blend('courses.Course')
        periodic_course = mixer.blend('courses.PeriodicCourse', course=course)
        notification1 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=7, priority=50,
        )
        notification2 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=14, priority=40,
        )

        students = make_student_in_past(
            course=course,
            date_created=days_ago_15,
            _cycle=10,
        )

        for student in students:
             mixer.blend(
                'courses.PeriodicStudentNotification',
                student=student,
                notification=notification1,
                course_id=course.pk,
                status=PeriodicStudentNotification.STATUS_SENT,
            )

        with django_assert_num_queries(7):
            prepare_periodic_course_notifications()

        student_notifications = PeriodicStudentNotification.objects.filter(
            student_id=students[0].pk,
            notification=notification2,
        )
        assert student_notifications.count() == 1

        student_notification = student_notifications.first()
        assert student_notification.course_id == course.pk

    def test_exluded_users(self, django_assert_num_queries):
        now = timezone.now().date()
        days_ago_8 = now - timedelta(days=8)

        course = mixer.blend('courses.Course')
        periodic_course = mixer.blend('courses.PeriodicCourse', course=course)
        notification1 = mixer.blend(
            'courses.PeriodicNotification',
            periodic_course=periodic_course,
            delay=7, priority=50,
        )
        students = make_student_in_past(
            course=course,
            date_created=days_ago_8,
            _cycle=5,
        )

        mixer.blend('courses.ExcludedUser', login=students[0].student.username)

        with django_assert_num_queries(6):
            prepare_periodic_course_notifications()

        student_notifications = PeriodicStudentNotification.objects.filter(
            student_id=students[0].pk,
        )
        assert not student_notifications.exists()

        student_notifications = PeriodicStudentNotification.objects.exclude(
            student_id=students[0].pk,
        )
        assert student_notifications.count() == len(students) - 1


@pytest.mark.django_db
class TestCreatePeriodicTrackerIssue:
    def test_create_issue(self):
        queue = factory.Faker('pystr', min_chars=5, max_chars=20).generate()
        issue_type = factory.Faker('pystr', min_chars=5, max_chars=20).generate()
        issue_status = factory.Faker('pystr', min_chars=5, max_chars=20).generate()
        issue_transition = factory.Faker('pystr', min_chars=5, max_chars=20).generate()
        notification = mixer.blend(
            'courses.PeriodicNotification',
            notify_type=PeriodicNotification.NOTIFY_TYPE_TRACKER,
            parameters={
                'queue': queue,
                'issue_type': issue_type,
                'issue_status': issue_status,
                'issue_transition': issue_transition,
            },
        )
        student_notification = mixer.blend(
            'courses.PeriodicStudentNotification',
            notification=notification,
        )

        summary_template = "Login {{ student_notification.student.student.username }}"
        summary = f"Login {student_notification.student.student.username}"

        description_template = """
        Дорогой {{ student_notification.student.student.get_full_name }}

        Пройдите пожалуйства курс {{ student_notification.notification.periodic_course.course.name }}
        """
        description = f"""
        Дорогой {student_notification.student.student.get_full_name()}

        Пройдите пожалуйства курс {student_notification.notification.periodic_course.course.name}
        """

        open_comment_template = """
        Уважаемый {{ student_notification.student.student.get_full_name }}!
        Ваши сотрудники не прошли курс
        """
        open_comment = f"""
        Уважаемый {student_notification.student.student.get_full_name()}!
        Ваши сотрудники не прошли курс
        """

        close_comment_template = """
        Уважаемый {{ student_notification.student.student.get_full_name }}!
        Все ваши сотрудники прошли курс
        """
        close_comment = f"""
        Уважаемый {student_notification.student.student.get_full_name()}!
        Все ваши сотрудники прошли курс
        """

        with ExitStack() as stack:
            summary_template_file = stack.enter_context(
                tempfile.NamedTemporaryFile('w', dir='src/kelvin/courses/templates/notification')
            )
            summary_template_file.write(summary_template)
            summary_template_file.seek(0)

            description_template_file = stack.enter_context(
                tempfile.NamedTemporaryFile(
                    'w', dir='src/kelvin/courses/templates/notification'
                )
            )
            description_template_file.write(description_template)
            description_template_file.seek(0)

            open_comment_template_file = stack.enter_context(
                tempfile.NamedTemporaryFile(
                    'w', dir='src/kelvin/courses/templates/notification'
                )
            )
            open_comment_template_file.write(open_comment_template)
            open_comment_template_file.seek(0)

            close_comment_template_file = stack.enter_context(
                tempfile.NamedTemporaryFile(
                    'w', dir='src/kelvin/courses/templates/notification'
                )
            )
            close_comment_template_file.write(close_comment_template)
            close_comment_template_file.seek(0)

            class FakeStudentNotification(NotificationTrackerTemplateBase):
                summary_template = f'notification/{os.path.split(summary_template_file.name)[1]}'
                description_template = f'notification/{os.path.split(description_template_file.name)[1]}'
                open_comment_template = f'notification/{os.path.split(open_comment_template_file.name)[1]}'
                close_comment_template = f'notification/{os.path.split(close_comment_template_file.name)[1]}'

                def get_fields(self):
                    ctx = self.get_ctx()

                    return {
                        "author": ctx["student_notification"].student.student.username,
                        "runId": ctx["student_notification"].id,
                    }

            QUEUE_TEMPLATE_MAP = {
                queue: FakeStudentNotification,
            }

            created_issue_number = factory.Faker('pyint').generate()
            FakeStartrek = MagicMock()
            created_issue = MagicMock()
            created_issue.key = f'{queue}-{created_issue_number}'
            FakeStartrek.create.return_value = created_issue

            with patch('kelvin.courses.tracker_templates.notification.QUEUE_TEMPLATE_MAP', new=QUEUE_TEMPLATE_MAP):
                with patch('kelvin.courses.services.startrek_api.issues', new=FakeStartrek):
                    create_periodic_tracker_issue(student_notification=student_notification)

        assert student_notification.result_data == f'{queue}-{created_issue_number}'
        FakeStartrek.create.assert_called_once_with(
            summary=f"Login {student_notification.student.student.username}",
            description=description,
            author=student_notification.student.student.username,
            runId=student_notification.id,
            queue=queue,
            type={'name': issue_type},
        )
