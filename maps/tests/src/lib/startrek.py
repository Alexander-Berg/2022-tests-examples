from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


def get_startrek_issues():
    return http_request_json('GET', get_url() + '/startrek/fake/issues') >> 200
