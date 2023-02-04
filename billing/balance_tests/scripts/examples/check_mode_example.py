# coding: utf-8

from hamcrest import has_entry, has_key

from btestlib.utils import CheckMode, check_mode

__author__ = 'fellow'

'''
Добавлен класс btestlib.utils::CheckMode который регулирует поведение степов при возникновении ошибки
Краткая предыстория.
При выполнении степа возможны три варианта реакции на происходящую в результате его выполнения ошибку:
1) Степ зафейлен (Failed). В результате выполнения возвращается ассерт.
   Такое поведение актуально, когда степ вызывается при целевой проверке теста.
2) Степ сломан (Broken). В результате выполнения возвращается эксепшен.
   Такое поведение логично когда степ вызывается при подготовке данных для теста. По сути означает
   "До целевой проверки теста не дошли, тест сломан"
3) Степ не проверяется (Ignored). Актуально для негативных тестов,
   когда в результате выполнения степа ожидатеся ошибка.


Суть класса CheckMode - обеспечить такое поведение в степах.

Как это работает:
1) Во-первых, есть одно единственное условие применимости всего этого в степам
   - Степ должен возвращать какое-либо значение (любой питоновский объект).
   Собственно, по возвращаемому значению и определяется корректность выполнения степа.
   Например, если xmlrpc-метод вернул словарь {'status': 'error', ...}
   Или степ вернул объект Client, у которого Client.id == None
2) Степ нужно декорировать @CheckMode.result_matches(matcher)
   На вход передается матчер, который и производит валидацию возвращаемого значения
3) По-умолчанию режим выполнения Broken
4) Если есть необходимость сменить режим, вызов степа (степов) оборачивается в контекст-менеджер

    ...

    step_1() - режим по-умолчанию Broken
    with check_mode(CheckMode.FAILED):
        step_2() - режим Failed

        with check_mode(CheckMode.IGNORED):
            step_3() - режим Ignored

        step_4() - снова режим Failed

    ...

'''


@CheckMode.result_matches(has_entry('status', 'success'))
def step_success():
    return {'status': 'success'}


@CheckMode.result_matches(has_key('some_key'))
def step_error():
    return {'status': 'success'}


# здесь декоратор не требуется
def complex_step():
    step_success()
    step_error()


def default_mode_is_broken():
    """
    Контекст по-умолчанию - Broken
    В данном случае будет выдано исклюючение UnsuccessfulResponse exception

    btestlib.utils.UnsuccessfulResponse: Error while running step 'step_error'
      Expected response: a dictionary containing key 'some_key'
      But actual response is: {'status': 'success'}

    """
    step_error()


def change_mode():
    with check_mode(CheckMode.IGNORED):
        step_error()  # отработает без ошибок


def complex_case():
    """
    Если несколько степов сгруппированы внутри одного сложного степа,
    то для всех вложенных степов работает выбранный режим

    В данном случае выкинется assert во время выполнения step_error

    AssertionError: Error while running step 'step_error'
      Expected response: a dictionary containing key 'some_key'
      But actual response is: {'status': 'success'}

    """
    with check_mode(CheckMode.FAILED):
        complex_step()


def inserted_context():
    """
    Можно определять контекст в другом контексте
    В данном примере первый step_error отработает без ошибки, а второй упадет с assertion'ом
    """

    with check_mode(CheckMode.FAILED):
        step_success()

        with check_mode(CheckMode.IGNORED):
            step_error()

        step_error()


if __name__ == '__main__':
    complex_case()
