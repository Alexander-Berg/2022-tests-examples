# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import LogHeaders
from awtest.wrappers import get_validation_exception


def test_log_headers():
    pb = modules_pb2.LogHeadersModule()

    log_headers = LogHeaders(pb)

    e = get_validation_exception(log_headers.validate)
    e.match('at least one of the "name_re", "response_name_re" must be specified')

    pb.name_re = '^X-Yandex-RandomUID'
    log_headers.update_pb(pb)
    e = get_validation_exception(log_headers.validate)
    e.match(r'is not a valid regexp: using \^ anchor is not allowed')

    pb.name_re = 'X-Yandex-RandomUID$'
    log_headers.update_pb(pb)
    e = get_validation_exception(log_headers.validate)
    e.match(r'is not a valid regexp: using \$ anchor is not allowed')

    pb.name_re = '.*'
    log_headers.update_pb(pb)
    log_headers.validate(chained_modules=True)

    pb.name_re = ''
    pb.response_name_re = '.*'
    log_headers.update_pb(pb)
    log_headers.validate(chained_modules=True)
