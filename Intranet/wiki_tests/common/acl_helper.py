from wiki.pages.models import Page
from django.conf import settings
from django.contrib.auth.models import Group as DjangoGroup
from wiki.pages.access.cache import CACHE_USER_GROUPS_BACKEND
from wiki.utils.per_request_memoizer import clean_after_memoize


def drop_acl_cache():
    CACHE_USER_GROUPS_BACKEND.clear()
    clean_after_memoize()


def make_user_staff(user):
    if settings.IS_INTRANET:
        employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
        employee_group.user_set.add(user)


def make_user_outstaff(user):
    if settings.IS_INTRANET:
        employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
        employee_group.user_set.remove(user)


def set_access_author_only(page: Page, new_authors: 'List[User]' = None):
    if new_authors:
        page.authors.set(new_authors)
    set_access(page, 'owner')


def set_access_everyone(page: Page):
    set_access(page, 'common')


def set_access_inherited(page: Page):
    set_access(page, 'inherited')


def set_access_custom(page: Page, users: 'List[User]' = None, groups: 'List[Group]' = None):
    set_access(page, 'restricted', {'users': users or [], 'groups': groups or []})


def set_access(page: Page, type_, restrictions=None):
    page.access_set.all().delete()
    if type_ == 'common':
        if page.parent:
            page.access_set.create(is_common=True)
    if type_ == 'anonymous':
        page.access_set.create(is_anonymous=True)
    if type_ == 'owner':
        page.access_set.create(is_owner=True)
    if type_ == 'inherited':
        # no access object in db
        pass
    if type_ == 'restricted':
        for user in restrictions.get('users', []):
            page.access_set.create(staff=user.staff)
        for group in restrictions.get('groups', []):
            page.access_set.create(group=group)
