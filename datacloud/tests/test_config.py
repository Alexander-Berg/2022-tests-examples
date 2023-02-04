from datacloud.dev_utils.yt import yt_utils
from datacloud.dev_utils.yt.yt_utils import ypath_join
from datacloud.features.dssm import config as dssm_config


def test_get_prod_log_tables(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/dssm/get-prod-log-tables/spy_log'
    yt_utils.create_folders([folder], yt_client)
    dates = ('2012-01-01', '2012-01-02', '2012-01-03', '2012-01-04')
    date_tables = [ypath_join(folder, date) for date in dates]
    for table in date_tables:
        yt_client.write_table(table, [{'fake': 'record'}])

    config = dssm_config.DSSMConfig('2012-01-03', days_to_take=2)
    config.grep_root = '//test/dssm/get-prod-log-tables'
    actual_tables = []
    for table in dssm_config.get_prod_log_tables(yt_client, config):
        actual_tables.append(table)
    expected_tables = [ypath_join(folder, date) for date in
                       ('2012-01-03', '2012-01-02')]
    assert actual_tables == expected_tables
