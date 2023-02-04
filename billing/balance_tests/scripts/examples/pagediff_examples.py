# coding: utf-8

from datetime import datetime

import balance.balance_db as db
import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.constants as constants
import btestlib.pagediff as pagediff
import btestlib.utils as utils
from btestlib.pagediff import Filter

u'''Работа с HTML отчетом!  НЕ ПЕРЕМЕЩАТЬ КОММЕНТАРИЙ. ОН ДОЛЖЕН БЫТЬ СРАЗУ ПОСЛЕ ИМПОРТОВ!

Открываем HTML отчет из аттача.
Настоятельно рекомендуется открывать в Хроме или Яузере.
В них консоль разработки лучше всего ищет и отображает комментарии в HTML.

Перед каждым элементом в котором было обнаружено различие
и перед каждым элементом который был отфильтрован в html страницы будет добавлен комментарий.
Шагая по этим комментариям можно найти в теле страницы все расхождения, что наглядно.

Комментарии можно искать точно также как и элементы через xpath
$x("//comment()[contains(., 'pagediff-edit')]")  -  селектор для изменившихся элементов
$x("//comment()[contains(., 'pagediff-insert')]")  -  для добавившихся элементов
$x("//comment()[contains(., 'pagediff-delete')]")  -  для пропавших
$x("//comment()[contains(., 'pagediff-move')]") - дла передвинутых (на одном уровне)
$x("//comment()[contains(., 'filtered-edit')]")  -  отфильтрованные изменения
$x("//comment()[contains(., 'filtered-insert')]")  -  отфильтрованные вставки
$x("//comment()[contains(., 'filtered-delete')]")  -  отфильтрованные удаления
$x("//comment()[contains(., 'filtered-move')]") - отфильтрованные перемещения

Ну и конечно можно посмотреть все неотфильтрованные изменения всех типов вместе
$x("//comment()[contains(., 'pagediff-')]")
И отфильтрованные всех типаов
$x("//comment()[contains(., 'filtered-')]")

Можно найти элементы отфильтрованные по конкретному фильтру по пути, значению, описанию, типу и т.д.
$x("//comment()[contains(., 'pagediff-removed') and contains(., '//body[1]/table[3]/tbody[1]/tr[1]')]")  -  по пути

В качестве исходного для отчета берется html текущей страницы, а не эталонной.
Поэтому комментарий о добавившемся элементе не содержит данных,
комментарий о пропавшем элементе находится на месте где этот элемент был и содержит его копию,
комментарий об изменившемся элементе находится перед ним и содержит элемент из страницы эталона.
Наиболее уязвимое место здесь кмк пропавший элемент, т.к. если пропал целый блок хз как это отобразится - todo.

Если вы всеже хотите работать в фаерфоксе или фаербаге это тоже можно. К каждому селектору нужно добавить
/following-sibling::*
Тогда селектор вернет список элементов после комментария, первый из которых тот что нам нужен
$x("//comment()[contains(., 'pagediff-changed')]/following-sibling::*") - измененные элементы

Важно понимать, что для формирования HTML отчета страницу приходится парсить через lxml в объект и обратно.
Несмотря на то что структуру HTML мы не меняем, только добавляем комментарии, при парсинге она может поменяться.
Я не нашел примеров, но теоретически это возможно - имейте в виду.
Когда сомневаетесь - сравните отчет с исходными html-инами страниц, которые тоже приаттачены в аллюр.

Видео с использованием https://jing.yandex-team.ru/files/igogor/screencast_2017-01-25_18-15-36.mp4
'''


