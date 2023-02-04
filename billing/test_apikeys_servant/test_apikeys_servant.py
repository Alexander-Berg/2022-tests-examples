import cgi

from bson import json_util

from billing.apikeys.apikeys.apikeys_servant import BottleServant, JsonResultHttp, RequestHandler, _path_split, get_dict_params
from billing.apikeys.apikeys.butils_port.application import getApplication
from billing.apikeys.apikeys.http_core import wsgi_legacy_style


class Pinger(RequestHandler):
    content_type = 'text/plain'

    def process_request(self, env_helper):
        if getApplication().checkalive():
            return 'SUCCESS'
        raise Exception('not alive')


class BsonResultHttp(JsonResultHttp):
    def process_request(self, env_helper):
        path_name = [p for p in _path_split(env_helper.path_info) if p][0]
        st = cgi.FieldStorage(fp=env_helper.environ['wsgi.input'], environ=env_helper.environ, keep_blank_values=True)
        query_params = get_dict_params(st)
        query_params['_http_method'] = env_helper.request_method
        query_params['_environ'] = env_helper.environ
        res = self.get_method(path_name)(query_params)
        return json_util.dumps(res)


class TestApiServant(BottleServant):
    def mount_all(self):
        self.bottle.mount('/ping', wsgi_legacy_style(Pinger()))
        self.bottle.mount('/mongo', wsgi_legacy_style(BsonResultHttp('billing.apikeys.apikeys.test_apikeys_servant.mongologic')))
        self.bottle.mount('/mapper', wsgi_legacy_style(JsonResultHttp('billing.apikeys.apikeys.test_apikeys_servant.mapperlogic')))
