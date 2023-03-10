# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import balance.balance_api as api
import btestlib.reporter as reporter

'''
Для того чтобы иметь легкий контроль над поведением отчета.
А также для того чтобы функции логирования и репортинга жили в одном модуле, для упрощения импортов.
Был заведен модуль btestlib.reporter
Модуль содержит все методы модуля allure, методы для логирования, обобщающие методы для репорта сложных объектов,
типа ссылок, картинок и т.д. Плюс утилитные методы для работы с кишками аллюра, на которые лучше не смотреть.
'''

# С помощью --allure_features можно фильтровать запускаемые тесты
# Примеры:
# 1) для запуска только первого теста --allure_features=test1
# 2) для запуска первых двух тестов --allure_features=test1,test2
# 3) для запуска всех --allure_features=common_feature

# Маркировка всех тестов фичей
pytestmark = [
    reporter.feature('common_feature')
]


# Обычная маркировка теста
@reporter.feature('test1')
def test_first():
    assert 1 == 1


@reporter.feature('test2')
def test_second():
    assert 1 == 1


def test_unmarked():
    assert 1 == 1


# Так же еще существуют story ("Подфичи") для выделения групп тестов внутри feature
# Фильтровать по story можно с помощью --allure_stories, причем он спокойно комбинируется с --allure_features
# При этом произойдет объединение тестов, которые подходят для allure_features и для allure_stories
# Примеры:
# 1) для запуска первого теста --allure_features=test1 (коллизии с feature при этом не будет)
# 2) для запуска всех тестов кроме в файле, кроме помеченного только common_feature
#    --allure_features=test1,test2 --allure_stories=test1,test2

@reporter.feature('big_feature')
@reporter.story('test1')
def test_story_1():
    assert 1 == 1


@reporter.feature('big_feature')
@reporter.story('test2')
def test_story_2():
    assert 1 == 1


def test_manage_automatic_reporting():
    """
    Мы сделали так что все запросы к апи и базе попадаются в отчет автоматически
    Иногда это бывает неудобно т.к. некоторые запросы слишком большие, а другие просто не нужны
    Поэтому я сделал возможность в конкретном месте кода настройть какая информация о вызове метода попадет в лог и аллюр
    Этот контекст менеджер управляет только автоматическим репортингом через ReportingCallable.
    На обычные лог, степы и аттачи не влияет
    Если передать в запуск переменную окружения reporter.levels=False то этот контекст менеджер будет проигнорирован и
    в отчет попадет все.
    """
    # в данном примере в лог и отчет попадет информация о том что метод был вызван в одну строчку
    with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
        csv_data = api.medium().GetDistributionSerpHits(datetime.now(), datetime.now())


def test_smart_attach():
    """
    Чтобы не плодить слишком большего кол-ва файлов аттачей было решено недлинные аттачи прикреплять в отчет как степ
    Чтобы не думать головой какой аттач слишком длинный я написал метод, который понимает это сам
    Также в этот метод в качестве аттача можно передавать объект, а не тольк строку.
    Он будет автоматически приведен к красивой строке и выведен.
    """
    with reporter.step(u'Описание'):
        reporter.attach(u'Описание', [u'Объект или строка с данными'])
    '''
    Также в методы добавлены параметры управления попадет ли текст в лог и в сам аллюр
    Если параметры не передаются явно, то по умолчанию попадут
    '''
    # log_ = False - ни степ, ни аттач не попадет в лог, но попадет в аллюр
    with reporter.step(u'Описание', log_=False):
        reporter.attach(u'Описание', [u'Объект или строка с данными'], log_=False)

    # allure_ = False, log_ не задан -  ни степ, ни аттач не попадет ни в лог, ни в аллюр
    # Имеет смысл только для использования с вычисляемым переменным условием показа. В основном в утилитах
    with reporter.step(u'Описание', allure_=lambda c: c == True):
        reporter.attach(u'Описание', [u'Объект или строка с данными'], allure_=lambda d: d == False)

    # если параметры переданы по отдельности, то между ними нет связи
    # allure_ = False, log_ = True - не попадет в аллюр, но попадет в лог
    with reporter.step(u'Описание', allure_=False, ):
        reporter.attach(u'Описание', [u'Объект или строка с данными'], allure_=lambda d: d == False)

    '''
    можно управлять тем выведется ли аттач как аттач или как степ
    '''
    # длина лейбла + аттача < maxlen - выведется степом, иначе аттачем
    reporter.attach(label=u'f' * 10, attachment=u'f' * 89, maxlen=100)
    reporter.attach(u'f' * 10, u'f' * 89, maxlen=0)  # гарантировано выведется аттачем
    reporter.attach(u'f' * 10, u'f' * 89, maxlen=-1)  # гарантировано выведется степом

    '''
    Также в метод добавлен параметр для вывода в одну строку даже если в форматировании есть \n.
    Все превышающее maxlen будет обрезано. Это можно заметить по ... в конце строки
    '''
    reporter.attach(u'f' * 1000, maxlen=100, oneline=True)  # выведет только 100 букв f в одну строку
    reporter.attach(u'f' * 1000, maxlen=-1, oneline=True)  # выведет строку полностью в одну строку
    reporter.attach(u'f' * 1000, oneline=True)  # выведет reporter.MAXLEN символов в одну строку


def test_logging():
    """
    Для принтинга и логирования есть упрощенный метод reporter.log(), а также метод доступа к логгеру по умолчанию
    reporter.logger()
    """
    # ВАЖНО: никогда не используйте print!!! Его очень трудно выпиливать и невозможно декорировать
    # Чтобы сделать принт
    reporter.log(u'Принтим это сообщение')  # аналогично reporter.logger().debug()
    # Или если хочется управлять уровнем логгинга можно получить доступ к логгеру напрямую
    reporter.logger().info(u'Принтим это сообщение')

    # Также не забываем, что сейчас, то что аттачится тоже попадает в лог
    # Для этого специально была сделана возможность вызвать аттач только с одним аргументом
    # Отличие от reporter.log() - будет добавлен отступ в соответствие со степом, в котором вызвали
    reporter.attach(u'Сообщение попадет в лог и в аллюр тоже!')


def test_pretty_print_unicode():
    """
    Стандартный pprint.pformat метод имеет серьезный недостаток в том что выводит юникод в виде \u кодов
    Чтобы выводить нормально в reporter добавлен свой претти принтер
    :return:
    """
    pretty_object_str = reporter.pformat([1, 2, 3, 4])
