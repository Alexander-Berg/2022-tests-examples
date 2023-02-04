from yandex.maps.proto.factory import tag_pb2

from common import canonical_message, set_time, set_bbox


def add_suggest(msg, text, begin, end):
    str_msg = msg.suggests.add()
    str_msg.text.text = text
    span_msg = str_msg.text.span.add()
    span_msg.begin = begin
    span_msg.end = end


def test_tags_suggest():
    msg = tag_pb2.TagSuggests()
    add_suggest(msg, "good_image", 5, 10)
    add_suggest(msg, "bad_image", 4, 9)
    return canonical_message(msg, 'tags_suggest')
