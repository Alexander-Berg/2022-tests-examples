import mock
import pytest


@pytest.fixture()
def approvers_conf():
    from staff.departments.models import DepartmentStaff
    from staff.person.models import Staff
    from staff.trip_questionary.controller.approvers import TripApprovers as TA, UnknownApprover as UA

    def role_f(person):
        return DepartmentStaff(staff=person)

    class Setup(object):
        UnknownApprover = UA
        TripApprovers = TA

        muggle = Staff(id=1, login='muggle')
        chief = Staff(id=2, login='chief')
        grand_chief = Staff(id=3, login='grand_chief')
        hrbp_1 = Staff(id=4, login='hrbp_1')
        hrbp_2 = Staff(id=5, login='hrbp_2')
        grand_hrbp = Staff(id=6, login='grand_hrbp')

        chiefs_role = {
            muggle: [role_f(chief), role_f(grand_chief)],
            hrbp_1: [role_f(chief), role_f(grand_chief)],
            hrbp_2: [role_f(grand_chief)],
            grand_hrbp: [role_f(grand_chief)],
            chief: [role_f(grand_chief)]
        }

        staff_list = [muggle, chief, grand_chief, hrbp_1, hrbp_2, grand_hrbp]
        same_params = dict(staff_list=staff_list, chiefs_role=chiefs_role)
        all_params = dict(author=chief, foreign=False, **same_params)

    return Setup()


def test_check_team_roles(approvers_conf):
    from staff.departments.models import DepartmentStaff

    s = approvers_conf

    role_chief_1 = DepartmentStaff(staff=s.chief)
    role_chief_2 = DepartmentStaff(staff=s.chief)
    role_grand_chief = DepartmentStaff(staff=s.grand_chief)
    roles = [role_chief_1, role_chief_2, role_grand_chief]

    with mock.patch.object(s.TripApprovers, 'get_team_top_roles', return_value=roles):
        obj = s.TripApprovers(**s.all_params)
        assert set(obj.check_team_roles(s.staff_list)) == {(s.chief, None), (s.grand_chief, None)}


def test_check_chiefs(approvers_conf):
    s = approvers_conf

    with mock.patch.object(s.TripApprovers, 'filtered_chiefs_role', s.chiefs_role):
        obj = s.TripApprovers(**s.all_params)
        assert set(obj.check_chiefs(s.staff_list)) == {
            (s.muggle, 'chief'),
            (s.hrbp_1, 'chief'),
            (s.hrbp_2, 'grand_chief'),
            (s.grand_hrbp, 'grand_chief'),
            (s.chief, 'grand_chief'),
        }


def test_check_author_roles(approvers_conf):
    s = approvers_conf

    with mock.patch('staff.trip_questionary.controller.approvers.is_ancestor', return_value=True):
        with mock.patch.object(s.TripApprovers, 'author_roles', s.chiefs_role[s.muggle]):
            obj = s.TripApprovers(**s.all_params)
            assert set(obj.check_author_roles(s.staff_list)) == {
                (s.muggle, None),
                (s.hrbp_1, None),
                (s.hrbp_2, None),
                (s.grand_hrbp, None),
                (s.grand_chief, None),
            }


def test_get_author_chiefs(approvers_conf):
    s = approvers_conf

    author_roles = [(s.muggle, None), (s.hrbp_1, None)]
    team_role = [(s.grand_chief, None)]
    chiefs = [(s.chief, 'grand_chief'), (s.hrbp_2, 'grand_chief')]

    with mock.patch.object(s.TripApprovers, 'check_author_roles', return_value=author_roles):
        with mock.patch.object(s.TripApprovers, 'check_team_roles', return_value=team_role):
            with mock.patch.object(s.TripApprovers, 'check_chiefs', return_value=chiefs):
                obj = s.TripApprovers(**s.all_params)
                assert obj.get() == {
                    s.muggle.id: None,
                    s.chief.id: 'grand_chief',
                    s.grand_chief.id: None,
                    s.hrbp_1.id: None,
                    s.hrbp_2.id: 'grand_chief',
                    s.grand_hrbp.id: s.UnknownApprover,
                }


@pytest.fixture()
def db_data():
    from staff.trip_questionary.tests.fixture import create_department_infrastructure
    obj = mock.Mock()
    create_department_infrastructure(obj)
    return obj


@pytest.mark.django_db()
def test_top_department_types(approvers_conf, db_data):
    s = approvers_conf
    obj = s.TripApprovers(**s.all_params)
    assert set(obj.top_department_types) == {db_data.dep_kind_lvl_0.id, db_data.dep_kind_lvl_1.id}


@pytest.mark.django_db()
def test_author_roles(approvers_conf, db_data):
    s = approvers_conf
    params = s.all_params.copy()
    params['author'] = db_data.chief_yandex
    params['foreign'] = True
    obj = s.TripApprovers(**params)
    res = obj.author_roles
    assert len(res) == 1
    assert res[0] == db_data.chief_yandex_role


@pytest.mark.django_db()
def test_get_team_top_roles(approvers_conf, db_data):
    s = approvers_conf
    obj = s.TripApprovers(**s.all_params)
    res = obj.get_team_top_roles([
        db_data.chief_yandex,
        db_data.chief_subyandex,
        db_data.chief_direction_1,
        db_data.chief_division_1,
    ])
    assert len(res) == 2
    assert set(res) == {db_data.chief_yandex_role, db_data.chief_subyandex_role}
