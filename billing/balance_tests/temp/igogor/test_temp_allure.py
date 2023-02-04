# coding: utf-8

import json
import os

import allure

import balance.balance_db as db
import btestlib.reporter as reporter


def test_reporter_attach():
    with reporter.step(u'Mandatory step'):
        reporter.attach(u'Сотня', [1 for i in range(30)])
        reporter.attach(u'Сотня', reporter.pformat([1 for i in range(30)]))
        reporter.attach(u'Сотня', reporter.pformat([1 for i in range(37)]))
    with reporter.step(u'Optional step', allure_=True):
        reporter.attach(u'Optional', {1: 2}, allure_=True)
    os.environ['reporter.remove.optional'] = 'False'
    with reporter.step(u'Optional step 2', allure_=True):
        reporter.attach(u'Optional 2', {3: 4}, allure_=True)


def test_reporter_urls():
    reporter.report_url('https://www.yandex.ru/', u'Yandex main page url')
    reporter.report_url(
        'https://www.yandex.ru/ppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppp',
        u'Yandex main page url')
    reporter.report_urls(u'Label', ('www.google.com', 'Google'), ('www.yandex.ru', 'Yandex'))


def test_allure_report():
    with reporter.step(u'Step0'):
        reporter.attach(u'Step0_Attach0', u'Step0_Attach0')
        with reporter.step(u'Step00'):
            reporter.attach(u'Step00_Attach0', u'Step00_Attach0')
            reporter.attach(u'Step00_Attach1', u'Step00_Attach1')
            reporter.put_data_in_report('pagediff',
                                        json.dumps({'unique_name': 'first_and_unique', 'diff': '11111'}))
        with reporter.step(u'Step01'):
            reporter.attach(u'Step01_Attach0', u'Step01_Attach0')
            with reporter.step(u'Step010'):
                reporter.attach(u'Step010_Attach0', u'Step010_Attach0')

    pass


def test_allure_report_second():
    with reporter.step(u'Step0'):
        reporter.attach(u'Step0_Attach0', u'Step0_Attach0')
        with reporter.step(u'Step00'):
            reporter.attach(u'Step00_Attach0', u'Step00_Attach0')
            reporter.attach(u'Step00_Attach1', u'Step00_Attach1')
        with reporter.step(u'Step01'):
            reporter.attach(u'Step01_Attach0', u'Step01_Attach0')
            with reporter.step(u'Step010'):
                reporter.attach(u'Step010_Attach0', u'Step010_Attach0')
        reporter.put_data_in_report('pagediff',
                                    json.dumps({'unique_name': 'second_and_unique', 'diff': '11111'}))

    pass


def test_allure_report_third():
    with reporter.step(u'Step0'):
        reporter.attach(u'Step0_Attach0', u'Step0_Attach0')
        print u'Первый принт'
        with reporter.step(u'Step00'):
            reporter.attach(u'Step00_Attach0', u'Step00_Attach0')
            reporter.attach(u'Step00_Attach1', u'Step00_Attach1')
            print u'Второй принт'
        with reporter.step(u'Step01'):
            reporter.attach(u'Step01_Attach0', u'Step01_Attach0')
            with reporter.step(u'Step010'):
                reporter.attach(u'Step010_Attach0', u'Step010_Attach0')
                print u'Третий принт'
            raise RuntimeError('runetime error')

    pass


def test_allure_report_fourth():
    with reporter.step(u'Step0'):
        reporter.attach(u'Step0_Attach0', u'Step0_Attach0')
        with reporter.step(u'Step00'):
            reporter.attach(u'Step00_Attach0', u'Step00_Attach0')
            reporter.attach(u'Step00_Attach1', u'Step00_Attach1')
        with reporter.step(u'Step01'):
            reporter.attach(u'Step01_Attach0', u'Step01_Attach0')
            with reporter.step(u'Step010'):
                reporter.attach(u'Step010_Attach0', u'Step010_Attach0')
        reporter.put_data_in_report('pagediff',
                                    json.dumps({'unique_name': 'fourth_and_unique', 'diff': '2222'}))
    assert 0

    pass


