from datetime import timedelta
from itertools import cycle
from unittest.mock import MagicMock, patch

import factory
import faker
import pytest

from django.contrib.contenttypes.models import ContentType
from django.utils.timezone import utc

from kelvin.accounts.factories import UserFactory
from kelvin.common.utils_for_tests import CaptureQueriesContext
from kelvin.tags.models import TagTypeStaffChief
from kelvin.tags.tests.factories import TagFactory, TaggedObjectFactory

from ..factories import (
    CourseFactory, CourseStudentFactory, ExcludedUserFactory, PeriodicCourseFactory, PeriodicRoleDigestFactory,
    RoleDigestFactory,
)
from ..models.periodic import RoleDigest
from ..services import get_users_with_students_with_expired_delay, update_role_digests

fake = faker.Faker()


class TestGetStudentsWithExpiredDelayService:
    @pytest.mark.django_db
    def test_get_users_with_students_with_expired_delay(self):

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        delay = 30
        course = CourseFactory()

        # руководители без студентов
        chiefs_without_students = UserFactory.create_batch(3)
        chiefs_without_students_tags = [
            TagFactory(
                type=TagTypeStaffChief.get_db_type(),
                value=chief.id,
            ) for chief in chiefs_without_students
        ]

        # студенты, у которых срок сдачи не прошел
        not_expired_students = CourseStudentFactory.create_batch(
            3, course=course, date_created=now_moment - timedelta(days=delay - 1),
        )
        not_expired_students_chief_tags = [
            TaggedObjectFactory(
                tag=TagFactory(
                    type=TagTypeStaffChief.get_db_type(),
                    value=UserFactory().id,
                ),
                content_type=ContentType.objects.get(app_label='accounts', model='user'),
                object_id=student.student_id,

            ) for student in not_expired_students
        ]

        # другие студенты (не включаем в выборку)
        other_students = CourseStudentFactory.create_batch(
            3, course=course, date_created=now_moment - timedelta(days=delay - 1),
        )
        other_students_chief_tags = [
            TaggedObjectFactory(
                tag=TagFactory(
                    type=TagTypeStaffChief.get_db_type(),
                    value=UserFactory().id,
                ),
                content_type=ContentType.objects.get(app_label='accounts', model='user'),
                object_id=student.student_id,

            ) for student in other_students
        ]

        # студенты, завершившие курс
        completed_students = CourseStudentFactory.create_batch(
            3, course=course, date_created=now_moment - timedelta(days=delay - 1), completed=True,
        )
        completed_students_chief_tags = [
            TaggedObjectFactory(
                tag=TagFactory(
                    type=TagTypeStaffChief.get_db_type(),
                    value=UserFactory().id,
                ),
                content_type=ContentType.objects.get(app_label='accounts', model='user'),
                object_id=student.student_id,

            ) for student in completed_students
        ]

        # студенты с руководителями в списке исключений
        excluded_chief_logins = ExcludedUserFactory.create_batch(3, exclude_from_issues=True)
        excluded_chiefs = [
            UserFactory(username=excluded_chief.login) for excluded_chief in excluded_chief_logins
        ]
        students_of_excluded_chiefs = CourseStudentFactory.create_batch(
            3, course=course, date_created=now_moment - timedelta(days=delay - 1), completed=True,
        )

        excluded_students_chief_tags = [
            TaggedObjectFactory(
                tag=TagFactory(
                    type=TagTypeStaffChief.get_db_type(),
                    value=chief.id,
                ),
                content_type=ContentType.objects.get(app_label='accounts', model='user'),
                object_id=student.student_id,

            ) for student, chief in zip(students_of_excluded_chiefs, cycle(excluded_chiefs))
        ]

        # студенты без руководителей
        students_without_chief = CourseStudentFactory.create_batch(
            3, course=course, date_created=now_moment - timedelta(days=delay + 1),
        )

        # студенты с руководителями
        student_chiefs = UserFactory.create_batch(3)
        students = CourseStudentFactory.create_batch(
            6, course=course, date_created=now_moment - timedelta(days=delay + 1),
        )
        student_chief_tags = [
            TaggedObjectFactory(
                tag=TagFactory(
                    type=TagTypeStaffChief.get_db_type(),
                    value=chief.id,
                ),
                content_type=ContentType.objects.get(app_label='accounts', model='user'),
                object_id=student.student_id,
            ) for student, chief in zip(students, cycle(student_chiefs))
        ]

        # студенты с руководителями в списке исключений не по созданию тикетов
        not_excluded_chief_logins = ExcludedUserFactory.create_batch(3, exclude_from_issues=False)
        not_excluded_chiefs = [
            UserFactory(username=excluded_chief.login) for excluded_chief in not_excluded_chief_logins
        ]
        students_of_not_excluded_chiefs = CourseStudentFactory.create_batch(
            6, course=course, date_created=now_moment - timedelta(days=delay + 1),
        )

        not_excluded_students_chief_tags = [
            TaggedObjectFactory(
                tag=TagFactory(
                    type=TagTypeStaffChief.get_db_type(),
                    value=chief.id,
                ),
                content_type=ContentType.objects.get(app_label='accounts', model='user'),
                object_id=student.student_id,
            ) for student, chief in zip(students_of_not_excluded_chiefs, cycle(not_excluded_chiefs))
        ]

        periodic_course = PeriodicCourseFactory(course=course)
        periodic_role_digest = PeriodicRoleDigestFactory(
            periodic_course=periodic_course,
            role='chief',
            first_run=factory.Faker('date').generate(),
            interval=factory.Faker('pyint').generate(),
            delay=delay,
            parameters={
                "queue": factory.Faker('pystr').generate(),
                "template": "chief",
                "issue_type": factory.Faker('pystr').generate(),
                "open_status": factory.Faker('pystr').generate(),
                "close_status": factory.Faker('pystr').generate(),
                "open_transition": factory.Faker('pystr').generate(),
                "close_resolution": factory.Faker('pystr').generate(),
                "close_transition": factory.Faker('pystr').generate(),
            },
        )
        with patch('kelvin.courses.services.timezone.now', new=mocked_now):
            with CaptureQueriesContext() as queries_context:
                res = get_users_with_students_with_expired_delay(
                    periodic_role_digest=periodic_role_digest,
                    for_users=chiefs_without_students + excluded_chiefs + student_chiefs + not_excluded_chiefs,
                )

        assert len(queries_context) == 1

        chiefs = student_chiefs + not_excluded_chiefs
        assert len(res) == len(chiefs)
        assert set(res) == {chief.id for chief in chiefs}


