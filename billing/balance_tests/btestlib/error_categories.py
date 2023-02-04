# coding: utf-8

from btestlib.utils import ConstantsContainer


class ErrorCategory(ConstantsContainer):
    constant_type = str
    """
    Product defects - ошибки сервиса (все ошибки не попавшие в другие категории)
    Environment errors - ошибки окружения, для починки нужны ручные действия, после чего можно делать перезапуск
                         (упал сервант, не открыт период в оебс, база лежит)
    Broken tests - сломанные тесты, скорее всего ошибка в кода тестов, но возможно изменилось поведение сервиса
    Probable noise - вероятный шум, у нас чаще появлятся как шум, но может быть и ошибкой сервиса
    Noise - 100% шум
    Хотели еще выдаелять в Muted - замьюченное в тимсити, но стали их скипать, поэтому категория не нужна
    """

    PRODUCT_DEFECTS = "Product defects"
    ENVIRONMENT_ERRORS = "Environment errors"
    BROKEN_TESTS = "Broken tests"
    PROBABLE_NOISE = "Probable noise"
    NOISE = "Noise"
    YAMONEY_ERRORS = 'Yamoney Errors'


# поля для фильтрации ошибок
# https://github.com/allure-framework/allure2/blob/master/allure-generator/src/main/java/io/qameta/allure/category/Category.java
class Regexes(object):
    def __init__(self, message_regex=None, trace_regex=None):
        # type: (basestring, basestring) -> None
        self.message_regex = message_regex
        self.trace_regex = trace_regex


# регексы ошибок общих для всех наших сервисов
# регексы ошибок специфичных для сервисов можно задавать в отдельном модуле
# в <service_dir>/tests и объединять их в balance.tests.conftest.ErrorCategories#generate_categories_json
# !!! будут использоваться в аллюре джава-кодом, регекс должен матчится в полную строку
# регексы удобно проверять на https://regexr.com/
COMMON_CATEGORY_TO_ERROR_REGEX_MAP = {

    ErrorCategory.NOISE: [
        Regexes('WebDriverException: Message: Session.*'),
        Regexes('XmlRpcError:[\s\S]+ORA-30006: resource busy; acquire with WAIT timeout expired.*'),
        Regexes('Fault: .*Error: DatabaseError[\s\S]+ORA-30006: resource busy; acquire with WAIT timeout expired.*'),
        Regexes('Fault: .*Error: Timeout\\\\nDescription:[\s\S]*'),
        Regexes(u'ServiceError: .*ошибка баланса по таймауту.*'),
        Regexes(u'ServiceError: .*http-ошибка по таймауту.*'),
    ],

    ErrorCategory.PROBABLE_NOISE: [
        Regexes('URLError: <urlopen error \[Errno 101\] Network is unreachable>.*'),

        # Ошибки xmlrpc вызовов. Или была выкладка и само все починится или нужно пойти и руками поднять сервант
        Regexes('ProtocolError: .+502 Bad Gateway.*'),
        Regexes('error: \[Errno 104\] Connection reset by peer.*'),
        Regexes('error: \[Errno 111\] Connection refused.*'),

        # Таймауты. Иногда это может быть ошибка теста, например, если в вейтере ждем именно успешного ответа,
        # а ошибочный ответ, который появился через 5 секунд - игнорим
        Regexes(u'TimeoutException: Message: Не произошел редирект на страницу yandex.ru в течение.*'),
        Regexes('ConditionHasNotOccurred: .+Waiting for data on page.*'),

        # иногда происходит при чтении данных из оебс из-за нечитаемого символа (xml-парсер падает с ошибкой)
        # Коля это поправил в ExecuteSQL, после переноса изменений в ExecuteOEBS должно полечится
        Regexes('ExpatError: not well-formed \(invalid token\).*'),
    ],

    ErrorCategory.ENVIRONMENT_ERRORS: [
        Regexes(u'XmlRpcError: [\s\S]+ORA-20000: Ошибка: Попытка создания акта не в открытом периоде.*'),
        Regexes('[\s\S]*TNS:listener does not currently know of service requested in connect descriptor[\s\S]*'),
        Regexes('[\s\S]*Non-closed month[\s\S]*'),
    ],

    ErrorCategory.BROKEN_TESTS: [
        Regexes('ParseError: .*'),
        # all classes from exceptions module except AssertionError and KeyboardInterrupt
        Regexes('ArithmeticError: .*'),
        Regexes('AttributeError: .*'),
        Regexes('BaseException: .*'),
        Regexes('BufferError: .*'),
        Regexes('BytesWarning: .*'),
        Regexes('DeprecationWarning: .*'),
        Regexes('EOFError: .*'),
        Regexes('EnvironmentError: .*'),
        Regexes('Exception: .*'),
        Regexes('FloatingPointError: .*'),
        Regexes('FutureWarning: .*'),
        Regexes('GeneratorExit: .*'),
        Regexes('IOError: .*'),
        Regexes('ImportError: .*'),
        Regexes('ImportWarning: .*'),
        Regexes('IndentationError: .*'),
        Regexes('IndexError: .*'),
        Regexes('KeyError: .*'),
        Regexes('LookupError: .*'),
        Regexes('MemoryError: .*'),
        Regexes('NameError: .*'),
        Regexes('NotImplementedError: .*'),
        Regexes('OSError: .*'),
        Regexes('OverflowError: .*'),
        Regexes('PendingDeprecationWarning: .*'),
        Regexes('ReferenceError: .*'),
        Regexes('RuntimeError: .*'),
        Regexes('RuntimeWarning: .*'),
        Regexes('StandardError: .*'),
        Regexes('StopIteration: .*'),
        Regexes('SyntaxError: .*'),
        Regexes('\[\'SyntaxError: .*'),
        Regexes('SyntaxWarning: .*'),
        Regexes('SystemError: .*'),
        Regexes('SystemExit: .*'),
        Regexes('TabError: .*'),
        Regexes('TypeError: .*'),
        Regexes('UnboundLocalError: .*'),
        Regexes('UnicodeDecodeError: .*'),
        Regexes('UnicodeEncodeError: .*'),
        Regexes('UnicodeError: .*'),
        Regexes('UnicodeTranslateError: .*'),
        Regexes('UnicodeWarning: .*'),
        Regexes('UserWarning: .*'),
        Regexes('ValueError: .*'),
        Regexes('Warning: .*'),
        Regexes('ZeroDivisionError: .*'),
    ],

    # Trust specific
    ErrorCategory.YAMONEY_ERRORS: [
        Regexes('NoAvailablePayMethod: no available pay method by name yandex_money.*'),
    ],
}
