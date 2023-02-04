import pytest

from django.core.management import call_command

pytestmark = [pytest.mark.django_db]


def migrate(page):
    call_command('tmp_owner_back', start_page_id=page.id, end_page_id=page.id, sleep=0)
    page.refresh_from_db()


def test_set_owner__pass_dismissed(wiki_users, test_page):
    test_page.owner = None
    test_page.save()
    test_page.authors.set([wiki_users.thasonic, wiki_users.asm])

    assert wiki_users.thasonic.id < wiki_users.asm.id

    # set first author
    migrate(test_page)
    assert test_page.owner == wiki_users.thasonic

    # set first not dismissed
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    migrate(test_page)
    assert test_page.owner == wiki_users.asm

    # set first if all dismissed
    wiki_users.asm.staff.is_dismissed = True
    wiki_users.asm.staff.save()

    migrate(test_page)
    assert test_page.owner == wiki_users.thasonic


def test_set_owner__by_status(wiki_users, test_page):
    authors = [wiki_users.thasonic, wiki_users.asm]
    authors.sort(key=lambda x: x.id)

    test_page.authors.set(authors)

    for status in [0, 19]:
        test_page.owner = wiki_users.volozh
        test_page.status = status
        test_page.save()

        migrate(test_page)
        assert test_page.owner == authors[0]


def test_set_owner__as_first_in_related_table(wiki_users, test_page):
    authors = [wiki_users.asm, wiki_users.volozh, wiki_users.chapson]
    authors.sort(key=lambda x: x.id, reverse=True)
    test_page.authors.set(authors[:1])
    test_page.authors.add(authors[1])
    test_page.authors.add(authors[2])

    authors_relation = test_page.authors.through.objects.filter(page=test_page)
    need_first_author_id = authors_relation.order_by('id').first().user_id

    assert need_first_author_id == authors[0].id
    assert test_page.owner.id != need_first_author_id

    migrate(test_page)
    assert test_page.owner.id == need_first_author_id
