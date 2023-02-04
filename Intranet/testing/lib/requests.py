import pickle

class Response(object):
    _response_str = "a"

    def __init__(self, serialize=False):
        if not serialize:
            f = open('/tmp/session_response.pdump', 'rb')
            tmp_dict = pickle.load(f)
            f.close()
            self.__dict__.update(tmp_dict.__dict__)

    def json(self):
        return self._response_str;


class Session(object):
    """ class for mocking requests.Session() """
    headers = {}
    _post_count = 0;

    def __init__(self, serialize=False):
        """ find mock initialization file to specify Session and Response parameters """
        if not serialize:
            f = open('/tmp/session.pdump', 'rb')
            tmp_dict = pickle.load(f)
            f.close()
            self.__dict__.update(tmp_dict.__dict__)

    def post(self, url='', params={}, verify=False):
        self._post_count += 1
        return Response()

    
