import json
import functools

from django.test.client import Client as BaseClient


def enable_json_param(func):
    """
    Добавляет к методам тестового HTTP-клиента
    возможность прокидывать json по аналогии с requests
    """
    @functools.wraps(func)
    def wrapper(self, path, data=None, json=None, **extra):
        assert not (data and json)
        assert not json or 'content_type' not in extra
        data = data or {}
        if json is not None:
            data = globals()['json'].dumps(json)
            extra['content_type'] = 'application/json'
        return func(self, path, data, **extra)
    return wrapper


class Client(BaseClient):

    def request(self, **request):
        result = super().request(**request)
        result.json = lambda: json.loads(result.content)
        return result

    def get(self, path, data=None, follow=False, **extra):
        data = data or {}
        data.setdefault('organization', 'yandex')

        # FIXME wtform в старой версии не умеет нормально  обрабатывать BooleanField:
        # единственный способ передать False - не передать поле вообще.
        # Удалить после обновления wtforms
        params_list = list(data.items())
        for k, v in params_list:
            if isinstance(v, bool) and not v:
                data.pop(k)

        return super().get(path, data, follow, **extra)

    @enable_json_param
    def post(self, path, data=None, **extra):
        return super().post(path, data, **extra)

    @enable_json_param
    def put(self, path, data=None, **extra):
        return super().put(path, data, **extra)

    @enable_json_param
    def delete(self, path, data=None, **extra):
        return super().delete(path, data, **extra)