class TestUpdateRoleDigestsService:
    @pytest.mark.django_db
    def test_update_role_digests(self):
        periodic_role_digest = PeriodicRoleDigestFactory(
            role='chief',
            delay=factory.Faker('pyint').generate(),
            first_run=factory.Faker('date').generate(),
            interval=factory.Faker('pyint').generate(),
            parameters={
                "queue": factory.Faker('pystr').generate(),
                "template": "chief",
                "issue_type": factory.Faker('pystr').generate(),
                "open_status": factory.Faker('pystr').generate(),
                "close_status": factory.Faker('pystr').generate(),
                "open_transition": factory.Faker('pystr').generate(),
                "close_resolution": factory.Faker('pystr').generate(),
                "close_transition": factory.Faker('pystr').generate(),
            },
        )

        users_to_create_digest_not_exists = UserFactory.create_batch(3)
        users_to_create_digest_but_exists_opened = UserFactory.create_batch(3)
        users_to_create_digest_but_exists_closed = UserFactory.create_batch(3)
        users_not_to_create_not_exists = UserFactory.create_batch(3)
        users_not_to_create_but_exists_opened = UserFactory.create_batch(3)
        users_not_to_create_but_exists_closed = UserFactory.create_batch(3)

        digests_for_users_to_create_digest_but_exists_opened = [
            RoleDigestFactory(
                periodic_role_digest=periodic_role_digest,
                user=user,
                target_issue_status=periodic_role_digest.parameters['open_status'],
                tracker_issue_key=f"{factory.Faker('pystr').generate()}-{factory.Faker('pyint').generate()}"
            ) for user in users_to_create_digest_but_exists_opened
        ]
        digests_for_users_to_create_digest_but_exists_closed = [
            RoleDigestFactory(
                periodic_role_digest=periodic_role_digest,
                user=user,
                target_issue_status=periodic_role_digest.parameters['close_status'],
                tracker_issue_key=f"{factory.Faker('pystr').generate()}-{factory.Faker('pyint').generate()}"
            ) for user in users_to_create_digest_but_exists_closed
        ]
        digests_for_users_not_to_create_but_exists_opened = [
            RoleDigestFactory(
                periodic_role_digest=periodic_role_digest,
                user=user,
                target_issue_status=periodic_role_digest.parameters['open_status'],
                tracker_issue_key=f"{factory.Faker('pystr').generate()}-{factory.Faker('pyint').generate()}"
            ) for user in users_not_to_create_but_exists_opened
        ]
        digests_for_users_not_to_create_but_exists_closed = [
            RoleDigestFactory(
                periodic_role_digest=periodic_role_digest,
                user=user,
                target_issue_status=periodic_role_digest.parameters['close_status'],
                tracker_issue_key=f"{factory.Faker('pystr').generate()}-{factory.Faker('pyint').generate()}"
            ) for user in users_not_to_create_but_exists_closed
        ]

        users_to_create_digest = (
            users_to_create_digest_not_exists +
            users_to_create_digest_but_exists_opened +
            users_to_create_digest_but_exists_closed
        )

        users_not_to_create_digest = (
            users_not_to_create_not_exists +
            users_not_to_create_but_exists_opened +
            users_not_to_create_but_exists_closed
        )

        def get_users_with_students_with_expired_delay_mocked(*args, **kwargs):
            return [user.id for user in users_to_create_digest]

        with patch(
            'kelvin.courses.services.get_users_with_students_with_expired_delay',
            new=get_users_with_students_with_expired_delay_mocked
        ):
            with CaptureQueriesContext() as queries_context:
                update_role_digests(periodic_role_digest=periodic_role_digest)

        assert len(queries_context) == 25, str(queries_context)

        users_ids_to_open = RoleDigest.objects.filter(
            periodic_role_digest=periodic_role_digest,
            user__in=users_to_create_digest,
            target_issue_status=periodic_role_digest.parameters['open_status'],
        ).values_list('user_id', flat=True)

        assert set(users_ids_to_open) == {user.id for user in users_to_create_digest}

        for role_digest in (
            digests_for_users_to_create_digest_but_exists_opened +
            digests_for_users_to_create_digest_but_exists_closed
        ):
            role_digest.refresh_from_db()

        assert [
            role_digest.target_issue_status for role_digest in (
                digests_for_users_to_create_digest_but_exists_opened +
                digests_for_users_to_create_digest_but_exists_closed
            )
        ] == [periodic_role_digest.parameters['open_status']] * 6

        users_ids_to_close = RoleDigest.objects.filter(
            periodic_role_digest=periodic_role_digest,
            user__in=users_not_to_create_digest,
            target_issue_status=periodic_role_digest.parameters['close_status'],
        ).values_list('user_id', flat=True)

        assert set(users_ids_to_close) == {
            user.id for user in users_not_to_create_but_exists_opened + users_not_to_create_but_exists_closed
        }

        for role_digest in (
            digests_for_users_not_to_create_but_exists_opened +
            digests_for_users_not_to_create_but_exists_closed
        ):
            role_digest.refresh_from_db()

        assert [role_digest.target_issue_status for role_digest in digests_for_users_not_to_create_but_exists_opened + digests_for_users_not_to_create_but_exists_closed] == [periodic_role_digest.parameters['close_status']] * 6
