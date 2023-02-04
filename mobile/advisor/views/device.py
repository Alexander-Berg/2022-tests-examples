# -*- coding: utf-8 -*-

import logging
import mongoengine as me
import requests
from datetime import datetime
from django.conf import settings
from django.http.request import HttpRequest
from rest_framework.exceptions import ValidationError, APIException, status
from rest_framework.response import Response

from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.models.lbs import LBSInfo
from yaphone.advisor.advisor.models.profile import Profile
from yaphone.advisor.advisor.serializers.device import (AndroidInfoSerializer, ProfileSerializer, ClientSerializer,
                                                        PackagesInfoSerializer, LbsInfoSerializer)
from yaphone.advisor.advisor.views.base import BaseAPIView
from yaphone.advisor.common.exceptions import NoDeviceInfoAPIError, KnownParseError

logger = logging.getLogger(__name__)

lbs_logger = logging.getLogger('lbs')

SUP_REQUEST_TIMEOUT = 1


class RequestError(APIException):
    status_code = status.HTTP_400_BAD_REQUEST
    default_detail = 'Request format error'


class PackagesInfoView(BaseAPIView):
    validator_class = PackagesInfoSerializer

    # noinspection PyUnusedLocal
    def post(self, request, *args, **kwargs):
        validated_data = self.get_validated_data(request.data)
        try:
            client = Client.objects.get(uuid=self.uuid)
            profile = client.profile
            profile.update_packages_info(validated_data['packages_info'])
            profile.crypta.update(
                device_id=profile.pk,
                uuid=self.uuid,
                passport_uid=profile.passport_uid,
                ad_id=profile.android_info.ad_id,
            )
            profile.save()
        except me.DoesNotExist:
            raise NoDeviceInfoAPIError
        except me.ValidationError as e:
            raise ValidationError(e.message)
        return Response()


class LbsInfoView(BaseAPIView):
    validator_class = LbsInfoSerializer

    # noinspection PyUnusedLocal
    def post(self, request, *args, **kwargs):
        validated_data = self.get_validated_data(request.data)
        try:
            return update_lbs(validated_data, self.uuid, self.ip)
        except me.ValidationError as e:
            raise ValidationError(e.message)
        except me.DoesNotExist:
            raise NoDeviceInfoAPIError


class AndroidClientInfoView(BaseAPIView):
    profile_required = False

    def initial(self, request, *args, **kwargs):
        if isinstance(self.request.stream, HttpRequest) and \
                self.request.stream.body.endswith('"carrier_name"'):
            # Do not log known bug https://st.yandex-team.ru/PHONE-3521
            raise KnownParseError()
        return super(AndroidClientInfoView, self).initial(request, *args, **kwargs)

    # noinspection PyUnusedLocal
    def post(self, request, *args, **kwargs):
        passport_info = self.get_passport_info(request)
        passport_uid = passport_info.get('passport_uid')
        try:
            update_profile(request.data, self.uuid, self.user_agent, passport_uid)
        except me.ValidationError as e:
            raise ValidationError(e.message)
        return Response()


def register_in_sup(device_id, uuid, passport_uid):
    params = {
        'installId': uuid.hex,
        'did': device_id.hex,
        'uid': passport_uid,
    }
    try:
        response = requests.get(url=settings.SUP_LOGIN_URL, params=params, timeout=SUP_REQUEST_TIMEOUT)
        response.raise_for_status()
        logger.info('Successfully registered user %s in SUP', device_id.hex)
    except (requests.exceptions.BaseHTTPError, requests.exceptions.RequestException) as e:
        logger.error('Failed to register user in SUP: %r, %s', params, e)
    except Exception as e:
        logger.error('Unknown error registering user in SUP: %r, %s', params, e)


def unregister_in_sup(device_id, uuid):
    params = {
        'installId': uuid.hex,
        'did': device_id.hex,
    }
    try:
        response = requests.get(url=settings.SUP_LOGOUT_URL, params=params, timeout=SUP_REQUEST_TIMEOUT)
        response.raise_for_status()
        logger.info('Successfully unregistered user %s in SUP', device_id.hex)
    except (requests.exceptions.BaseHTTPError, requests.exceptions.RequestException) as e:
        logger.error('Failed to unregister user in SUP: %r, %s', params, e)
    except Exception as e:
        logger.error('Unknown error unregistering user in SUP: %r, %s', params, e)


def update_profile(update_data, uuid, user_agent=None, passport_uid=None):
    # known bug, avoid ValidationError here: https://st.yandex-team.ru/ADVISOR-1912
    if update_data.get('device_id', '').startswith('reckit_'):
        raise RequestError()

    validator = ProfileSerializer(data=update_data)
    validator.is_valid(raise_exception=True)

    device_id = update_data['device_id']
    update_data['uuid'] = uuid

    current_client = Client.objects(uuid=uuid).no_dereference().first()
    current_profile = Profile.objects(device_id=device_id).first()

    info_serializer = AndroidInfoSerializer(data=update_data)
    info_serializer.is_valid(raise_exception=True)
    android_info = info_serializer.validated_data
    android_info.updated_at = datetime.utcnow()

    profile_serializer = ProfileSerializer(current_profile, data=update_data)
    profile_serializer.is_valid(raise_exception=True)

    profile = profile_serializer.save(android_info=android_info, passport_uid=passport_uid,
                                      updated_at=datetime.utcnow())

    # Pass locale to save it to client object
    update_data['locale'] = update_data['user_settings']['locale']

    client_serializer = ClientSerializer(current_client, data=update_data)
    client_serializer.is_valid(raise_exception=True)

    # UserAgent is already serialized model
    if user_agent:
        client_serializer.validated_data['user_agent'] = user_agent
    client = client_serializer.save(uuid=uuid, profile=profile, updated_at=datetime.utcnow())
    if passport_uid:
        register_in_sup(uuid=client.pk, device_id=profile.pk, passport_uid=passport_uid)
    else:
        unregister_in_sup(uuid=client.pk, device_id=profile.pk)


def update_lbs(data, client_uuid, ip):
    # load only lbs field in profile
    client = Client.objects.no_dereference().get(uuid=client_uuid)
    profile = Profile.objects.only('lbs_info', 'operators').get(device_id=client.profile.id)

    location = data.get('location')
    cells = data.get('cells')
    wifi_networks = data.get('wifi_networks')
    time_zone = data.get('time_zone')

    lbs_info = profile.lbs_info or LBSInfo()
    lbs_info.update_lbs(location, cells, profile.operators, wifi_networks, time_zone, ip, client_uuid)
    profile.modify(lbs_info=lbs_info, updated_at=datetime.utcnow())
    client.modify(country=lbs_info.country_init)

    return Response({
        'country_init': profile.lbs_info.country_init,
        'country': profile.lbs_info.country,
    })
