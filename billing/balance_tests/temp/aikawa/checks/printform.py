# -*- coding: utf-8 -*-
import re

import requests as r
from BeautifulSoup import BeautifulSoup as bs
from lxml import etree

from btestlib import utils


def parse_html_check(html_check):
    print html_check
    root = etree.HTML(html_check)
    tree = etree.ElementTree(root)

    div_elements = tree.xpath('//div')
    text_elements = {div.get('class'): div.text.strip() for div in div_elements if div.text.strip()}
    tables = {table.get('class'): table for table in tree.xpath('//table')}
    first_table_cells = parse_table_cells(tree.xpath('//table[@class="info-table"]')[0])
    first_table = dict([split_with_end_number(cell) for cell in utils.flatten(first_table_cells)[:-1]])
    first_table[u'Дата'] = first_table_cells[1][1]

    receipt_table = parse_table_with_headers(tree.xpath('//table[@class="receipt-table"]')[0])

    totals_table_cells = parse_table_cells(tree.xpath('//table[@class="totals-table"]')[0])
    totals_table = {first_cell: second_cell if second_cell else first_cell for first_cell, second_cell in
                    totals_table_cells}

    fourth_table_cell = parse_table_cells(tree.xpath('//table[@class="info-table"]')[1])
    fourth_table_cell[1][1] = u'fp:' + fourth_table_cell[1][1]  # чтоб дальше в одну строку
    fourth_table = dict([cell.split(u':') for cell in utils.flatten(fourth_table_cell)])

    fifth_table_cells = parse_table_cells(tree.xpath('//table[@class="info-table"]')[2])
    fifth_table = dict([cell.split(u':') for cell in utils.flatten(fifth_table_cells)[
                                                     :-1]])  # функция получает текст содержащийся с самом тэге и всех детях

    return text_elements, first_table, receipt_table, totals_table, fourth_table, fifth_table


def create_dict_from_html(text_elements, first_table, receipt_table, totals_table, fourth_table, fifth_table):
    result_dict = {}
    result_dict['id'] = fourth_table[u'N ФД']
    result_dict['dt'] = first_table[u'Дата']
    result_dict['fp'] = fourth_table[u'fp']
    result_dict['document_index'] = first_table[u'N']
    result_dict['shift_number'] = first_table[u'Смена N']
    result_dict['firm'] = {}
    result_dict['firm']['inn'] = text_elements['firm-inn']
    result_dict['firm']['name'] = text_elements['firm-name']
    result_dict['firm']['reply_email'] = fifth_table[u'Эл. адр. отправителя']
    result_dict['fn'] = {}
    result_dict['fn']['sn'] = fourth_table[u'N ФН']
    result_dict['receipt_content'] = {}
    result_dict['receipt_content']['agent_type'] = totals_table[u'АГЕНТ']
    result_dict['receipt_content']['client_email_or_phone'] = fifth_table[u'Эл. адр. получателя']
    result_dict['receipt_type'] = text_elements['header']
    result_dict['receipt_content']['taxation_type'] = fourth_table[u'СНО']
    result_dict['receipt_calculated_content'] = {}
    result_dict['receipt_calculated_content']['total'] = totals_table[u'Итого']
    result_dict['receipt_calculated_content']['rows'] = {}
    result_dict['receipt_calculated_content']['rows']['qty'] = receipt_table[0][u'Колич. пр.']
    result_dict['receipt_calculated_content']['rows']['price'] = receipt_table[0][u'Цена за ед. пр.']
    result_dict['receipt_calculated_content']['rows']['amount'] = receipt_table[0][u'Сум. пр.']
    result_dict['receipt_calculated_content']['rows']['tax_type'] = receipt_table[0][u'НДС']
    result_dict['receipt_calculated_content']['rows']['text'] = receipt_table[0][
        u'\u041d\u0430\u0438\u043c. \u043f\u0440.']
    result_dict['receipt_calculated_content']['tax_totals'] = {}
    result_dict['receipt_calculated_content']['tax_totals']['tax_type'] = totals_table[
        u'\u0421\u0423\u041c\u041c\u0410 \u041d\u0414\u042110%']
    result_dict['receipt_calculated_content']['totals'] = {}
    result_dict['receipt_calculated_content']['totals']['amount'] = totals_table[
        u'\u042d\u041b\u0415\u041a\u0422\u0420\u041e\u041d\u041d\u042b\u041c\u0418']
    result_dict['ofd'] = {}
    result_dict['ofd']['check_url'] = fifth_table[u'\u0421\u0430\u0439\u0442 \u0424\u041d\u0421']
    result_dict['kkt'] = {}
    result_dict['kkt']['sn'] = fourth_table[u'\u0417\u041d \u041a\u041a\u0422']
    result_dict['kkt']['rn'] = fourth_table[u'N \u041a\u041a\u0422']
    result_dict['kkt']['automatic_machine_number'] = first_table[u'N \u0410\u0412\u0422']
    result_dict['location'] = {}
    result_dict['location']['address'] = text_elements['location']
    return result_dict


