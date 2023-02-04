from itertools import permutations

from staff.departments.controllers.proposal_action import ordered_actions
from staff.departments.models import Department
from staff.headcounts.tests.factories import HeadcountPositionFactory


def test_action_ordering(
    company, department_creating_action, department_moving_action, department_deleting_action,
    department_renaming_action, person_editing_action, vacancy_editing_action, headcount_editing_action,
):
    """
    Проверка на то, что порядок в заявке не важен.
    """

    edit_yandex = department_renaming_action(company.yandex.url)
    create_dep3_under_yandex = department_creating_action(company.yandex.url, fake_id='dep3')
    move_dep12_to_dep3 = department_moving_action(company.dep12.url, fake_parent_id='dep3')

    delete_dep1 = department_deleting_action(company.dep1.url)
    delete_dep11 = department_deleting_action(company.dep11.url)

    edit_person = person_editing_action('yandex-person', sections=['salary', 'office'])
    edit_vacancy = vacancy_editing_action(company['vacancies']['dep1-vac'].id, fake_department='dep3')
    edit_headcount = headcount_editing_action(HeadcountPositionFactory().code, fake_department='dep3')

    for additional_action in (edit_vacancy, edit_headcount):
        actions_list = [
            edit_yandex, create_dep3_under_yandex, move_dep12_to_dep3,
            delete_dep1, delete_dep11,
            edit_person, additional_action,
        ]

        for action in actions_list:
            action['id'] = Department.objects.get(url=action['url']).id if action.get('url') else None

        expected = [
            edit_yandex, create_dep3_under_yandex, move_dep12_to_dep3,
            edit_person, additional_action,
            delete_dep11, delete_dep1,
        ]
        # TODO: Сложность O(n!), больше 7 элементов не осиливает
        for shuffled_actions_list in permutations(actions_list):
            ordered = list(ordered_actions(shuffled_actions_list))
            for act in ordered:
                act.pop('_execution_order')
            assert ordered == expected
