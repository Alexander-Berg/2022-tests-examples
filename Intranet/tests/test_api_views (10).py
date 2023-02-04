from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.mentorships.tests.factories import MentorshipFactory
from lms.users.tests.factories import UserFactory

from .factories import StaffProfileFactory


class LabStaffLanguagesListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-staff-profile-detail'

    def build_expected(self, is_head=False, is_mentor=False):
        return {
            'is_head': is_head,
            'is_mentor': is_mentor,
        }

    def setUp(self) -> None:
        self.user = UserFactory()
        self.client.force_login(self.user)

    def test_url(self):
        self.assertURLNameEqual('my/staff/profile/', base_url=settings.API_BASE_URL)

    def test_no_auth(self):
        self.client.logout()
        self.detail_request(self.get_url(), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_all_negative(self):
        self.detail_request(self.get_url(), expected=self.build_expected(), num_queries=5)

    def test_mentor(self):
        MentorshipFactory(mentor=self.user)
        self.detail_request(self.get_url(), expected=self.build_expected(is_mentor=True), num_queries=5)

    def test_former_mentor(self):
        MentorshipFactory(mentor=self.user, is_active=False)
        self.detail_request(self.get_url(), expected=self.build_expected(), num_queries=5)

    def test_head(self):
        StaffProfileFactory(head=self.user.staffprofile)
        self.detail_request(self.get_url(), expected=self.build_expected(is_head=True), num_queries=5)

    def test_head_of_dismissed_employee(self):
        StaffProfileFactory(head=self.user.staffprofile, is_dismissed=True)
        self.detail_request(self.get_url(), expected=self.build_expected(), num_queries=5)
