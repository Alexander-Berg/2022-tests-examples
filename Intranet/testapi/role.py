# coding: utf-8
"""
API для автотестов, частью переиспользует обычное и фронтэндовоe
"""


import logging

from django.conf.urls import url
from tastypie.authorization import Authorization
from tastypie.exceptions import ImmediateHttpResponse
from tastypie.utils.urls import trailing_slash

from idm.api.exceptions import BadRequest
from idm.api.frontend.apifields import ApiUserField, SystemForeignKey, RoleNodeForeignKey
from idm.api.frontend.base import FrontendApiResource
from idm.api.testapi.forms import RoleRawUpdateForm
from idm.core import exceptions
from idm.core.models import System, Role
from idm.framework.requester import requesterify
from idm.users.models import User
from idm.users.constants.user import USER_TYPES
from idm.utils import json

log = logging.getLogger(__name__)


class RoleResource(FrontendApiResource):
    """
    Расширенный ресурс роли для тестировщиков на тестинге
    """

    system = SystemForeignKey()
    user = ApiUserField('user', default=None)
    node = RoleNodeForeignKey()

    class Meta(FrontendApiResource.Meta):
        abstract = False
        object_class = Role
        queryset = Role.objects.select_related('system', 'user', 'group', 'node__system')
        resource_name = 'roles'
        list_allowed_methods = ('get', 'post')
        detail_allowed_methods = ('get', 'put', 'delete')
        fields = [
            'data',
            'granted_at',
            'id',
            'is_active',
            'expire_at',
            'review_at',
            'state',
            'system',
            'system_specific',
            'user',
        ]
        filtering = {'state': 'exact', 'user': 'exact', 'system': 'exact', 'is_active': 'exact'}
        ordering = ('state', 'added', 'updated', 'granted_at', 'expire_at', 'review_at', 'data', 'id', 'system', 'user')
        limit = 100
        excludes = []
        authorization = Authorization()

    def dehydrate(self, bundle):
        bundle.data['human'] = bundle.obj.humanize()
        bundle.data['data'] = bundle.obj.node.data
        return bundle

    def put_detail(self, request, **kwargs):
        request.role = self.obj_get(self.build_bundle(request=request), **self.remove_api_resource_names(kwargs))
        return super(RoleResource, self).put_detail(request, **kwargs)

    def alter_deserialized_detail_data(self, request, data):
        """
        позволяет изменять поля пользователя и системы у роли передавая username и slug соответственно
        """
        form = RoleRawUpdateForm(request.role, data)
        if not form.is_valid():
            raise BadRequest(form.errors)
        query = form.cleaned_data
        if query['user']:
            data['user'] = query['user']
        if query['system']:
            data['system'] = query['system']
        if query['node']:
            data['node'] = query['node']

        if 'node' in data:
            data['node'].fetch_system()

        return data

    def save(self, bundle, skip_errors=False):
        """
        копипаста из базового класса, чтобы указать update_fields при сохранении роли
        """
        self.is_valid(bundle)

        if bundle.errors and not skip_errors:
            raise ImmediateHttpResponse(response=self.error_response(bundle.request, bundle.errors))

        # Check if they're authorized.
        if bundle.obj.pk:
            self.authorized_update_detail(self.get_object_list(bundle.request), bundle)
        else:
            self.authorized_create_detail(self.get_object_list(bundle.request), bundle)

        # Save FKs just in case.
        self.save_related(bundle)

        # Save the main object.
        bundle.obj.save(update_fields=['review_at', 'node', 'system_specific'])
        bundle.objects_saved.add(self.create_identifier(bundle.obj))

        # Now pick up the M2M bits.
        m2m_bundle = self.hydrate_m2m(bundle)
        self.save_m2m(m2m_bundle)
        return bundle

    def prepend_urls(self):
        """
        На тестинге для тестировщиков у ресурса роли есть метод, позволяющий
        сделать роль протухшей вручную
        """
        return [
            # подвинуть роль по этапам
            url(r'^(?P<resource_name>%s)/(?P<pk>\d+)/(?P<action>(expire|deprive))%s$' %
                (self._meta.resource_name, trailing_slash()),
                self.wrap_view('move_role'), name='api_move_role'),
        ]

    def move_role(self, request, **kwargs):
        """
        Подвинуть роль по этапам
        """
        self.method_check(request, allowed=['post'])
        action = kwargs.pop('action')
        comment = request.POST.get('comment', '')

        # create a basic bundle object for self.get_cached_obj_get
        basic_bundle = self.build_bundle(request=request)
        role = self.obj_get(
            bundle=basic_bundle,
            **self.remove_api_resource_names(kwargs)
        )

        states = {
            'expire': ('depriving', 'expire'),
            'deprive': ('depriving', 'deprive'),
        }

        if action not in states:
            raise BadRequest('Unknown action %s' % action)

        try:
            state, transition = states[action]
            role.set_state(
                state,
                transition=transition,
                requester=requesterify(request.user),
                comment=comment,
            )

            return self.create_response(request, None, status=204)
        except exceptions.RoleStateSwitchError as e:
            raise BadRequest(str(e))

    def post_list(self, request, **kwargs):
        """
        Создать запрос на выдачу роли
        """
        data = json.loads(request.body)
        requester_type = data.get('requester_type', USER_TYPES.USER)
        requester = User.objects.get(username=data['requester'], type=requester_type)

        # в случае отсутствия явно указанного пользователя, берем человека, который посылает запрос
        user_type = data.get('user_type', USER_TYPES.USER)
        user = User.objects.get(username=data['user'], type=user_type) if data.get('user', None) else requester
        system = System.objects.select_related('actual_workflow').get(slug=data['system'])
        role_data = data['data']
        comment = data.get('comment', None)
        system_specific = data.get('system_specific', None)

        Role.objects.request_role(requester, user, system, comment, role_data, system_specific)
        return self.create_response(request, None, status=201)
