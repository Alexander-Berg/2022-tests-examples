import pytest
from django.core.exceptions import ValidationError

from staff.departments.models import Department

from staff.proposal.forms.department import DepartmentConsistancyValidator

#                        На этой структуре (company) будем строить
#        yandex        варианты изменения структуры и проверять валидность
#       /      \
#     dep1    dep2     Кейсы корректных рокировок:
#     |   \            1) dep1 -> dep2;    dep11 -> dep12;
#  dep11   dep12       2) dep111 -> yandex;    <new1> -> dep111;    dep2 -> <new1>;
#     |                3) dep11 -> dep2;    <new1> -> dep111;    <new2> -> <new1>;    dep1 -> <new2>;
#  dep111              4) dep11 -> <delete>;    dep111 -> dep2;    dep12 -> dep111;
#
#                      Кейсы некорректных рокировок:
#                      1) <new1> -> <new2>;    <new2> -> <new3>;    <new3> -> <new1>;
#                      2) dep1 -> dep111;
#                      3) dep11 -> dep12;    dep1 -> dep111;
#                      4) dep1 -> dep2;    <new1> -> dep11;    dep111 -> <new1>;    dep2 -> dep111;
#                      5) yandex -> dep12;


LEGAL_MOVES = [
    [('dep1', 'dep2'), ('dep11', 'dep12')],
    [('dep111', 'yandex'), ('new1', 'dep111'), ('dep2', 'new1')],
    [('dep11', 'dep2'), ('new1', 'dep111'), ('new2', 'new1'), ('dep1', 'new2')],
    [('dep11', 'delete'), ('dep111', 'dep2'), ('dep12', 'dep111')],
]

ILLEGAL_MOVES = [
    [('new1', 'new2'), ('new2', 'new3'), ('new3', 'new1')],
    [('dep1', 'dep111')],
    [('dep11', 'dep12'), ('dep1', 'dep111')],
    [('dep1', 'dep2'), ('new1', 'dep11'), ('dep111', 'new1'), ('dep2', 'dep111')],
    [('yandex', 'dep12')],
]

ALL_MOVES = [(move, True) for move in LEGAL_MOVES] + [(move, False) for move in ILLEGAL_MOVES]


@pytest.mark.django_db
@pytest.mark.parametrize('moves_list, correct', ALL_MOVES)
def test_department_consistancy_validator_moves(
    company, moves_list, correct, department_creating_action, department_moving_action, department_deleting_action
):

    url_by_code = dict(Department.objects.values_list('code', 'url'))

    def generate_actions(moves_list):
        actions = []
        for code, parent_code in moves_list:
            department_url = url_by_code.get(code)
            parent_url = url_by_code.get(parent_code)
            fake_parent_url = parent_code if not parent_url else ''
            if parent_code == 'delete':
                actions.append(department_deleting_action(department_url))
            elif code in url_by_code:
                actions.append(department_moving_action(department_url, parent_url, fake_parent_url))
            else:
                actions.append(department_creating_action(parent_url, fake_parent_url, fake_id=code))
        return actions

    actions = generate_actions(moves_list)
    validator = DepartmentConsistancyValidator(actions)
    if correct:
        validator.check_creating_cycles()  # Не упало значит ок
        validator.check_moving_to_deleting_department()
    else:
        with pytest.raises(ValidationError) as exc:
            validator.check_creating_cycles()
            validator.check_moving_to_deleting_department()
        assert exc.value.code in ['cyclic_relations', 'wrong_relations']


@pytest.mark.django_db
def test_validator_checking_code_uniqueness(company):
    parent_dep = company.dep1
    child_dep = company.dep12
    actions = [
      {
        'sections': [
          'name',
          'hierarchy',
          'administration',
          'technical'
        ],
        'action_id': 'act_06888',
        'url': '',
        'fake_id': 'dep_89383',
        'name': {
          'name': 'test name',
          'name_en': 'test name en',
          'hr_type': 'true'
        },
        'hierarchy': {
          'parent': parent_dep.url,
          'fake_parent': ''
        },
        'administration': {
          'chief': ''
        },
        'technical': {
          'code': child_dep.code,
          'department_type': company.kinds['21'],
          'category': 'technical',
          'order': ''
        }
      }
    ]

    validator = DepartmentConsistancyValidator(actions)
    code_conflicts = validator.find_creating_dep_with_duplicate_code()

    assert code_conflicts == [(0, child_dep.code)]
