# -*- coding: utf-8 -*-
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_scores_table import PartnerScoresTable
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_tokens_table import PartnerTokensTable
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.score_path_table import ScorePathTable
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.geo_path_table import GeoPathTable
from datacloud.score_api.storage.ydb.ydb_tables.score_tables.score_table import ScoreTable, MetaScoreTable
from datacloud.score_api.storage.ydb.ydb_tables.score_tables.crypta_table import CryptaTable
from datacloud.score_api.storage.ydb.ydb_tables.stability_tables.stability_table import StabilityTable
from datacloud.score_api.storage.ydb.ydb_tables.geo_tables.geo_logs_table import GeoLogsTable


def get_partner_scores_data():
    record = PartnerScoresTable.Record
    return [
        record('partner_a', 'score_a', 'partner_a-score_a', True),
        record('partner_a', 'score_b', 'partner_a-score_b', True),
        record('partner_a', 'score_c', 'partner_a-score_c', True),
        record('partner_b', 'score_a', 'partner_b-score_a', True),
        record('partner_b', 'score_b', 'partner_b-score_b', True),
        record('partner_b', 'score_c', 'partner_b-score_c', True),
        record('partner_c', 'score_a', 'partner_c-score_a', True),
        record('partner_c', 'score_b', 'partner_c-score_b', True),
        record('partner_c', 'score_c', 'partner_c-score_c', True),
    ]


def get_partner_tokens_data():
    record = PartnerTokensTable.Record
    return [
        record('partner_a', 'xieciethiefexodieheikipiumaingaizahyabuquiokeezooreikieghangekoh'),
        record('partner_b', 'naepeerohcahkeediebouchuangokohyeetohkeighoongoequiyierefohxeina'),
        record('partner_c', 'lolohlaephacesohngaachaeseelaghepuquohweejuudoakeigeejahgeiphofo')
    ]


def get_score_path_data():
    record = ScorePathTable.Record
    return [
        record('partner_a-score_a', 'partner_a/score_a/date-23-10-2018'),
        record('partner_a-score_b', 'partner_a/score_b/date-23-10-2018'),
        record('partner_a-score_c', 'partner_a/score_c/date-23-10-2018'),
        record('partner_b-score_a', 'partner_b/score_a/date-23-10-2018'),
        record('partner_b-score_b', 'partner_b/score_b/date-23-10-2018'),
        record('partner_b-score_c', 'partner_b/score_c/date-23-10-2018'),
        record('partner_c-score_a', 'partner_c/score_a/date-23-10-2018'),
        record('partner_c-score_b', 'partner_c/score_b/date-23-10-2018'),
        record('partner_c-score_c', 'partner_c/score_c/date-23-10-2018'),
    ]


def get_crypta_data():
    record = CryptaTable.Record
    return [
        record(12345, 54321),
        record(67891, 19876)
    ]


def get_score_data():
    record = ScoreTable.Record
    return [
        record(54321, 0.8),
        record(12345, 0.2341)
    ]


def get_meta_score_data():
    record = MetaScoreTable.Record
    return [
        record(12345, 0.8),
        record(67891, 0.2341)
    ]


def get_stability_data():
    record = StabilityTable.Record
    return [
        record('2018-10-23', 'partner_a', 'score_a', 'daily', '[[1,2,3],[4,5,6]]'),
        record('2018-10-23', 'partner_b', 'score_b', 'daily', '[[6,5,4],[3,2,1]]'),
    ]


def get_geo_path_data():
    record = GeoPathTable.Record
    return [
        record('test-internal-score-name', 'test-crypta-table', 'test-geo-logs-table', '1. 1. 1. 1. 1. 0. 0. 0. 0. 0. -1. -1. -1. -1. -1. 2. 3. 4.'),
    ]


def get_geo_logs_data():
    record = GeoLogsTable.Record
    return [
        record(1, 'ucftd3q09x ucfv1nkr3n'),
        record(2, 'v6554r9j9y v6554pzzuh v6554pq40d'),
        record(3, 'ubkhsznuqm'),
        record(4, 'ucfs8mkvw2 ucfs8mm7z8'),
        record(5, 'v64txqte7z v655kqgsm1 v655h3jfd0 v655h8uv6y'),
    ]


def get_geo_crypta_data():
    record = CryptaTable.Record
    return [
        record(1001, 1),
        record(1002, 2),
        record(1002, 2),
        record(1003, 3),
        record(1004, 4),
        record(1005, 5),
        record(14844322036364398012, 2),
    ]
