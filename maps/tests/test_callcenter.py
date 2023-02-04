import json
from maps.bizdir.sps.yang import callcenter
from google.protobuf import text_format
from . import testdata


def test_task_proto_to_yang_input() -> None:
    for task, expected in testdata.YANG_INPUTS:
        actual = callcenter.convert_task_proto_to_yang_input(task)['data']

        pretty_print = lambda x: json.dumps(x, ensure_ascii=False, indent=4)
        assert pretty_print(expected) == pretty_print(actual)
        assert expected == actual


def test_yang_solution_to_task_result() -> None:
    for yang_item, expected in testdata.YANG_SOLUTIONS:
        actual = callcenter.convert_yang_item_to_task_result(yang_item)

        pretty_print = lambda x: text_format.MessageToString(x, as_utf8=True)
        assert pretty_print(expected) == pretty_print(actual)
        assert expected == actual
