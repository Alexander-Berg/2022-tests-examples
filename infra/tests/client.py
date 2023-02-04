import weakref
import json

from django.test import Client
from django.test.client import MULTIPART_CONTENT
from django.utils.encoding import force_text


class CauthClient(Client):
    def __init__(self, *args, **kwargs):
        super(CauthClient, self).__init__(*args, **kwargs)
        self.json = JsonClient(weakref.proxy(self))


class JsonClient(object):
    """Класс, аналогичный django.test.client.Client, но делающий все запросы с заголовком content_type,
    равным application/json, а для запросов post, put и patch кодирующий тело запроса в JSON.
    Также добавляет к ответу метод json(), который пытается декодировать тело ответа из формата JSON.
    """
    def __init__(self, client):
        self.client = client

    def get(self, path, data=None, **extra):
        if data is None:
            data = {}
        response = self.client.get(path, data=data, **extra)
        return self.jsonify(response)

    def post(self, path, data=None, content_type=MULTIPART_CONTENT, **extra):
        if content_type == 'application/json':
            if data is None:
                data = {}
            data = json.dumps(data) if data else data
        return self.jsonify(self.client.post(path, data=data, content_type=content_type, **extra))

    def head(self, path, data=None, **extra):
        return self.jsonify(self.client.head(path, data=data, **extra))

    def options(self, path, data='', content_type='application/json', **extra):
        return self.jsonify(self.client.options(path, data=data, content_type=content_type, **extra))

    def put(self, path, data='', content_type='application/json', **extra):
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.put(path, data=data, content_type=content_type, **extra))

    def patch(self, path, data='', content_type='application/json', **extra):
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.patch(path, data=data, content_type=content_type, **extra))

    def delete(self, path, data='', content_type='application/json', **extra):
        data = json.dumps(data) if data else data
        return self.jsonify(self.client.delete(path, data=data, content_type=content_type, **extra))

    def jsonify(self, response):
        response.json = lambda: json.loads(force_text(response.content)) if hasattr(response, 'content') else None
        return response
