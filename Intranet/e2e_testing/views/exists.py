from django.db.models import Q
from rest_framework.response import Response

from wiki.api_core.framework import WikiAPIView
from wiki.api_core.raises import raises
from wiki.pages.models import Page
from wiki.sync.connect.org_ctx import get_org


class PageExistsView(WikiAPIView):
    @raises()
    def post(self, request, *args, **kwargs):
        org_id = None if get_org() is None else get_org().id
        supertag = request.data['supertag']

        exists = Page.objects.filter(Q(supertag=supertag) & Q(org_id=org_id)).exists()
        return Response({'exists': exists}, 200)
