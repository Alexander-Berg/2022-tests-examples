import pytest

from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from wiki.featured_pages.homepage import preprocess_markup
from wiki.featured_pages.user_api.views import get_user_visibility

pytestmark = [pytest.mark.django_db]


@only_intranet
def test_filter_by_group(wiki_users, add_user_to_group, groups, geo):
    user = wiki_users.kolomeetz
    add_user_to_group(group=groups.child_group, user=user)
    user.staff.office = geo.redrose
    user.staff.save()
    uv = get_user_visibility(user, 'ru')

    user = wiki_users.asm
    add_user_to_group(group=groups.root_group, user=user)
    user.staff.office = geo.neva
    user.staff.save()
    uv2 = get_user_visibility(user, 'ru')
    spell = f"""#if group={groups.root_group.id}
Root
#endif
// big comment
#if group={groups.child_group.id},{groups.side_group.id}
Child and Size
#endif
#if country={geo.ru.id}
#if office={geo.redrose.id}
RR
#endif
#if office={geo.neva.id}
NV
#endif
#endif
#if country={geo.il.id}
IL
#endif"""
    data = preprocess_markup(spell, uv)
    data2 = preprocess_markup(spell, uv2)

    assert (
        data
        == """Root
Child and Size
RR"""
    )
    assert (
        data2
        == """Root
NV"""
    )
