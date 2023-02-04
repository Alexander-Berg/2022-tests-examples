import logging
from django.conf import settings

from staff.lib.auth.utils import get_or_create_test_user
from staff.lib.middleware import AuthMiddleware

from django_yauth.user import YandexUser

logger = logging.getLogger(__name__)


class TestAuthMiddleware(AuthMiddleware):

    def assign_yauser(self, request):
        request.__class__.yauser = YandexUser(
            uid=settings.AUTH_TEST_PERSON_UID,
            fields={'login': settings.AUTH_TEST_USER},
            mechanism=self,
        )

    def assign_user(self, request):
        request.__class__.user = get_or_create_test_user()

    def process_request(self, request):
        request.real_user_ip = '::1'
        return super(AuthMiddleware, self).process_request(request)
