import zipfile
from datetime import datetime, timedelta

import pytest

STATEMENT_DIR = '/u01/YNDXRUMM/OUT'
STATEMENT_DIR_YATECH = '/u01/YNDXRUMM/YNDXTRUMM/IMBKRUMM'

ACC_LIST_YA = [('40702810600014307627', 99), ('40702840200014307628', 170)]
ACC_LIST_YATECH = [('40702810920010001241', 140)]


@pytest.mark.regression
def test_h2h_statement_upload_allday(
        bcl_test_sftp, statement_start_amount, path_to_file, rerun_task, wait_processing_statement,
        get_last_uploaded_statement, get_statement, delete_statement, read_fixture, unicredit, upload_statement):
    upload_and_check_statement(
        path_to_file, statement_start_amount, get_last_uploaded_statement,
        wait_processing_statement, get_statement, delete_statement, read_fixture, unicredit, upload_statement
    )


@pytest.mark.regression
def test_h2h_statement_upload_intraday(
        bcl_test_sftp, statement_start_amount, path_to_file, rerun_task, wait_processing_statement, unicredit,
        get_last_uploaded_statement, get_statement, delete_statement, get_all_uploaded_statements, read_fixture, upload_statement):
    for statement_num in get_all_uploaded_statements(unicredit['id'], '', True):
        delete_statement(unicredit['id'], statement_num)
    upload_and_check_statement(
        path_to_file, statement_start_amount, get_last_uploaded_statement,
        wait_processing_statement, get_statement, delete_statement, read_fixture, unicredit, upload_statement, is_intraday=True
    )


def upload_and_check_statement(
        path_to_file, statement_start_amount, get_last_uploaded_statement,
        wait_processing_statement, get_statement, delete_statement, read_fixture, bank_data, upload_statement, is_intraday=False):

    statement_date = datetime.now() if is_intraday else datetime.now() - timedelta(days=1)
    statement_files = []

    def prepare_file(account):
        file_path = path_to_file(
            'IMBKRUMM_camt.%s.001.02_%s000000_00011_00677206_%s.xml' % (
                '054' if is_intraday else '053', statement_date.strftime('%Y%m%d'), account
            )
        )
        statement_text = read_fixture('044525545_rub_%s_statement.xml' % ('I' if is_intraday else 'A'))
        file_path.write_text(
            statement_text.format(**{
                'account': account, 'statement_date': statement_date.strftime('%Y-%m-%d'),
                'balance': '0' if is_intraday else statement_start_amount(account)
            }))
        return file_path
    for acc, acc_id in ACC_LIST_YA + ACC_LIST_YATECH:
        file_path = prepare_file(acc)
        statement_files.append(file_path)

    upload_statement(bank_data['id'], statement_files)

    for account, acc_id in ACC_LIST_YA + ACC_LIST_YATECH:
        statement_number = get_last_uploaded_statement(bank_data['id'], acc_id)
        wait_processing_statement(bank_data['id'], statement_number=statement_number)

        result = get_statement(account, statement_date, is_intraday)
        delete_statement(bank_data['id'], statement_number)
        assert result.get('statement_body', None) is not None
        assert len(result.get('statement_body', [])) == 2
        if is_intraday:
            assert not result.get('opening_balance', None)
            assert not result.get('opening_balance', None)
        else:
            assert result.get('opening_balance', 1) == result.get('closing_balance', 0)

