from infra.rtc.docker_registry.docker_torrents.exceptions import SubrequestException, BlackboxException
from requests.exceptions import HTTPError, Timeout


class BlackboxClient:

    def __init__(self):
        self.ip_header = 'X-Real-Ip'

    def login(self, headers: dict, user_ip: str = None) -> str:
        if headers['Authorization'] == 'BADTOKEN':
            raise BlackboxException('bad', 'login')
        if headers['Authorization'] == 'NOBODY_TOKEN':
            return 'nobody'
        if headers['Authorization'] == 'GOOD_TOKEN':
            return 'good_boy'
        if headers['Authorization'] == '500':
            raise SubrequestException(HTTPError(), 'Internal server error')
        if headers['Authorization'] == 'timeout':
            raise Timeout()
        return None
