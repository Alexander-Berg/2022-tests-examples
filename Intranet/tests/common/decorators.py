from functools import wraps


def assert_num_queries(num_queries):
    """
    Декоратор для установки числа sql-запросов, ожидаемого в методе TestCase.
    """
    def decorator(func):
        @wraps(func)
        def wrapper(self, *args, **kwargs):
            with self.assertNumQueries(num_queries):
                return func(self, *args, **kwargs)
        return wrapper

    return decorator
