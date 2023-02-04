import pytest
from map_reduce_for_enrichment import Collector

TOKENS = [
    "warning",
    "uid"
]


@pytest.mark.parametrize("in_result", [True, False])
def test_add_row_if(in_result):
    collector = Collector(TOKENS)
    collector.add_row_if({"test": "test_val"}, in_result)
    if in_result:
        assert collector.saved_rows[0].get("test") == "test_val"
    else:
        assert len(collector.saved_rows) == 0


@pytest.mark.parametrize("context_to_set", [{"uid": 123456}])
def test_set_context(context_to_set):
    collector = Collector(TOKENS)
    collector.set_context(context_to_set)
    assert collector.context.get("uid") == 123456


@pytest.mark.parametrize("context_to_set", [{"uid": 123456}])
def test_get_context(context_to_set):
    collector = Collector(TOKENS)
    collector.context.update(context_to_set)
    assert collector.get_context("uid") == 123456


def test_get_warning():
    collector = Collector(TOKENS)
    collector.context["warning"] = "test warning"
    assert collector.get_warning() == "test warning"


def test_set_warning():
    collector = Collector(TOKENS)
    collector.set_warning("test warning")
    assert collector.context["warning"] == "test warning"


@pytest.mark.parametrize("warning", [None, "NOT IMPLEMENTED"])
def test_add_warning(warning):
    collector = Collector(TOKENS)
    collector.add_warning(warning)
    if warning is None:
        assert collector.context["warning"] is None
    else:
        assert collector.context["warning"] == warning


@pytest.mark.parametrize("warning", [None, "NOT IMPLEMENTED"])
def test_add_warning_above_prev(warning):
    collector = Collector(TOKENS)
    collector.add_warning("test warning")
    collector.add_warning(warning)
    if warning is None:
        assert collector.context["warning"] == "test warning"
    else:
        assert collector.context["warning"] == "{prev_warn}\n{new_warn}".format(prev_warn="test warning",
                                                                                new_warn=warning)


def test_clear_on_context():
    collector = Collector(TOKENS)
    collector.context["service_id"] = 123456
    collector.saved_rows.append("test warning")
    collector.clear()
    for value in collector.context.values():
        if value is not None:
            assert value is None


def test_clear_on_saved_rows():
    collector = Collector(TOKENS)
    collector.context["service_id"] = 123456
    collector.saved_rows.append("test warning")
    collector.clear()
    assert len(collector.saved_rows) == 0


@pytest.mark.parametrize("saved_rows,context", [(
                                                [{"uid": 123456, "warning": "NOT IN METHOD", "test": False}, {"uid": 22355, "warning": None, "test": False}],
                                                {"uid": 0, "warning": "METHOD TRUNCATED"}
                                                )])
def test_process_and_clear(saved_rows, context):
    collector = Collector(TOKENS)
    collector.context.update(context)
    collector.saved_rows = saved_rows

    result_saved_rows = collector.process_and_clear()
    for result_saved_row in result_saved_rows:
        for key, val in context.items():
            assert result_saved_row[key] == val
