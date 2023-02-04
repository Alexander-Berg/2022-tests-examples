# coding: utf-8
__author__ = 'sandyk'

import balalayka.file_utils as file_utils
import pytest

import balalayka.balalayka_steps as steps
import balalayka.banks as mapper
from balalayka.statement import *


# def test_file_input():
# with web.Driver() as driver:
#         statement_page = web.StatementPage.open(bank_id='044525222', driver=driver)
#         statement_page.input_file(
#             file_path='C:\Users\sandyk\Downloads\mt940_25477.txt')
#         pass

#
# def test_get_statement():
#     path = 'C:\Users\sandyk\Downloads\statements\obfuscation\ing_test.txt'
#     response = api.Balalayka().GetStatement(str(get_account_from_file_ing(path)), get_date_from_file_ing(path), False)
#     print get_date_from_file_ing(path)
#     print response
#     assert response['closing_balance'] is not None
#     pass


@pytest.mark.parametrize(("statement", "bank"), [
    (Statement(account=file.get_account_from_file_ing(mapper.bank_data['ing']['path']),
               statement_date=file.get_payment_dt_from_file_ing(mapper.bank_data['ing']['path']), isintraday=False,
               file_path=utils.project_file(mapper.bank_data['ing']['path']), bank_name=mapper.bank_data['ing']['bank_name']),
     mapper.bank_data['ing']['bank'])  ##инг
    ## ,(Statement(account = mapper.bank_data['uni']['account'], date = mapper.bank_data['uni']['date'], isintraday=False, file_path=utils.project_file(mapper['uni']['path']), bank_name = mapper.bank_data['uni']['bank_name']), mapper.bank_data['uni']['bank']),  ##юникредит
])
def test_smoke_ing(statement, bank):
    ## with web.Driver() as driver:
    ##    steps.Statement.upload_statement(statement.file_path, bank_id=bank, driver=driver)
    print 'deletion state:'
    print steps.Statement.delete_concurrent_from_oebs(statement.account, statement.date)
    print 'upload state:'
    print steps.Statement.upload_concurrent(statement.account, statement.bank_name, statement.date)
    response_balalayka = steps.Statement.get_statement_balalayka(account=statement.account, date=statement.date,
                                                                 isintraday=statement.isintraday)
    response_oebs_header = steps.Statement.get_statement_header_oebs(account=statement.account, date=statement.date)
    response_oebs_lines, lines_summ = steps.Statement.get_statement_lines_oebs(account=statement.account,
                                                                               date=statement.date)
    ## проверки заколовка и get_statement

    assert float(response_balalayka['opening_balance']) == float(
        response_oebs_header[0]['control_begin_balance']), 'Mismatch opening_balance and control_begin_balance'
    assert float(response_balalayka['closing_balance']) == float(
        response_oebs_header[0]['control_end_balance']), 'Mismatch closing_balance and control_end_balance'
    assert float(response_balalayka['debet_turnover']) == float(
        response_oebs_header[0]['control_total_dr']), 'Mismatch debet_turnover and control_total_dr'
    assert float(response_balalayka['credit_turnover']) == float(
        response_oebs_header[0]['control_total_cr']), 'Mismatch credit_turnover and control_total_cr'
    ## сверяем суммы строк и хедера в оебс
    assert str(float(response_oebs_header[0]['control_begin_balance']) + float(lines_summ)) == str(
        response_oebs_header[0]['control_end_balance']), 'Mismatch lines and header sum'

    print steps.Statement.get_statement_lines_oebs(account=statement.account, date=statement.date)


def oebs():
    path = 'C:\Users\sandyk\Downloads\mt940_7792.txt'
    account = str(file_utils.get_account_from_file_ing(path))
    date = file_utils.get_payment_dt_from_file_ing(path)
    bank_name = mapper['ing']['bank_name']
    # print account,bank_name,date
    # t = steps.Statement.delete_concurrent_from_oebs(account, date)
    # t = steps.Statement.upload_cuncurrent(account, bank_name, date)
    # # s = steps.Statement.get_statement_header_oebs(account=account, date=date)
    # # rez =  steps.Statement.get_statement_header_oebs(account, date)
    # # print rez[0]['control_end_balance']
    # # print steps.Statement.upload_cuncurrent()
    # print account,bank_name,date, t
    # print get_account_from_file_ing(mapper['ing']['path'])
    # print get_date_from_file_ing(mapper['ing']['path'])
    # print utils.project_file(mapper['ing']['path'])
    # print mapper['ing']['bank_name']
    # print mapper['ing']['bank']
    # header = steps.Statement.get_statement_header_oebs(account, date)
    # print header
    # lines, summ = steps.Statement.get_statement_lines_oebs(account, date)
    # print float(header[0]['control_begin_balance']) + float(summ)
    # print float(header[0]['control_end_balance'])
    # assert str(float(header[0]['control_begin_balance']) + float(summ)) == str(header[0]['control_end_balance'])
    # import xmlrpclib
    # conn = xmlrpclib.ServerProxy('https://tmongo1g.yandex.ru:8024/autotest_xmlrpc', allow_none=1, use_datetime=1)
    steps.Statement.delete_statement_by_file_balalayka('7792')

if __name__ == "__main__":
    pytest.main("-s -v balalayka_sandyk.py")
# #
# test_get_statement()
# test_file_input()
# oebs()
# print get_date_from_file_ing('C:\Users\sandyk\Downloads\mt940_25262.txt')
# pass
