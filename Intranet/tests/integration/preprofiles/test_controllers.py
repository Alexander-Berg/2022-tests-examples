import pytest

from unittest.mock import patch, PropertyMock

from intranet.femida.src.offers import choices
from intranet.femida.src.offers.choices import EMPLOYEE_TYPES
from intranet.femida.src.offers.controllers import RemotePreprofile
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@patch(
    target='intranet.femida.src.offers.controllers.PreprofileCtl.newhire_data',
    new_callable=PropertyMock,
)
@pytest.mark.parametrize('test_case', [
    {
        'data': {
            'citizenship': choices.CITIZENSHIP.RU,
            'username': 'username',
            'first_name': 'first_name',
            'last_name': 'last_name',
            'position': 'fake_position',
        },
        'newhire_data': {
            'org': 1,
            'department': 1,
            'boss': 'boss',
            'recruiter': 'recruiter',
            'position': 'position'
        },
    },
    {
        'data': {
            'citizenship': choices.CITIZENSHIP.KZ,
            'username': 'user',
            'first_name_en': 'First',
            'last_name_en': 'Last',
            'position': 'fake_position',
        },
        'newhire_data': {
            'username': 'valid_username',
            'org': 2,
            'department': 6,
            'boss': 'head',
            'recruiter': 'recr',
            'position': 'position',
            'employee_type': EMPLOYEE_TYPES.former,
        },
    },
])
def test_remote_preprofile(mocked_newhire_data, test_case):
    data = test_case['data']
    newhire_data = test_case['newhire_data']
    mocked_newhire_data.return_value = newhire_data

    org = f.OrganizationFactory(id=newhire_data['org'])
    department = f.DepartmentFactory(id=newhire_data['department'])
    boss = f.UserFactory(username=newhire_data['boss'])
    recruiter = f.UserFactory(username=newhire_data['recruiter'])

    preprofile = f.PreprofileFactory(data=data)
    remote_preprofile = RemotePreprofile(preprofile)

    # Данные из модели
    assert remote_preprofile.id == preprofile.id
    assert remote_preprofile.data == preprofile.data
    assert remote_preprofile.photo is None

    # Пересекающиеся данные берем из Наниматора
    for k in set(data) & set(newhire_data):
        assert getattr(remote_preprofile, k) == newhire_data[k]
        data.pop(k)

    # Данные из json-данных
    for k in data:
        assert getattr(remote_preprofile, k) == data[k]

    # Данные из Наниматора
    assert remote_preprofile.org == org
    assert remote_preprofile.department == department
    assert remote_preprofile.boss == boss
    assert remote_preprofile.recruiter == recruiter
