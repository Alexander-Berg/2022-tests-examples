import asyncio

from decorator import decorator


def asynctest(f):
    """
    Адаптер, превращающий синхронную функцию в асинхронную.

    Похожа на pytest.mark.asyncio. Так же позволяет
    писать асинхронные тесты, но возвращает результат.
    Полезна при написании канонизированных тестов.
    :param f: асинхронная функция
    :return: синхронная функция-обертка.
    """

    def wrapper(f, *args, **kwargs):
        loop = asyncio.get_event_loop()

        return loop.run_until_complete(f(*args, **kwargs))

    return decorator(wrapper, f)