def test_logging_allure():
    # reporter.log(u'Залогированное')
    with reporter.step(u'Опциональный', allure_=False):
        with reporter.step(u'Обязательный', log_=False):
            reporter.attach(u'Foo')
            reporter.attach(u'Без логирования', log_=False)
            reporter.attach(u'Обычный без логирования', u'pff', log_=False)
    with reporter.step(u'Cтеп 0'):
        with reporter.step(u'Cтеп 1'):
            reporter.attach(u'Аттач 2', reporter.pformat(dict(zip('abcdefghijk', range(10)))))
            with reporter.step(u'Степ 2'):
                reporter.attach(u'Аттач 3',
                                reporter.pformat(dict(zip(range(10), ['abcdefghijk'] * 10))))
        reporter.attach(u'Аттач 1', reporter.pformat([1, 2, 3, 4, 5, 6]))
        with reporter.step(u'Степ 1'):
            reporter.attach(u'Аттач 2', reporter.pformat({'abcdefghijk': 'sdfkjkasdlfkj'}))
    with reporter.step(u'Cтеп 0'):
        with reporter.step(u'Cтеп 1'):
            reporter.attach(u'Аттач 2', reporter.pformat(dict(zip('abcdefghijk', range(10)))))
            with reporter.step(u'Степ 2'):
                reporter.attach(u'Аттач 3',
                                reporter.pformat(dict(zip(range(10), ['abcdefghijk'] * 10))),
                                oneline=True)
        reporter.attach(u'Аттач 1', reporter.pformat([1, 2, 3, 4, 5, 6]))
        with reporter.step(u'Степ 1'):
            reporter.attach(u'Аттач 2', reporter.pformat({'abcdefghijk': 'sdfkjkasdlfkj'}))
    reporter.attach(u'Аттач только с лейблом')
    reporter.attach(u'Really fucking long' * 20)


def test_reporting():
    with reporter.step(u'Outside step'):
        reporter.attach(u'Outside attach')
    with reporter.reporting(level=reporter.Level.AUTO_STEPS_ONLY):
        with reporter.step(u'Manual step'):
            db.balance().execute(query='SELECT * FROM t_client WHERE id = 13241234')
            with reporter.step(u'Nested Manual step'):
                reporter.attach(u'Nested Manual attach')
    with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
        with reporter.step(u'Manual step'):
            db.balance().execute(query='SELECT * FROM t_client WHERE id = 13241234')
            with reporter.step(u'Nested Manual step'):
                reporter.attach(u'Nested Manual attach')
    with reporter.reporting(level=reporter.Level.MANUAL_ONLY):
        with reporter.step(u'Manual step'):
            db.balance().execute(query='SELECT 1 FROM DUAL')
            with reporter.step(u'Nested Manual step'):
                reporter.attach(u'Nested Manual attach')
    with reporter.reporting(level=reporter.Level.MANUAL_STEPS_ONLY):
        with reporter.step(u'Manual step'):
            db.balance().execute(query='SELECT 1 FROM DUAL')
            with reporter.step(u'Nested Manual step'):
                reporter.attach(u'Nested Manual attach')
    with reporter.reporting(level=reporter.Level.NOTHING):
        with reporter.step(u'Manual step'):
            db.balance().execute(query='SELECT 1 FROM DUAL')
            with reporter.step(u'Nested Manual step'):
                reporter.attach(u'Nested Manual attach')


def test_log_step_levels():
    with reporter.step('A'):
        with reporter.step('B'):
            pass
        reporter.attach('C')
        reporter.attach('D' * 500)

    with reporter.step('1', allure_=True, log_=True):
        with reporter.step('2', allure_=False, log_=True):
            reporter.attach('3', allure_=False)
        with reporter.step('4', allure_=True, log_=False):
            reporter.attach('5')

    with reporter.step('6', allure_=True, log_=False):
        with reporter.step('7', allure_=False, log_=False):
            with reporter.step('8', allure_=True, log_=True):
                reporter.attach('9', log_=False)
        with reporter.step('10'):
            reporter.attach('11')


def test_log_label():
    reporter.attach(u'label', u'attach')
    reporter.attach(u'label2', u'attach2', log_label=False)
    reporter.attach(u'label', log_label=False)


def test_simple_link():
    with allure.step(u'Самая простая ссылка https://www.google.ru/'):
        allure.attach(u'Самая простая ссылка https://www.google.ru/', 'https://www.google.ru/')
