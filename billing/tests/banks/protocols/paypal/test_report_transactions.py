import re
from datetime import datetime

from bcl.banks.protocols.paypal.report_transactions import TransactionReport


def test_parse(read_fixture, sftp_client):

    def get_report(fdata):
        sftp_client(files_contents=fdata)
        return TransactionReport(TransactionReport.get_remote_data('ru'))

    report = get_report({
        'file1.txt': '12345',
        'file2.txt': '67890',
    })

    assert report.raw_data == '1234567890'

    date = datetime(2016, 11, 21)

    pattern = TransactionReport.get_filename_pattern(date)
    assert re.match(pattern, f'TRR-20161121.01.{TransactionReport.VERSION_STR}.CSV')
    assert not re.match(pattern, 'TRR-20161121.01.000.CSV')

    pattern = TransactionReport.get_filename_pattern(date, version='121')
    assert re.match(pattern, 'TRR-20161121.01.121.CSV')
    assert not re.match(pattern, 'TRR-20161121.01.000.CSV')

    report = get_report({
        'TRR-20161021.01.011-mock.CSV': read_fixture('TRR-20161021.01.011-mock.CSV', decode='utf-8-sig'),
    })
    result = report.compose()

    data = result['data']
    assert result['account_id'] == 'TP5CKP49PXXTS'
    assert data['account_id'] == 'TP5CKP49PXXTS'

    trans = data['transactions']
    assert len(trans) == 6

    assert trans[3]['billing_addr_line_1'] == ',ул. Кововой, дом 60, квартира 2228,'
