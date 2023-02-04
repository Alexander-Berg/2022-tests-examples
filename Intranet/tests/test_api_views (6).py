from django.conf import settings
from django.utils import timezone

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import CourseStudentFactory, StudentCourseProgressFactory
from lms.users.tests.factories import UserFactory

from .factories import MentorshipFactory


class MyColleagueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-colleague-list'

    def setUp(self):
        self.user = UserFactory()
        UserFactory.create_batch(10)
        MentorshipFactory.create_batch(10)
        MentorshipFactory.create_batch(10, mentee=self.user)

    def build_expected_staff_profile(self, profile):
        return {
            'joined_at': serializers.DateTimeField().to_representation(profile.joined_at),
        }

    def build_expected_students(self, students):
        expected_students = []
        for student in students:
            progress = student.course_progresses.first()
            expected_students.append({
                'id': student.id,
                'status': student.status,
                'is_passed': student.is_passed,
                'score': None if not progress else progress.score,
            })
        return expected_students

    def build_expected_user(self, user):
        return {
            'id': user.id,
            'username': user.username,
            'first_name': user.first_name,
            'last_name': user.last_name,
        }

    def build_expected_mentorships(self, mentorships):
        return [{
            'id': mentorship.id,
            'due_date': serializers.DateTimeField().to_representation(mentorship.due_date),
            'mentor': self.build_expected_user(mentorship.mentor),
        } for mentorship in mentorships]

    def build_expected(self, users):
        return [{
            'id': user.id,
            'username': user.username,
            'first_name': user.first_name,
            'last_name': user.last_name,
            'profile': self.build_expected_staff_profile(user.staffprofile),
            'students': self.build_expected_students(user.in_courses.all()),
            'mentorships': self.build_expected_mentorships(user.mentorships_as_mentee.all()),
        } for user in users]

    def test_url(self):
        self.assertURLNameEqual('my/colleagues/', args=(), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        self.detail_request(url=self.get_url(), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_empty_list(self):
        self.client.force_login(user=self.user)
        self.list_request(self.get_url(), check_ids=False, expected=[], num_queries=3)

    def test_list(self):
        # создаём подчинённых
        subordinate_users = UserFactory.create_batch(5)
        subordinates = []
        for subordinate_user in subordinate_users:
            staff_profile = subordinate_user.staffprofile
            staff_profile.head = self.user.staffprofile
            staff_profile.save()
            subordinates.append(subordinate_user)

        # увольняем одного из подчинённых
        dismissed_subordinate = subordinates.pop().staffprofile
        dismissed_subordinate.is_dismissed = True
        dismissed_subordinate.save()

        # создаём прогресс для одного подчинённого
        subordinate_with_progress = subordinates[0]
        subordinate_student = CourseStudentFactory(user=subordinate_with_progress)
        StudentCourseProgressFactory(score=0, student=subordinate_student)

        # создаём студента без прогресса для одного из подчинённых
        CourseStudentFactory(user=subordinates[-1])

        # создаём подопечных и прогресс для одного из них
        mentorships = MentorshipFactory.create_batch(5, mentor=self.user, due_date=timezone.now())
        mentees = [mentorship.mentee for mentorship in mentorships]
        mentee_with_progress = mentees[0]
        mentee_student = CourseStudentFactory(user=mentee_with_progress)
        StudentCourseProgressFactory(score=77, student=mentee_student)

        # создаём наставничество для одного из подчинённых
        MentorshipFactory(mentor=self.user, mentee=subordinates[-1])

        expected = self.build_expected(subordinates + mentees)

        self.client.force_login(user=self.user)
        self.list_request(self.get_url(), check_ids=False, expected=expected, num_queries=7)


class MyColleagueDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-colleague-detail'

    def setUp(self):
        self.user = UserFactory()
        UserFactory.create_batch(10)
        MentorshipFactory.create_batch(10)
        MentorshipFactory.create_batch(10, mentee=self.user)
        self.target_user = UserFactory()
        self.students = CourseStudentFactory.create_batch(3, user=self.target_user)
        StudentCourseProgressFactory(score=0, student=self.students[0])

    def build_expected_course(self, course):
        return {
            'id': course.id,
            'name': course.name,
            'type': course.course_type,
        }

    def build_expected_students(self, students):
        expected_students = []
        for student in students:
            progress = student.course_progresses.first()
            expected_students.append({
                'id': student.id,
                'status': student.status,
                'is_passed': student.is_passed,
                'start_date': None if not progress else progress.created,
                'score': None if not progress else progress.score,
                'course': self.build_expected_course(student.course),
            })
        return expected_students

    def build_expected(self, user):
        return {
            'id': user.id,
            'username': user.username,
            'first_name': user.first_name,
            'last_name': user.last_name,
            'students': self.build_expected_students(user.in_courses.all()),
        }

    def test_url(self):
        self.assertURLNameEqual('my/colleagues/{}/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        self.detail_request(url=self.get_url(1,), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_neither_subordinate_nor_mentee(self):
        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(self.target_user.id), status_code=status.HTTP_404_NOT_FOUND, num_queries=3)

    def test_detail_subordinate(self):
        staff_profile = self.target_user.staffprofile
        staff_profile.head = self.user.staffprofile
        staff_profile.save()

        expected = self.build_expected(self.target_user)

        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(self.target_user.id), expected=expected, num_queries=9)

    def test_detail_dismissed(self):
        staff_profile = self.target_user.staffprofile
        staff_profile.head = self.user.staffprofile
        staff_profile.is_dismissed = True
        staff_profile.save()

        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(self.target_user.id), status_code=status.HTTP_404_NOT_FOUND, num_queries=3)

    def test_detail_mentee(self):
        MentorshipFactory(mentor=self.user, mentee=self.target_user)
        expected = self.build_expected(self.target_user)
        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(self.target_user.id), expected=expected, num_queries=9)

    def test_detail_inactive_mentee(self):
        MentorshipFactory(mentor=self.user, mentee=self.target_user, is_active=False)
        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(self.target_user.id), status_code=status.HTTP_404_NOT_FOUND, num_queries=3)
