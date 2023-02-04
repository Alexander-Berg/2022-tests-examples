from infra.rtc.docker_registry.docker_torrents.exceptions import SubrequestException,\
    InsufficientParametersException
from requests.exceptions import Timeout, HTTPError
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import jyggalag_manifest, xenial_manifest
import copy


class RegistryClient:

    def __init__(self):
        self.manifests = {
            'ubuntu:xenial': xenial_manifest,
            'jyggalag:latest': jyggalag_manifest,
            'secret_ubuntu:xenial': xenial_manifest,
            'secret_jyggalag:latest': jyggalag_manifest
        }

    def resolve_tags(self, scope: str, headers: dict) -> list:
        if 'Authorization' not in headers:
            raise InsufficientParametersException('Authorization header is mandatory')
        return list()

    def resolve_manifest(self, scope: str, tag: str, headers: dict) -> dict:
        if 'Authorization' not in headers:
            raise InsufficientParametersException('Authorization header is mandatory')
        if headers['Authorization'] == 'BADTOKEN':
            raise SubrequestException(HTTPError(), '401')
        if scope == '500':
            raise SubrequestException(HTTPError(), '500')
        if scope == 'timeout':
            raise Timeout()
        return copy.deepcopy(self.manifests['{}:{}'.format(scope, tag)])
