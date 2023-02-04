import random
from unittest import mock

import pytest
from django.db.models import QuerySet, Q

from parts.filter_backends import PartsFilterBackend

pytestmark = pytest.mark.django_db


@pytest.fixture()
def search_query():
    with mock.patch.object(PartsFilterBackend, "_search_query") as _search_query:
        yield _search_query


def test__filter_smart_search_invalid(search_query):
    invalid_text = "@invalid dude"
    qs = mock.MagicMock(spec=QuerySet)
    search_query.return_value = invalid_text
    result = PartsFilterBackend._filter_smart_search(qs, {})
    search_query.assert_called()
    assert result is None


def test_boolean_filters_positive():
    criteria = "yes"
    q = PartsFilterBackend._query_boolean_toggles(
        dict(
            has_images=criteria,
            has_compatibilities=criteria,
            has_analogues=criteria,
            is_checked=criteria,
            is_correct=criteria,  # is_correct should not be supported
        )
    )
    assert q.connector == Q.AND
    assert 4 == len(q.children)
    assert ("images__isnull", False) in q.children
    assert ("partcompatibilities__isnull", False) in q.children
    assert ("analogs__length__gt", 0) in q.children
    assert ("is_checked__length__gt", 0) in q.children

    criteria = "no"
    q = PartsFilterBackend._query_boolean_toggles(
        dict(
            has_images=criteria,
            has_compatibilities=criteria,
            has_analogues=criteria,
            is_checked=criteria,
            is_correct=criteria,  # is_correct should not be supported
        )
    )

    assert q.connector == Q.AND
    assert 4 == len(q.children)
    assert ("images__isnull", True) in q.children
    assert ("partcompatibilities__isnull", True) in q.children
    assert ("analogs__length", 0) in q.children
    assert ("is_checked__length", 0) in q.children


def test_smart_search_support_allowed(search_query: mock.MagicMock):
    for term, filter_column in PartsFilterBackend.ALLOWED_SMART_PARAMS.items():
        searchable = random.randint(0, 1000)
        search_text = f"@{term} {searchable}"
        qs = mock.MagicMock(spec=QuerySet)
        search_query.return_value = search_text
        result = PartsFilterBackend._filter_smart_search(qs, {})
        assert search_query.has_calls
        (q,), *_ = qs.filter.call_args
        assert Q(**{filter_column: f"{searchable}"}) == q


def test_filter_search_by_title_without_title(search_query):
    search_query.return_value = ""
    qs = mock.MagicMock()
    q = PartsFilterBackend._filter_search_title(qs, {"search_title": "true"})
    assert not q


def test_filter_search_by_title_without_toggle(search_query):
    search_query.return_value = "valid search term"
    qs = mock.MagicMock()
    q = PartsFilterBackend._filter_search_title(qs, {"search_title": "false"})
    assert not q


def test_filter_search_by_title_startswith_if_no_category(search_query):
    wanted = "wanted"
    search_query.return_value = wanted
    qs = mock.MagicMock()
    with mock.patch.object(PartsFilterBackend, "_query_category") as _query_category:
        _query_category.return_value = Q()
        r = PartsFilterBackend._filter_search_title(qs, {"search_title": "true"})
        assert r is qs.filter.return_value
        queries, _ = qs.filter.call_args
        assert len(queries) == 1
        q = queries[0]
        assert len(q.children) == 1
        lookup, value = q.children[0]
        assert value == wanted
        assert "istartswith" in lookup


def test_filter_search_by_title_still_icontains_if_there_is_category(search_query):
    wanted = "wanted"
    wanted_category = 123
    search_query.return_value = wanted
    qs = mock.MagicMock()
    with mock.patch.object(PartsFilterBackend, "_query_category") as _query_category:
        _query_category.return_value = Q(category_id=wanted_category)
        r = PartsFilterBackend._filter_search_title(qs, {"search_title": "true"})
        assert r is qs.filter.return_value
        queries, _ = qs.filter.call_args
        assert len(queries) == 1
        q = queries[0]
        assert len(q.children) == 2
        lookups, values = zip(*q.children)
        assert wanted_category in values
        assert wanted in values
        assert "category_id" in lookups
        assert any(lkp.endswith("icontains") for lkp in lookups)


def test_filter_search_by_title_positive(search_query):
    qs = mock.MagicMock(spec=QuerySet)
    wanted_category_query = Q(id=random.randint(1, 999))
    with mock.patch.object(PartsFilterBackend, "_query_category") as query_category, mock.patch.object(
        PartsFilterBackend, "_search_query"
    ) as search_query:
        query_category.return_value = wanted_category_query
        wanted_title = "wanted title"
        search_query.return_value = wanted_title
        q = PartsFilterBackend._filter_search_title(qs, {"search_title": "true"})
        assert q
        assert q == qs.filter.return_value
        (iq), *_ = qs.filter.call_args
        qs.filter.assert_called_once_with(wanted_category_query & Q(origin_title__icontains=wanted_title))
