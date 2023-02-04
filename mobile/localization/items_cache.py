import logging
import threading
import time

from .local_items import EnableConditions, ItemLocalization, ItemOptions

logger = logging.getLogger(__name__)

APPLICATION_ALIASES_KEYS = (u'ios_bundle', u'android_package_name', u'wp_app_id')
RELOAD_TIMEOUT_SECONDS = 600  # 10 minutes
MIN_UPDATE_TIMEOUT = 0.1
COLLECTION_NAME_FORMAT = u'localization.{cache_name}'
APPLICATION_CACHE_NAME = u'_Yandex_apps'
ON_ERROR_UPDATE_THREAD_RESTART_SECONDS = 0.5


class LocalizationError(RuntimeError):
    pass


class ItemNotFound(LocalizationError):
    pass


class BaseCache(object):
    def __init__(self, name, reload_timeout_seconds=RELOAD_TIMEOUT_SECONDS, items=None):
        self.reload_timeout_seconds = reload_timeout_seconds
        self.expire_time = None
        self.items = items
        self.collection_name = COLLECTION_NAME_FORMAT.format(cache_name=name)

    def is_expired(self):
        return self.expire_time is None or self.expire_time < time.time()

    def update_cache(self, *args, **kwargs):
        self._update_cache(*args, **kwargs)
        self.expire_time = time.time() + self.reload_timeout_seconds

    def _update_cache(self, *args, **kwargs):
        raise NotImplementedError


class ItemsCache(BaseCache):
    def is_ready(self):
        return self.items is not None

    def retrieve_items_if_ready(self):
        """
        Returns self.items. Other methods should use it to avoid accessing two different versions of self.items
        """
        if not self.is_ready():
            raise LocalizationError(u'Localizations not ready')
        return self.items

    def is_item_enabled(self, item_name, user_info, items=None):
        items = items or self.retrieve_items_if_ready()
        if item_name not in items:
            return False
        options, localizations = items[item_name]
        for localization in localizations:
            if user_info.matches_conditions(localization.conditions, options):
                return True
        return False

    def get_item_value(self, item_name, user_info, items=None):
        items = items or self.retrieve_items_if_ready()
        if item_name not in items:
            raise LocalizationError(u'No such item!')
        options, localizations = items[item_name]
        for localization in localizations:
            if user_info.matches_conditions(localization.conditions, options):
                return localization.value
        raise LocalizationError(u'Item is not enabled')

    def maybe_get_item_value(self, item_name, user_info, items=None):
        items = items or self.retrieve_items_if_ready()
        if item_name in items:
            options, localizations = items[item_name]
            for localization in localizations:
                if user_info.matches_conditions(localization.conditions, options):
                    return localization.value

    def get_all_items(self):
        items = self.retrieve_items_if_ready()
        return list(items)

    def get_all_enabled_items(self, user_info):
        result = []
        items = self.retrieve_items_if_ready()
        for name in items:
            if self.is_item_enabled(name, user_info, items=items):
                result.append(name)
        return result

    def get_all_enabled_items_with_values(self, user_info):
        result = []
        items = self.retrieve_items_if_ready()
        for name in items:
            value = self.maybe_get_item_value(name, user_info, items=items)
            if value is not None:
                result.append((name, value))
        return result

    def _update_cache(self, collection):
        items = {}
        for entry in collection.find():
            localizations = []
            for loc in entry.get(u'values', []):
                conditions = EnableConditions(**loc.get(u'conditions'))
                localizations.append(ItemLocalization(value=loc.get(u'value'), conditions=conditions))
            options = entry.get(u'options', {})
            items[entry[u'_id']] = ItemOptions(audience_salt=options.get(u'audience_salt')), localizations
        self.items = items


class ApplicationsCache(BaseCache):
    def _update_cache(self, collection):
        if self.items is None:
            self.items = {}
        for entry in collection.find():
            entry_id = entry['_id']
            entry_val = set()
            for key in APPLICATION_ALIASES_KEYS:
                if key in entry:
                    entry_val.add(entry[key].lower())
            self.items[entry_id] = entry_val


_update_thread = None
_applications_cache = ApplicationsCache(name=APPLICATION_CACHE_NAME)
_cache_instances = dict()


def load_cache(cache, mongo_db):
    collection = mongo_db.get_collection(cache.collection_name)
    cache.update_cache(collection=collection)
    return cache.expire_time


def init_caches(projects, mongo_db):
    load_cache(_applications_cache, mongo_db)
    for project in projects:
        cache = ItemsCache(name=project)
        load_cache(cache, mongo_db)
        _cache_instances[project] = cache


def _cache_updating_worker(mongo_db):
    while True:
        try:
            if _applications_cache.is_expired():
                load_cache(_applications_cache, mongo_db)
            sleep_until = _applications_cache.expire_time
            for cache in _cache_instances.values():
                if cache.is_expired():
                    load_cache(cache, mongo_db)
                sleep_until = min(sleep_until, cache.expire_time)
            time.sleep(max(sleep_until - time.time(), MIN_UPDATE_TIMEOUT))
        except Exception as e:
            logger.error(u'Error %s in cache updating worker. Waiting for %s seconds. Error: %s', e.__class__.__name__, ON_ERROR_UPDATE_THREAD_RESTART_SECONDS, e)
            time.sleep(ON_ERROR_UPDATE_THREAD_RESTART_SECONDS)


def start_update_thread(mongo_db):
    global _update_thread
    if _update_thread is None or not _update_thread.is_alive():
        _update_thread = threading.Thread(target=_cache_updating_worker, args=(mongo_db,))
        _update_thread.daemon = True
        _update_thread.start()


def get_cache(project):
    try:
        return _cache_instances[project]
    except KeyError:
        logger.error(u'Unknown project %s', project)
        raise LocalizationError(u'Trying to get unknown project {}'.format(project))
