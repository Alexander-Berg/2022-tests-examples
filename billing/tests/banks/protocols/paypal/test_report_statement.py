import re
from datetime import datetime

from bcl.banks.protocols.paypal.report_settlement import SettlementReport


def test_parse(read_fixture, sftp_client):

    def get_report(fdata):
        sftp_client(files_contents=fdata)
        return SettlementReport(SettlementReport.get_remote_data('ru'))

    report = get_report({
        'file1.txt': '12345',
        'file2.txt': '67890',
    })

    assert report.raw_data == '1234567890'

    date = datetime(2016, 11, 21)

    pattern = SettlementReport.get_filename_pattern(date)
    assert re.match(pattern, f'STL-20161121.01.{SettlementReport.VERSION_STR}.CSV')
    assert not re.match(pattern, 'STL-20161121.01.000.CSV')

    pattern = SettlementReport.get_filename_pattern(date, mam_settings=True)
    assert re.match(pattern, f'STL-20161121.R.01.01.{SettlementReport.VERSION_STR}.CSV')

    pattern = SettlementReport.get_filename_pattern(date, version='121')
    assert re.match(pattern, 'STL-20161121.01.121.CSV')
    assert not re.match(pattern, 'STL-20161121.01.000.CSV')

    report = get_report({
        'file1.txt': read_fixture('STL-20161109.01.009-mock.CSV', decode='utf-8-sig'),
        'file2.txt': read_fixture('STL-20161109.02.009-mock.CSV', decode='utf-8-sig'),
    })
    data = report.parse()
    assert 'FH' in data
    assert 'FF' in data

    report = get_report({
        'STL-20161014.01.009.CSV': read_fixture('STL-20161014.01.009.CSV', decode='utf-8-sig'),
    })
    result = report.compose()

    data = result['data']
    assert result['account_id'] == 'WPC4LH2WEUB2U'
    assert len(data) == 1
    assert 'WPC4LH2WEUB2U' in data
    assert len(data['WPC4LH2WEUB2U']['totals']) == 3

    trans = data['WPC4LH2WEUB2U']['transactions']

    assert len(trans) == 5

    assert len(trans[0]['paypal_trans_children']) == 1
    assert trans[0]['paypal_trans_children'][0]['id'] == '72R352948X2624130'

    assert trans[4]['id'] == '7KV89101AM048371P'
    assert trans[4]['paypal_trans_parent']['id'] == '6GP35717TS126231K'

    assert not trans[0]['external']