# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_tokens_table import PartnerTokensTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestPartnerTokensTable(BaseTableTest):

    _table = 'test-confi-partner-tokens-table'
    _TableClass = PartnerTokensTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        partner_id, token = 'test_partner_id_1', 'test_token'
        input_record = PartnerTokensTable.Record(partner_id, token)
        table.insert([input_record])
        result_record = table.get_one(token)
        assert_that(input_record, equal_to(result_record))

        partner_id = 'test_partner_id_2'
        input_record = PartnerTokensTable.Record(partner_id, token)
        table.insert([input_record])
        result_record = table.get_one(token)
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
