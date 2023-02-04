# coding: utf-8


import yenv
from django.contrib.admin import site

if yenv.type != 'production':
    # список разрешенных email'ов нужен везде, кроме продакшна
    from idm.testenv.models import AllowedEmail, AllowedPhone
    from idm.framework.base_admin import DefaultIdmModelAdmin
    site.register(
        AllowedEmail,
        DefaultIdmModelAdmin,
        list_display=(
            'email',
            'comment',
            'added_at',
        ),
        search_fields=('email', 'comment'),
    )
    site.register(
        AllowedPhone,
        DefaultIdmModelAdmin,
        list_display=(
            'mobile_phone',
            'comment',
            'added_at',
        ),
        search_fields=('mobile_phone', 'comment'),
    )
