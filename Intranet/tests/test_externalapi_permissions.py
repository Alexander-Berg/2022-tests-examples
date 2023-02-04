from unittest.mock import MagicMock

from django.test import RequestFactory, TestCase, override_settings

from lms.courses.permissions import CourseAllowedTVMServicePermission
from lms.courses.tests.factories import ServiceAccountCourseFactory
from lms.users.tests.factories import ServiceAccountFactory


class CourseAllowedTVMServicePermissionTestCase(TestCase):
    def setUp(self):
        self.permission = CourseAllowedTVMServicePermission()
        self.request = RequestFactory().get('/')

    @override_settings(
        TVM_DEBUG=False,
        TVM_ENABLED=False
    )
    def test_disabled_tvm_middleware(self):
        assert not self.permission.has_permission(self.request, view=None)

    def test_missing_tvm_service_id_in_request(self):
        self.request.tvm_service_id = None
        assert not self.permission.has_permission(self.request, view=None)

    def test_inactive_service_account(self):
        service_account = ServiceAccountFactory(tvm_id=42, is_active=False)
        self.request.tvm_service_id = service_account.tvm_id
        mocked_view = MagicMock()
        mocked_view.get_course_id.return_value = 43
        assert not self.permission.has_permission(self.request, view=mocked_view)

    def test_course_allowed_for_inactive_service_account(self):
        service_account = ServiceAccountFactory(tvm_id=42, is_active=False)
        self.request.tvm_service_id = service_account.tvm_id
        linked = ServiceAccountCourseFactory(service_account=service_account)
        mocked_view = MagicMock()
        mocked_view.get_course_id.return_value = linked.course_id
        assert not self.permission.has_permission(self.request, view=mocked_view)

    def test_course_allowed_for_active_service_account(self):
        service_account = ServiceAccountFactory(tvm_id=42, is_active=True)
        self.request.tvm_service_id = service_account.tvm_id
        linked = ServiceAccountCourseFactory(service_account=service_account)
        mocked_view = MagicMock()
        mocked_view.get_course_id.return_value = linked.course_id
        assert self.permission.has_permission(self.request, view=mocked_view)

    def test_course_not_allowed_for_active_service_account(self):
        service_account = ServiceAccountFactory(tvm_id=42, is_active=True)
        self.request.tvm_service_id = service_account.tvm_id
        mocked_view = MagicMock()
        mocked_view.get_course_id.return_value = 1
        assert not self.permission.has_permission(self.request, view=mocked_view)
