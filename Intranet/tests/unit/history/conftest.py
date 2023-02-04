import pretend
import pytest

from django.conf import settings
from common import factories


@pytest.fixture
def data_history(db, owner_role, deputy_role, responsible_role, superuser, staff_factory):
    factories.ServiceTypeFactory(
        name='Сервис',
        name_en='Service',
        code='undefined'
    )

    meta_service = factories.ServiceFactory(
        slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG,
        is_exportable=False
    )

    owner = staff_factory()
    service = factories.ServiceFactory(
        owner=owner,
        is_exportable=True,
    )

    return pretend.stub(
        service=service,
        staff=owner,
        meta_service=meta_service,
    )
