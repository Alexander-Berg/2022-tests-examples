# coding: utf-8
import pytest

from btestlib import reporter

# def test_links_as_text():
#     with allure.step(u'Степчик'):
#         allure.attach('https://www.yandex.ru/', u'Ссылка на морду')


htmlnik = '''
<html><head><meta charset="utf-8"></head>
<body>
  <div>
    <a href="https://www.yandex.ru/">Yandex main page url</a>
  </div>
</body>
</html>'''


#
# def test_attach_url():
#     with allure.step(u'Крепе урл: https://www.yandex.ru/'):
#         allure.attach('https://www.yandex.ru/', 'https://www.yandex.ru/')
#
#
# def test_attach_url_list():
#     with allure.step(u'Аттачим список урлов'):
#         allure.attach(u'zozo:/www.yandex.ru/\nhttps://google.com/\n' +
#                       u'fffffffff\nюююююююю', u'Список урлов', allure.attachment_type.URI_LIST)
#
#
# def test_attach_html():
#     with allure.step(u'Крепе html.'):
#         allure.attach(htmlnik, name=u'Конкретно html', attachment_type=allure.attachment_type.HTML)


def test_report_urls():
    with reporter.step(u'Репортим один урл'):
        reporter.report_url(u'Яндекс', 'https://www.yandex.ru/')
    with reporter.step(u'Репортим несколько урлов'):
        reporter.report_urls(u'Несколько урловdd', (u'Яндекс', 'https://www.yandex.ru/'),
                             (u'Гугол', 'https://www.google.ru/'))


def test_stdout():
    print 'hyint asdfasdfasdf lkajsdfoiqwejrqwer'


@pytest.mark.xfail(reason='Mya xfail messatsu')
def test_xfail_fail():
    raise IndentationError('Mya error message')


@pytest.mark.xfail(reason='Mya xfail messatsu')
def test_xfail_pass():
    pass


@pytest.mark.xfail(True, reason='Mya xfail messatsu')
def test_xfail_condition_true():
    raise SyntaxError('Mya error message')


@pytest.mark.xfail(False, reason='Mya xfail messatsu')
def test_xfail_condition_false():
    raise SyntaxError('Mya error message')
