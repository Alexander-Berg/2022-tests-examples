# -*- coding: utf-8 -*-

from datetime import datetime
from dateutil.relativedelta import relativedelta

from tests.base import BalanceTest
from balance import mapper
from balance.completions_fetcher.configurable_partner_completion import create_and_enqueue_resources

NUM_DAYS = 3
NUM_DAYS_WITHOT_TODAY = NUM_DAYS - 1
TODAY = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
START_DT = TODAY - relativedelta(days=NUM_DAYS_WITHOT_TODAY)
END_DT = TODAY

SQL_COUNT = ''' select count(*) as num
                from bo.t_export e
                join bo.t_partner_completions_resource pcr
                  on (e.object_id = pcr.id)
                where
                    pcr.source_name = :sname and
                    e.type = :queue and
                    e.state = 0
            '''

SQL_UPDATE = ''' update bo.t_export
                 set state = 1
                 where type = :queue
                   and object_id in (select id from bo.t_partner_completions_resource
                                     where source_name = :sname)
             '''


class TestEnqueuer(BalanceTest):

    # Tests

    """ Проверяем постановку за период по старой и новой схеме """
    def test_configurable_scheme(self):
        queue = 'PARTNER_COMPL'
        sourcename = 'api_market'
        self._check_enqueuer(queue, sourcename, create_and_enqueue_resources)

    def test_new_scheme(self):
        queue = 'ENTITY_COMPL'
        sourcename = 'video_distr'
        self._check_enqueuer(queue, sourcename, create_and_enqueue_resources)

    """ Проверяем, что если для источника на дату ресурс уже существует, то используется существующий """

    def test_existing_resource(self):
        sourcename = 'video_distr'
        pcr_query = (self.session.query(mapper.PartnerCompletionsResource)
                     .filter_by(dt=END_DT, source_name=sourcename))

        export_query = (self.session.query(mapper.Export)
                        .filter_by(type='ENTITY_COMPL')
                        .join(mapper.PartnerCompletionsResource,
                              mapper.Export.object_id == mapper.PartnerCompletionsResource.id)
                        .filter_by(dt=END_DT, source_name=sourcename))

        self.assertEqual(pcr_query.exists(), False, "Ресурс не должен существовать")
        self.assertEqual(export_query.exists(), False, "Задача не должна быть проставлена")

        # Создаём ресурс вручную
        resource = mapper.PartnerCompletionsResource(dt=END_DT, source_name=sourcename)
        self.session.add(resource)
        self.session.flush()

        # И запоминаем его айдишник
        pcr_id = pcr_query.one().id

        # Запускаем постановщик
        create_and_enqueue_resources(self.session, END_DT, END_DT, sourcename)

        exports = export_query.all()
        self.assertEqual(len(exports), 1, "Задача проставлена")
        self.assertEqual(exports[0].object_id, pcr_id, "Задача проставлена с правильным ресурсом")

    # Supporting methods

    def _enqueued_count(self, queue, sourcename):
        return self.session.execute(
            SQL_COUNT, {'sname': sourcename, 'queue': queue}
        ).fetchone()['num']

    def _check_count(self, queue, sourcename, expected):
        self.assertEqual(self._enqueued_count(queue, sourcename), expected)

    def _update_state(self, queue, sourcename):
        """ Проставляю state=1, чтобы потом проверить, что простановщик обновит его в 0 """
        self.session.execute(SQL_UPDATE, {'sname': sourcename, 'queue': queue})

    def _check_enqueuer(self, queue, sourcename, enqueue_):
        self._check_count(queue, sourcename, expected=0)
        """ Здесь должны создаться ресурсы и проставиться новые записи в t_export """
        enqueue_(self.session, START_DT, END_DT, sourcename)
        self._check_count(queue, sourcename, expected=NUM_DAYS)
        """ Меняем записи в t_export, чтобы проверить, что они обновятся """
        self._update_state(queue, sourcename)
        self._check_count(queue, sourcename, expected=0)
        """ Проверяем, что постановщик обновляет уже существующие записи """
        enqueue_(self.session, START_DT, END_DT, sourcename)
        self._check_count(queue, sourcename, expected=NUM_DAYS)


if __name__ == '__main__':
    import unittest
    unittest.main()
