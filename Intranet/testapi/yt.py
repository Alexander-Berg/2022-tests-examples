from yt.wrapper import YtClient, select_rows

from tastypie.http import (
    HttpResponse,
    HttpNotFound,
    HttpForbidden,
    HttpBadRequest,
    HttpUnauthorized,
)
from constance import config
from django.conf import settings

from idm.core.models import System
from idm.users.constants.user import USER_TYPES


def export_roles(request):
    if not request.user.is_authenticated:
        return HttpUnauthorized()

    if request.user.type == USER_TYPES.TVM_APP:
        tvm_id = request.user.username
    elif request.user.is_superuser:
        tvm_id = request.GET.get('tvm_id', '')
    else:
        return HttpForbidden()

    if not tvm_id.isdigit():
        return HttpBadRequest('tvm_id should be integer')

    system_slug = request.GET.get('system_slug')

    if system_slug and not System.objects.filter(slug=system_slug).exists():
        return HttpBadRequest('system with specified slug is not found')

    query = 'blob FROM [{path}] WHERE tvm_id = {tvm_id} {extra} ORDER BY revision DESC LIMIT 1'.format(
        path=config.YT_EXPORT_TABLE,
        tvm_id=tvm_id,
        extra=f'AND try_get_string(meta, "/system_slug") = "{system_slug}"' if system_slug else '',
    )

    client = YtClient(proxy=config.YT_EXPORT_CLUSTER, token=settings.IDM_YT_OAUTH_TOKEN)
    row = next(select_rows(query=query, client=client), None)

    if row:
        return HttpResponse(row.get('blob', '{}'), content_type='application/json')
    else:
        return HttpNotFound()
