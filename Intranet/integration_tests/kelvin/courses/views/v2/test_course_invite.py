from datetime import timedelta

import pytest

from django.utils import timezone

from kelvin.accounts.factories import UserFactory
from kelvin.accounts.models import UserProject
from kelvin.common.utils_for_tests import CaptureQueriesContext
from kelvin.courses.factories import CourseInviteFactory
from kelvin.courses.models import CourseStudent
from kelvin.courses.models.invite import UserCourseInviteActivation


@pytest.mark.django_db
class TestActionValidator:
    url = '/api/v3/course-invite/'
    def test_not_authentificated(self, jclient):
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {})

        assert response.status_code == 401
        assert len(queries_context) == 0

    def test_key_not_passed(self, jclient):
        user = UserFactory()

        jclient.login(user=user)
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {})
        assert response.status_code == 400
        assert len(queries_context) == 3

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'required'
        assert response_json['errors'][0]['source'] == 'key'

    def test_invite_code_not_found(self, jclient):
        user = UserFactory()

        jclient.login(user=user)
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {"key": "qwerty"})
        assert response.status_code == 404
        assert len(queries_context) == 4

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'not_found'
        assert response_json['errors'][0]['message'] == 'No CourseInvite matches the given query.'

    def test_invite_code_is_not_active(self, jclient):
        user = UserFactory()
        invite = CourseInviteFactory(is_active=False)

        jclient.login(user=user)
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {"key": invite.key})
        assert response.status_code == 400
        assert len(queries_context) == 4

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'invalid'
        assert response_json['errors'][0]['message'] == 'Invite is not active'

    def test_course_invite_not_started(self, jclient):
        user = UserFactory()
        invite = CourseInviteFactory(is_active=True, start_at=timezone.now() + timedelta(minutes=1))

        jclient.login(user=user)
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {"key": invite.key})
        assert response.status_code == 400
        assert len(queries_context) == 4

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'invalid'
        assert response_json['errors'][0]['message'] == 'Course invite not started'

    def test_course_invite_expired(self, jclient):
        user = UserFactory()
        invite = CourseInviteFactory(is_active=True, expire_at=timezone.now() - timedelta(minutes=1))

        jclient.login(user=user)
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {"key": invite.key})
        assert response.status_code == 400
        assert len(queries_context) == 4

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'invalid'
        assert response_json['errors'][0]['message'] == 'Course invite expired'

    def test_course_max_attempts_exceeded(self, jclient):
        user = UserFactory()
        invite = CourseInviteFactory(is_active=True, max_attempts=0)

        jclient.login(user=user)
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, {"key": invite.key})
        assert response.status_code == 400
        assert len(queries_context) == 5

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'invalid'
        assert response_json['errors'][0]['message'] == 'Activation max attempts exceeded'

    def test_invite_activate(self, jclient, django_assert_num_queries):
        user = UserFactory()
        invite = CourseInviteFactory(is_active=True, max_attempts=1)

        jclient.login(user=user)
        with django_assert_num_queries(26):
            response = jclient.post(self.url, {"key": invite.key})
        assert response.status_code == 201

        response_json = response.json()
        assert response_json == {'course_id': invite.course_id}

        assert UserProject.objects.filter(user=user, project=invite.course.project).count() == 1
        assert CourseStudent.objects.filter(
            student=user, course=invite.course, deleted=False, assignment_rule__isnull=True,
        ).count() == 1
        assert UserCourseInviteActivation.objects.filter(course_invite=invite, user=user).count() == 1
