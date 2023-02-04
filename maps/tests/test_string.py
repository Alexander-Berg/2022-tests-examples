from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2 import string_pb2

validator = Validator('common2')


def add_span(msg, begin, end):
    span = msg.span.add()
    span.begin = begin
    span.end = end


def test_string():
    title = string_pb2.SpannableString()

    title.text = 'Площадь Ильича'
    add_span(title, 8, 14)  # Ильича

    validator.validate_example(title, 'string')
