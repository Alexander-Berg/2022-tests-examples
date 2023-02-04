# -*- coding: utf-8 -*-

import xlrd

def named(row):
    l = len(row)
    data = dict()

    for idx in xrange(1, l):

        d = idx // 26
        m = idx % 26

        if m == 0:
            d -= 1
            m = 26

        column = chr(64 + m)
        if d:
            column = chr(64 + d) + column

        data[column] = row[idx-1]

    return data

# book = 'C:\\torvald\_TEST_TOOLS\\balance-tests\\temp\\torvald\NewCommissionTypes_2019.xlsx'
book = 'C:\\torvald\_TEST_TOOLS\\balance-tests\\temp\\torvald\Checklist_proposal.xlsx'
sheet = u'АИ'

b = xlrd.open_workbook(book)
current = b.sheet_by_name(sheet)

data = dict()
smoke = 0
manual = 0
for rownum in range(current.nrows):
    row = named(current.row_values(rownum))
    if row['E'] == u'Smoke':
        smoke += 1

print(smoke)

