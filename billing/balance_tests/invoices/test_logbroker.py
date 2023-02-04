# coding: utf-8

import mock
import simplejson

from balance import constants
from balance.mapper.exportable_ng import ExportNg
from balance.processors.logbroker_proc import process_logbroker_personal_account
from balance.son_schema import invoices
from tests.base import BalanceTest
from tests.balance_tests.invoices.invoice_common import create_personal_account


class TestPersonalAccountLogBroker(BalanceTest):
    """Тестирование выгрузки PersonalAccount в логброкер"""

    @staticmethod
    def encoder(obj):
        return simplejson.dumps(
            {
                'obj': invoices.PersonalAccountSchema().dump(obj).data,
                'classname': 'PersonalAccount',
                'version': obj.version_id or 0,
            },
            ensure_ascii=False,
            use_decimal=True
        ).encode('UTF-8')

    def test_logbroker_on_personal_account_create(self):
        """При создании PersonalAccount должна появиться задача в export_ng и позже произойти выгрузка в логброкер"""

        # Создаем аккаунт
        account = create_personal_account(session=self.session)
        self.session.add(account)
        self.session.flush()

        # убеждаемся, что в ExportNg присутствует задача на выгрузку
        export_ng_object = self._find_export_ng_task(account)
        assert export_ng_object is not None

        # проверяем выгрузку в логброкер в заданный топик
        with mock.patch('balance.processors.logbroker_proc._write_batch') as _write_mock:
            expected_invoice = self.encoder(account)
            process_logbroker_personal_account([export_ng_object])
            _write_mock.assert_called_once_with('lbkx', 'personal-account', [expected_invoice])

    def test_logbroker_on_fictive_personal_account_create(self):
        """FictivePersonalAccount не должен выгружаться в export_ng"""

        # Создаем fictive аккаунт
        account = create_personal_account(session=self.session, personal_account_fictive=True)
        self.session.add(account)
        self.session.flush()

        # убеждаемся, что в ExportNg отсутствует задача на выгрузку
        export_ng_object = self._find_export_ng_task(account)
        assert export_ng_object is None

    def _find_export_ng_task(self, exporting_obj):
        """Находит в очереди ExportNg задачу выгрузки `exporting_obj`"""

        return self.session \
            .query(ExportNg) \
            .filter((ExportNg.type == 'LOGBROKER-PERSONAL-ACCOUNT')
                    & (ExportNg.object_id == exporting_obj.id)
                    & (ExportNg.state == constants.ExportState.enqueued)
                    & (ExportNg.in_progress == None)) \
            .one_or_none()
