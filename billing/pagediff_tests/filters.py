# coding: utf-8
from datetime import datetime

from btestlib.pagediff import filter_
import btestlib.utils as utils

CURRENT_DT = datetime.now()
SHIFT_DT_5 = utils.Date.shift_date(CURRENT_DT, days=5)
SHIFT_DT_7 = utils.Date.shift_date(CURRENT_DT, days=7)
SHIFT_DT_10 = utils.Date.shift_date(CURRENT_DT, days=10)
MONTH_RU = {1: u'января', 2: u'февраля', 3: u'марта',
            4: u'апреля', 5: u'мая', 6: u'июня',
            7: u'июля', 8: u'августа', 9: u'сентября',
            10: u'октября', 11: u'ноября', 12: u'декабря'}


def contract_edit_filters(dynamic_values=None):
    contract_header_footer_filters = [
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Дата и время в шапке'),
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[3]', attributes=['text_'],
                descr=u'Курс доллара'),
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[3]/br[1]', attributes=['text_'],
                descr=u'Курс евро'),
        filter_(xpath='//body[1]/table[5]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Copyright 1997—2016 Яндекс'),
        filter_(
            xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[1]/form[1]/div[1]/div[16]/span[2]/input[2]',
            attributes=['value'],
            descr=u'Дата начала действия'),
        filter_(
            xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[1]/form[1]/div[1]/div[15]/span[2]/input[2]',
            attributes=['value'],
            descr=u'Дата начала действия'),
        filter_(value='greed-tm'),
        filter_(xpath='//body[1]/div[3]/div[2]/table[1]/tbody[1]', descr=u'Не очень понятный скрытый datepick'),
        filter_(xpath='//div[@id=\'datepick-div\']', attributes=['text_'], descr=u'Даты'),
        filter_(xpath='//tr[@class=\'datepick-days-row\']', attributes=['text_'], descr=u'Даты'),
    ]

    checkbox_date_filters = [
        # эти фильтры убирают значения даты для незачеканых чекбоксов: Подписан по факсу и т.д.
        # т.к. в них проставляется текущая дата до тех пор пока их не зачекают
        filter_(
            xpath="//input[contains(@name, 'is-signed-dt') or contains(@name, 'is-faxed-dt') "
                  "or contains(@name, 'sent-dt-dt') or contains(@name, 'is-cancelled-dt') "
                  "or contains(@name, 'deal-passport-dt') or contains(@name, 'is-suspended-dt')"
                  "or contains(@name, 'act-signed-dt')]",
            attributes=['value'], descr=u'Инпуты дат всех чекбоксов')]
    filters = contract_header_footer_filters + checkbox_date_filters  # + garbage_filters

    if dynamic_values is not None:
        return filters + [filter_(descr=u'Динамическое значение', value=unicode(value)) for value in dynamic_values]
    else:
        return filters


def contract_filters():
    filters = [
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Дата и время в шапке'),
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[3]', attributes=['text_'],
                descr=u'Курс доллара'),
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[3]/br[1]', attributes=['text_'],
                descr=u'Курс евро'),
        filter_(xpath='//body[1]/table[5]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Copyright 1997—2016 Яндекс'),
        filter_(value='greed-tm')
    ]

    return filters


