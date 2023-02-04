import pytest


@pytest.fixture
@pytest.mark.django_db
def company(company_with_module_scope):
    return company_with_module_scope
