from unittest import mock

import pytest
from rest_framework.test import APIClient

from tree import views, models

pytestmark = pytest.mark.django_db


@pytest.fixture
def rf(admin_user) -> APIClient:
    client = APIClient()
    client.login(username=admin_user.username, password="password")
    return client


def test_brand_model_view_set_props(rf: APIClient):
    expected_bm = models.BrandModel(properties=[{"expected": "prop"}])
    with mock.patch.object(
        views.BrandModelViewSet, "get_object", spec=views.BrandModelViewSet.get_object
    ) as get_object:
        get_object.return_value = expected_bm
        response = rf.get("/api/tree/brand-model/123/props", follow=True)
        assert expected_bm.properties == response.json()