def invoice_ci_filters(dynamic_values):
    filters = [
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[6]/table[1]/tbody[1]/tr[1]/td[2]/a[1]',
                attributes=['href'], descr=u'Значение ссылки кнопки Выход'),
        filter_(descr=u'Хэш на кнопке Изменить реквизиты', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[3]/div[1]/div[5]/form[1]/input[2]'),
        filter_(descr=u'Хэш на кнопке Изменить реквизиты', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[4]/div[1]/div[5]/form[1]/input[2]'),
        filter_(descr=u'Хэш Выслать на e-mail', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[8]/form[1]/input[3]'),
        filter_(descr=u'Хэш Выслать на e-mail', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[9]/form[1]/input[3]'),
        filter_(descr=u'Хэш Выслать на e-mail', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[10]/form[1]/input[3]'),
        filter_(descr=u'Хэш на кнопке Изменить реквизиты', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[2]/div[1]/div[5]/form[1]/input[2]'),
        filter_(descr=u'Хэш на кнопке Оплатить', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[2]/form[1]/input[2]'),
        filter_(descr=u'Хэш на ссылке Изменить способ оплаты', attributes=['href'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[3]/a[1]'),
        filter_(descr=u'Хэш на ссылке Изменить способ оплаты', attributes=['href'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[2]/a[1]'),
        filter_(descr=u'Дата', attributes=['text_'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/table[1]/tbody[1]/tr[2]/td[1]'),
        filter_(descr=u'Дата', attributes=['text_'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[3]/table[1]/tbody[1]/tr[2]/td[1]'),
        filter_(descr=u'Дата', attributes=['text_'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[2]/table[1]/tbody[1]/tr[2]/td[1]'),
        filter_(xpath='//body[1]/div[4]/table[1]/tbody[1]/tr[1]/td[6]/div[1]', attributes=['text_'],
                descr=u'© 2001–2016'),
        filter_(xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[5]/table[1]/tbody[1]/tr[4]/td[2]/div[1]',
                attributes=['text_'],
                descr=u'Курс валюты'),
        filter_(value='greed-tm'),
        filter_(xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[4]/table[1]/tbody[1]/tr[4]/td[2]/div[1]',
                attributes=['text_'], descr=u'Курс валюты'),
        filter_(xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[7]/form[1]/input[3]',
                attributes=['value'], descr=u'Какой-то хэш'),
        filter_(attributes=['src'], xpath='/html/body/table[2]/tbody/tr/td/div[7]/form/div[1]/img',
                descr=u'Картинка капчи'),
        filter_(attributes=['src'], xpath='/html/body/table[2]/tbody/tr/td/div[9]/form/div[1]/img',
                descr=u'Картинка капчи'),
        filter_(attributes=['src'], xpath='/html/body/table[2]/tbody/tr/td/div[8]/form/div[1]/img',
                descr=u'Картинка капчи'),
        filter_(attributes=['src'], xpath='/html/body/table[2]/tbody/tr/td/div[10]/form/div[1]/img',
                descr=u'Картинка капчи'),
        filter_(attributes=['value'], xpath='/html/body/table[2]/tbody/tr/td/div[7]/form/div[2]/input[1]',
                descr=u'Код капчи'),
        filter_(attributes=['value'], xpath='/html/body/table[2]/tbody/tr/td/div[9]/form/div[2]/input[1]',
                descr=u'Код капчи'),
        filter_(attributes=['value'], xpath='/html/body/table[2]/tbody/tr/td/div[8]/form/div[2]/input[1]',
                descr=u'Код капчи'),
        filter_(attributes=['value'], xpath='/html/body/table[2]/tbody/tr/td/div[10]/form/div[2]/input[1]',
                descr=u'Код капчи'),
        filter_(attributes=['text_'], xpath='/html/body/table[2]/tbody/tr/td/div[1]/p/br[3]',
                descr=u'Расчетный счет'),
        filter_(attributes=['text_'], xpath='/html/body/table[2]/tbody/tr/td/div[1]/p/br[4]',
                descr=u'Адрес банка'),
        filter_(attributes=['text_'], xpath='/html/body/table[2]/tbody/tr/td/div[1]/p/br[5]',
                descr=u'Корсчет банка'),
        filter_(attributes=['text_'], xpath='/html/body/table[2]/tbody/tr/td/div[1]/p/br[6]',
                descr=u'БИК банка'),
    ]
    dynamic_values_filters = [filter_(descr=u'Динамическое значение', value=unicode(value)) for value in dynamic_values]
    return filters + dynamic_values_filters


def invoice_publish_filters(dynamic_values, external_invoice_id=None, external_contract_id=None):
    dynamic_values_filters = [filter_(descr=u'Динамическое значение', value=unicode(value)) for value in dynamic_values]
    filters = [filter_(xpath='//body[1]/table[4]/tbody[1]/tr[2]/td[1]/p[1]/span[1]',
                       attributes=['text_'], descr=u'Invoice Date'),
               filter_(xpath='//body[1]/table[4]/tbody[1]/tr[2]/td[3]/p[1]/span[1]',
                       attributes=['text_'], descr=u'Due Date'),
               filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[1]/p[2]/span[1]',
                       attributes=['text_'], descr=u'Дата для пф с 17 офертой'),
               filter_(xpath='//body[1]/table[4]/tbody[1]/tr[1]/td[1]/p[1]/span[1]/b[1]',
                       attributes=['text_'], descr=u'Счет ... от ...'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[2]/div[19]',
                       attributes=['text_'], descr=u'СЧЕТ №... от ...'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[2]/div[18]',
                       attributes=['text_'], descr=u'СЧЕТ-ОФЕРТА №... от ...'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[1]', descr=u'Не смтрим на findbar, к оферте не относится'),
               filter_(xpath='//body[1]/div[1]/div[1]', descr=u'Не смтрим на sidebarContainer, к оферте не относится'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[2]',
                       descr=u'Не смтрим на secondaryToolbar, к оферте не относится'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[3]',
                       descr=u'Не смтрим на toolbarContainer, к оферте не относится'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[2]/div[14]',
                       descr=u'Не проверяем адрес/счет банка'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[2]/div[15]',
                       descr=u'Не проверяем адрес/счет банка'),
               filter_(xpath='//body[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[2]/div[18]',
                       descr=u'Не проверяем адрес/счет банка'),
               filter_(attributes=['style', 'text_'], xpath='/html/body/div[1]/div[2]/div[4]/div/div[1]/div[2]/div[11]',
                       descr=u'Не проверяем адрес банка'),
               filter_(attributes=['style', 'text_'], xpath='/html/body/div[1]/div[2]/div[4]/div/div[1]/div[2]/div[12]',
                       descr=u'Не проверяем адрес банка'),
               filter_(attributes=['text_', 'style'], xpath='/html/body/div[1]/div[2]/div[4]/div/div[1]/div[2]/div[21]',
                       descr=u'Payment Terms'),
               filter_(attributes=['text_', 'style'], xpath='/html/body/div[1]/div[2]/div[4]/div/div[1]/div[2]/div[22]',
                       descr=u'Payment Terms'),
               filter_(attributes=['text_'], xpath='/html/body/div[1]/div[2]/div[4]/div/div[1]/div[2]/div[23]',
                       descr=u'Дата'),
               filter_(attributes=['href'], xpath='/html/body/div[1]/div[2]/div[2]/div/a', descr=u'Незначащая ссыль'),
               filter_(attributes=['href'], xpath='/html/body/div[1]/div[2]/div[3]/div/div[1]/div[2]/a',
                       descr=u'Незначащая ссыль'),
               filter_(value='greed-tm'),
               ]

    date_filter = [
        filter_(value=CURRENT_DT.strftime('%d.%m.%Y')),
        filter_(value=u'{} {} {} г.'.format(CURRENT_DT.day, MONTH_RU[CURRENT_DT.month], CURRENT_DT.year)),
        filter_(value=CURRENT_DT.strftime('%m/%d/%Y')),
        filter_(value=SHIFT_DT_10.strftime('%m/%d/%Y')),
        filter_(value=SHIFT_DT_5.strftime('%d.%m.%Y')),
        filter_(value=SHIFT_DT_7.strftime('%d.%m.%Y')),
        filter_(value=SHIFT_DT_10.strftime('%d.%m.%Y')),
        filter_(
            value=u'СЧЕТ No {} от {} {} {} г.'.format(external_invoice_id, CURRENT_DT.day, MONTH_RU[CURRENT_DT.month],
                                                      CURRENT_DT.year)),
        filter_(
            value=u'Счет на оплату No {} от {} {} {} г.'.format(external_invoice_id, CURRENT_DT.day,
                                                                MONTH_RU[CURRENT_DT.month],
                                                                CURRENT_DT.year)),
        filter_(
            value=u'Договор No {} от {}'.format(external_contract_id, CURRENT_DT.strftime('%d.%m.%Y'))),
    ]
    return filters + dynamic_values_filters + date_filter


def paypreview_filters(dynamic_values):
    filters = [
        filter_(descr=u'Хэш на кнопке Выставить счет (paypreview)', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[3]/table[2]/tbody[1]/tr[1]/td[1]/form[1]/input[1]'),
        filter_(descr=u'Хэш на кнопке Выставить счет (paypreview)', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[2]/table[2]/tbody[1]/tr[1]/td[1]/form[1]/input[1]'),
        filter_(descr=u'Хэш на кнопке Выставить счет (paypreview)', attributes=['value'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[4]/table[2]/tbody[1]/tr[1]/td[1]/form[1]/input[1]'),
        filter_(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[6]/table[1]/tbody[1]/tr[1]/td[2]/a[1]',
                attributes=['href'], descr=u'Значение ссылки кнопки Выход'),
        filter_(descr=u'Ссылка Изменить способ оплаты или плательщика(paypreview)', attributes=['href'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[3]/table[1]/tbody[1]/tr[1]/td[4]/div[1]/a[1]'),
        filter_(descr=u'Ссылка Изменить способ оплаты или плательщика(paypreview)', attributes=['href'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[2]/table[1]/tbody[1]/tr[1]/td[4]/div[1]/a[1]'),
        filter_(descr=u'Ссылка Изменить способ оплаты или плательщика(paypreview)', attributes=['href'],
                xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[4]/table[1]/tbody[1]/tr[1]/td[4]/div[1]/a[1]'),
        filter_(descr=u'Ссылка переключения языка(paypreview)', attributes=['href'],
                xpath='//body[1]/div[4]/table[1]/tbody[1]/tr[1]/td[3]/div[1]/div[1]/table[1]/tbody[1]/tr[1]/td[1]/div[1]/div[1]/ul[1]/li[2]/div[1]/a[1]'),
        filter_(descr=u'Ссылка переключения языка(paypreview)', attributes=['href'],
                xpath='//body[1]/div[4]/table[1]/tbody[1]/tr[1]/td[3]/div[1]/div[1]/table[1]/tbody[1]/tr[1]/td[1]/div[1]/div[1]/ul[1]/li[3]/div[1]/a[1]'),
        filter_(xpath='//body[1]/div[4]/table[1]/tbody[1]/tr[1]/td[6]/div[1]', attributes=['text_'],
                descr=u'© 2001–2016'),
        filter_(xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[2]/table[1]/tfoot[1]/tr[4]/td[2]/div[1]',
                attributes=['text_'], descr=u'Курс валюты'),
        filter_(attributes=['text_'], xpath='/html/body/table[2]/tbody/tr/td[3]/div[3]/div/table/tfoot/tr[4]/td[2]/div',
                descr=u'Курс валюты'),
        filter_(xpath='//body[1]/div[2]/ul[1]/li[1]/span[1]',
                attributes=['text_'], descr=u'Дата на панели разработчика'),
        filter_(xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[2]/a[1]', attributes=['href'],
                descr=u'Ссыль на изменение способа оплаты/плательщика'),
        filter_(xpath='//body[1]/table[2]/tbody[1]/tr[1]/td[3]/div[5]/div[2]/form[1]/input[1]', attributes=['value'],
                descr=u'Хэш на кновке выставления счета'),
        filter_(attributes=['value'], xpath='/html/body/table[2]/tbody/tr/td[3]/div[5]/div/form/input[1]',
                descr=u'Хэш на кновке выставления счета'),
        filter_(attributes=['value'], xpath='/html/body/table[2]/tbody/tr/td[3]/div[6]/div[2]/form/input[1]',
                descr=u'Хэш на кновке выставления счета'),
        filter_(xpath='/html/body/div[3]/div/table/tbody/tr/td[2]/div/span',
                descr=u'Не проверяем верхнюю плашку, которая может не успевать подгружаться'),
        filter_(attributes=['style'], xpath='/html/body/div[3]', descr=u'Стиль верхней плашки'),
        filter_(value='greed-tm')
    ]
    dynamic_values_filters = [filter_(descr=u'Динамическое значение', value=unicode(value)) for value in dynamic_values]

    return filters + dynamic_values_filters


def check_mobile_filters():
    filters = [
        filter_(xpath='//body[1]/div[1]/table[1]/tbody[1]/tr[1]/td[1]', attributes=['text_'],
                descr=u'Номер чека'),
        filter_(xpath='//body[1]/div[1]/table[1]/tbody[1]/tr[2]/td[1]', attributes=['text_'],
                descr=u'Номер смены'),
        filter_(xpath='//body[1]/div[1]/table[1]/tbody[1]/tr[2]/td[2]', attributes=['text_'],
                descr=u'Дата чека'),
        filter_(xpath='//body[1]/div[1]/div[11]/table[2]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Номер ФД'),
        filter_(xpath='//body[1]/div[1]/div[11]/table[4]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Номер ФП'),
        filter_(xpath='//body[1]/div[1]/div[13]/img[1]', attributes=['src'],
                descr=u'QR-код'),
        filter_(value='greed-tm')]

    return filters


def check_html_filters():
    filters = [
        filter_(xpath='//body[1]/div[1]/table[1]/tbody[1]/tr[1]/td[1]', attributes=['text_'],
                descr=u'Номер чека'),
        filter_(xpath='//body[1]/div[1]/table[1]/tbody[1]/tr[2]/td[1]', attributes=['text_'],
                descr=u'Номер смены'),
        filter_(xpath='//body[1]/div[1]/table[1]/tbody[1]/tr[2]/td[2]', attributes=['text_'],
                descr=u'Дата чека'),
        filter_(xpath='//body[1]/div[1]/table[4]/tbody[1]/tr[1]/td[2]', attributes=['text_'],
                descr=u'Номер ФД'),
        filter_(xpath='//body[1]/div[1]/table[4]/tbody[1]/tr[2]/td[2]', attributes=['text_'],
                descr=u'Номер ФП'),
        filter_(value='greed-tm')]

    return filters


def check_pdf_filters():
    filters = [
        ##надо добавить фильтры, когда станет понятно, какие именно
        filter_(value='greed-tm')
    ]

    return filters
