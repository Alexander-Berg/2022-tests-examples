import re
import json
import logging
from calendar import timegm
from distutils.version import LooseVersion
from traceback import format_stack

import six
import pymongo

from .user_info import UserInfo
from .items_cache import get_cache, start_update_thread, init_caches, ItemNotFound, LocalizationError


logger = logging.getLogger(__name__)

DEVICE_PHONE = u'phone'
DEVICE_TABLET = u'tablet'


def init_cache(db_name, mongo_uri, projects):
    try:
        client = pymongo.MongoClient(mongo_uri)
        client.server_info()
        db = client.get_database(db_name)
    except Exception:
        logger.error(u'Could not establish connection to mongodb or database %s not found', db_name)
        raise
    logger.info(u'Initializing cache for projects %s', projects)
    init_caches(projects, mongo_db=db)
    logger.info(u'Cache initialized')
    start_update_thread(mongo_db=db)
    logger.info(u'Localization update thread started')


class LocalizationItem(object):
    def __init__(self, project, user_info, name, value=None):
        self.project = project
        self.user_info = user_info
        self.name = str(name)
        self._value = value

    @property
    def value(self):
        if self._value is None:
            try:
                value = get_cache(self.project).get_item_value(self.name, self.user_info)
                self._value = value.decode('utf-8') if isinstance(value, six.binary_type) else value
            except RuntimeError:
                raise ItemNotFound('Item %r not found.' % self.name)

        return self._value

    def to_json(self):
        try:
            return json.loads(self.value)
        except ValueError:
            logger.warning('Cannot parse json value for "%s". Trying to fix it.' % self.name)
            try:
                value = try_to_fix_json(self.value)
                return json.loads(value)
            except ValueError as e:
                raise LocalizationError('Cannot parse json value for "%s": %s' % (self.name, e))

    @property
    def expire_time(self):
        return get_cache(self.project).getExpirationDate(self.name, self.user_info)


@six.python_2_unicode_compatible
class LocalizationUser(object):
    def __init__(self, uuid=None, language=None, app_name=None, app_version=None, created_at=None, clids=None,
                 country=None, region_ids=None, region_ids_init=None, loyalty=None, device_type=DEVICE_PHONE,
                 device_model=None, device_manufacturer=None, os_name=None, os_version=None,
                 serial_number=None, yphone_id=None, yphone_batch=None, build_version=None, mac_address_wifi=None,
                 mac_address_ethernet=None):
        """
        :param uuid: user's UUID
        :param language: ISO 639-1 2-letter language code
        :param app_name: application name
        :param app_version: application version
        :param created_at: user creation time (datetime object)
        :param clids: dict of user's clids {clid number: integer clid}
        :param country: user's ISO 3166 country code
        :param region_ids: user's current location geo code
        :param region_ids_init: user's geo code array
        :param loyalty: user's loyalty from crypta
        :param device_type: type of a device (e.g. phone, tablet). Default is phone.
        :param device_model: mobile device model
        :param device_manufacturer: mobile device manufacturer
        :param os_name: user's operating system name
        :param os_version: user's operating system version
        :param serial_number: device serial number
        :param yphone_id: Yandex Phone ID
        :param build_version: build version from fingerprint
        :param mac_address_wifi: mac address wifi
        :param mac_address_ethernet: mac address ethernet
        """
        self.uuid = uuid
        self.language = language
        self.app_name = app_name
        self.app_version = app_version
        self.created_at = created_at
        self.clids = clids
        self.country = country
        self.region_ids = region_ids
        self.region_ids_init = region_ids_init
        self.loyalty = loyalty
        self.device_type = device_type
        self.device_model = device_model
        self.device_manufacturer = device_manufacturer
        self.os_name = os_name
        self.os_version = os_version
        self.serial_number = serial_number
        self.yphone_id = yphone_id
        self.yphone_batch = yphone_batch
        self.build_version = build_version
        self.mac_address_wifi = mac_address_wifi
        self.mac_address_ethernet = mac_address_ethernet

    def __str__(self):
        fields = filter(lambda p: p[1] is not None, six.iteritems(self.__dict__))
        return u'LocalizationUser(%s)' % u', '.join((u'%s="%s"' % (key, value) for key, value in fields))

    def _to_user_info(self):
        user_info = UserInfo(
            uuid=self.uuid,
            language=self.language, country=self.country,
            region_ids=self.region_ids, region_ids_init=self.region_ids_init,
            clids=self.clids,
            device_type=self.device_type, device_model=self.device_model, device_vendor=self.device_manufacturer,
            app_name=self.app_name, app_version=self.app_version, build_version=self.build_version,
            os_version=self.os_version
        )
        if self.created_at:
            user_created_at_string = six.text_type(int(timegm(self.created_at.timetuple())))
            user_info.setExtendedParam(
                'user_creation_date', user_created_at_string,
                lambda user_value, config_value: int(user_value) > int(config_value)
            )
            user_info.setExtendedParam(
                'user_creation_date_to', user_created_at_string,
                lambda user_value, config_value: int(user_value) < int(config_value)
            )

        if self.loyalty:
            user_info.setExtendedParam(
                'crypta_loyalty', six.text_type(self.loyalty),
                lambda user_value, config_value: float(user_value) > float(config_value)
            )

        if self.os_name is not None:
            user_info.setExtendedParam(
                'os_name', self.os_name,
                lambda user_value, config_value: user_value.lower() == config_value.lower()
            )

        if self.os_version is not None:
            user_info.setExtendedParam(
                'min_os_version', self.os_version,
                lambda user_value, config_value: LooseVersion(user_value) >= LooseVersion(config_value)
            )
            user_info.setExtendedParam(
                'max_os_version', self.os_version,
                lambda user_value, config_value: LooseVersion(user_value) <= LooseVersion(config_value)
            )

        if self.serial_number is not None:
            user_info.setExtendedParam(
                'serial_number', self.serial_number,
                in_serialized_list
            )

        if self.yphone_id is not None:
            user_info.setExtendedParam(
                'yphone_id', self.yphone_id,
                in_serialized_list
            )

        if self.yphone_batch is not None:
            user_info.setExtendedParam(
                'yphone_batch', self.yphone_batch,
                in_serialized_list
            )
        if self.mac_address_wifi is not None:
            user_info.setExtendedParam(
                'mac_address_wifi', self.mac_address_wifi,
                in_serialized_list
            )
        if self.mac_address_ethernet is not None:
            user_info.setExtendedParam(
                'mac_address_ethernet', self.mac_address_ethernet,
                in_serialized_list
            )

        return user_info


