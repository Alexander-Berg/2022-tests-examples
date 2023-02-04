# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('WebAuthConfig', 'webauth.lua',
    kwargs={
        'auth_status': None,
        'csrf_state': None,
        'csrf_token': None,
        'app_id': None,
        'redir_url': None,
        'set_cookie': None,
        'role': 'some_role',
        'path': '/check-oauth',
        'allow_options_passthrough': False,
        'header_name_redirect_bypass': None,
    },
    backends=['backend'],
    logs=['access_log'],
)


gen_config_class('WebAuthUncompletedConfig', 'webauth_uncompleted.lua',
    kwargs={
        'auth_status': None,
        'csrf_state': None,
        'csrf_token': None,
        'app_id': None,
        'redir_url': None,
        'set_cookie': None,
    },
    backends=['backend'],
    logs=['access_log'],
)
