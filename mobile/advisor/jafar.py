import logging
from datetime import datetime
from functools import partial
from itertools import chain

import requests
import requests.exceptions
from django.conf import settings

from yaphone.advisor.advisor.exceptions import (JafarConnectionError, JafarHTTPException, JafarReadTimeout)
from yaphone.advisor.advisor.models.app import App, Placeholder
from yaphone.advisor.advisor.serializers.apps import AppsBlockSerializerExtended
from yaphone.advisor.advisor.tools import get_random_universal_title_key
from yaphone.advisor.common.tools import make_requests_session

JAFAR_VERSION_HEADER = 'X-Jafar-Version'

logger = logging.getLogger(__name__)
logging.getLogger('py.warnings').setLevel(logging.ERROR)


class JafarResponse(object):
    def __init__(self, blocks, expire_at=None):
        self.blocks = blocks
        self.expire_at = expire_at
        self.title = None


class RecommendationContext(object):
    def __init__(self, experiment='', placement_id=None, categories=None,
                 group_type=None, host=None, request_params=None,
                 group_count=None, group_size=4, page=0, contained_apps=None, card_configs=None):
        self.categories = categories
        self.experiment = experiment
        self.group_type = group_type
        self.placement_id = placement_id
        self.host = host
        self.request_params = request_params
        self.group_count = group_count
        self.group_size = group_size
        self.page = page
        self.contained_apps = contained_apps or []
        self.card_configs = card_configs


class RecommendationBlock(object):
    @property
    def serializer(self):
        raise NotImplementedError

    @property
    def all_items(self):
        raise NotImplementedError


class AppsBlock(RecommendationBlock):
    serializer = AppsBlockSerializerExtended

    def __init__(self, apps, count=None, context=None, title=None, card_type=None,
                 resulting_experiment=None, reserve_apps=None,
                 explanation=None, external_ads=None, subtitle=None, rotation_interval=None):
        self.apps = apps
        self.count = count or len(apps)
        self.context = context
        self.card_type = card_type
        self.resulting_experiment = resulting_experiment
        self.title = title or get_random_universal_title_key()
        self.reserve_apps = reserve_apps or []
        self.explanation = explanation
        self.subtitle = subtitle
        self.external_ads = external_ads or []
        self.mark_as_sponsored = False
        if rotation_interval is not None:
            self.rotation_interval = rotation_interval

    @property
    def all_items(self):
        return chain(self.apps, self.reserve_apps)


def make_jafar_url(method, experiment_name=None):
    base_url = settings.JAFAR_URL
    if experiment_name is not None:
        return '{base}/{method}/{name}'.format(base=base_url, name=experiment_name, method=method)
    return '{base}/{method}'.format(base=base_url, method=method)


class Jafar(object):
    JAFAR_TIMEOUT = 1

    http = make_requests_session(pool_maxsize=100, max_retries=2)
    request_method = 'POST'

    def __init__(self, experiment_name):
        self.experiment_name = experiment_name

    def _get_parsed_response(self, method, query):
        response, jafar_version = self._send_request(method, query, self.experiment_name)
        return self._parse_jafar_response(response.json(), jafar_version)

    def _send_request(self, method, query, experiment_name=None, raise_for_status=True):
        url = make_jafar_url(method, experiment_name or self.experiment_name)
        try:
            if self.request_method == 'POST':
                resp = self.http.post(url, json=query, timeout=self.JAFAR_TIMEOUT)
            elif self.request_method == 'GET':
                resp = self.http.get(url, params=query, timeout=self.JAFAR_TIMEOUT)
            else:
                raise NotImplementedError('Unknown request method: %s' % self.request_method)
            if raise_for_status:
                resp.raise_for_status()
            jafar_version = resp.headers.get(JAFAR_VERSION_HEADER, '')
            return resp, jafar_version
        except requests.exceptions.HTTPError as e:
            raise JafarHTTPException(request=e.request, response=e.response)
        except requests.exceptions.Timeout as e:
            raise JafarReadTimeout(request=e.request, response=e.response)
        except requests.exceptions.RequestException as e:
            raise JafarConnectionError(request=e.request, response=e.response)


def make_app(item, jafar_version):
    if item.get('placeholder'):
        app = Placeholder(popup_type=item.get('popup_type'))
    else:
        app = App(
            package_name=item['package_name'],
            offer_id=item.get('offer_id'),
            cpm=item.get('cpm'),
            expected_fee=item.get('expected_fee'),
            score=item.get('score'),
            mark_as_sponsored=item.get('mark_sponsored', bool(item.get('offer_id'))),
            popup_type=item.get('popup_type'),
        )
    app.impression_id.jafar_version = jafar_version
    app.impression_id.content_type = 'apps'
    return app


class JafarSearch(Jafar):
    request_method = 'GET'
    JAFAR_TIMEOUT = 3

    def __init__(self, client, context, cache_key):
        super(JafarSearch, self).__init__(experiment_name=context.experiment)
        self.context = context
        self.client = client
        self.cache_key = cache_key

    def _parse_jafar_response(self, json_response, jafar_version):
        if json_response:
            make_versioned_app = partial(make_app, jafar_version=jafar_version)
            apps = list(map(make_versioned_app, json_response["search_results"]))

            return JafarResponse(blocks=[AppsBlock(
                count=len(apps),
                apps=apps,
                resulting_experiment=self.experiment_name,
            )], expire_at=datetime.utcnow())

    def search(self, query):
        jafar_query = dict(query=query, count=self.context.group_size, country=self.client.profile.current_country)
        return self._get_parsed_response('search', jafar_query)
