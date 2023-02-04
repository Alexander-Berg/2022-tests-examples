import pytest

from staff.femida.utils import FemidaUtils


@pytest.mark.parametrize(
    'url, key',
    [
        ("test", None),
        ("domain/TEST-0000", None),
        ("domainJOB-0000", None),
        ("domain/TJOB-0000", "TJOB-0000"),
        ("domain/TJOB-000", "TJOB-000"),
    ],
)
def test_get_vacancy_issue_key_from_url(url, key):
    result = FemidaUtils().get_vacancy_issue_key_from_url(url)
    assert result == key
