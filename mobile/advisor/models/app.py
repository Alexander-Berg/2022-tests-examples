# -*- coding: utf-8 -*-

import logging
import re
from datetime import date
from collections import namedtuple

from django.core.cache import caches

from yaphone.advisor.advisor.impression_id import ImpressionID
from yaphone.advisor.advisor.serializers.apps import PlaceholderSetializer, AppSerializerExtended

logger = logging.getLogger(__name__)

GOOGLE_PLAY_UNIQUE_SCREENSHOTS_COUNT = 8  # ADVISOR-292
GOOGLE_PLAY_MAX_TITLE_LENGTH = 50

cache = caches['shared']

TRIMMING_REGEXP = re.compile(r'^(?P<trimmed_text>.*\w)\W', flags=re.UNICODE | re.DOTALL)
ORIGINAL_IMAGE_POSTFIX = '/orig'


def ensure_unicode(string):
    """
    Temporary solution when both maas and YT dyntables are supported.
    Does nothing if input is unicode, converts it to unicode when input is str.
    """
    # TODO: Remove it when maas will be eliminated
    if isinstance(string, unicode):
        return string
    return string.decode('utf8')


def make_snippet(text, max_length=150, min_length=50, add_dots=True):
    def trim_by_dot():
        results = []
        for char in ('.', '!', '?'):
            results.append(text[:max_length].rpartition(char)[0] + char)

        return sorted(results, key=lambda x: len(x), reverse=True)[0]

    def trim_by_word():
        match = TRIMMING_REGEXP.match(text[:max_length])
        if match is None:
            return text[:max_length]
        trimmed = match.groupdict()['trimmed_text']
        if add_dots:
            trimmed += u'\u2026'  # …
        return trimmed

    if len(text) <= max_length:
        return text

    result = trim_by_dot()
    if len(result) < min_length:
        result = trim_by_word()

    return result


class RecBlockItem(object):
    serializer = None
    _impression_id = None

    def __init__(self, package_name, popup_type=None, offer_id=None):
        self.popup_type = popup_type
        self.package_name = package_name
        self.offer_id = offer_id

    @property
    def impression_id(self):
        if self._impression_id is None:
            self._impression_id = ImpressionID()
        return self._impression_id

    @property
    def is_promo(self):
        return self.offer_id is not None


# noinspection PyProtectedMember
class App(RecBlockItem):
    serializer = AppSerializerExtended
    Screenshot = namedtuple('Screenshot', ['preview', 'full'])

    title = None
    icon = None
    icon_context_colors = None
    description = None
    categories = None
    genres = None
    rating = None
    rating_count = None
    is_free = None
    release_date = None
    adult = None
    disclaimer = None
    promo_forced = False
    download_url = None
    publisher = None

    def __init__(self, package_name=None, offer_id=None, cpm=None, expected_fee=None, score=None,
                 mark_as_sponsored=False,
                 *args, **kwargs):
        super(App, self).__init__(package_name, *args, **kwargs)
        self.offer_id = offer_id
        self.cpm = cpm
        self.expected_fee = expected_fee
        self.mark_as_sponsored = mark_as_sponsored
        self.score = score
        self.screenshots = []
        self.screenshot_urls = []
        self.screenshots_info = []

    @property
    def adnetwork_name(self):
        return self.offer_id.split("_", 1)[0]

    def patch_icon_url(self, size_name):
        if self.icon.endswith(ORIGINAL_IMAGE_POSTFIX):
            self.icon = '%s/%s' % (self.icon[:-len(ORIGINAL_IMAGE_POSTFIX)], size_name)
        self.icon = self.icon.replace('http:', 'https:')

    def add_screenshot_block(self, preview, full):
        self.screenshots.append(self.Screenshot(preview=preview, full=full))

    def fill_info(self, app_info):
        self.title = ensure_unicode(app_info['name'])[:GOOGLE_PLAY_MAX_TITLE_LENGTH]
        self.icon = app_info['icon_url']
        self.description = make_snippet(ensure_unicode(app_info['description']))
        self.categories = app_info['categories']
        self.genres = map(ensure_unicode, app_info['genres'])
        self.rating = app_info['rating']
        self.rating_count = app_info['rating_count']
        self.is_free = app_info['is_free']
        self.screenshot_urls = app_info['screenshots'][:GOOGLE_PLAY_UNIQUE_SCREENSHOTS_COUNT]
        self.screenshots_info = app_info['screenshots_info'][:GOOGLE_PLAY_UNIQUE_SCREENSHOTS_COUNT]
        self.release_date = date.fromtimestamp(app_info['release_date'])
        self.content_rating = app_info['content_rating']
        self._short_title = app_info.get('short_title')
        self.icon_context_colors = app_info.get('icon_colorwiz')
        self.publisher = ensure_unicode(app_info['publisher'])

    @property
    def short_title(self):
        return self._short_title or self.get_trimmed_title()

    trimmer_max_length = 20
    trimmer_patterns = [
        u'.*"(?P<title>.{0,%d})".*' % trimmer_max_length,
        # unicode angle-quotes
        u'.*\u00ab(?P<title>.{0,%d})\u00bb.*' % trimmer_max_length,
        # obvious delimiters which cannot be a part of a name
        u'(?P<title>.{0,%d})[,:!{[(]' % trimmer_max_length,
        # dash can be a part of a name if there are no spaces around
        ur'(?P<title>.{0,%d})(\s[-\u2014\u2013]|[-\u2014\u2013]\s)' % trimmer_max_length,
        ur'(?P<title>.{0,%d}\w)\\b' % (trimmer_max_length - 1),
    ]
    trimmer_regexps = [re.compile(pattern, re.UNICODE) for pattern in trimmer_patterns]

    def get_trimmed_title(self):
        for trimmer_regexp in self.trimmer_regexps:
            match = trimmer_regexp.match(string=self.title)
            if match:
                return match.groupdict()['title'].strip()
        return self.title

    def __hash__(self):
        return hash(self.package_name)

    def __str__(self):
        return self.package_name

    def __repr__(self):
        return self.package_name

    def __eq__(self, other):
        return self.package_name == other.package_name


class Placeholder(App):
    serializer = PlaceholderSetializer
    use_external_ads = True
