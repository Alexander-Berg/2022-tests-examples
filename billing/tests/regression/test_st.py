import os.path
import requests

from dwh.grocery.tools.conf import (
    CONF,
    SECRET,
)
from dwh.grocery.targets.st_target import StTarget


os.environ['REQUESTS_CA_BUNDLE'] = '/etc/ssl/certs/ca-certificates.crt'
TOKEN = SECRET['ST_TOKEN']


class TestSt:

    def setup(self):
        self.target = StTarget("DWH-199", 'kekeke', path='./dwh-199')

    def test_exists(self):
        assert not self.target.exists()

    def test_write(self):
        self.target.write("KEKEKE")
        assert self.target.exists()
        API_COMMENT = f"{CONF['ST']['API']}issues/{self.target.ticket}/comments"
        print("API", API_COMMENT)
        g = requests.get(
            API_COMMENT,
            headers={
                "Authorization": f"OAuth {TOKEN}"
            }
        )
        data = g.json()
        print(data)
        assert API_COMMENT == "https://st-api.test.yandex-team.ru/v2/issues/DWH-199/comments"
        assert data[-1]['text'] == "KEKEKE"

    def teardown(self):
        try:
            self.target.clear()
        except:
            pass
