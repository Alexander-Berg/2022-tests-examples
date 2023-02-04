# coding: utf-8
__author__ = 'sandyk'

import datetime
import btestlib.utils as utils
import random

CURR_DT_MT940 = str(datetime.datetime.now().strftime("%y%m%d"))

class Generator:
    def __init__(self, format):
        self.template = format
        if self.template == 'mt940':
            self.header, self.footer, self.tran = self.get_templates(self.template)
            pass

    def get_templates(self, template):
        mt940_header_template = ''':20:STMT20{0}
:25:{1}
:28C:10022
:60F:C{0}EUR{2}
'''
        mt940_footer_template = ''':62F:C{0}EUR{1}
:64:C{0}EUR{1}
:86:NAME ACCOUNT OWNER:wwwww
ACCOUNT DESCRIPTION:  CURR
'''
        mt940_trans_template =''':61:{0}{1}{2}NTRF //11
:86:~01wwwww~026666666666~0366666666666666666666~04wwwww~05wwwww~06wwwww~11~1320{0}~1520{0}~1705~1801~1900
'''
        if template == 'mt940':
            return mt940_header_template, mt940_footer_template, mt940_trans_template

    def generate_tran(self, credit_type = 'CR', sum = 1000, dt = CURR_DT_MT940):
        return self.tran.format(dt, credit_type, str(sum))

    def generate_mass_trans(self, number_of_tran = 2, sum = 1000, dt = CURR_DT_MT940):
        balance = 0
        all_tran = ''
        credit_type = random.choice(['DR','CR'])
        for i in xrange(number_of_tran):
            all_tran = all_tran + self.generate_tran(credit_type, sum, dt)
            if credit_type == 'DR':
                balance -= sum
            if credit_type == 'CR':
                balance += sum
        return all_tran, balance

    def generate_file(self, account, open_balance, dt = CURR_DT_MT940):
        if self.template == 'mt940':
            path = utils.project_file('temp/sandyk/statements/' + self.template + '_' + str(
                datetime.datetime.now().strftime("%y_%m_%d_%H%M%S")) + '.txt')
            all_trans, close_balance = self.generate_mass_trans()
            f = open(path, 'w')
            f.write(self.header.format(dt, account,str(open_balance)))
            f.write(all_trans)
            f.write(self.footer.format(dt, str(open_balance+close_balance)))
            f.close()


gen = Generator('mt940')
gen.generate_file('40702978100091003838', 2333)




# class Statement:
#     def __init__(self, format):
#         ##format, account, date, is_intraday
#         self.format = format
#         # self.account = account
#         # self.date = date
#         # self.is_intraday = is_intraday
# class Transaction:
#     def __init__(self, credit_type, sum, date, ):
#         self.credit_type = credit_type
#         self.sum = sum
#         self.date = date
#
#     @staticmethod
#     def create_statement(format, account, income):
#         if format == 'mt940':
#             path = utils.project_file('balalayka/resources/statements/' + format + '_' + str(
#                 datetime.datetime.now().strftime("%y_%m_%d_%H%M%S")) + '.txt')
#             print path
#             f = open(path, 'w')
#             curr_date = str(datetime.datetime.now().strftime("%Y%m%d"))
#             f.write(
#                 ''':20:STMT{0}
# :25:{1}
# :28C:10022
# :60F:C{0}EUR{2}
# '''.format(curr_date, account,str(income)))
#             f.close()


# print Statement.create_statement('mt940', '40702978100091003838', '2333')















