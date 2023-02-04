from unittest.mock import MagicMock

from sqlalchemy.exc import DatabaseError as AlchemyDatabaseError
from cx_Oracle import DatabaseError, _Error


class FakeSession:
    """
    Mock настоящей сессии SQLAlchemy
    Передается в функции, которые вызывают session.execute
    Но сам вызов изменен: по-умолчанию он падает с ошибкой ORA-08103
    и успешно возвращает заранее заданное значение лишь начиная со второго раза
    Есть возможность переопределить номер ошибки,
    можно заставить функцию execute падать всегда,
    либо не падать никогда.
    Можно передать кол-во вызовов execute в тестируемой функции.
    Можно передать список с возвращаемыми значениями каждого вызова execute.
    Если список не передан, заполняется None по кол-ву вызовов execute.
    """

    def __init__(self, returns=None, error_to_fail_with=8103, func_execute_calls=2):
        self.return_result = returns
        self.fail_forever = False
        self.never_fail = False
        self.error_obj = _Error(
            f'ORA-{str(error_to_fail_with).zfill(5)}: Error title',  # message
            error_to_fail_with,  # code
            0,  # offset
            '',  # context
        )
        self.execute_called = 0
        self.func_execute_calls = func_execute_calls
        if returns is None:
            self.return_result = [None] * func_execute_calls
        self.begin = MagicMock

    def make_execute_fail_forever(self):
        self.fail_forever = True

    def make_execute_never_fail(self):
        self.never_fail = True

    def execute(self, clause, params=None, mapper=None, bind=None, **kw):
        self.execute_called += 1
        if not self.never_fail and (self.fail_forever or self.execute_called == 1):
            raise AlchemyDatabaseError("SELECT 'test'", params=None, orig=DatabaseError(self.error_obj))
        # Если в первый вызов отдаем ошибку, то идем по списку возвращаемых значений, начиная со второго вызова execute.
        # Если не падаем никогда, сразу идем по списку.
        result = self.return_result[self.execute_called - 2]
        if self.never_fail:
            result = self.return_result[self.execute_called - 1]
        return result


class FakeApplication:
    """
    Mock для приложения из butils
    Создан для функций, которые с помощью экземпляра приложения создают новую сессию для выполнения операций с базой,
    чтобы подсунуть им mock сессии
    """

    def __init__(self, env="dev", cfg=None):
        self.session = FakeSession()
        if not cfg:
            self.cfg = MagicMock()
        else:
            self.cfg = cfg
        self.env = env

    def new_session(self, database_id='meta'):
        return self.session

    def get_current_env_type(self):
        return self.env
