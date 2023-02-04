import random
from time import time

import vcr
from django.conf import settings
from django.contrib.auth import get_user_model

from intranet.wiki.tests.wiki_tests.common.data_helper import get_cassette_library_dir
from intranet.wiki.tests.wiki_tests.common.ddf_compat import new
from wiki.grids.models import Grid
from wiki.intranet.models import Staff
from wiki.pages.api import save_page
from wiki.pages.models import Page, Revision
from wiki.personalisation.user_cluster import create_personal_page
from wiki.sync.connect.models import Organization
from wiki.users.models import Group
from wiki.utils import timezone
from wiki.utils.supertag import translit

if settings.IS_INTRANET:
    from wiki.intranet.models import Department, GroupMembership
    from wiki.intranet.models.consts import AFFILIATION

if settings.IS_BUSINESS:
    from wiki.users.models import GROUP_TYPES


class FixtureMixin(object):
    _default_user = None
    _default_org = None
    now = timezone.now()

    test_vcr = vcr.VCR(cassette_library_dir=get_cassette_library_dir())

    @property
    def create_user_clusters(self):
        return False

    @property
    def has_personal_cluster(self):
        return True

    def use_cassette(self, cassette_name):
        return self.test_vcr.use_cassette(self.__class__.__name__ + '/' + cassette_name + '.yaml')

    @property
    def department_yandex(self):
        if not hasattr(self, '_department_yandex'):
            if settings.IS_INTRANET:
                try:
                    self._department_yandex = Department.objects.get(url='yandex')
                except Department.DoesNotExist:
                    self._department_yandex = new(
                        Department,
                        parent=None,
                        name='Yandex',
                        code='yandex',
                        url='yandex',
                        id=1,
                    )
                    self._department_yandex.save_base()
            else:
                self._department_yandex = None
        return self._department_yandex

    def create_page(self, **kwargs):
        """
        Method allows to create pages for testing environment by right way.
        Requires only one named param "tag". All other fields of Page model
        are optional.
        """
        if not self._default_user:
            self._default_user = self.get_or_create_user('thasonic')

        page_data = {
            'tag': 'Page',
            'supertag': 'page',
            'title': 'Page',
            'owner': self._default_user,
            'authors_to_add': [self._default_user],
            'last_author': self._default_user,
            'modified_at': self.now,
            'created_at': self.now,
            'page_type': Page.TYPES.PAGE,
            'lang': 'ru',
            'status': 1,
            'comments': 0,
            'files': 0,
            'is_blocking': False,
            'data': '',
            'body': '',
            'with_new_wf': True,
            'is_readonly': False,
        }

        storage_fields = dict((f, kwargs.pop(f)) for f in Page._serialized_props if f in kwargs)
        page_data.update(kwargs)
        if 'tag' in kwargs and 'supertag' not in kwargs:
            page_data['supertag'] = translit(page_data['tag'])

        if Page.objects.filter(supertag=page_data['supertag']).exists():
            return Page.objects.get(supertag=page_data['supertag'])
        page = Page() if page_data['page_type'] in (Page.TYPES.PAGE, Page.TYPES.CLOUD) else Grid()
        for attr in page_data:
            setattr(page, attr, page_data[attr])

        if settings.IS_BUSINESS:
            if not self._default_org:
                self._default_org = self._default_user.orgs.first()
            page.org = self._default_org

        page.save()

        page.authors.add(*page.authors_to_add)

        # if there are fields in storage, set it by setter and save model again
        is_storage_in_use = False
        for f in storage_fields:
            if storage_fields[f] is not None:
                setattr(page, f, storage_fields[f])
                is_storage_in_use = True
        if is_storage_in_use:
            if 'body' in storage_fields:
                save_page(page, str(storage_fields['body']), page_data.get('title'))
            else:
                page.save()
                Revision.objects.create_from_page(page)
        else:
            Revision.objects.create_from_page(page)

        return page

    def get_group_yandex(self):
        if not hasattr(self, '_group_yandex'):
            if settings.IS_INTRANET:
                try:
                    self._group_yandex = Group.objects.get(url='yandex')
                except Group.DoesNotExist:
                    self._group_yandex = new(
                        Group,
                        # An evil hack for cached yandex_group() in wiki.pages.access
                        # Id is from src/wiki/fixtures/group.json
                        id=962,  # An evil hack for cached yandex_group() in access.py
                        name='Яндекс',
                        code='yandex',
                        url='yandex',
                        department=self.department_yandex,
                        parent=None,
                    )
                    self._group_yandex.save_base()
            else:
                self._group_yandex = None
        return self._group_yandex

    def get_or_create_user(self, user_login):
        """
        Returns django user by login. Will create if he does not exist.
        """

        # Staff is the first, because creating of django user emits signal to
        # which creates an old wiki user.
        staff = Staff.objects.get_or_create(
            login=user_login,
            defaults={
                'login_ld': user_login,
                'work_email': user_login + '@yandex-team.ru',
                'lang_ui': 'ru',
                'user': None,
                'created_at': timezone.now(),
                'modified_at': timezone.now(),
                'from_staff_id': 0,
                'tz': 'Europe/Moscow',
                'uid': int(time() * 10000),
                'id': random.randint(1000, 10000),
            },
        )[0]
        if settings.IS_INTRANET:
            staff.affiliation = AFFILIATION.YANDEX
        staff.save()

        if settings.IS_INTRANET:
            GroupMembership.objects.get_or_create(
                group=self.get_group_yandex(),
                staff=staff,
            )
            staff.department = self.department_yandex
            staff.save()

        user = get_user_model().objects.get_or_create(
            username=user_login, defaults={'email': user_login + '@yandex-team.ru'}
        )[0]
        user.profile['use_nodejs_frontend'] = False
        if self.create_user_clusters:
            create_personal_page(user)
        user.has_personal_cluster = self.has_personal_cluster
        user.save()

        if not staff.user:
            staff.user = user
            staff.save()

        return user

    def get_or_create_org(self, dir_id):
        try:
            org = Organization.objects.get(dir_id=dir_id)
        except Organization.DoesNotExist:
            org = Organization(name=dir_id, dir_id=dir_id, label=dir_id, status=Organization.ORG_STATUSES.enabled)
            org.save()
        return org

    def create_file(self, page, user=None, **kwargs):
        from wiki.files.models import File

        return File.objects.create(page=page, user=user or page.owner, **kwargs)

    def create_group(self, **kwargs):
        name = kwargs.pop('name', 'Group')
        if settings.IS_INTRANET:
            return Group.objects.create(
                name=name,
                url=kwargs.pop('url', 'groupurl'),
                created_at=kwargs.pop('created_at', timezone.now()),
                modified_at=kwargs.pop('modified_at', timezone.now()),
                **kwargs
            )
        elif settings.IS_BUSINESS:
            return Group.objects.create(
                name=name,
                dir_id=kwargs.pop('dir_id', str(random.randint(0, 10000))),
                group_type=kwargs.pop('group_type', GROUP_TYPES.group),
            )
        else:
            return Group.objects.create(name=name)
