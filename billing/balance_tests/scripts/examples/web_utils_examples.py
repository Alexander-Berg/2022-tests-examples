# coding: utf-8

from urllib import urlencode
from urlparse import urljoin

from selenium.webdriver.common.by import By

import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.environments as envs
import btestlib.utils as utils
from btestlib.constants import User


class ExampleActsPage(object):
    STATIC_LOCATOR = (By.XPATH, "//input[@name='invoice_eid']", u'имя статического локатора')

    DYNAMIC_LOCATOR = classmethod(utils.Web.dynamic_locator(
        By.XPATH,
        u"//form[@id='ecopies-get-form']/table//td[contains(., '{number}')]",
        u"имя динамического локатора (динамическое значение {number})"))
    # classmethod нужен, чтобы к атрибуту можно было обратиться через класс - ExampleActsPage.DYNAMIC_LOCATOR(...)
    # теоретически его можно и не указывать, но тогда к атрибуту можно будет обращаться только через экземпляр
    #   self.DYNAMIC_LOCATOR(...)
    # Динамический параметр указывается в пути к элементу и при необходимости в имени локатора


    ACTS_TABLE = classmethod(utils.Web.table(
        (By.XPATH, u"//form[@id='ecopies-get-form']/table"),
        column_names=['checkbox', 'act_number', 'factura_number', 'date', 'amount',
                      'tax', 'pay_till', 'invoice_number'],
        name=u"Таблица актов"))

    def __init__(self, driver):
        self.driver = driver

    @classmethod
    def open(cls, driver):
        # метод служит для того чтобы открыть страницу и создать вернуть объект страницы
        # в метод также часто удобно передавать id сущностей, чтобы открывать страницы для них - в примере нет
        page_url = urljoin(envs.balance_env().balance_ci, '/acts.xml')
        driver.get(page_url, name=u"Страница поиска актов")

        return cls(driver)

    @classmethod
    def on_opened(cls, driver):
        # метод служит для того, чтобы инициализировать объект, если страница была уже открыта (напр по нажатию кнопки)
        return cls(driver)

    def use_static_locator(self):
        self.driver.find_element(*ExampleActsPage.STATIC_LOCATOR)
        # для краткости можно вызывать так
        self.driver.find_element(*self.STATIC_LOCATOR)

    def use_dynamic_locator(self, number):
        # в качестве имени аргумента передавать то же что задали в качестве имени параметра в строке формата
        self.driver.find_element(*ExampleActsPage.DYNAMIC_LOCATOR(number=number))
        # для краткости можно вызывать так
        self.driver.find_element(*self.DYNAMIC_LOCATOR(number=number))

    def use_table(self):
        # чтобы начать работать с таблицей когда страница уже окрыта - надо инициализировать объект передав браузер
        acts_table = self.ACTS_TABLE(self.driver)

        # все отображаемые ячейки таблицы как WebElement'ы
        elements = acts_table.elements
        # все отображаемые ячейки таблицы как текст
        values = acts_table.values
        # далее с этими элементами можно работать как с обычным списком словарей,
        # где ключи словарей - colum_names что мы задали

        # но для упрощения добавил несколько служебных методов
        # фильтрованный набор строк. В качестве фильтров передаем именные параметры column_name=value
        invoice_act_rows = acts_table.rows(invoice_number=u'Б-102200089-1')  # вернет все отображаемые акты для счета
        # вернет все отображаемые оплаченные акты для счета
        paid_invoice_act_rows = acts_table.rows(pay_till=u'Оплачен', invoice_number=u'Б-102200089-1')

        # если фильтруем по уникальному полю, то можно использовать метод row
        unic_act_row = acts_table.row(act_number=u'55925299')  # вернет строку конкретного акта
        # в целом от метода пользы нет, кроме минимального сокращения (не надо обращаться к индексу строки)
        # и также ошибки если строка не одна

        # проверку наличия строки можно делать проверяя, что список возвращаемый rows не пуст
        # или что row возвращает не None
        row_present = unic_act_row is not None

        # тоже для укорочения есть метод cell который возвращает конкретную ячейку,
        # он кроме фильтров значения строки еще принимает имя столбца
        act_amount = acts_table.cell(column='amount', act_number=u'55925299')
        # полностью аналогично, но более говорящая ошибка если имя столбца не верное
        same_act_amount = acts_table.row(act_number=u'55925299')['amount']

        # если мы хотим для методов rows, row, cell получить текст а не веб элементы
        cell_value = acts_table.cell(column='amount', act_number=u'55925299', as_elements=False)

        # возвращаемые элементы - это td, если внутри td есть другой элемент не забываем, и нам нужно к нему обратиться
        checkbox = unic_act_row['checkbox'].find_element(By.XPATH, './input')

        # получить доступ к хедерам можно так
        headers = acts_table.headers(as_elements=False)

        # Стоит особо упомянуть ситуацию, когда хедеры таблицы являются ее первой строкой а не в блоке thead
        # это надо учитывать при инициализации
        ExampleActsPage.ACTS_TABLE_EXAMPLE = utils.Web.table(
            (By.XPATH, "//form[@id='ecopies-get-form']/table"),  # локатор корневого элемента таблицы
            # список имен столбцов таблицы - задать можно любые, но потом надо использовать именно их. Перечислить все.
            column_names=['checkbox', 'act_number', 'factura_number', 'date', 'amount', 'tax', 'pay_till',
                          'invoice_number'],
            headers_as_row=True,
            name=u"Таблица актов")

        # и тогда в качестве хэдеров будет возвращен первый столбец
        row_headers = self.ACTS_TABLE_EXAMPLE(self.driver).headers(as_elements=False)

        pass


def test_work_with_page():
    user = User(436363514, 'yb-atst-user-2')
    steps.ClientSteps.link(2458135, user.login)

    with web.Driver(user=user) as driver:
        # открываем явно
        acts_page = ExampleActsPage.open(driver)

        page_url = '{base_url}?{params}'.format(base_url=urljoin(envs.balance_env().balance_ci, '/acts.xml'),
                                                params=urlencode(utils.encode_obj({'invoice_eid': 'Б-102200089-1'})))
        driver.get(page_url)
        # или если уже открыта
        acts_page = ExampleActsPage.on_opened(driver)

        acts_page.use_dynamic_locator(number=u'55925299')
        acts_page.use_static_locator()
        acts_page.use_table()


# -------------------------------------------------------------------------------
# Пример работы с новым форматом страниц для баланса

# Для того, чтобы создать страницу нужно отнаследоваться от класса DefaultPage из balance_web

# Уже реализован метод open, нужно лишь переопределить метод url, если есть необходимость в использовании url и open.
# В противном случае при использовании этих методов будет проброшен NotImplementedError
# url по дефолту принимает *args, **kwargs; open - driver, *args, **kwargs;
# При наследовании можно и почти всегда нужно изменять сигнатуру url для читабельности

# Также опционально можно переопределить поле с описанием странцы (поле класса с названием DESCRIPTION)

from balance.balance_web import DefaultPage


class ExamplePage(DefaultPage):
    DESCRIPTION = u'Пример страницы'

    @classmethod
    def url(cls, example_arg):
        return 'http://example.com/{example_arg}'.format(example_arg=example_arg)


def test_example_page():
    from balance.balance_web import Driver
    with Driver() as driver:
        ExamplePage.open(driver, 'example')
