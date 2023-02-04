import logging
import os
import requests

from maps.routing.matrix_router.proto.matrix_pb2 import Matrix
from maps.routing.matrix_router.proto.request_pb2 import Request
import google.protobuf.text_format
import yatest.common

REQUESTS_FILE_PATH = yatest.common.source_path(
    "maps/routing/matrix_router/yacare/tests/reference_response_test/data/requests")

DEBUG_REQUESTS_FILE_PATH = yatest.common.source_path(
    "maps/routing/matrix_router/yacare/tests/reference_response_test/data/debug_requests")


logger = logging.getLogger("maps.routing.matrix_router.tests.reference_response_test")


def has_printable_content(string):
    return string and not string.isspace()


def read_paragraphs(input_file_path):
    with open(input_file_path) as input_file:
        paragraph = ""

        for line in input_file:
            if not has_printable_content(line):
                if has_printable_content(paragraph):
                    yield paragraph
                paragraph = ""
            else:
                paragraph += line

        if has_printable_content(paragraph):
            yield paragraph


def test_basic():
    port = int(os.environ["MATRIX_ROUTER_PORT"])
    url = "http://localhost:{}/calculate".format(port)
    logger.info("testing matrix router at url: {}".format(url))

    responses = []
    for request_text in read_paragraphs(REQUESTS_FILE_PATH):
        request = Request()
        google.protobuf.text_format.Parse(request_text, request)
        request_data = request.SerializeToString()

        response = requests.post(url, data=request_data)
        response.raise_for_status()

        matrix = Matrix()
        matrix.ParseFromString(response.content)
        matrix_string = google.protobuf.text_format.MessageToString(matrix)
        responses.append(matrix_string)

    return responses


def test_debug_mode():
    port = int(os.environ["MATRIX_ROUTER_PORT"])
    url = f"http://localhost:{port}/calculate?debug=1"
    logger.info("testing matrix router at url: {}".format(url))

    request_texts = list(read_paragraphs(DEBUG_REQUESTS_FILE_PATH))
    assert len(request_texts) == 1

    request = Request()
    google.protobuf.text_format.Parse(request_texts[0], request)
    request_data = request.SerializeToString()

    response = requests.post(url, data=request_data)
    response.raise_for_status()

    matrix = Matrix()
    matrix.ParseFromString(response.content)
    matrix_string = google.protobuf.text_format.MessageToString(matrix)

    return matrix_string


def test_localized_matrix():
    port = int(os.environ["MATRIX_ROUTER_PORT"])
    url = f"http://localhost:{port}/v2/matrix?" \
          f"srcll=37.5706,55.786268~37.571257,55.724496&" \
          f"dstll=37.660909,55.737036~37.648741,55.764002&" \
          f"dtm=1606306264"
    logger.info("testing matrix router at url: {}".format(url))

    response = requests.get(url, headers={'accept': "text/x-protobuf"})
    response.raise_for_status()

    return response.content.decode('utf-8')


def test_localized_matrix_ru_is_default():
    port = int(os.environ["MATRIX_ROUTER_PORT"])
    url = f"http://localhost:{port}/v2/matrix?" \
          f"srcll=37.5706,55.786268~37.571257,55.724496&" \
          f"dstll=37.660909,55.737036~37.648741,55.764002&" \
          f"dtm=1606306264"
    logger.info("testing matrix router at url: {}".format(url))

    response = requests.get(url, headers={'accept': "text/x-protobuf"})
    response.raise_for_status()

    response_with_lang = requests.get(url + "&lang=ru_RU", headers={'accept': "text/x-protobuf"})
    response_with_lang.raise_for_status()

    assert response.content == response_with_lang.content
