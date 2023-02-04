
from django.conf import settings
from django.utils.deprecation import MiddlewareMixin

from wiki.sync.connect.models import Organization


class OrgDetectorMiddleware(MiddlewareMixin):
    def process_request(self, request):
        if not settings.IS_BUSINESS:
            # в интранете нет организаций
            request.org = None
            return

        dir_org_id = '42'
        request.org = Organization.objects.get(dir_id=dir_org_id)
        return
