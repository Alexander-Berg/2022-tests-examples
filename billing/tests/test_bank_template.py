"""
Этот шаблон содержит перечень базовых и необходимых проверок,
которые требуется не забыть произвести, когда создаётся
новый набор тестов при реализации интеграции с новой внешней системой.

Можно копировать содержимое в новый файл и заполнять тела тестовых функций.

"""


def test_payment_creator(build_payment_bundle):
    """Тест для проверки работы сборщика пакетов.
    Генерируем пакет и проверяем, как улеглись наши данные в нужный формат.

    * Если сборщиков несколько, тестовых функций тоже должно быть несколько.

    """
    # associate = ...
    # bundle = build_payment_bundle(associate)
    # payment = bundle.payments[0]
    # assert f'{payment.f_acc}' in bundle.tst_compiled


def test_statement_parser(parse_statement_fixture):
    """Тест для проверки работы разборщика выписок.
    Берётся фикстура выписки из файловой системы и проверяются результаты её разбора.

    * Если разборщиков несколько, тестовых функций тоже должно быть несколько.
    * Если поддерживаются разные типы выписок (итоговая, промежуточная), то каждый должен быть проверен.

    """
    # associate = ...
    # register, payments = parse_statement_fixture(
    #     'statement.txt', associate, '1234567890', 'RUB')[0]


def test_balance_getter(get_assoc_acc_curr, response_mock, django_assert_num_queries, init_user):
    """В этом тесте ожидается проверка работы получателя баланса,
    (если таковой поддерживается).
    """
    # use_sandbox = False
    # with django_assert_num_queries(10) as _:
    #     with response_mock('', bypass=use_sandbox):
    #         ...


def test_payment_automation(
    response_mock, run_task, build_payment_bundle,
    django_assert_num_queries, init_user
):
    """Тест для проверки работы автоматизированных (для сервер-сервер)
    сборки и отправки пакетов платежей, а также получения статусов пакета/платежей
    (если внешняя система это поддерживает).

    ВНИМАНИЕ: следует обязательно проверить сценарий повторной отправки пакета платежей.
        Внешняя система должна гарантировать (используя предоставленный нами реквизит,
        например, номер пакета), что платежи из данного пакета не будут проведены повторно
        в случае повторных отправок.

    """
    # with django_assert_num_queries(10) as _:
    #     ...


def test_statement_automation(
    get_assoc_acc_curr, response_mock, run_task, build_payment_bundle,
    django_assert_num_queries, init_user,
):
    """Тест для проверки работы автоматизированного (для сервер-сервер)
    получения и разбора выписок.
    """
    # run_task('process_statements')
