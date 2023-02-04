class TestStageDescription(object):
    """ Описание этапа интеграционного теста

    Attributes:
        name_program          Название бинарника.
        cmd                   Команда, вызов которой проверяется на этом этапе теста.

        func                  Функция, вызов которой проверяется на этом этапе теста.
        kwargs                Аргументы тестируемой функции.

        input_tables          Входные таблицы тестируемого приложения.
        output_tables         Выходные таблицы тестируемого приложения.
        input_files           Входные файлы тестируемого приложения.
        output_files          Выходные файлы тестируемого приложения.
    """

    name_program = ''
    cmds = []

    func = None
    kwargs = {}
    kwargs_list = [{}]

    input_tables = []
    output_tables = []
    testless_output_tables = []

    input_files = []
    output_files = []
    testless_output_files = []
