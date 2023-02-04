# -*- coding: utf-8 -*-
import logging
from rest_framework.exceptions import APIException
from rest_framework.response import Response
from rest_framework.views import APIView
from yaphone.advisor.demo import serializers

from yaphone.advisor.common.localization_helpers import get_impersonal_config_value

logger = logging.getLogger(__name__)


class BaseDemoView(APIView):
    result_serializer = None
    format_key = None
    defaults = None

    # noinspection PyUnusedLocal
    def get(self, request, *args, **kwargs):
        params = {}
        for key, default_value in self.defaults.iteritems():
            localization_key = self.format_key % key
            project = kwargs['application']
            params[key] = get_impersonal_config_value(localization_key, default_value, True, project=project)
        serializer = self.result_serializer(data=params)
        if serializer.is_valid(raise_exception=False):
            return Response(serializer.validated_data)

        extra = {'request': request._request, 'sample_rate': 0.1}
        logger.error('invalid data in localization: %s', serializer.errors, extra=extra)
        raise APIException(serializer.errors)


class DemoProvisioningView(BaseDemoView):
    result_serializer = serializers.ProvisioningSerializer
    format_key = 'provisioning.%s'
    defaults = {
        'device_owner_url': 'https://example.com',
        'device_owner_hash': '0CAFDiy1b-yZDxb9EGleFTr9Bkywg5upNSR7Wp5MKaA',
    }


class DemoDeviceOwnerView(BaseDemoView):
    result_serializer = serializers.DeviceOwnerSerializer
    format_key = 'device_owner.%s'
    defaults = {
        'video_url': 'https://some.uri',
        'configure_apps': [
            # All preinstalled yandex apps
            {'enable': True, 'package_name': 'ru.beru.android'},
            {'enable': True, 'package_name': 'ru.yandex.searchplugin'},
            {'enable': True, 'package_name': 'com.yandex.browser'},
            {'enable': True, 'package_name': 'ru.yandex.disk'},
            {'enable': True, 'package_name': 'ru.yandex.androidkeyboard'},
            {'enable': True, 'package_name': 'com.yandex.launcher'},
            {'enable': True, 'package_name': 'ru.yandex.mail'},
            {'enable': True, 'package_name': 'ru.yandex.yandexmaps'},
            {'enable': True, 'package_name': 'ru.yandex.money'},
            {'enable': True, 'package_name': 'ru.yandex.music'},
            {'enable': True, 'package_name': 'ru.yandex.taxi'},
            # camera
            {'enable': True, 'package_name': 'org.codeaurora.snapcam'},
            # clock(it's on the first screen)',
            {'enable': True, 'package_name': 'com.google.android.deskclock'},
        ],
        'organization_name': u'Этим устройством управляет компания Яндекс',
        'demo_user_name': u'Яндекс Демо',
        'disable_factory_reset': True,
        'preferred_launcher': 'com.yandex.launcher',
    }
