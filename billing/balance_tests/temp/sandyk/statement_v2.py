# coding: utf-8
__author__ = 'sandyk'

import datetime
import btestlib.utils as utils
import random

DEFAULT_SUM = 1000


class Statement_options:
    def __init__(self, statement_type):
        self.template, self.tran_template, self.date = self.get_templates(statement_type)

    @property
    def template(self):
        return self.template

    @property
    def tran_template(self):
        return self.tran_template

    @property
    def date(self):
        return self.date

    def get_templates(self, statement_type):
        mt940_template = ''':20:STMT20{0}
:25:{1}
:28C:10022
:60F:C{0}EUR{2}{4}
:62F:C{0}EUR{3}
:64:C{0}EUR{3}
:86:NAME ACCOUNT OWNER:wwwww
ACCOUNT DESCRIPTION:  CURR
'''
        mt940_trans = '''
:61:{0}{1}{2}NTRF //11
:86:~01wwwww~026666666666~0366666666666666666666~04wwwww~05wwwww~06wwwww~11~1320{0}~1520{0}~1705~1801~1900'''

        if statement_type == 'mt940':
            return mt940_template, mt940_trans, str(datetime.datetime.now().strftime("%y%m%d"))


class Generator:
    def __init__(self, statement_options):
        self.options = statement_options

    def generate_file(self, account, open_balance, close_balance=None, trans=''):
        if close_balance is None:
            close_balance = open_balance
        path = utils.project_file('temp/sandyk/statements/' + str(
            datetime.datetime.now().strftime("%y_%m_%d_%H%M%S")) + '.txt')
        # all_trans, close_balance = self.generate_mass_trans()
        f = open(path, 'w')
        f.write(self.options.template.format(self.options.date, account, str(open_balance),
                                             str(open_balance + close_balance), trans))
        f.close()


class Transactions:
    def __init__(self, statement_options):
        self.options = statement_options

    def generate_tran(self, credit_type=random.choice(['DR', 'CR']), summ=DEFAULT_SUM):
        return self.options.tran_template.format(self.options.date, credit_type, str(summ))

    # def invalid_purpose(self):

    def generate_mass_trans(self, number_of_tran=2, summ=DEFAULT_SUM):
        balance = 0
        all_tran = ''
        for i in xrange(number_of_tran):
            credit_type = random.choice(['DR', 'CR'])
            all_tran = all_tran + self.generate_tran(credit_type, summ)
            if credit_type == 'DR':
                balance -= summ
            if credit_type == 'CR':
                balance += summ
        return all_tran, balance


if __name__ == "__main__":
    opt = Statement_options('mt940')
    trans, balance = Transactions(opt).generate_mass_trans(4)
    Generator(opt).generate_file('40702978100091003838', 2333, balance, trans)


















