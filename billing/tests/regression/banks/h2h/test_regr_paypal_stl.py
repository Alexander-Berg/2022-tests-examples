import uuid
from datetime import datetime, timedelta

import pytest

TRANSFER_GROUND = 'PAYPAL_TRANSFER по договору 00056384.0 от 18.04.2016'
HOLD_GROUND = 'PAYPAL_HOLD по договору 00056384.0 от 18.04.2016'
RECEIPT_GROUND = 'PAYPAL_RECEIPT по договору 00056384.0 от 18.04.2016'
COMMISSION_GROUND = 'PAYPAL_COMMISSION по договору 00056384.0 от 18.04.2016'
EXCHANGE_GROUND = 'PAYPAL_EXCHANGE по договору 00056384.0 от 18.04.2016'
PAYMENT_GROUND = 'PAYPAL_PAYMENT по договору 00056384.0 от 18.04.2016'
REFUND_GROUND = 'PAYPAL_REFUND по договору 00056384.0 от 18.04.2016'
STATEMENT_DATE = (datetime.now() - timedelta(days=1))


def get_dir_template(country_code):
    return '/ppreports/outgoing' if country_code == 'ru' else '/u01/YNDXRUMM/paypal/{country}/ppreports/outgoing'.format(
        country=country_code)


PARAMS = [
    {
        'country_code': 'ru', 'template': 'paypal_correct_template.csv',
        'account': 'TP5CKP49PXXTS',
        'account_id': 445,
        'registry_data': [
            {
                'TP5CKP49PXXTS': {'opening_balance': '33.82', 'closing_balance': '72.45',
                                  'statement_body': {
                                      '19.67': {'ground': TRANSFER_GROUND, 'direction': 'OUT'},
                                      '80.59': {'ground': HOLD_GROUND, 'direction': 'IN'},
                                      '-2.93': {'ground': RECEIPT_GROUND, 'direction': 'IN'},
                                      '19.36': {'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                  }}
            }
        ]
    },
    {
        'country_code': 'ch', 'template': 'paypal_correct_template_ch.csv',
        'account': 'EVERVCDGBV3A2',
        'account_id': 373,
        'registry_data': [
            {
                'EVERVCDGBV3A2': {'opening_balance': '245668.25', 'closing_balance': '245973.40',
                                  'credit_turnover': '328.81', 'debet_turnover': '23.66',
                                  'statement_body': {
                                      '-368.49': {'account': 'EVERVCDGBV3A2', 'bik': '',
                                                  'ground': HOLD_GROUND, 'direction': 'IN'},
                                      '697.30': {'account': 'EVERVCDGBV3A2', 'bik': '',
                                                 'ground': RECEIPT_GROUND, 'direction': 'IN'},
                                      '23.66': {'account': 'EVERVCDGBV3A2', 'bik': '',
                                                'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                  }},
                'EVERVCDGBV3A2/EUR': {'opening_balance': '33493.28', 'closing_balance': '1352.72',
                                      'credit_turnover': '1430.81', 'debet_turnover': '33571.37',
                                      'statement_body': {
                                          '33493.28': {'account': 'EVERVCDGBV3A2/EUR', 'bik': '',
                                                       'doc_number': '7KN40688LF012804R', 'ground': EXCHANGE_GROUND,
                                                       'direction': 'OUT'},
                                          '-368.49': {'account': 'EVERVCDGBV3A2/EUR', 'bik': '',
                                                      'ground': HOLD_GROUND, 'direction': 'IN'},
                                          '1799.30': {'account': 'EVERVCDGBV3A2/EUR', 'bik': '',
                                                      'ground': RECEIPT_GROUND, 'direction': 'IN'},
                                          '78.09': {'account': 'EVERVCDGBV3A2/EUR', 'bik': '',
                                                    'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                      }},
                'EVERVCDGBV3A2/CHF': {'opening_balance': '2269.02', 'closing_balance': '37086.19',
                                      'credit_turnover': '34837.19', 'debet_turnover': '20.02',
                                      'statement_body': {
                                          '34763.19': {'account': 'EVERVCDGBV3A2/CHF', 'bik': '',
                                                       'doc_number': '6Y846898CU346274R', 'ground': EXCHANGE_GROUND,
                                                       'direction': 'IN'},
                                          '22.69': {'account': 'EVERVCDGBV3A2/CHF', 'bik': '',
                                                    'ground': TRANSFER_GROUND, 'direction': 'OUT'},
                                          '74.00': {'account': 'EVERVCDGBV3A2/CHF', 'bik': '',
                                                    'ground': RECEIPT_GROUND, 'direction': 'IN'},
                                          '-2.67': {'account': 'EVERVCDGBV3A2/CHF', 'bik': '',
                                                    'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                      }},
            }
        ]
    },
    {
        'country_code': 'ch2', 'template': 'paypal_correct_template_ch2.csv',
        'account': '37ZY3NUB6BYAN',
        'account_id': 28,
        'registry_data': [
            {
                '37ZY3NUB6BYAN': {'opening_balance': '245668.25', 'closing_balance': '273980.39',
                                  'credit_turnover': '28318.53', 'debet_turnover': '6.39',
                                  'statement_body': {
                                      '5.31': {'account': '37ZY3NUB6BYAN', 'bik': '',
                                               'ground': PAYMENT_GROUND, 'direction': 'OUT'},
                                      '28418.28': {'account': '37ZY3NUB6BYAN', 'bik': '',
                                                   'doc_number': '5EJ859601G827092W', 'ground': EXCHANGE_GROUND,
                                                   'direction': 'IN'},
                                      '-2.45': {'account': '37ZY3NUB6BYAN', 'bik': '',
                                                'ground': HOLD_GROUND, 'direction': 'IN'},
                                      '-97.30': {'account': '37ZY3NUB6BYAN', 'bik': '',
                                                 'ground': RECEIPT_GROUND, 'direction': 'IN'},
                                      '1.08': {'account': '37ZY3NUB6BYAN', 'bik': '',
                                               'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                  }},
                '37ZY3NUB6BYAN/CHF': {'opening_balance': '2269.02', 'closing_balance': '2268.99',
                                      'credit_turnover': '-0.30', 'debet_turnover': '-0.27',
                                      'statement_body': {
                                          '30000.00': {'account': '37ZY3NUB6BYAN/CHF', 'bik': '',
                                                       'doc_number': '8V649637JB992671S', 'ground': EXCHANGE_GROUND,
                                                       'direction': 'OUT'},
                                          '-0.30': {'account': '37ZY3NUB6BYAN/CHF', 'bik': '', 'ground': HOLD_GROUND,
                                                   'direction': 'IN'},
                                          '-30000.00': {'account': '37ZY3NUB6BYAN/CHF', 'bik': '',
                                                        'ground': TRANSFER_GROUND, 'direction': 'OUT'},
                                          '-0.27': {'account': '37ZY3NUB6BYAN/CHF', 'bik': '',
                                                    'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                      }},
            }
        ]
    },
    {
        'country_code': 'us', 'template': 'paypal_correct_template_us.csv',
        'account': 'YRJQEWS6KJDR2',
        'account_id': 448,
        'registry_data': [
            {
                'YRJQEWS6KJDR2': {'opening_balance': '245668.25', 'closing_balance': '244705.50',
                                  'credit_turnover': '371.81', 'debet_turnover': '1334.56',
                                  'statement_body': {
                                      '1315.63': {'account': 'YRJQEWS6KJDR2', 'bik': '',
                                                  'ground': TRANSFER_GROUND, 'direction': 'OUT'},
                                      '-368.49': {'account': 'YRJQEWS6KJDR2', 'bik': '',
                                                  'ground': HOLD_GROUND, 'direction': 'IN'},
                                      '740.30': {'account': 'YRJQEWS6KJDR2', 'bik': '',
                                                 'ground': RECEIPT_GROUND, 'direction': 'IN'},
                                      '18.93': {'account': 'YRJQEWS6KJDR2', 'bik': '',
                                                'ground': COMMISSION_GROUND, 'direction': 'OUT'},
                                  }},
            }
        ]
    }
]


@pytest.mark.regression
def test_incorrect_end_amount(get_statement, path_to_file, bcl_test_sftp, run_force_task, get_last_uploaded_statement,
                              wait_processing_statement, read_fixture, paypal, delete_statement, rerun_task):
    acc = 'YRJQEWS6KJDR2'
    statement_balalayka_old = get_statement(acc, STATEMENT_DATE)
    statement_template = read_fixture('paypal_correct_template_us.csv')
    data = statement_template.replace('24470550', '24470950')

    statement_number, statement_status = upload_statement(
        bcl_test_sftp, 'us', path_to_file, data, acc, 448, run_force_task, get_last_uploaded_statement,
        wait_processing_statement, paypal, rerun_task
    )
    statement_balalayka = get_statement(acc, STATEMENT_DATE)
    delete_statement(paypal['id'], statement_number)
    assert 'Ошибка' in statement_status
    assert 'Ошибка сверки' in statement_status
    assert statement_balalayka_old == statement_balalayka


@pytest.mark.regression
def test_incorrect_file(bcl_send_payment, wait_processing_payment, path_to_file, bcl_test_sftp_paypal,
                        run_force_task, get_last_uploaded_statement, wait_processing_statement,
                        get_statement, delete_statement, paypal):
    statement_data = '''"RH",{date} 17:41:54 +0400,"R","TP5CKP49PXXTS",009,
"FH",01
"SH",{date} 00:00:00 +0400,{date} 23:59:59 +0400,"TP5CKP49PXXTS",""
"SF","USD",0,0,0,0,"CR",0,"CR",0,"CR",0,"CR",0,"CR",0,"CR",0,8
"SC",8
'''.format(date=STATEMENT_DATE.strftime('%Y/%m/%d'))
    statement_dir = '/ppreports/outgoing'
    bcl_test_sftp_paypal().clear_dir(statement_dir)

    file_path = path_to_file(
        f"STL-{STATEMENT_DATE.strftime('%Y%m%d')}.01.009.CSV"
    )
    file_path.write_bytes(statement_data.encode('utf-8-sig'))

    bcl_test_sftp_paypal().upload_file(file_path, statement_dir)
    response = run_force_task(
        'paypal_download_statements_ru', 500
    )
    statement_number = get_last_uploaded_statement(paypal['id'])
    statement_status = wait_processing_statement(paypal['id'], statement_number=statement_number)
    delete_statement(paypal['id'], statement_number)

    assert 'Ошибка' in statement_status
    assert 'SettlementReport' in statement_status
    assert 'SettlementReport: File headers count mismatches file footers count' in response.text


@pytest.mark.regression
@pytest.mark.parametrize('stl_data', PARAMS, ids=lambda x: x['country_code'] + '_' + x['account'])
def test_correct_file(
    stl_data, bcl_test_sftp, bcl_test_sftp_paypal, path_to_file, run_force_task, get_statement,
    wait_processing_statement, get_last_uploaded_statement, delete_statement, read_fixture, paypal, rerun_task
):
    bcl_sftp = bcl_test_sftp_paypal
    statement_template = read_fixture(stl_data['template'])
    statement_number, _ = upload_statement(
        bcl_sftp, stl_data['country_code'], path_to_file, statement_template,
        stl_data['account'], stl_data['account_id'], run_force_task, get_last_uploaded_statement,
        wait_processing_statement, paypal, rerun_task
    )

    for registry_check in stl_data['registry_data']:
        for account in registry_check.keys():
            statement_balalayka = get_statement(account, STATEMENT_DATE)
            assert statement_balalayka['opening_balance'] == registry_check[account]['opening_balance']
            assert statement_balalayka['closing_balance'] == registry_check[account]['closing_balance']

            if registry_check[account].get('debet_turnover', None):
                assert statement_balalayka['credit_turnover'] == registry_check[account]['credit_turnover']
                assert statement_balalayka['debet_turnover'] == registry_check[account]['debet_turnover']

            assert len(statement_balalayka['statement_body']) == len(registry_check[account]['statement_body'])

            for res_check in statement_balalayka['statement_body']:
                for key in registry_check[account]['statement_body'][res_check['summ']]:
                    assert res_check[key] == registry_check[account]['statement_body'][res_check['summ']][key]
    delete_statement(paypal['id'], statement_number)


info_by_summ = {
    '19.37': {'ground': TRANSFER_GROUND, 'direction': 'OUT'},
    '8.12': {'ground': 'PAYPAL_PAYMENT Тестирование BCL по договору 00056384.0 от 18.04.2016', 'direction': 'OUT'},
    '80.59': {'ground': HOLD_GROUND, 'direction': 'IN'},
    '-3.84': {'ground': RECEIPT_GROUND, 'direction': 'IN'},
    '19.36': {'ground': COMMISSION_GROUND, 'direction': 'OUT'},
    '0.11': {'ground': 'PAYPAL_RECEIPT Тестирование BCL по договору 00056384.0 от 18.04.2016', 'direction': 'IN'},
    '0.20': {'ground': 'PAYPAL_COMMISSION Тестирование BCL по договору 00056384.0 от 18.04.2016', 'direction': 'OUT'},
}


@pytest.mark.regression
def test_separate_payments_paypal(
    bcl_send_payment, wait_processing_payment, path_to_file, bcl_test_sftp_paypal, run_force_task,
    get_last_uploaded_statement, wait_processing_statement, get_statement, delete_statement, read_fixture, paypal,
    rerun_task
):
    def send_and_wait_payment():
        transaction_id = str(uuid.uuid4()).upper()
        result = bcl_send_payment(
            transaction_id, f_acc='paypalch@yandex-team.ru', f_bik='044525303', t_acc='PVC@yandex-test.ru',
            currency='USD', t_acc_type='email'
        )
        wait_processing_payment([transaction_id], source_oebs=False)
        return result['doc_number']

    doc_number_1 = send_and_wait_payment()
    doc_number_2 = send_and_wait_payment()
    doc_number_3 = send_and_wait_payment()

    statement_template = read_fixture('paypal_separate.csv')
    data = statement_template. \
        replace('doc_number_2', str(doc_number_2)). \
        replace('doc_number_1', str(doc_number_1)). \
        replace('doc_number_3', str(doc_number_3))

    statement_number, _ = upload_statement(
        bcl_test_sftp_paypal, 'ru', path_to_file, data,
        'TP5CKP49PXXTS', 445, run_force_task, get_last_uploaded_statement, wait_processing_statement, paypal, rerun_task
    )
    statement_balalayka = get_statement('TP5CKP49PXXTS', STATEMENT_DATE)
    assert statement_balalayka['opening_balance'] == '33.82'
    assert statement_balalayka['closing_balance'] == '63.63'

    assert len(statement_balalayka['statement_body']) == len(info_by_summ)

    for res_check in statement_balalayka['statement_body']:
        for key in info_by_summ[res_check['summ']]:
            assert res_check[key] == info_by_summ[res_check['summ']][key]
    delete_statement(paypal['id'], statement_number)


def upload_statement(bcl_sftp, country_code, path_to_file_func, statement_template, account, account_id, run_force_task,
                     get_last_uploaded_statement, wait_processing_statement, bank, rerun_task, correct_statement=True):
    statement_dir = '/ppreports/outgoing'
    bcl_sftp().clear_dir(statement_dir)

    data = statement_template.format(
        account=account, currency='RUB' if country_code == 'ru' else 'USD',
        report_date=STATEMENT_DATE.strftime('%Y/%m/%d'), header_date=datetime.now().strftime('%Y/%m/%d')
    )

    additional_name = '' if country_code != 'ch2' else '.R.01'
    file_path = path_to_file_func(
        f"STL-{STATEMENT_DATE.strftime('%Y%m%d')}{additional_name}.01.009.CSV"
    )
    file_path.write_bytes(data.encode('utf-8-sig'))

    bcl_sftp().upload_file(file_path, statement_dir)

    if country_code == 'ch2':
        rerun_task(
            6, proc_name='automate_statements', on_date=datetime.now() - timedelta(days=1),
            additional_params='&mam_settings=1'
        )
    else:
        run_force_task(
            f'paypal_download_statements_ru', 200
        )
    if correct_statement:

        statement_number = get_last_uploaded_statement(bank['id'], account_id)
        statement_status = wait_processing_statement(bank['id'], statement_number=statement_number)
        return statement_number, statement_status
