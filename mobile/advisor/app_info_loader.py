import gevent
import logging
import random
import yt.wrapper as yt
from OpenSSL import SSL
from django.conf import settings
from django.core.cache import cache
from rest_framework.exceptions import APIException, status
from yt.packages.requests.exceptions import RequestException, BaseHTTPError

logger = logging.getLogger(__name__)
DYNAMIC_TABLES_CONFIG = {
    'proxy': {
        'retries': {
            'count': 1,
        },
        'request_timeout': 2000,
        'connect_timeout': 500,
    },
    'token': settings.YT_TOKEN,
    'dynamic_table_retries': {
        'enable': False,
    },
    # Uncomment when RPC backend will support correct timeouts
    # See https://st.yandex-team.ru/ADVISOR-1948
    # 'backend': 'rpc',
}

# hard timeout for YT wrapper to instantiate YtClient and make a call
YT_REQUEST_TIMEOUT = 3

# Possible errors that can be thrown from the yt call
YT_ERRORS = yt.YtError, RequestException, BaseHTTPError, SSL.Error

FALLBACK_LANGUAGES = ['en', 'ru']
APP_INFO_TTL_RANGE = (23 * 60 * 60, 25 * 60 * 60)


class AppInfoLoaderException(APIException):
    status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    default_detail = "App Info backend is unavailable"


class AppInfoNotFound(AppInfoLoaderException):
    def __init__(self, unknown_apps, language, *args, **kwargs):
        self.unknown_apps = unknown_apps
        self.language = language
        super(AppInfoNotFound, self).__init__(*args, **kwargs)

    status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    default_detail = "App Info not found"


class YtRequestGreenlet(gevent.Greenlet):
    def __init__(self, proxy, *args, **kwargs):
        self.proxy = proxy
        super(YtRequestGreenlet, self).__init__(None, *args, **kwargs)

    def _run(self, command, *args, **kwargs):
        # We have to create YtClient every time to prevent deadlocks because YtClient is not thread-safe
        client = yt.YtClient(proxy=self.proxy, config=DYNAMIC_TABLES_CONFIG)
        method = getattr(client, command)
        return method(*args, **kwargs)


def call_multiple_yt_clusters(command, *args, **kwargs):
    """"
    Spawns greenlets with same request to different Yt proxies.
    Returns first successful result.
    """
    greenlets = [YtRequestGreenlet.spawn(proxy, command, *args, **kwargs) for proxy in settings.YT_PROXIES]
    try:
        for greenlet in gevent.iwait(greenlets, timeout=YT_REQUEST_TIMEOUT):
            # noinspection PyBroadException
            try:
                return greenlet.get()
            except YT_ERRORS as e:
                logger.warning('YT error on %s: %s', greenlet.proxy, e)
            except Exception:
                logger.exception('Unhandled exception')
        else:
            logger.error('YT request to all proxies is failed')
            raise AppInfoLoaderException
    finally:
        # Kill all greenlets that is not finished yet, but not needed anymore
        gevent.killall(greenlets, block=False)


class AppInfoLoader(object):
    def __init__(self):
        self.cache = cache

    @staticmethod
    def make_cache_key(package_name, language):
        return "app_info_cache_{}_{}".format(package_name, language)

    def get_app_info(self, package_name, language):
        app_info_map = self.get_apps_info([package_name], language)
        return app_info_map[package_name]

    def get_apps_info(self, package_names, language, raise_on_missing=True):
        unknown_apps = set(package_names)
        app_info_map = dict()
        for lang in [language] + FALLBACK_LANGUAGES:
            self.fill_apps_info_from_cache(app_info_map, lang, unknown_apps)
            if unknown_apps:
                self.fill_app_info_from_yt(app_info_map, lang, unknown_apps)
            if not unknown_apps:
                return app_info_map
        if raise_on_missing:
            raise AppInfoNotFound(unknown_apps, language)
        return app_info_map

    @staticmethod
    def _get_ttl():
        return random.randint(*APP_INFO_TTL_RANGE)

    def fill_app_info_from_yt(self, app_info_map, lang, unknown_apps):
        query = [{'package_name': app, 'language': lang} for app in unknown_apps]
        apps_lookuper = self.lookup_rows(
            table=settings.YT_APPS_DYNAMIC_TABLE,
            input_stream=query,
            format=yt.format.YsonFormat('binary')
        )
        for app_info in apps_lookuper:
            package_name = app_info['package_name']
            self.cache.add(key=self.make_cache_key(package_name, lang), value=app_info, timeout=self._get_ttl())
            app_info_map[package_name] = app_info
        unknown_apps.difference_update(app_info_map)

    def fill_apps_info_from_cache(self, app_info_map, lang, unknown_apps):
        for package_name in unknown_apps:
            key = self.make_cache_key(package_name, lang)
            app_info = self.cache.get(key)
            if app_info is not None:
                app_info_map[package_name] = app_info
        unknown_apps.difference_update(app_info_map)

    @staticmethod
    def lookup_rows(*args, **kwargs):
        return call_multiple_yt_clusters('lookup_rows', *args, **kwargs)


app_info_loader = AppInfoLoader()
