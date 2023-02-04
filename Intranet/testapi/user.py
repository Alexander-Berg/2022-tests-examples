# coding: utf-8
"""
API для автотестов, частью переиспользует обычное и фронтэндовоe
"""


import logging

from django.conf.urls import url

from tastypie.authorization import Authorization
from tastypie.utils.urls import trailing_slash

from idm.api.frontend.user import UserResource as FrontendUserResource, USERNAME_REGEXP
from idm.sync.ldap.connector import NewLDAP
from idm.users.models import User

log = logging.getLogger(__name__)


class UserResource(FrontendUserResource):
    """
    Ресурс пользователя
    """
    class Meta(FrontendUserResource.Meta):
        allowed_methods = None
        list_allowed_methods = ('get',)
        detail_allowed_methods = ('get', 'put')
        authorization = Authorization()

    def prepend_urls(self):
        """
        В отличие от фронтэнд варианта, возвращаем отдельно активные, отдельно неактивные роли,
        а также добавляем ручку списка протухающих ролей.
        """
        def _generate_user_api_method(method_name, view_name, ref_name):
            """
            хелпер, чтобы генерировать url'ы сразу для обращения как по user_id, так и по username
            """
            generated = []
            template = r'^(?P<resource_name>{resource_name})/{id_type_regexp}/{method_name}{slash_regexp}$'
            for id_type_regexp in ('(?P<pk>\d+)', '(?P<username>%s)' % USERNAME_REGEXP):
                url_regexp = template.format(
                    id_type_regexp=id_type_regexp,
                    resource_name=self._meta.resource_name,
                    method_name=method_name,
                    slash_regexp=trailing_slash()
                )
                generated.append(url(url_regexp, self.wrap_view(view_name), name=ref_name))
            return generated

        result_urls = [
            # позволяем находить пользователей в API по логинам (но исключаем служебный урл /schema)
            url(r'^(?P<resource_name>{resource_name})/(?P<username>(?!schema){username_regexp})/$'.format(
                resource_name=self._meta.resource_name,
                username_regexp=self._meta.resource_name
            ), self.wrap_view('dispatch_detail'), name='api_dispatch_detail')
        ]

        result_urls.extend(_generate_user_api_method('ad-restore', 'ad_restore_user', 'api_ad_restore_user'))
        return result_urls

    def ad_restore_user(self, request, username=None, **kwargs):
        user = User.objects.users().get(username=username)
        self.method_check(request, allowed=['put'])

        with NewLDAP() as ldap:
            ldap.restore_user(user, ad_reason_data={
                'reason': 'по запросу из test API'
            })

            user.ldap_active = True
            user.ldap_blocked = False
            user.ldap_blocked_timestamp = None
            user.save()

        return self.create_response(request, None, status=204)
