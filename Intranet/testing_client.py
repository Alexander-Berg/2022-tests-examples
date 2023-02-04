# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.directory_logging.logger import log

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.utils import get_environment
from intranet.yandex_directory.src.yandex_directory.common.billing.client import BillingClient


class BillingClientWithUidReplacement(BillingClient):
    """
    Тестинг Биллинга использует продакшеновые UID-ы паспорта.
    Поэтому, при запросах к ним мы все uid-ы будем заменять на один продакшеновый :(

    uid берем из настройки BILLING_CLIENT_UID_FOR_TESTING
    """
    def __init__(self, *args, **kwargs):
        self.assert_in_testing_environment()
        super(BillingClientWithUidReplacement, self).__init__(*args, **kwargs)

    def assert_in_testing_environment(self):
        if not get_environment().startswith('testing'):
            raise RuntimeError('BillingClientWithUidReplacement must be used in testing environment only!')

    def _prepare_uid(self, uid):
        # если это тестинг и установлена переменная BILLING_CLIENT_UID_FOR_TESTING,
        # заменяем uid на неё
        replace_to_uid = app.config['BILLING_CLIENT_UID_FOR_TESTING']
        if replace_to_uid:
            with log.fields(original_uid=uid, replace_to_uid=replace_to_uid):
                log.info('BillingClientWithUidReplacement: Replacing uid by BILLING_CLIENT_UID_FOR_TESTING setting')
            return str(replace_to_uid)
        else:
            return super(BillingClientWithUidReplacement, self)._prepare_uid(uid=uid)
