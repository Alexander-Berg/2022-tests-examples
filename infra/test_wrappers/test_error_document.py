# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import ErrorDocument
from awtest.wrappers import get_validation_exception


def test_error_document():
    pb = modules_pb2.ErrorDocumentModule()

    error_document = ErrorDocument(pb)

    e = get_validation_exception(error_document.validate)
    e.match('status.*is required')

    pb.status = 600
    error_document.update_pb(pb)

    e = get_validation_exception(error_document.validate)
    e.match('status.*is not a valid HTTP status')

    pb.status = 500
    pb.file = 'test.txt'
    error_document.update_pb(pb)

    error_document.validate()
    pb.content = 'test'

    e = get_validation_exception(error_document.validate)
    e.match('"file", "content" and "base64" are mutually exclusive')
