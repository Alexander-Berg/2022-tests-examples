from django.test import RequestFactory, TestCase, override_settings

from lms.users.permissions import IsTVMAuthenticated
from lms.users.tests.factories import ServiceAccountFactory


class IsTVMAuthenticatedPermissionTestCase(TestCase):
    def setUp(self):
        self.request_factory = RequestFactory()
        self.permission = IsTVMAuthenticated()
        self.url = '/v1/'

    @override_settings(
        TVM_DEBUG=False,
        TVM_ENABLED=False
    )
    def test_disabled_tvm_middleware(self):
        request = self.request_factory.get(self.url)
        assert not self.permission.has_permission(request, view=None)

    def test_missing_service_ticket_src_attr(self):
        request = self.request_factory.get(self.url)
        request.tvm_service_id = None
        assert not self.permission.has_permission(request, view=None)

    def test_disabled_service_account(self):
        request = self.request_factory.get(self.url)
        request.tvm_service_id = 42
        ServiceAccountFactory(tvm_id=42, is_active=False)
        assert not self.permission.has_permission(request, view=None)

    def test_active_service_account(self):
        request = self.request_factory.get(self.url)
        request.tvm_service_id = 42
        ServiceAccountFactory(tvm_id=42, is_active=True)
        assert self.permission.has_permission(request, view=None)

    def test_unknown_tvm_service(self):
        request = self.request_factory.get(self.url)
        request.tvm_service_id = 43
        ServiceAccountFactory(tvm_id=42, is_active=True)
        assert not self.permission.has_permission(request, view=None)
