# coding: utf-8
"""
API для автотестов, частью переиспользует обычное и фронтэндовоe
"""


import logging

from django.utils import timezone
from tastypie.authorization import Authorization
from tastypie.exceptions import BadRequest

from idm.api.frontend.system import SystemResource as FrontendSystemResource
from idm.core.models import System, Workflow
from idm.utils import json

log = logging.getLogger(__name__)


class SystemResource(FrontendSystemResource):
    """
    Расширенный ресурс системы для тестировщиков на тестинге
    """
    class Meta(FrontendSystemResource.Meta):
        list_allowed_methods = ['get', 'post']
        detail_allowed_methods = ['get', 'delete', 'post', 'put']
        fields = [
            'slug', 'name', 'name_en', 'added', 'updated', 'is_active', 'is_broken',
            'has_review', 'passport_policy', 'group_policy', 'request_policy',
            'role_grant_policy', 'roletree_policy', 'review_on_relocate_policy',
            'inconsistency_policy', 'workflow_approve_policy', 'audit_method',
            'emails', 'check_certificate', 'use_requests', 'is_sox', 'use_mini_form',
            'description', 'description_en', 'base_url', 'plugin_type', 'node_plugin_type', 'roles_tree_url', 'use_webauth',
            'sync_interval', 'endpoint_timeout', 'endpoint_long_timeout',
        ]
        authorization = Authorization()

    def dehydrate(self, bundle):
        """
        используем оригинальный набор полей модели, без наворотов, которые включены для фронтенда
        """
        return super(SystemResource, self).dehydrate(bundle)

    def post_list(self, request, **kwargs):
        """
        Создание тестовой системы
        """
        data = json.loads(request.body)

        for field in ('slug', 'workflow', 'url'):
            if field not in data:
                raise BadRequest('Field "%s" is missing' % field)

        if System.objects.filter(slug=data['slug']).exists():
            raise BadRequest('System with slug "%s" already exists' % data['slug'])

        system = System.objects.create(
            slug=data['slug'],
            name=data.get('name', data['slug']),
            base_url=data['url'],
        )

        workflow = Workflow.objects.create(
            system=system,
            workflow=data['workflow'],
            state='approved',
            user=request.user,
            approver=request.user,
            approved=timezone.now(),
        )
        workflow.approve(request.user, send_mail=False, bypass_checks=True)

        return self.create_response(request, None, status=204)

    def delete_detail(self, request, slug, **kwargs):
        """
        Удаляем систему
        """
        system = System.objects.get(slug=slug)
        workflows = system.workflows.order_by('-pk')
        for wf in workflows:
            wf.delete()
        system.delete()
        return self.create_response(request, data=None, status=204)