def enclosed_text(element):
    text = unicode(element.text or '').strip()
    tail = unicode(element.tail or '').strip()
    # igogor: сорян за рекурсию
    if not element.getchildren():
        return text + tail
    children_text = u''.join([enclosed_text(child) for child in element.getchildren()])
    return element.text.strip() + children_text + element.tail.strip()


# список строк таблицы, каждая строка список текстов ячеек
def parse_table_cells(table):
    return [[enclosed_text(td) for td in tr] for tr in table]


def parse_table_with_headers(table):
    headers = [td.text.strip() for td in table[0]]
    rows = [[td.text.strip() for td in tr] for tr in table[1:]]
    return [dict(zip(headers, row)) for row in rows]


def split_with_end_number(text):
    split = re.findall(u'(.*) (\d+)$', text)
    return split[0] if split else [text]


data = {
    "id": 3358,
    "dt": "2017-06-21 12:59:00",
    "fp": 892710360,
    "document_index": 30,
    "shift_number": 121,
    "ofd_ticket_received": 0,
    "firm": {
        "inn": "7736207511",
        "name": "ООО \"ЯНДЕКС\"",
        "reply_email": "me@yandex-team.ru"
    },
    "fn": {
        "sn": "9999078900005747",
        "model": "ФН-1"
    },
    "receipt_content": {
        "agent_type": "agent",
        "client_email_or_phone": "isupov@yandex-team.ru",
        "firm_inn": "7736207543",
        "payments": [
            {
                "amount": "33300",
                "payment_type": "card"
            }
        ],
        "receipt_type": "income",
        "rows": [
            {
                "payment_type_type": "prepayment",
                "price": "10",
                "qty": "10",
                "tax_type": "nds_18",
                "text": "string 7777777"
            }
        ],
        "taxation_type": "OSN"
    },
    "receipt_calculated_content": {
        "total": "1343400",
        "money_received_total": "100",
        "rows": [
            {
                "qty": "10666",
                "price": "10",
                "payment_type_type": "prepayment",
                "amount": "10440",
                "tax_pct": "18.00",
                "tax_amount": "15.25",
                "tax_type": "nds_18",
                "text": "string 7777777"
            }
        ],
        "tax_totals": [
            {
                "tax_type": "nds_10",
                "tax_pct": "18.00",
                "tax_amount": "15.25"
            }
        ],
        "totals": [
            {
                "payment_type": "card",
                "amount": "100"
            }
        ]
    },
    "ofd": {
        "inn": "7709364346",
        "name": "АО \"ЭНЕРГЕТИЧЕСКИЕ СИСТЕМЫ И КОММУНИКАЦИИ\"",
        "check_url": "nalog.ru"
    },
    "kkt": {
        "sn": "00000000381001543159",
        "rn": "6666660666059469",
        "url": "http://yandex.ru",
        "automatic_machine_number": "6660666"
    },
    "location": {
        "id": "666",
        "address": "119021, Россия, г. Москва, ул. Льва Толстого, д. 16",
        "description": "Стойка номер 666"
    },
    "check_url": "https://check.greed-ts1f.yandex.ru/?n=3358&fn=9999078900005747&fpd=892710360"
}

html_url = 'https://check.greed-ts1f.yandex.ru'

response = r.post(url=html_url, json=data)
print response.text

soup = bs(response.text)  # make BeautifulSoup
text_elements, first_table, receipt_table, totals_table, fourth_table, fifth_table = parse_html_check(response.text)
print create_dict_from_html(text_elements, first_table, receipt_table, totals_table, fourth_table, fifth_table)
