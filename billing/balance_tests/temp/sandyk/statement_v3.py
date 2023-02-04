# coding: utf-8
__author__ = 'sandyk'

import datetime
import io
import xml.dom.minidom as xm

import btestlib.utils as utils
import statement_templates as template


def get_date_in_format(dt, bank):
    if bank == 'ING':
        return str(dt.strftime("%y%m%d")), None
    if bank == 'UNI':
        return str(dt.strftime("%Y-%m-%d")), str(dt.strftime("%Y%m%d"))
    if bank == 'RAIF':
        return str(dt.strftime("%d.%m.%Y")), None


TRANSACTION_86_DEFAULTS = {'~01': 'wwwwwwwwww',  ##наименование получателя
                           '~02': '6666666666',  ##ИНН получателя
                           '~03': '66666666666666666666',  ##номер счета получателя
                           '~04': '666666666',  ##БИК банка получателя
                           '~05': 'wwwwwwwwww',  ##Наименование банка получателя
                           '~06': 'wwwwwwwwww',  ##Назначение (цель) платежа (до 255 символов)
                           '~11': '66666666666666666666',  ## Корреспондентский счет банка получателя
                           # '~13': internal ##Дата документа
                           # '~15': internal ##Дата получения документа (для входящих переводов)
                           # '~16': internal ##Электронный номер операции для платежей в рублях
                           '~17': '05',  ##Очередность платежа
                           '~18': '01',  ##Тип операции
                           '~19': '00',  ##Тип обработки платежей в рублях
                           '~20': '666666666',  ##КПП банка получателя

                           '~21': 'wwwwwwwwww',  ##наименование плательщика
                           '~22': '6666666666',  ##ИНН плательщика
                           '~23': '66666666666666666666',  ##номер счета плательщика
                           '~24': '666666666',  ##БИК банка плательщика
                           '~25': 'wwwwwwwwww',  ##Наименование банка плательщика
                           '~26': '66666666666666666666',  ## Корреспондентский счет банка плательщика
                           '~27': '666666666'  ##КПП банка получателя
}


def get_templates(statement_type):
    if statement_type == 'mt940':
        return template.mt940_allday_header_template, template.mt940_allday_footer_template, template.mt940_allday_transaction_template
    if statement_type == 'iso':
        return template.iso_header_template, template.iso_footer_template, template.iso_transaction_template
    if statement_type == '1c':
        return template.onec_header_template, template.onec_footer_template, template.onec_transaction_template


class Transaction:
    def __init__(self, bank='ING', credit_type='CR', amount=1113.13, transaction_dt=datetime.datetime.now(),
                 params=None, currency='RUB'):
        defaults = TRANSACTION_86_DEFAULTS.copy()
        if params:
            defaults.update(params)
        self.body = defaults
        self.body['amount'] = amount
        self.body['currency'] = currency
        self.body['credit_type'] = credit_type
        if bank == 'UNI':
            self.body['credit_type'] = 'CRDT' if credit_type == 'CR'  else 'DBIT'
        if bank == 'RAIF':
            self.body['credit_type'] = 'ДатаПоступило' if credit_type == 'CR'  else 'ДатаСписано'
        self.body['transaction_dt'] = get_date_in_format(transaction_dt, bank=bank)[0]


class Statement:
    def __init__(self, account, isintraday, bank, start_amount, currency='RUB', statement_dt=datetime.datetime.now()):
        # TODO: support currency: depends on account
        self.body = dict()
        self.body['account'] = account
        self.body['currency'] = currency
        self.body['isintraday'] = isintraday
        self.body['bank'] = bank
        self.body['start_amount'] = self.body['end_amount'] = start_amount
        if bank == 'UNI':
            self.body['statement_dt_long'], self.body['statement_dt_short'] = get_date_in_format(statement_dt,
                                                                                                 bank=bank)
        if bank == 'ING' or bank == 'RAIF':
            self.body['statement_dt'] = get_date_in_format(statement_dt, bank=bank)[0]

        self.transactions_list = list()

    def add_transaction(self, transaction):
        self.transactions_list.append(transaction)
        if transaction.body['credit_type'] == 'CR' or transaction.body['credit_type'] == 'CRDT' or transaction.body[
            'credit_type'] == 'ДатаПоступило':
            self.body['end_amount'] += transaction.body['amount']
        if transaction.body['credit_type'] == 'DR' or transaction.body['credit_type'] == 'DBIT' or transaction.body[
            'credit_type'] == 'ДатаСписано':
            self.body['end_amount'] -= transaction.body['amount']

    def generate_data(self, statement_type):
        header, footer, trans = get_templates(statement_type)
        data = header.format(**self.body)
        for item in self.transactions_list:
            data += trans.format(**item.body)
        data += footer.format(**self.body)
        # data = data.replace('.', ',')
        print data
        if statement_type == 'iso':
            xml = xm.parseString(data)
            data = xml.toxml(encoding='utf8')

        path = utils.project_file(
            'temp/sandyk/statements/' + statement_type + '_' + str(
                datetime.datetime.now().strftime("%y_%m_%d_%H%M%S")) + '.txt')
        f = io.open(path, 'w', encoding="cp1251")
        f.write(data)
        # f = f.encode("cp1251")
        f.close()
        return data


if __name__ == "__main__":

    BANK = 'UNI'

    MAPPER = {'UNI': {'account': '40702810600014307627', 'format': 'iso'}
        , 'ING': {'account': '40702978100091003838', 'format': 'mt940'}
        , 'RAIF': {'account': '40702974200001400742', 'format': '1c'}}

    st = Statement(account=MAPPER[BANK]['account'],
                   statement_dt=datetime.datetime(2016, 2, 15).replace(hour=0, minute=0, second=0, microsecond=0),
                   isintraday=False, bank=BANK, start_amount=0)
    tr = []
    tr.append(Transaction(credit_type='CR', amount=123.12, transaction_dt=datetime.datetime(2016, 2, 15), bank=BANK,
                          params={'06': 'jabberwocky'}))
    # tr.append(Transaction(credit_type='DR', amount=123.12, transaction_dt=datetime.datetime(2016, 2, 15), bank=BANK,
    #                       params={'06': 'jabberwocky'}))
    for tran in tr:
        st.add_transaction(tran)
    print (st.generate_data(MAPPER[BANK]['format']))




