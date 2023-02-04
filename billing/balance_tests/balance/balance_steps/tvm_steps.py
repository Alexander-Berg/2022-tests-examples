import requests
from time import time

from btestlib.utils import cached
from btestlib.secrets import get_secret


@cached
def get_tvm_token(dst_client_id, src_client_id, secret):
    import ticket_parser2 as tp2
    from ticket_parser2.api.v1 import ServiceContext

    secret = get_secret(*secret)

    ts = int(time())

    tvm_keys = requests.get('https://tvm-api.yandex.net/2/keys?lib_version={version}'
                            .format(version=tp2.__version__)).content.decode('ascii')

    service_context = ServiceContext(src_client_id, secret, tvm_keys)

    ticket_response = requests.post('https://tvm-api.yandex.net/2/ticket/',
                                    data={'grant_type': 'client_credentials',
                                          'src': src_client_id,
                                          'dst': dst_client_id,
                                          'ts': ts,
                                          'sign': service_context.sign(ts, dst_client_id)}).json()

    return ticket_response[str(dst_client_id)]['ticket']
