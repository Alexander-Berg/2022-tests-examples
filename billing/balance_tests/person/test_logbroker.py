# -*- coding: utf-8 -*-
import logging

import json
import mock

from balance import constants
from balance import mapper
from balance.mapper.exportable_ng import ExportNg
from balance.processors.logbroker_proc import process_logbroker_person
from butils import logger
from tests.base import BalanceTest
import tests.object_builder as ob

log = logger.get_logger()


class TestPersonLogBroker(BalanceTest):
    """Тестирование выгрузки Person в логброкер"""

    @staticmethod
    def encoder(obj):
        return json.dumps({
            'obj': obj,
            'classname': 'Person',
            'version': obj.version_id,
        }, ensure_ascii=False, cls=mapper.clients.PersonEncoder).encode("UTF-8")

    def _find_export_ng_task(self, exporting_obj):
        """Находит в очереди ExportNg задачу выгрузки `exporting_obj`"""

        return self.session \
            .query(ExportNg) \
            .filter((ExportNg.type == 'LOGBROKER-PERSON')
                    & (ExportNg.object_id == exporting_obj.id)
                    & (ExportNg.state == constants.ExportState.enqueued)
                    & (ExportNg.in_progress == None)) \
            .one_or_none()

    def test_logbroker_on_person_create(self):
        client = ob.ClientBuilder.construct(self.session)
        person = ob.PersonBuilder.construct(self.session, client=client, type='ur', kpp='123123123')
        self.session.add(person)
        self.session.flush()

        export_ng_object = self._find_export_ng_task(person)
        assert export_ng_object is not None

        # проверяем выгрузку в логброкер в заданный топик
        with mock.patch(
                'balance.processors.logbroker_proc._write_batch'
        ) as _write_mock:
            expected_person = self.encoder(person)
            process_logbroker_person([export_ng_object])
            _write_mock.assert_called_once_with(
                'lbkx',
                'person',
                [expected_person]
            )

    def test_logbroker_on_person_change(self):
        import random
        person = self.session.query(mapper.Person)\
            .filter(mapper.Person.id < random.randint(10000000, 20000000)).first()
        person.name = 'EXPORTTHIS!'
        self.session.flush()

        export_ng_object = self._find_export_ng_task(person)
        assert export_ng_object is not None

        # проверяем выгрузку в логброкер в заданный топик
        with mock.patch(
                'balance.processors.logbroker_proc._write_batch'
        ) as _write_mock:
            expected_person = self.encoder(person)
            process_logbroker_person([export_ng_object])
            _write_mock.assert_called_once_with(
                'lbkx',
                'person',
                [expected_person]
            )
