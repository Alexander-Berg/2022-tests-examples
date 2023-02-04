import ujson as json

import logging
from calendar import timegm
from operator import itemgetter, eq
from yaphone import localization

logger = logging.getLogger(__name__)


class LocalizationItem(object):
    _value = None
    _internal_dict_fields = ('value', 'report_to_appmetrika', 'report_to_appmetrika_environment')
    report_to_appmetrika = True
    report_to_appmetrika_environment = False

    extractor = itemgetter(*_internal_dict_fields)

    def __init__(self, localization_item):
        self.localization_item = localization_item
        self._parse_raw_value(self.localization_item.value)

    @property
    def name(self):
        return self.localization_item.name

    @property
    def value(self):
        return self._value

    @property
    def serialized_value(self):
        if isinstance(self._value, basestring):
            return self._value
        return json.dumps(self._value)

    @property
    def expire_time(self):
        expire = self.localization_item.expire_time
        return int(timegm(expire.timetuple()))

    def _check_format(self, obj):
        return all(
            item in obj for item in self._internal_dict_fields
        )

    def _parse_raw_value(self, value):
        if value is None:
            self._value = None
            return

        try:
            value = json.loads(value)
            if isinstance(value, dict) and self._check_format(value):
                self._value, self.report_to_appmetrika, self.report_to_appmetrika_environment = self.extractor(value)
            else:
                self._value = value
        except ValueError:
            self._value = value
        except:
            raise


class UserSpecificConfig(localization.UserSpecificConfig):
    def get_item(self, param_name):
        return LocalizationItem(
            super(UserSpecificConfig, self).get_item(param_name)
        )

    def get_all_enabled_items(self):
        items = super(UserSpecificConfig, self).get_all_enabled_items()
        return [LocalizationItem(item) for item in items]

    def get_value(self, param_name, default_value=None, log_missing=True):
        try:
            return self.get_item(param_name).value
        except localization.ItemNotFound as e:
            if log_missing:
                logger.warning(e, exc_info=True)

        return default_value


class UserContextSpecificConfig(UserSpecificConfig):
    """ Localization config that uses context parameters for targeting """

    def __init__(self, user, context, project):
        """
        :param user: localization.LocalizationUser instance with information about user
        :param context: RecommendationContext instance with information about recommendation place
        :param project: localization project name
        """
        self.context = context
        super(UserContextSpecificConfig, self).__init__(user, project)

    def get_user_info(self):
        user_info = super(UserContextSpecificConfig, self).get_user_info()
        if self.context.placement_id:
            user_info.setExtendedParam('placement_id', str(self.context.placement_id), eq)
        return user_info


def get_config_item(client, key):
    return client.user_specific_config.get_item(key)


def get_config_value(client, key, default_value=None, log_missing=True):
    return client.user_specific_config.get_value(key, default_value, log_missing)


def translate(client, key):
    return client.translator.translate(key)


def get_impersonal_config_value(key, default_value=None, log_missing=True, project='launcher'):
    return UserSpecificConfig(localization.LocalizationUser(), project).get_value(
        key,
        default_value,
        log_missing
    )
