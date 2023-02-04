import json
import random
from unittest import mock

import pytest
from django.db.models import Q, QuerySet
from rest_framework import status
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework.test import APIClient

import parts.views
from parts.models import PartInfo
from parts.views import PartViewSet, SearcherViewSet, searcher

pytestmark = pytest.mark.django_db


@pytest.fixture()
def part_detail():
    return reverse("partinfo-detail")


def test_hard_delete_called_when_viewset_destory_called():
    part = mock.MagicMock()
    PartViewSet().perform_destroy(part)
    assert part.hard_delete.has_calls


def test_count(api_client: APIClient):
    url = reverse("parts-count")
    expected = "expected"
    expected_count = random.randint(0, 99999999999)
    expected_response = {"count": expected_count}
    with mock.patch.object(PartViewSet, "filter_queryset") as filter_queryset, mock.patch.object(
        PartViewSet, "get_queryset", spec=PartViewSet.get_queryset
    ) as get_queryset:
        get_queryset.return_value = expected
        filtered_qs = mock.MagicMock(spec=QuerySet)
        filtered_qs.count.return_value = expected_count
        filter_queryset.return_value = filtered_qs
        ret_val = api_client.get(url)
        assert ret_val.status_code == 200
        filter_queryset.assert_called_once_with(expected)
        assert ret_val.json() == expected_response


@pytest.mark.skip
def test_images_shown_in_listing(part_info: PartInfo, api_client: APIClient):
    # have a problem with images here
    url = reverse("parts-list")
    response = api_client.get(url, data={"format": "datatables"})
    assert response.status_code == 200
    assert len(response.data) == 1
    # idk why tho
    the_part = response.data[0]
    assert the_part["active"] == part_info.active
    assert len(the_part["images"]) == part_info.images.count() == 1


def test_searcher_viewset_parse_function_called(api_client: APIClient):
    url = reverse("searcher-parse")
    with mock.patch.object(SearcherViewSet, "parse") as _parse:
        expected = {"some": "data"}
        _parse.return_value = Response(data=expected)
        resp = api_client.get(url)
        assert resp.status_code == 200
        assert _parse.has_calls
        assert resp.data is expected


def part_info():
    ...


def test_part_view_set_compatibilities_or_404(api_client: APIClient):
    url = reverse("parts-compatibilities", kwargs={"pk": -1})
    response = api_client.get(url)
    assert response.status_code == status.HTTP_404_NOT_FOUND


def test_part_view_set_compatibilities(api_client: APIClient, part_info: PartInfo):
    url = reverse("parts-compatibilities", kwargs={"pk": part_info.id})
    response = api_client.get(url)
    assert part_info.jsonify_compatibilities() == response.data


def test_part_view_set_batch_delete_empty(api_client: APIClient):
    url = reverse("parts-batch-delete")
    r = api_client.post(url, [], content_type="application/json")
    assert r.status_code == status.HTTP_200_OK


def test_part_view_set_batch_delete_non_existent(api_client: APIClient):
    url = reverse("parts-batch-delete")
    nonexistent = [{"brand_id": -1, "part_number": "nonexistent"}]
    with mock.patch.object(parts.views.PartInfo, "get_by_number_and_brand") as get_by_number_and_brand:
        get_by_number_and_brand.return_value = None
        r = api_client.post(url, data=json.dumps(nonexistent), content_type="application/json")
        assert status.HTTP_200_OK == r.status_code
        assert get_by_number_and_brand.has_calls


def test_part_view_set_batch_delete(api_client: APIClient):
    url = reverse("parts-batch-delete")
    parts_for_deletion = [{"brand_id": -1, "part_number": "nonexistent"}]
    with mock.patch.object(parts.views.PartInfo, "get_by_number_and_brand") as get_by_number_and_brand:
        part_mock = mock.MagicMock()
        get_by_number_and_brand.return_value = part_mock
        r = api_client.post(url, data=json.dumps(parts_for_deletion), content_type="application/json")
        assert status.HTTP_200_OK == r.status_code
        assert get_by_number_and_brand.has_calls
        assert part_mock.hard_delete.has_calls


def test_searcher_view_set_parse(api_client: APIClient):
    url = reverse("searcher-parse")
    with mock.patch("parts.views.SearcherClient") as SearcherClient:
        search_text = " some_text "
        expected = "expected_result"
        mock_with_parse = mock.MagicMock()
        mock_with_parse.parse.return_value = expected
        SearcherClient.return_value = mock_with_parse
        response = api_client.get(url, data={"text": search_text})
        assert response.data == expected
        assert SearcherClient.has_calls
        assert mock_with_parse.has_calls
        assert mock_with_parse.called_once_with(search_text.strip(), parse=True)


def test_searcher_view_set_parse_without_query(api_client: APIClient):
    url = reverse("searcher-parse")
    with mock.patch("parts.views.searcher") as SearcherClient:
        SearcherClient.return_value = SearcherClient
        SearcherClient.parse.return_value = ""
        response = api_client.get(url, data={})
        assert response.status_code == status.HTTP_400_BAD_REQUEST


def test_part_view_set_parse_part_empty_query(api_client: APIClient):
    url = reverse("parts-parse-part")
    query = ""
    response: Response = api_client.get(url, data={"text": query})
    assert status.HTTP_400_BAD_REQUEST == response.status_code


def test_part_view_set_parse_part_positive(api_client: APIClient):
    url = reverse("parts-parse-part")
    with mock.patch.object(PartViewSet, "_search_part") as _search_part, mock.patch("parts.views.asdict") as asdict:
        expected = {"expected": "object"}
        asdict.return_value = expected
        query = " some text "
        response: Response = api_client.get(url, data={"text": query})
        assert _search_part.called_once_with(query.strip())
        assert response.data == expected


def test_part_view_set_parse_part_raises(api_client: APIClient):
    url = reverse("parts-parse-part")
    with mock.patch.object(PartViewSet, "_search_part") as _search_part, mock.patch("parts.views.asdict") as asdict:
        expected_ans = {"error": "expected"}
        err_msg = "dude"
        expected_exception = Exception(err_msg)
        _search_part.side_effect = expected_exception
        asdict.return_value = expected_ans
        query = " some text "
        response: Response = api_client.get(url, data={"text": query})
        assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
        assert asdict.called_once_with(expected_exception)
        assert response.data == expected_ans


def test_part_view_set_search_part_no_searcher_response():
    with mock.patch.object(searcher, "parse") as searcher_parse, mock.patch(
        "parts.views._search_part_info"
    ) as _search_part_info:
        expected = {"expected": "result"}
        searcher_parse.return_value = None
        _search_part_info.return_value = expected
        term = "some text to find"
        result = PartViewSet._search_part(term)
        assert expected == result
        assert _search_part_info.called_once_with(term)


def test_part_view_set_search_part_parse_searcher_called():
    with mock.patch.object(searcher, "parse") as searcher_parse, mock.patch(
        "parts.views._parse_searcher"
    ) as _parse_searcher:
        expected = {"expected": "result"}
        searcher_answer = {"searcher": "returns"}
        searcher_parse.return_value = searcher_answer
        _parse_searcher.return_value = expected
        term = "some text to find"
        result = PartViewSet._search_part(term)
        assert expected == result
        assert _parse_searcher.called_once_with(searcher_answer)
