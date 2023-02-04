import os
import sys
api_folder = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if api_folder not in sys.path:
    sys.path.append(api_folder)

from saas_api.message import SaasMessage
from saas_api.context import ToJsonContext


def test_min():
    sm = SaasMessage()
    sm.gen_document(url='123', body='sometext')
    mess_js = sm.to_json(ToJsonContext())
    assert mess_js['docs'][0]['url'] == '123'
    assert mess_js['docs'][0]['body'] == 'sometext'