def in_serialized_list(user_value, config_value):
    return any(user_value.lower() == value.lower() for value in config_value.split('|'))


class UserSpecificConfig(object):
    def __init__(self, user, project):
        self.user = user
        self.project = project

    def get_value(self, param_name, default_value=None, log_missing=True):
        """
        Returns LocalizationItem value "as is".
        """
        item = self.get_item(param_name)
        try:
            return item.value
        except ItemNotFound as e:
            if log_missing:
                self._warn_item_not_found(e)
        return default_value

    def get_object(self, param_name, default_value=None, log_missing=True):
        """
        Returns LocalizationItem value as deserialized json object.
        """
        item = self.get_item(param_name)
        try:
            return item.to_json()
        except ItemNotFound as e:
            if log_missing:
                self._warn_item_not_found(e)
        return default_value

    def _warn_item_not_found(self, exception):
        stack = format_stack(limit=5)[:-2]
        text = u'{message}. {user}.\ncalls stack (most recent call last):\n{stack}'
        logger.warning(text.format(message=exception, stack=''.join(stack), user=self.user), extra={'sample_rate': 0.1})

    def get_item(self, param_name):
        return LocalizationItem(
            self.project,
            self.get_user_info(),
            param_name
        )

    def get_user_info(self):
        return self.user._to_user_info()

    def get_all_enabled_items(self):
        user_info = self.get_user_info()

        all_enabled_items = []
        for name in get_cache(self.project).get_all_enabled_items(user_info):
            item = LocalizationItem(self.project, user_info, name)
            if item.value is not None:
                all_enabled_items.append(item)

        return all_enabled_items

    def get_all_enabled_items_with_values(self):
        user_info = self.get_user_info()

        all_enabled_items_values = []
        for name, value in get_cache(self.project).get_all_enabled_items_with_values(user_info):
            if value is not None:
                item = LocalizationItem(self.project, user_info, name, value=value)
                all_enabled_items_values.append(item)
        return all_enabled_items_values

    def list_all_item_names_with_values(self):
        user_info = self.get_user_info()

        all_items = []
        for name in sorted(get_cache(self.project).get_all_items()):
            all_items.append((name,  get_cache(self.project).maybe_get_item_value(name, user_info)))

        return all_items


class TranslationWrapper(six.text_type):

    def __new__(cls, s, language=None):
        # we need __new__ method of six.text_type to ignore language parameter
        return super(TranslationWrapper, cls).__new__(cls, s)

    def __init__(self, s, language=None):
        self.language = language


class Translator(UserSpecificConfig):

    def __init__(self, user, project, default_locale='en_US'):
        super(Translator, self).__init__(user, project)
        self.default_language, self.default_country = default_locale.split('_')

    def _swap_locale(self, language, country):
        old_values = self.user.language, self.user.country
        self.user.language, self.user.country = language, country
        return old_values

    def translate(self, key):
        result = self.get_value(key, default_value=None, log_missing=False)
        if result is not None:
            result = TranslationWrapper(result, language=self.user.language)
            return result

        old_values = self._swap_locale(self.default_language, self.default_country)
        result = self.get_value(key, log_missing=False)
        if result is not None:
            result = TranslationWrapper(result, language=self.user.language)
        self._swap_locale(*old_values)
        return result


def try_to_fix_json(value):
    value = re.sub("'", '"', value)
    value = re.sub(r',\s*}', '}', value)
    value = re.sub(r',\s*]', ']', value)
    return value
