from django.utils.translation import override

from common import factories


def check_inflections(staff, expected_inflections):
    assert staff.inflections.subjective == expected_inflections[0]
    assert staff.inflections.genitive == expected_inflections[1]
    assert staff.inflections.dative == expected_inflections[2]
    assert staff.inflections.accusative == expected_inflections[3]
    assert staff.inflections.ablative == expected_inflections[4]
    assert staff.inflections.prepositional == expected_inflections[5]


def test_staff_inflections():
    staff = factories.StaffFactory(first_name='Иван', last_name='Иванов')
    expected_inflections = [
        'Иван Иванов', 'Ивана Иванова', 'Ивану Иванову', 'Ивана Иванова', 'Иваном Ивановым', 'Иване Иванове'
    ]
    with override('ru'):
        check_inflections(staff, expected_inflections)


def test_uninflectable_staff_inflections():
    staff = factories.StaffFactory(first_name='Цинь', last_name='Шихуанди')
    expected_inflections = ['Цинь Шихуанди'] * 6
    with override('ru'):
        check_inflections(staff, expected_inflections)


def test_latin_staff_inflections():
    staff = factories.StaffFactory(first_name='George', last_name='Washington')
    expected_inflections = ['George Washington'] * 6
    with override('ru'):
        check_inflections(staff, expected_inflections)
