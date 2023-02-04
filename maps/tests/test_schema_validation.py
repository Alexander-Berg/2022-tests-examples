import pytest
from maps.b2bgeo.test_lib.schema_validation import check_russian_translations


def test_acceptable_dict():
    data = {
        "entity": {
            "description": "foo",
            "x-description-ru":  "foo",
            "summary": "bar",
            "x-summary-ru": "bar"
        }
    }
    check_russian_translations(data)


def test_list_item_fail():
    data = {
        "enitites": [
            {
                "description": "foo",
                "x-description-ru":  "foo"
            },
            {
                "description": "foo",
            }
        ]
    }

    with pytest.raises(AssertionError, match=r"x-description-ru is not provided for /enitites\[1\]"):
        check_russian_translations(data)


def test_nested_fail():
    data = {
        "outer_entity": {
            "inner_entity": {
                "description": "foo",
            }
        }
    }

    with pytest.raises(AssertionError, match="x-description-ru is not provided for /outer_entity/inner_entity"):
        check_russian_translations(data)


def test_summary_fail():
    data = {
        "entity": {
            "summary": "foo"
        }
    }
    with pytest.raises(AssertionError, match="x-summary-ru is not provided for /entity"):
        check_russian_translations(data)
