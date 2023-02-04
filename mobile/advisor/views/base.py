# -*- coding: utf-8 -*-
import logging
import mongoengine
import yenv
from datetime import datetime
from django.utils.http import parse_http_date
from pymongo.errors import ConnectionFailure, ExecutionTimeout
from raven.contrib.django.raven_compat.models import client as raven_client
from rest_framework import status
from rest_framework.exceptions import ParseError, ValidationError, AuthenticationFailed
from rest_framework.response import Response
from rest_framework.views import APIView, exception_handler
from ticket_parser2.api.v1 import BlackboxClientId
from uuid import UUID
from yaphone.advisor.common import passport

from yaphone.advisor.advisor.app_info_loader import AppInfoNotFound
from yaphone.advisor.advisor.exceptions import ClientSaveRaceException
from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.models.lbs import get_region_parents
from yaphone.advisor.advisor.models.profile import Locale
from yaphone.advisor.advisor.user_agent_creator import user_agent_creator
from yaphone.advisor.common.exceptions import BadRequestAPIError, NoDeviceInfoAPIError, KnownParseError
from yaphone.advisor.common.tools import get_http_header
from yaphone.utils import geo

AUTHORIZATION_HEADER = 'Authorization'
AUTHORIZATION_HEADER_PREFIX = 'OAuth '
IF_MODIFIED_SINCE_FORMAT = '%a, %d %b %Y %H:%M:%S GMT'

logger = logging.getLogger(__name__)


# noinspection PyProtectedMember
def custom_exception_handler(exc, context):
    if isinstance(exc, (ParseError, ValidationError)) and not isinstance(exc, KnownParseError):
        action = 'parse' if isinstance(exc, ParseError) else 'validation'
        logger.error('%s error: %s', action, exc.detail, exc_info=True,
                     extra={'request': context['view'].request._request})
    elif isinstance(exc, AppInfoNotFound):
        for package_name in exc.unknown_apps:
            logger.error('App Info %s for %s not found', package_name, exc.language)
    elif isinstance(exc, (ConnectionFailure, ExecutionTimeout)):
        logger.warning('MongoDB connection error', exc_info=True)
        return Response('MongoDB error: %s' % repr(exc), status.HTTP_503_SERVICE_UNAVAILABLE)
    return exception_handler(exc, context)


class RequestValidatorMixin(object):
    validator_class = None

    def get_validated_data(self, request_data, validator_class=None):
        validator_class = validator_class or self.validator_class
        validator = validator_class(data=request_data)
        validator.is_valid(raise_exception=True)
        return validator.validated_data


class MobileUser(object):
    """
    Used for sentry user UUID logging
    """

    def __init__(self, uuid):
        self.pk = uuid

    def is_authenticated(self):
        return True


