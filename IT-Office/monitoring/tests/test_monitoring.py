from source.config import *
from source.monitoring.monitoring import *
from source.monitoring.equipment_stock_monitoring import EquipmentStockMonitoring
from source.tests.testdata import *
from source.utils import (BotApiUtils)
from source.monitoring.main_big_dashboard import UnitedDashboardBacklogCollector
from source.monitoring.tests.data_monitoring import *
from source.monitoring.tests.fixtures_monitoring import *

@pytest.mark.parametrize("input_data, ex_result", DISK_FINDER_MAIN_DATA)
def test_disk_finder(monkeypatch, mock_get_st_macros, input_data, ex_result):

    request = FakeCallFunction(input_data["_check_inv_by_sn"])
    yt_data = FakeCallFunction(input_data["_fetch_data_from_cmdb"])
    test_ticket = MockIssues()

    monkeypatch.setattr(st_client,'issues', test_ticket)
    monkeypatch.setattr(DiskFinder,'_fetch_data_from_cmdb', yt_data)
    monkeypatch.setattr(DiskFinder,'_check_inv_by_sn', request)
    monkeypatch.setattr(DiskFinder,'_get_office_id_by_login', request)

    DiskFinder().process()
    print(test_ticket.created_dict)
    assert test_ticket.created_dict == ex_result

@pytest.mark.parametrize("input_data, ex_result", DISK_FINDER_FIND_SN)
def test_disk_finder_check_inv_by_sn(monkeypatch, mock_get_st_macros, input_data, ex_result):

    request = FakeResponse(input_data["bot_data"])

    monkeypatch.setattr(requests,'get', request)

    assert DiskFinder()._check_inv_by_sn('blank_here') == ex_result

@pytest.mark.parametrize("input_data, ex_result", TRANCIEVERS_FINDER_TESTDATA)
def test_case(mock_noc_export, mock_get_st_macros, monkeypatch, input_data, ex_result):
    test_ticket = MockIssues()
    bot_response = TrancieversFinderReponse({"sn":input_data["switch_info"],
                                             "fqdn":input_data["trancievers_infos"]})
    monkeypatch.setattr(st_client,'issues', test_ticket)

    monkeypatch.setattr(BotApiUtils,'fetch_osinfo',bot_response)
    TransceiversFinder().process()
    assert test_ticket.created_dict == ex_result

@pytest.mark.parametrize("input_data, ex_result", UNITED_DASHBOARD_BACKLOG_TESTDATA)
def test_case2(input_data, ex_result):
    result = UnitedDashboardBacklogCollector()._fetch_data(input_data)
    assert result == ex_result

@pytest.mark.parametrize("input_data, ex_result", UNITED_DASHBOARD_SLA_TESTDATA)
def test_case2(input_data, ex_result, mock_sla_fetcher):
    solve_time = UnitedDashboardBacklogCollector()._fetch_sla_count(input_data, "solve", "time")
    solve_count = UnitedDashboardBacklogCollector()._fetch_sla_count(input_data, "solve", "count")
    reaction_time = UnitedDashboardBacklogCollector()._fetch_sla_count(input_data, "reaction", "time")
    reaction_count = UnitedDashboardBacklogCollector()._fetch_sla_count(input_data, "reaction", "count")
    assert solve_time == ex_result["solve_time"]
    assert solve_count == ex_result["solve_count"]
    assert reaction_time == ex_result["reaction_time"]
    assert reaction_count == ex_result["reaction_count"]

def test_ext_dismissal_pinger_table():
    yt_data = [{'NB.instance_number': '100000665',
                'NB.segment2': 'APPLE - MACBOOK AIR 13'},
               {'NB.instance_number': '123123123',
                'NB.segment2': 'Dell LATITUDE 100'},
               {'NB.instance_number': '123123411',
                'NB.segment2': 'MAC BOK PRO'},
               ]
    expected = "#|\n||100000665 | APPLE - MACBOOK AIR 13 ||\n||123123123 | Dell LATITUDE 100 ||\n||123123411 | MAC BOK PRO ||\n\n|#"
    result = ExtDismissalFinder()._generate_table_infs(yt_data)

    assert result == expected

def test_ext_dismissal_pinger_merge_data():
    yt_data = [{'NB.instance_number': '100000665', 'NB.ext_login': 'azizazh',
                'NB.oebs_login': None, 'ST.quit_at': '2018-12-20',
                'ST.dep_path': 'Внешние консультанты>Внешние консультанты Яндекс.Такси>Внешние консультанты направления стратегии и роста>Внешние консультанты службы бренда и маркетинговых коммуникаций Яндекс.Такси>Внешние консультанты группы регионального маркетинга',
                'ST.position': 'Специалист по маркетингу',
                'NB.segment2': 'APPLE - MACBOOK AIR 13'},
               {'NB.instance_number': '100000667', 'NB.ext_login': 'azizazh',
                'NB.oebs_login': None, 'ST.quit_at': '2018-12-20',
                'ST.dep_path': 'Внешние консультанты>Внешние консультанты Яндекс.Такси>Внешние консультанты направления стратегии и роста>Внешние консультанты службы бренда и маркетинговых коммуникаций Яндекс.Такси>Внешние консультанты группы регионального маркетинга',
                'ST.position': 'Специалист по маркетингу',
                'NB.segment2': 'MONITOR'},
               {'NB.instance_number': '100000667', 'NB.ext_login': 'vasya',
                'NB.oebs_login': None, 'ST.quit_at': '2018-12-20',
                'ST.dep_path': 'Внешние консультанты>Внешние консультанты Яндекс.Такси>Внешние консультанты направления стратегии и роста>Внешние консультанты службы бренда и маркетинговых коммуникаций Яндекс.Такси>Внешние консультанты группы регионального маркетинга',
                'ST.position': 'Специалист по маркетингу',
                'NB.segment2': 'MONITOR'},
               ]
    result = ExtDismissalFinder()._merge_yt_data(yt_data)
    excepted = [{'login': 'azizazh',
                 'division': 'Внешние консультанты>Внешние консультанты Яндекс.Такси>Внешние консультанты направления стратегии и роста>Внешние консультанты службы бренда и маркетинговых коммуникаций Яндекс.Такси>Внешние консультанты группы регионального маркетинга',
                 'duty': 'Специалист по маркетингу',
                 'table': '#|\n||100000665 | APPLE - MACBOOK AIR 13 ||\n||100000667 | MONITOR ||\n\n|#',
                 'summonees': 'litovskikhd'}, {'login': 'vasya',
                                               'division': 'Внешние консультанты>Внешние консультанты Яндекс.Такси>Внешние консультанты направления стратегии и роста>Внешние консультанты службы бренда и маркетинговых коммуникаций Яндекс.Такси>Внешние консультанты группы регионального маркетинга',
                                               'duty': 'Специалист по маркетингу',
                                               'table': '#|\n||100000667 | MONITOR ||\n\n|#',
                                               'summonees': 'litovskikhd'}]
    assert result == excepted

@pytest.mark.parametrize('role, scale, result', data_stat_crm_api)
def test_stat_crm(role, result, scale, fakeCRMStat):
    assert CrmStat()._process(role, scale) == result

def test_equipment_stock_monitoring():
    monitor = EquipmentStockMonitoring('Test monitoring', 'NOT_USED')
    result = monitor._calculate_models(EQ_STOCK_MONITORING['yt_data'])
    csvfile = monitor._generate_csv(result)
    assert EQ_STOCK_MONITORING['models_result'] == dict(result)
    assert EQ_STOCK_MONITORING['csv_result'] == csvfile.getvalue()
