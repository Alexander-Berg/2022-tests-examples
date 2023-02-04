# -*- coding: utf-8 -*-
import json
from datetime import datetime


# noinspection PyPep8Naming
class UserInfo(object):
    def __init__(self, language=None, country=None, clids=None, **kwargs):
        self.language = language
        self.country = country
        self.clids = clids or {}
        self.kwargs = kwargs

    def setClid(self, clid_number, clid_value):
        self.clids[clid_number] = clid_value

    def setLocale(self, *args):
        if len(args) == 2:
            self.language, self.country = args
        elif len(args) == 1:
            self.language, self.country = args[0].split("_")

    def __getattr__(self, item):
        return lambda *args, **kwargs: None

    def __hash__(self):
        return 0

    def __eq__(self, other):
        for attr in ('language', 'country'):
            if getattr(self, attr) is not None and getattr(other, attr) != getattr(self, attr):
                return False
        for clid_id in self.clids:
            if self.clids[clid_id] != other.clids.get(clid_id):
                return False
        return True


localization_values = {
    'launcher_translations': {
        'backend_editors_choice_col10': {
            UserInfo(language='ru'): 'Свежие новости',
            UserInfo(language='en'): 'News media',
        }
    },
    'demo': {
        # provisioning
        'provisioning.device_owner_url': {UserInfo(): 'https://some.uri'},
        'provisioning.device_owner_hash': {UserInfo(): 'some hash'},
        # device_owner
        'device_owner.video_url': {UserInfo(): 'https://some.uri'},
        'device_owner.configure_apps': {UserInfo(): json.dumps(
            [
                {
                    'package_name': 'ru.beru.android',
                    'enable': True,
                },
                {
                    'package_name': 'ru.yandex.disk',
                    'enable': False,
                },
            ])
        },
        'device_owner.organization_name': {UserInfo(): 'Этим устройством управляет компания Яндекс'},
        'device_owner.demo_user_name': {UserInfo(): 'Яндекс Демо'},
        'device_owner.disable_factory_reset': {UserInfo(): 'True'},
        'device_owner.preferred_launcher': {UserInfo(): 'com.yandex.launcher'},
    },
    'launcher': {
        'test_id': {
            UserInfo(): 'test_id_value'
        }
    },
    'recommendation_widget': {
        'config_name': {
            UserInfo(): 'widget_1'
        }
    },
    'mobile_browser': {},
    'advisor_retail': {},
    'app_installer': {
        'setup_wizard_experiment': {
            UserInfo(): 'setup_wizard'
        }
    }
}


# noinspection PyPep8Naming,PyUnusedLocal,PyMethodMayBeStatic
class ItemsCache(object):
    def __init__(self, project, silent):
        self.project = project

    def get_all_enabled_items(self, user_info):
        return [key for key in localization_values[self.project]
                if user_info in localization_values[self.project][key]]

    def get_item_value(self, name, user_info):
        try:
            return localization_values[self.project][name][user_info]
        except KeyError:
            raise RuntimeError

    def getExpirationDate(self, name, user):
        return datetime.utcnow()

    def is_ready(self):
        return True


# noinspection PyUnusedLocal,PyPep8Naming
def get_cache(project):
    return ItemsCache(project, silent=False)


def mock_localization():
    from yaphone import localization

    localization.UserInfo = UserInfo
    localization.items_cache.ItemsCache = ItemsCache
    localization.items_cache.get_cache = get_cache
    localization.get_cache = get_cache
