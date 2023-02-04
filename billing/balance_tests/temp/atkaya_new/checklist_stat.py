# -*- coding: utf-8 -*-

import xlrd
import datetime
import yt.wrapper as yt
from yt.wrapper import YtClient

file_path = '/Users/yuelyasheva/Downloads/Чек-лист 1908.xls'
sheet_AI = u'АИ'
sheet_CI = u'КИ'

def get_ci_stat():
    xls_file = xlrd.open_workbook(file_path)
    ci_sheet = xls_file.sheet_by_name(sheet_CI)

    # 0 (А) - признак заголовка (пустое, если нет)
    # 4 (E) - признак смоук (пустое, если нет)
    # 5 (F) - признак ручной регресс (пустое, если нет)
    # 6 (G) - признак асессоры (пустое, если нет)
    # автоматизацию пока не считаем, т.к. в КИ ее не делаем на данный момент

    columns = {'header': 0, 'smoke': 4, 'manual': 5, 'assessors': 6}

    # total = 0
    # smoke_total = 0
    # manual_reg_total = 0
    # assessors_total = 0
    # assessors_in_smoke = 0
    # assessors_in_manual = 0
    # manual_in_smoke = 0

    stats = {
        'total': 0,
        'smoke_total': 0,
        'manual_total': 0,
        'assessors_total': 0,
        'assessors_of_smoke': 0,
        'assessors_of_manual': 0,
        'manual_of_smoke': 0,
        'automated_total': 0,
    }

    for rownum in range(1, ci_sheet.nrows):
        row = ci_sheet.row_values(rownum)

        # если не заголовок
        if not row[columns['header']]:
            stats['total'] += 1

            # если в колонке "смоук" не пусто
            if row[columns['smoke']]:
                stats['smoke_total'] += 1

            # если в колонке "ручной регресс" не пусто
            if row[columns['manual']]:
                stats['manual_total'] += 1

                # если в колонке "смоук" не пусто
                if row[columns['smoke']]:
                    stats['manual_of_smoke'] += 1

            # если в колонке "асессоры" не пусто
            if row[columns['assessors']]:
                stats['assessors_total'] += 1

                # если в колонке "смоук" не пусто
                if row[columns['smoke']]:
                    stats['assessors_of_smoke'] += 1

                # если в колонке "ручной регресс" не пусто
                if row[columns['manual']]:
                    stats['assessors_of_manual'] += 1

    return stats


def get_ai_stat():
    xls_file = xlrd.open_workbook(file_path)
    ai_sheet = xls_file.sheet_by_name(sheet_AI)

    # 0 (А) - признак заголовка (пустое, если нет)
    # 4 (E) - признак смоук (пустое, если нет)
    # 5 (F) - признак ручной регресс (пустое, если нет)
    # 6 (G) - признак асессоры (пустое, если нет)
    # 7 (H) - признак автоматизирован в Гермионе (пустое, если нет)
    # 8 (I) - признак автоматизирован в jest (пустое, если нет)
    # 9 (J) - признак автоматизирован в snout (пустое, если нет)

    headers = {'not_react': ['HO', 'SHO'], 'react': ['H', 'SH']}

    columns = {'header': 0, 'smoke': 4, 'manual': 5, 'assessors': 6, 'hermione': 7, 'jest': 8, 'snout': 9}

    stats = {
        'total': 0,
        'react_total': 0,
        'smoke_total': 0,
        'smoke_react': 0,
        'manual_total': 0,
        'assessors_total': 0,
        'automated_total': 0,
        'automated_react': 0,
        'automated_of_smoke': 0,
        'automated_react_of_smoke': 0,
        'automated_of_manual': 0,
        'automated_react_of_manual': 0,
        'manual_of_smoke': 0,
    }

    react_flag = False
    for rownum in range(1, ai_sheet.nrows):
        row = ai_sheet.row_values(rownum)

        # смотрим, переведен ли функционал на реакт
        if row[columns['header']] in headers['react']:
            react_flag = True
        elif row[columns['header']] in headers['not_react']:
            react_flag = False

        # если не заголовок
        if not row[columns['header']]:
            stats['total'] += 1

            # если функционал переведен на реакт
            if react_flag:
                stats['react_total'] += 1

            # если в колонке "смоук" не пусто
            if row[columns['smoke']]:
                stats['smoke_total'] += 1

                # если в колонке "ручной регресс" не пусто
                if row[columns['manual']]:
                    stats['manual_of_smoke'] += 1

                # если функционал переведен на реакт
                if react_flag:
                    stats['smoke_react'] += 1

            # если в колонке "ручной регресс" не пусто
            if row[columns['manual']]:
                stats['manual_total'] += 1

            # если в колонке "асессоры" не пусто
            if row[columns['assessors']]:
                stats['assessors_total'] += 1

            # если в любой из колонок автоматизации не пусто
            if row[columns['hermione']] or row[columns['jest']] or row[columns['snout']]:
                stats['automated_total'] += 1

                # если в колонке "смоук" не пусто
                if row[columns['smoke']]:
                    stats['automated_of_smoke'] += 1

                # если в колонке "ручной регресс" не пусто
                if row[columns['manual']]:
                    stats['automated_of_manual'] += 1

                # если функционал переведен на реакт
                if react_flag:
                    stats['automated_react'] += 1

                    # если в колонке "ручной регресс" не пусто
                    if row[columns['manual']]:
                        stats['automated_react_of_manual'] += 1

                    # если в колонке "смоук" не пусто
                    if row[columns['smoke']]:
                        stats['automated_react_of_smoke'] += 1
    return stats


def write_test_table(table_path, data):
    cluster = 'hahn'
    client = YtClient(
        config={
            'yamr_mode': {'create_recursive': True},
            'pickling': {'force_using_py_instead_of_pyc': True},
            'proxy': {'url': '%(cluster)s.yt.yandex.net' % {'cluster': cluster}},
        }
    )
    client.write_table(table_path, data, format=yt.JsonFormat())


def fill_test_cases_ci_stat():
    ci_stat = get_ci_stat()
    dst = yt.TablePath('//home/balance-test/metrics/test_cases_ci_stat', append=True)
    ci_stat.update({'dt': datetime.datetime.today().strftime('%Y-%m-%d')})
    write_test_table(dst, [ci_stat])

def fill_test_cases_ai_stat():
    ai_stat = get_ai_stat()
    dst = yt.TablePath('//home/balance-test/metrics/test_cases_ai_stat', append=True)
    ai_stat.update({'dt': datetime.datetime.today().strftime('%Y-%m-%d')})
    write_test_table(dst, [ai_stat])

# print(get_ci_stat())
fill_test_cases_ci_stat()

# print(get_ai_stat())
fill_test_cases_ai_stat()