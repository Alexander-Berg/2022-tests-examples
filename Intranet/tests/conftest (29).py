import pytest


@pytest.fixture
def company(company_with_module_scope):
    return company_with_module_scope