def test_contract_example():
    page_url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=293231'
    # todo-igogor процесс получения уникального имени можно спионерить у кэша. Пока пусть будет явно.
    unique_name = 'test_flexible_contract_with_termination_collateral'

    # Если ты захочешь вручную перегенерить эталон, то это можно сделать передав имя теста в переменную окружения.
    # Такая же переменная окружения будет на тимсити. И ее в целом можно задать в идее, но так проще.
    # os.environ['pagediff.rebuild'] = unique_name

    # Метод получения эталона. Хранит html, скриншот и координаты видимых элементов для отчета.
    # Если эталон не был сохранен - собирает текущее состояние страницы и сохраняет в хранилище.
    # Если эталон был сохранен - получит из хранилища.
    # Если передана переменная окружения pagediff.rebuild - пересоберет и сохранит.
    # expected_page = pagediff.get_etalon(unique_name=unique_name, url=page_url)

    # получаем данные текущей страницы - html и скриншот
    # actual_page = pagediff.get_page_data(url=page_url)

    # эти фильтры придется применять во всех страница договоров поэтому их можно вынести куда-нибудь как константу
    contract_header_filters = [
        Filter.Xpath(xpath="//div[@id='col-orig-form']//option[contains(., 'CreatedByScript')]",
                     descr=u'Опции выпадающего списка начальных условий договора без осмысленного значения'),
        Filter.Attributes(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[2]', attributes=['text'],
                          descr=u'Дата и время в шапке'),
        Filter.Attributes(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[3]', attributes=['text'],
                          descr=u'Курс доллара'),
        Filter.Attributes(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[3]/br[1]', attributes=['text'],
                          descr=u'Курс евро')
    ]

    contract_checkbox_date_filters = [
        # эти фильтры убирают значения даты для незачеканых чекбоксов: Подписан по факсу и т.д.
        # т.к. в них проставляется текущая дата до тех пор пока их не зачекают
        Filter.Attributes(descr=u'Инпут даты чекбокса Подписан по факсу договора', attributes=['value'],
                          xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[1]/form[1]/div[3]/div[4]/span[2]/input[2]'),
        Filter.Attributes(descr=u'Инпут даты чекбокса Отправлен договора', attributes=['value'],
                          xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[1]/form[1]/div[3]/div[6]/span[2]/input[2]'),
        Filter.Attributes(
            xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[1]/form[1]/div[3]/div[7]/span[2]/input[2]',
            attributes=['value'], descr=u'Инпут даты чекбокса Аннулирован договора'),
    ]
    # collateral_checkbox_date_filters = [
    #     Filter.Attributes(descr=u'Инпут даты чекбокса Подписан формы создания допсоглашения', attributes=['value'],
    #                       xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[2]/form[1]/div[3]/div[5]/span[2]/input[2]'),
    #     Filter.Attributes(descr=u'Инпут даты чекбокса Подписан по факсу формы создания допсоглашения',
    #                       attributes=['value'],
    #                       xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[2]/form[1]/div[3]/div[4]/span[2]/input[2]'),
    #     Filter.Attributes(
    #         xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[2]/form[1]/div[3]/div[6]/span[2]/input[2]',
    #         attributes=['value'], descr=u'Инпут даты чекбокса Отправлен формы создания допсоглашения'),
    #     Filter.Attributes(descr=u'Инпут даты чекбокса Аннулирован формы создания допсоглашения', attributes=['value'],
    #                       xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[2]/form[1]/div[3]/div[7]/span[2]/input[2]'),
    #     Filter.Attributes(descr=u'Инпут даты чекбокса Подписан по факсу первого ДС', attributes=['value'],
    #                       xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[3]/form[1]/div[3]/div[4]/span[2]/input[2]'),
    #     Filter.Attributes(descr=u'Инпут даты чекбокса Отправлен первого ДС', attributes=['value'],
    #                       xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[3]/form[1]/div[3]/div[6]/span[2]/input[2]'),
    #     Filter.Attributes(descr=u'Инпут даты чекбокса Аннулирован первого ДС', attributes=['value'],
    #                       xpath='//body[1]/table[3]/tbody[1]/tr[1]/td[1]/div[6]/div[3]/form[1]/div[3]/div[7]/span[2]/input[2]'),
    # ]

    collateral_checkbox_date_filters = [
        Filter.Attributes(descr=u'Инпуты дат чекбоксов для всех ДС', attributes=['value'],
                          xpath="//input[contains(@name, 'col-') and contains(@name, '-dt')]")]

    all_checkbox_date_filters = [
        Filter.Attributes(descr=u'Инпуты дат всех чекбоксов', attributes=['value'],
                          xpath="//input[contains(@name, 'is-signed-dt') or contains(@name, 'is-faxed-dt') "
                                "or contains(@name, 'sent-dt-dt') or contains(@name, 'is-cancelled-dt')]",
                          except_xpaths=["//input[@name='is-faxed-dt']"])
    ]

    all_current_dates = [
        Filter.Value(descr=u'Все текущие даты в value в iso формате',
                     value=utils.Date.date_to_iso_format(utils.Date.nullify_time_of_date(datetime.now())),
                     except_xpaths=["//input[@name='is-faxed-dt']"])
    ]

    pagediff.pagediff(unique_name=unique_name, url=page_url,
                      filters=contract_header_filters + all_current_dates)


def test_creating_contracts_with_stored_etalon():
    test_unique_name = 'test_creating_contracts_with_stored_etalon_example'

    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, constants.PersonTypes.UR.code)

    to_iso = utils.Date.date_to_iso_format
    NOW = datetime.datetime.now()
    HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
    HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
    contract_id, external_id = steps.ContractSteps.create_contract_new('opt_agency', {'CLIENT_ID': client_id,
                                                                                      'PERSON_ID': person_id,
                                                                                      'SERVICES': [7],
                                                                                      'PAYMENT_TYPE': 3,
                                                                                      'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                      'PERSONAL_ACCOUNT_FICTIVE': 0
                                                                                      })
    collateral_ids = [res['id'] for res in db.balance().execute(
        query='SELECT id from T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID = {}'.format(contract_id))]
    client_name = db.balance().execute(query='SELECT name FROM T_CLIENT WHERE id = {}'.format(client_id),
                                       single_row=True)['name']

    contract_url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=' + str(contract_id)

    pagediff.pagediff(unique_name=test_unique_name, url=contract_url,
                      filters=[Filter.Attributes(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[2]', attributes=['text'],
                                                 descr=u'Дата и время в шапке')] +
                              [Filter.Value(value=str(value)) for value
                               in [contract_id, external_id, person_id, client_id, client_name] + collateral_ids])
    pass


def test_oferta_example():
    page_url = 'https://balance.greed-tm1f.yandex.ru/invoice.xml?invoice_id=67901587'

    # если рассматривать всю страницу, то в урле кнопки Выход будут динамические данные
    filters = [Filter.Attributes(xpath='//body[1]/table[1]/tbody[1]/tr[1]/td[6]/table[1]/tbody[1]/tr[1]/td[2]/a[1]',
                                 attributes=['href'], remove=False, descr=u'Значение ссылки кнопки Выход')]

    # добавил параметр blocks (опциональный) - список xpath'ов,
    # будем сравнивать только элементы внутри этих блоков (и сами элементы блоков)
    # добавил параметр prepare_page (опциональный) - функция
    # открывает страницу и выполняет действия с ней, чтобы можно было сравнивать не только состояние страницы после загрузки
    # конкретно в этом примере нужно, т.к. на странице счета данные грузятся динамически аяксом.
    pagediff.pagediff(unique_name='test_oferta_example_market', url=page_url,
                      blocks=[web.ClientInterface.InvoicePage.OFERTA_TEXTAREA[1]],
                      prepare_page=lambda driver_, url_: web.ClientInterface.InvoicePage.open_url(driver_, url_))
