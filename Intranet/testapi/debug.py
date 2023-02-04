# coding: utf-8
"""
Ресурс для отладки мониторинга
"""

import logging
from time import sleep

import waffle
from tastypie.exceptions import BadRequest
from tastypie.resources import Resource

log = logging.getLogger(__name__)


class DebugResource(Resource):
    class Meta:
        resource_name = 'debug'
        list_allowed_methods = ['get']
        detail_allowed_methods = []

    def obj_get_list(self, bundle, **kwargs):
        if waffle.switch_is_active('debug_resource_500'):
            raise AssertionError('debug 500')
        if waffle.switch_is_active('debug_resource_400'):
            raise BadRequest('debug 400')
        if waffle.switch_is_active('debug_resource_timeout'):
            sleep(60)
        return []
