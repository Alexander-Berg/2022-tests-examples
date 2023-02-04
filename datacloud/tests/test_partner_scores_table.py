# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_scores_table import PartnerScoresTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestPartnerScoresTable(BaseTableTest):

    _table = 'test-config-partner-scores-table'
    _TableClass = PartnerScoresTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record

        partner_id, partner_score_name = 'test-partner-id', 'test-partner-score-name'
        internal_score_name, is_active = 'test-internal-score-name', True
        input_record = Record(partner_id, partner_score_name, internal_score_name, is_active)
        table.insert([input_record])
        result_record = table.get_one(Record(partner_id, partner_score_name))
        assert_that(input_record, equal_to(result_record))

        is_active = False
        input_record = Record(partner_id, partner_score_name, internal_score_name, is_active)
        table.insert([input_record])
        result_record = table.get_one(Record(partner_id, partner_score_name))
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()

    # TODO: Add Get Multiple Test
    # TODO: Aad __le__ to record and enable test
    # def test_get_available_partner_scores(self):
    #     table = self._TableClass(self._ydb_manager, self._database, self._table)
    #     Record = self._TableClass.Record

    #     partner_id = 'test-partner-id-multiple'
    #     record1 = Record(partner_id, 'score-1', 'inner-score-1', True)
    #     record2 = Record(partner_id, 'score-2', 'inner-score-2', True)
    #     record3 = Record(partner_id, 'score-3', 'inner-score-3', False)
    #     table.insert([record1, record2, record3])

    #     expected = [record1, record2, record3]
    #     result = sorted(list(table.get_available_partner_scores(partner_id)))
    #     self.assertEqual(expected, result)

    #     record2.internal_score_name = 'inner-score-4'
    #     record2.is_active = False
    #     record3.is_active = True
    #     table.insert([record2, record3])

    #     expected = [record1, record2, record3]
    #     result = sorted(list(table.get_available_partner_scores(partner_id)))
    #     self.assertEqual(expected, result)
