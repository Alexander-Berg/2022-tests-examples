from collections import defaultdict

import logging
from django.conf import settings
from mongoengine import Document, StringField, ListField, DictField

from yaphone.advisor.common.updates_manager import UpdatesManager

logger = logging.getLogger(__name__)


class YandexCategory(Document):
    name = StringField(required=True, db_field='yandex_name')
    google_names = ListField(field=StringField(), required=False, db_field="google_name")
    title = DictField(required=True, db_field='yandex_title')

    meta = {
        'strict': False,
        'collection': 'categories',
        'index_background': True,
        'auto_create_index': False,
    }

    @classmethod
    def find_by_google_name(cls, google_name):
        return cls.objects(google_name=google_name)

    @classmethod
    def find_by_google_names(cls, google_names):
        return cls.objects(google_name__in=google_names)

    @classmethod
    def get_by_name(cls, yandex_name):
        return cls.objects(yandex_name=yandex_name.upper()).first()

    def get_title(self, language):
        return self.title.get(language) or self.title.get('en')


class YaCategoryCache(object):
    def __init__(self):
        self.updates_manager = UpdatesManager(self.init, lifetime=settings.CACHES_LIFETIME['categories'])

    @property
    def names(self):
        return self._by_names.keys()

    def init(self):
        logger.info('Updating YaCategoryCache')

        categories = list(YandexCategory.objects)
        self._by_names = {category.name: category for category in categories}

        self._by_google_names = defaultdict(set)
        for category in categories:
            for google_name in category.google_names:
                self._by_google_names[google_name].add(category)

    def get_by_name(self, name):
        self.updates_manager.maybe_reinit()
        return self._by_names.get(name)

    def get_by_google_name(self, name):
        self.updates_manager.maybe_reinit()
        return self._by_google_names.get(name)


ya_category_cache = YaCategoryCache()
