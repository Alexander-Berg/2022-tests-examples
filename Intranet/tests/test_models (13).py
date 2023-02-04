import random
from builtins import object

import pytest

from kelvin.accounts.models import User, UserProject
from kelvin.accounts.utils import get_user_projects
from kelvin.projects.models import Project


class TestGetUserProjects(object):
    """
    Тесты для проверки связи прользователя и проекта
    """
    @pytest.mark.django_db
    def test_user_with_several_projects(self):
        project1 = Project(slug='prj1', nda=False)
        project1.save()
        project2 = Project(slug='prj2', nda=False)
        project2.save()

        user = User(
            username=u"tester {}".format(random.randrange(1, 32767)),
        )

        user.save()

        up1 = UserProject(
            user=user,
            project=project1,

        )
        up1.save()
        up2 = UserProject(
            user=user,
            project=project2,
        )
        up2.save()

        result = get_user_projects(user)

        assert set(result) == set([up1.project, up2.project])

    @pytest.mark.django_db
    def test_user_with_nda_projects(self):
        project1 = Project(slug='prj1', nda=True)
        project1.save()
        project2 = Project(slug='prj2', nda=True)
        project2.save()

        user = User(
            username=u"tester {}".format(random.randrange(1, 32767)),
        )

        user.save()

        up1 = UserProject(
            user=user,
            project=project1,
            nda_accepted=True,

        )
        up1.save()
        up2 = UserProject(
            user=user,
            project=project2,
            nda_accepted=False,
        )
        up2.save()

        result = get_user_projects(user)

        assert up2.project not in result, 'Проект с непринятым NDA не должен попадать в спискок проектов'
