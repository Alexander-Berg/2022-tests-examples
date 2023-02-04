import mock
import pytest


@pytest.fixture()
def approvers_conf():
    from staff.person.models import Staff
    from staff.departments.models import DepartmentStaff
    from staff.trip_questionary.controller.approvers import ConfApprovers, UnknownApprover as UA

    def hrbp_dict(hrbp):
        return {'id': hrbp.id, 'login': hrbp.login}

    def role_f(person):
        return DepartmentStaff(staff=person)

    class Setup(object):
        UnknownApprover = UA

        muggle = Staff(id=1, login='muggle')
        chief = Staff(id=2, login='chief')
        grand_chief = Staff(id=3, login='grand_chief')
        hrbp_1 = Staff(id=4, login='hrbp_1')
        hrbp_2 = Staff(id=5, login='hrbp_2')
        grand_hrbp = Staff(id=6, login='grand_hrbp')

        chiefs_role = {
            muggle: [role_f(grand_chief), role_f(chief)],
            hrbp_1: [role_f(grand_chief), role_f(chief)],
            hrbp_2: [role_f(grand_chief)],
            grand_hrbp: [role_f(grand_chief)],
            chief: [role_f(grand_chief)]
        }
        hr_partners = {
            muggle.id: [hrbp_dict(hrbp_1), hrbp_dict(hrbp_2)],
            hrbp_1.id: [hrbp_dict(grand_hrbp)],
            chief.id: [hrbp_dict(grand_hrbp)]
        }

        staff_list = [muggle, chief, grand_chief, hrbp_1, hrbp_2, grand_hrbp]
        same_params = dict(staff_list=staff_list, chiefs_role=chiefs_role, hr_partners=hr_partners)

        hrbp_ids = [hrbp_1.id, hrbp_2.id, grand_hrbp.id]

        def __call__(self, author, foreign):
            with mock.patch.object(ConfApprovers, 'get_hrbp_ids', return_value=self.hrbp_ids):
                return ConfApprovers(author=author, foreign=foreign, **self.same_params).get()

    return Setup()


@pytest.mark.skip
def test_get_conf_approvers_muggle_foreign(approvers_conf):
    s = approvers_conf
    assert s(author=s.muggle, foreign=True) == {
        s.muggle.id: s.hrbp_1.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: s.chief.login,
        s.hrbp_2.id: s.grand_chief.login,
        s.grand_hrbp.id: s.grand_chief.login,
    }


@pytest.mark.skip
def test_get_conf_approvers_muggle_sng(approvers_conf):
    s = approvers_conf
    assert s(author=s.muggle, foreign=False) == {
        s.muggle.id: s.hrbp_1.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: s.chief.login,
        s.hrbp_2.id: s.grand_chief.login,
        s.grand_hrbp.id: s.grand_chief.login,
    }


@pytest.mark.skip
def test_get_conf_approvers_chief_foreign(approvers_conf):
    s = approvers_conf
    assert s(author=s.chief, foreign=True) == {
        s.muggle.id: s.hrbp_1.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: None,
        s.hrbp_2.id: s.grand_chief.login,
        s.grand_hrbp.id: s.grand_chief.login,
    }


@pytest.mark.skip
def test_get_conf_approvers_chief_sng(approvers_conf):
    s = approvers_conf
    assert s(author=s.chief, foreign=False) == {
        s.muggle.id: s.hrbp_1.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: None,
        s.hrbp_2.id: s.grand_chief.login,
        s.grand_hrbp.id: s.grand_chief.login,
    }


@pytest.mark.skip
def test_get_conf_approvers_hrbp_foreign(approvers_conf):
    s = approvers_conf
    assert s(author=s.hrbp_1, foreign=True) == {
        s.muggle.id: s.chief.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: s.chief.login,
        s.hrbp_2.id: s.grand_chief.login,
        s.grand_hrbp.id: s.grand_chief.login,
    }


@pytest.mark.skip
def test_get_conf_approvers_hrbp_sng(approvers_conf):
    s = approvers_conf
    assert s(author=s.hrbp_1, foreign=False) == {
        s.muggle.id: None,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: s.chief.login,
        s.hrbp_2.id: s.grand_chief.login,
        s.grand_hrbp.id: s.grand_chief.login,
    }


@pytest.mark.skip
def test_get_conf_approvers_grand_chief_foreign(approvers_conf):
    s = approvers_conf
    assert s(author=s.grand_chief, foreign=True) == {
        s.muggle.id: s.hrbp_1.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: s.chief.login,
        s.hrbp_2.id: None,
        s.grand_hrbp.id: None,
    }


@pytest.mark.skip
def test_get_conf_approvers_grand_chief_sng(approvers_conf):
    s = approvers_conf
    assert s(author=s.grand_chief, foreign=False) == {
        s.muggle.id: s.hrbp_1.login,
        s.chief.id: s.grand_hrbp.login,
        s.grand_chief.id: s.UnknownApprover,
        s.hrbp_1.id: s.chief.login,
        s.hrbp_2.id: None,
        s.grand_hrbp.id: None,
    }
