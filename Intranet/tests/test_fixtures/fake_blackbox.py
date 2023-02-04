# coding: utf-8

import blackbox

from at.common import utils

DATA = []


def mock_blackbox(mock_data):
    for login, uid in list(mock_data.items()):
        DATA.append(
            {'error': None,
             'fields': {'display_name': login,
                        'login': login,
                        'social': None,
                        'social_aliases': None},
             'uid': str(uid)})
    fake_bb = blackbox.Blackbox(blackbox.BLACKBOX_URL)
    fake_bb.userinfo = fake_userinfo
    utils.utils_blackbox = fake_bb


def fake_userinfo(uid_or_login, userip, dbfields=None, by_login=False, social_info=True, **kw):
    for element in DATA:
        if element['uid'] == str(uid_or_login) or element['fields']['login'] == uid_or_login:
            return element
    return {'error': None,
            'fields': {'display_name': None,
                       'login': None,
                       'social': None,
                       'social_aliases': None},
            'uid': None}