# noinspection PyOldStyleClasses
class BaseAPIView(APIView, RequestValidatorMixin):
    clid_headers_required = []
    load_fast_profile = True
    profile_required = True
    locale_required = False
    save_client = False

    def initial(self, request, *args, **kwargs):
        super(BaseAPIView, self).initial(request, *args, **kwargs)
        if self.clid_headers_required:
            for clid_id in self.clid_headers_required:
                if get_clid(request, clid_id) is None:
                    raise BadRequestAPIError('X-Ya%s header is missing' % clid_id)

        self.host = get_http_header(request, 'HOST')
        self.ip = get_user_ip(request)
        self.app = kwargs.get('application', '')
        self.user_agent = get_user_agent(self.app, request)
        self.uuid = self.get_uuid(request)

        request.user = MobileUser(self.uuid)
        raven_client.tags_context(dict(
            app=self.user_agent.app_name,
            app_version=self.user_agent.app_version_string,
            device='{0.device_manufacturer} {0.device_model}'.format(self.user_agent),
            os='{0.os_name} {0.os_version}'.format(self.user_agent),
        ))
        self.load_database_client(request)

    @staticmethod
    def get_uuid(request):
        uuid_string = get_http_header(request, 'X_YAUUID')
        if not uuid_string:
            raise BadRequestAPIError("X-YAUUID header is missing")
        try:
            return UUID(uuid_string)
        except ValueError:
            raise BadRequestAPIError("Badly formed hexadecimal UUID string [%s]" % uuid_string)

    @staticmethod
    def get_if_modified_since(request, raise_on_missing=False):
        dtime = get_http_header(request, 'If-Modified-Since')
        try:
            return datetime.fromtimestamp(parse_http_date(dtime))
        except (ValueError, TypeError):
            if raise_on_missing:
                raise BadRequestAPIError("Badly formed 'If-Modified-Since' header [%s]" % dtime)

    def load_database_client(self, request):
        clids = {clid_id: get_clid(request, clid_id) for clid_id in self.clid_headers_required}
        locale = get_locale(request, raise_on_missing=self.locale_required)

        # Try to load client from database or create new one.
        self.client = Client.objects(uuid=self.uuid).first()
        if self.client is None:
            self.client = Client(
                uuid=self.uuid,
                user_agent=self.user_agent,
                clids=clids,
                locale=locale,
            )
            if self.save_client:
                try:
                    # Like get_or_create, but without transactions and with possible race conditions.
                    # mongoengine raises NotUniqueError if document exists
                    # noinspection PyProtectedMember
                    self.client.save(force_insert=True)
                except mongoengine.errors.NotUniqueError:
                    # Race condition when saving client document
                    # Return 503 error forcing balancer to retry
                    logger.warning('Race condition for client %s', self.uuid.hex)
                    raise ClientSaveRaceException

        try:
            self.region = geo.geobase_lookuper.get_region_by_ip(self.ip)['id']
            self.country_id = geo.geobase_lookuper.get_country_id(self.region)
            self.client.regions_by_ip = get_region_parents(self.region)
        except RuntimeError:
            logger.warning('Region and country by IP geobase lookup failed')

        if locale:
            # Acceot-Language overrides database value
            self.client.locale = locale

        if self.profile_required:
            err = NoDeviceInfoAPIError('Information about user "%s" not found' % self.uuid.hex)
            try:
                if self.client.profile is None:
                    raise err
            except mongoengine.errors.DoesNotExist:
                raise err

        self.client.application = self.app
        self.client.user_agent = self.user_agent

        # Add clid1 information from request headers if it isn't in database.
        # It is needed for targeting in case of RecKit being installed in Yandex launcher
        clid1 = get_clid(request, 'clid1')
        if clid1 is not None and 'clid1' in self.client.clids:
            db_header = self.client.clids['clid1']
            if db_header != clid1:
                logger.warning('Header in request %r is not equal to header from db %r', clid1, db_header)
        if clid1 is not None and 'clid1' not in self.client.clids:
            self.client.clids['clid1'] = clid1

        yphone_id = get_http_header(request, 'X-Phone-ID')
        if yphone_id:
            self.client.yphone_id = yphone_id

    @property
    def profile_exists(self):
        return self.client.profile is not None

    def get_authenticate_header(self, request):
        result = super(BaseAPIView, self).get_authenticate_header(request)
        if not result:
            oauth_string = get_http_header(request, AUTHORIZATION_HEADER)
            if oauth_string:
                # 'OAuth' return to avoid conversion to http403  from http401 in self.handle_exception()
                result = 'OAuth'
        return result

    def get_passport_info(self, request):
        passport_token = get_oauth_token(request)
        if passport_token:
            try:
                return passport.get_info(authorization_token=passport_token, user_ip=self.ip)
            except passport.PassportTokenError:
                if yenv.type == 'testing':
                    # Going first to mimino, if failed falling back to test blackbox in testing environment.
                    # For more info see https://st.yandex-team.ru/ADVISOR-1787
                    try:
                        return passport.get_info(authorization_token=passport_token,
                                                 user_ip=self.ip,
                                                 blackbox_client=BlackboxClientId.Test,
                                                 is_fallback=True)
                    except passport.PassportTokenError:
                        pass
                raise AuthenticationFailed('invalid_token')
        return {}

    @staticmethod
    def get_uid_from_passport_info(passport_info):
        passport_uid = passport_info.get('passport_uid')
        if not passport_uid:
            raise BadRequestAPIError("Failed to get passport uid via token")
        return passport_uid


class MobileApiView(BaseAPIView):
    """
    Basic view for apprec and old launcher endpoints.
    It is required to send android_client_info, lbs_info and packages_info before
    requesting views subclassed from this.
    """
    profile_required = True


class StatelessView(BaseAPIView):
    """
    Allows to create client object from request data/headers.
    Doesn't require device info to be sent before and never answers with code 418
    """
    clid_headers_required = ('clid1',)
    profile_required = False
    locale_required = True


def get_user_ip(request):
    x_forwarded_for_y = get_http_header(request, 'X_FORWARDED_FOR_Y')
    if x_forwarded_for_y:
        return x_forwarded_for_y.split(',')[0].strip()
    else:
        return request.META.get('REMOTE_ADDR')


def get_user_agent(app, request):
    user_agent_string = get_http_header(request, 'USER_AGENT')
    if user_agent_string is None:
        raise BadRequestAPIError("User-Agent header is missing")
    user_agent = user_agent_creator.create(app, user_agent_string)
    if not user_agent:
        raise BadRequestAPIError("User-Agent header has wrong format")
    return user_agent


def get_locale(request, raise_on_missing=True):
    locale_string = get_http_header(request, 'ACCEPT_LANGUAGE')
    if locale_string:
        return Locale(locale_string)
    if raise_on_missing:
        raise BadRequestAPIError('Accept-Language header is missing')
    return None


def get_clid(request, clid_id):
    clid_id = clid_id.upper()
    clid_string = get_http_header(request, 'X_YA%s' % clid_id)
    if clid_string:
        try:
            int(clid_string)
        except (TypeError, ValueError):
            raise BadRequestAPIError('X-Ya%s header should be a number casting to int' % clid_id)
        return clid_string
    return None


def get_oauth_token(request):
    oauth_string = get_http_header(request, AUTHORIZATION_HEADER)
    if not oauth_string:
        return None

    oauth_string = oauth_string.strip()
    if not oauth_string.startswith(AUTHORIZATION_HEADER_PREFIX):
        raise BadRequestAPIError("Authorization header's value should start with '%s'" % AUTHORIZATION_HEADER_PREFIX)
    return oauth_string[len(AUTHORIZATION_HEADER_PREFIX):].strip()
