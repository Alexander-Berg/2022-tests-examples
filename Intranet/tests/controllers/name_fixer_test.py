import pytest

from staff.preprofile.controllers import NameFixer
from staff.preprofile.models import NamePreposition


@pytest.mark.parametrize(
    'value, expected_result',
    [
        ('', ''),
        (None, None),
        ('someone', 'Someone'),
        ('some one', 'Some One'),
        ('SOME ONE', 'Some One'),
        ('SomeOne', 'Someone'),
        ('SomeOne(i think)', 'Someone(I Think)'),
        ('Some-one', 'Some-One'),
        ('Some‐one', 'Some‐One'),
        ('Some−one', 'Some−One'),
        ('Some–one', 'Some–One'),
        ('Some—one', 'Some—One'),
        ('Some\'one', 'Some\'One'),
        ('Some`one', 'Some`One'),
        ('Ба́укова', 'Ба́укова'),
        ('İbrahim Ömer', 'İbrahim Ömer'),
    ],
)
@pytest.mark.django_db
def test_fix_name(value: str, expected_result: str) -> None:
    assert NameFixer().fix_name(value) == expected_result


@pytest.mark.parametrize(
    'name, exclusion, expected_result',
    [
        ('name test lastname', 'TeSt', 'Name TeSt Lastname'),
        ('naMe TEST LASTNAME', 'TeSt', 'Name TeSt Lastname'),
        ('Test LASTNAME', 'TeSt', 'TeSt Lastname'),
        ('name Test', 'TeSt', 'Name TeSt'),
        ('test', 'TeSt', 'Test'),
        ('von', 'von', 'Von'),
        ('von schneider', 'von', 'von Schneider'),
    ],
)
@pytest.mark.django_db
def test_fix_name_exclusion(name: str, exclusion: str, expected_result: str) -> None:
    NamePreposition.objects.create(preposition=exclusion)

    target = NameFixer()

    assert target.fix_name(name) == expected_result
