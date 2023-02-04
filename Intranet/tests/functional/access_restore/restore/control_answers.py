# coding: utf-8

outer_admin = 'i_am_admin'
outer_deputy = 'i_am_deputy'

inner_admin = 'admin'
inner_admin_uid = 1130000077777888

valid_users = ['user1', 'user2', 'user3']
valid_user_aliases = ['user_1', 'user_2', 'user_3']
valid_users_different_case = ['USer1', 'useR2', 'USER3']
valid_maillists = ['buh', 'sales', 'support']
all_maillist = ['all']

# на список подключенных сервисов и платность пока не смотрим
# подробнее алгоритм здесь https://wiki.yandex-team.ru/ws/Vosstanovlenie-dostupa/


all_cases = {
    'all_empty': {
        'admins': [],
        'maillists': [],
        'users': [],
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_empty_emails': {
        'admins': [],
        'maillists': [],
        'users': [],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_empty_users_no_maillists': {
        'admins': [],
        'maillists': [],
        'users': [],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_no_users_empty_maillists': {
        'admins': [],
        'maillists': [],
        'users': [],
        'forgot_admins': True,
        'no_users': True,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_no_users_valid_maillists': {
        'admins': [],
        'maillists': valid_maillists,
        'users': [],
        'forgot_admins': True,
        'no_users': True,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'has_admin_no_users_no_maillists': {
        'admins': [outer_admin],
        'maillists': [],
        'users': [],
        'forgot_admins': False,
        'no_users': True,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'has_admins_empty_users_empty_maillists': {
        'admins': [outer_admin],
        'maillists': [],
        'users': [],
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'empty_admins_valid_users_no_maillists': {
        'admins': [],
        'maillists': [],
        'users': valid_users,
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_valid_users_no_maillists': {
        'admins': [],
        'maillists': [],
        'users': valid_users[:2],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_valid_users_empty_maillists': {
        'admins': [],
        'maillists': [],
        'users': valid_users[:1],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_valid_users_all_maillist': {
        'admins': [],
        'maillists': all_maillist,
        'users': valid_users[:2],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_has_inner_admin_as_user_no_maillists': {
        'admins': [],
        'maillists': [],
        'users': [inner_admin],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'has_inner_admin_as_admin_has_inner_admin_as_user_no_maillists': {
        'admins': [inner_admin],
        'maillists': [],
        'users': [inner_admin],
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'has_inner_admin_valid_users_with_inner_admin_no_maillists': {
        'admins': [inner_admin],
        'maillists': [],
        'users': [inner_admin] + valid_users,
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'has_admin_valid_users_no_maillists': {
        'admins': [inner_admin],
        'maillists': [],
        'users': valid_users,
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'has_admin_valid_users_all_maillist': {
        'admins': [inner_admin],
        'maillists': all_maillist,
        'users': valid_users,
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'has_admin_valid_users_valid_maillists': {
        'admins': [inner_admin],
        'maillists': valid_maillists[:2],
        'users': valid_users_different_case[:2],
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'has_admin_valid_users_valid_maillists_with_all': {
        'admins': [inner_admin],
        'maillists': valid_maillists[:2] + all_maillist,
        'users': valid_users_different_case[:2],
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'has_admin_valid_users_with_incorrect_no_maillists': {
        'admins': [inner_admin],
        'maillists': [],
        'users': valid_users_different_case + ['some_other_user'],
        'forgot_admins': False,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admins_one_valid_user_no_maillists': {
        'admins': [],
        'maillists': [],
        'users': valid_users[:1],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': True,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admin_valid_users_valid_maillists': {
        'admins': [],
        'maillists': valid_maillists[:2],
        'users': valid_users_different_case[:2],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },

    'forgot_admin_valid_aliases_valid_maillists': {
        'admins': [],
        'maillists': valid_maillists[:2],
        'users': valid_user_aliases + [valid_users[2]],
        'forgot_admins': True,
        'no_users': False,
        'no_maillists': False,
        'enabled_services': [],
        'paid': False,
    },
}
