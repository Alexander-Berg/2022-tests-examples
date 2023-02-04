import re

import logging
import mongoengine as me
from datetime import datetime
from time import time

from yaphone.advisor.advisor.models.crypta import CryptaInfo
from yaphone.advisor.advisor.models.lbs import LBSInfo
from yaphone.advisor.common.exceptions import NoDeviceInfoAPIError, BadRequestAPIError

ANDROID_ID_REGEXP = re.compile(r'^[\da-f]{1,16}$', re.IGNORECASE)

logger = logging.getLogger(__name__)


# noinspection PyClassHasNoInit
class Version(me.EmbeddedDocument):
    codename = me.StringField(required=True)
    incremental = me.StringField(required=True)
    release = me.StringField(required=True)
    sdk_int = me.IntField(min_value=1, required=True)


# noinspection PyClassHasNoInit
class OSBuild(me.EmbeddedDocument):
    string_fields = me.ListField(field=me.DictField(), required=True)
    version = me.EmbeddedDocumentField(document_type=Version, required=True)

    meta = {'strict': False}


# noinspection PyClassHasNoInit
class DisplayMetrics(me.EmbeddedDocument):
    height_pixels = me.IntField(required=True)
    width_pixels = me.IntField(required=True)
    density = me.FloatField(required=True)
    density_dpi = me.FloatField(required=True)
    xdpi = me.FloatField(required=True)
    ydpi = me.FloatField(required=True)
    scaled_density = me.FloatField(default=1.0)

    meta = {'strict': False}


class BaseLocale(object):
    class WrongFormat(BadRequestAPIError):
        default_detail = 'Bad locale format. Locale must fit POSIX standard.'

    # locale format [language[_territory][.codeset][@modifier]]
    # example: ru_RU.UTF-8, en_US, br, de_AT.UTF-8@something
    REGEXP = re.compile("(?P<language>[a-z]{2})(_(?P<territory>[A-Z]{2})"
                        r"(\.(?P<codeset>[^@]+)(@(?P<modifier>\w+)?)?)?)?")

    def __str__(self):
        res = self.language
        if self.territory:
            res = '{}_{}'.format(res, self.territory)
        if self.codeset:
            res = '{}.{}'.format(res, self.codeset)
        if self.modifier:
            res = '{}@{}'.format(res, self.modifier)
        return res

    def __init__(self, *args, **kwargs):
        # locale object can be initialized with string:  Locale("ru_RU")
        # noinspection PyTypeChecker
        if args and isinstance(args[0], basestring):
            match = BaseLocale.REGEXP.match(args[0])
            if match is None:
                raise BaseLocale.WrongFormat
            kwargs = match.groupdict()
            args = ()
        # noinspection PyArgumentList
        super(BaseLocale, self).__init__(*args, **kwargs)

    def __repr__(self):
        return "<Locale object: %s>" % str(self)

    def to_dict(self):
        res = {'lang': self.language}
        if self.territory:
            res['territory'] = self.territory
        if self.codeset:
            res['codeset'] = self.codeset
        if self.modifier:
            res['modifier'] = self.modifier
        return res


class Locale(BaseLocale, me.EmbeddedDocument):
    language = me.StringField(required=True)
    territory = me.StringField(required=False, default=None)
    codeset = me.StringField(required=False, default=None)
    modifier = me.StringField(required=False, default=None)

    def __init__(self, *args, **kwargs):
        super(Locale, self).__init__(*args, **kwargs)


# noinspection PyClassHasNoInit
class UserSettings(me.EmbeddedDocument):
    locale = me.EmbeddedDocumentField(document_type=Locale, required=True)

    meta = {'strict': False}


class Operator(me.EmbeddedDocument):
    carrier_name = me.StringField(required=False)
    display_name = me.StringField(required=False)
    country_iso = me.StringField(required=False, null=True, min_length=2, max_length=2)
    mcc = me.IntField(required=True, min_value=1, max_value=999)
    mnc = me.IntField(required=True, min_value=0, max_value=999)
    data_roaming = me.BooleanField(required=False)
    sim_slot_index = me.IntField(required=False)
    is_embedded = me.BooleanField(required=False)
    icc_id = me.StringField(required=False)

    meta = {'strict': False}

    def __str__(self):
        return '%s(mcc=%d;mnc=%d)' % (self.display_name, self.mcc, self.mnc)


# noinspection PyClassHasNoInit
class AndroidInfo(me.EmbeddedDocument):
    ad_id = me.UUIDField(required=False, null=True, default=None)
    android_id = me.StringField(required=False, regex=ANDROID_ID_REGEXP, default=None)
    display_metrics = me.EmbeddedDocumentField(document_type=DisplayMetrics, required=True)
    features = me.ListField(field=me.StringField(), required=True)
    shared_libraries = me.ListField(field=me.StringField(), required=True)
    os_build = me.EmbeddedDocumentField(document_type=OSBuild, required=True)
    user_settings = me.EmbeddedDocumentField(document_type=UserSettings, required=True)
    updated_at = me.DateTimeField(required=False, default=datetime.utcnow)

    gl_extensions = me.ListField(field=me.StringField(), required=False, default=tuple)
    gl_es_version = me.IntField(required=False)

    meta = {'strict': False}

    def clean(self):
        if self.android_id and not AndroidInfo.android_id.regex.match(self.android_id):
            del self.android_id


class BaseProfile(object):
    @property
    def all_apps(self):
        if not self.installed_apps_info:
            return []
        return [app['package_name'] for app in self.installed_apps_info]

    @property
    def user_apps(self):
        if not self.installed_apps_info:
            return []
        return [app['package_name'] for app in self.installed_apps_info
                if not app['is_system'] and not app['is_disabled']]

    @property
    def non_system_apps(self):
        if not self.installed_apps_info:
            return []
        return [app['package_name'] for app in self.installed_apps_info if not app['is_system']]

    @property
    def removed_apps(self):
        if not self.removed_apps_info:
            return []
        return [app['package_name'] for app in self.removed_apps_info]

    @property
    def country(self):
        try:
            # noinspection PyUnresolvedReferences
            return self.lbs_info.country_init
        except AttributeError:
            raise NoDeviceInfoAPIError("Country_init is not set. Please send lbs_info")

    @property
    def current_country(self):
        try:
            # noinspection PyUnresolvedReferences
            return self.lbs_info.country
        except AttributeError:
            raise NoDeviceInfoAPIError("Country is not set. Please send lbs_info")

    @property
    def region_ids(self):
        try:
            # noinspection PyUnresolvedReferences
            return self.lbs_info.region_ids
        except AttributeError:
            raise NoDeviceInfoAPIError("region_ids is not set. Please send lbs_info")

    @property
    def region_ids_init(self):
        try:
            # noinspection PyUnresolvedReferences
            return self.lbs_info.region_ids_init
        except AttributeError:
            raise NoDeviceInfoAPIError("region_ids_init is not set. Please send lbs_info")

    @property
    def locale(self):
        try:
            # noinspection PyUnresolvedReferences
            return self.android_info.user_settings.locale
        except AttributeError:
            raise NoDeviceInfoAPIError("Locale is not set. Please send android_client_info")

    def _string_fields_key(self, key):
        if not hasattr(self, 'android_info'):
            raise NoDeviceInfoAPIError("%s is not set. Please send android_client_info" % key.title())

        # noinspection PyUnresolvedReferences
        string_fields = {item['key']: item['value'] for item in self.android_info.os_build.string_fields}
        return string_fields.get(key)

    @property
    def device_vendor(self):
        return self._string_fields_key('MANUFACTURER')

    @property
    def device_model(self):
        return self._string_fields_key('MODEL')


class Profile(me.Document, BaseProfile):
    device_id = me.UUIDField(required=True, primary_key=True)
    lbs_info = me.EmbeddedDocumentField(LBSInfo, required=False, default=LBSInfo)
    android_info = me.EmbeddedDocumentField(AndroidInfo, required=False, default=AndroidInfo)
    operators = me.EmbeddedDocumentListField(document_type=Operator, required=False)
    phone_id = me.StringField(required=False)

    installed_apps_info = me.DynamicField(required=False, default=list)
    # installed apps field is huge. MongoEngine spend ages to parse it, so read it "as is"
    # installed apps structure example:
    # [
    #   {
    #    "package_name": "ru.yandex.mail", - package name
    #    "first_install_time": 123123, - unix timestamp of install
    #    "last_update_time": 123123, - unix timestamp of last package update
    #    "is_system": false,  - if app was pre-installed (system)
    #    "is_disabled": false - if app is system and was disabled by user
    #   }
    # ]

    removed_apps_info = me.DynamicField(required=False, default=list)
    # removed apps field may be huge too.
    # removed apps structure example:
    # [
    #   {
    #    "package_name": "ru.yandex.mail", - package name
    #    "removal_ts": 123123, - unix timestamp of last app uninstall time
    #   }
    # ]

    feedbacks = me.DynamicField(required=False, default=list)
    # feedbacks are not so small.
    # feedbacks structure example:
    # [
    #   {
    #    "package_name": "ru.yandex.mail", - package name
    #    "timestamp": 123123, - unix timestamp of last app uninstall time
    #    "action_type": "close",
    #    "reason": "dislike"
    #   }
    # ]

    created_at = me.DateTimeField(required=False, default=datetime.utcnow)
    packages_info_updated_at = me.DateTimeField(required=False, default=None)
    updated_at = me.DateTimeField(required=False, default=datetime.utcnow)
    crypta = me.EmbeddedDocumentField(CryptaInfo, required=False, default=CryptaInfo)
    passport_uid = me.LongField(required=False)

    meta = {
        'db_alias': 'primary_only',
        'strict': False,
        'auto_create_index': False,
        'index_background': True,
    }

    def add_feedback(self, feedback):
        feedback['timestamp'] = int(time())
        # NOTE: if we use .append here, me doesn't know that the field has changed.
        # NOTE x=x+1 is not the same as to x+=1 in case of me fields
        # noinspection PyAugmentAssignment
        self.feedbacks = self.feedbacks + [feedback]

    def update_packages_info(self, installed_apps):
        previously_installed = frozenset(self.user_apps)
        self.installed_apps_info = installed_apps
        currently_installed = frozenset(self.user_apps)

        removed_apps = previously_installed - currently_installed
        removal_ts = int(time())

        # workaround for self.removed_apps_info=None
        if self.removed_apps_info is None:
            self.removed_apps_info = []

        # exclude from removed apps list apps that was installed back
        # noinspection PyTypeChecker
        self.removed_apps_info = [app_info for app_info in self.removed_apps_info
                                  if app_info['package_name'] not in currently_installed]

        # add newly removed apps to removed_apps_info
        self.removed_apps_info = self.removed_apps_info + [{'package_name': package_name, 'removal_ts': removal_ts}
                                                           for package_name in removed_apps]
        self.packages_info_updated_at = datetime.utcnow()
        self.updated_at = datetime.utcnow()

    def save(self, *args, **kwargs):
        # touch created_at field in case it is not initialized (https://st.yandex-team.ru/ADVISOR-2020)
        self._mark_as_changed('created_at')
        return super(Profile, self).save(*args, **kwargs)
