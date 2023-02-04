import random

import pytest

from kelvin.accounts.models import User
from kelvin.courses.models import Course
from kelvin.projects.models import Project
from kelvin.subjects.models import Subject
from kelvin.tags.models import Tag, TagTypeGenericString


@pytest.fixture()
def simple_project():
    project = Project()
    project.save()
    return project


@pytest.fixture()
def simple_user():
    user = User(
        username=u"tagtester {}".format(random.randrange(1, 32767)),
    )
    user.save()
    return user


@pytest.fixture()
def simple_course(simple_user):
    r = random.randrange(1, 32767)
    user = simple_user

    subject = Subject(
        name=u"generic subject {}".format(r)
    )
    subject.save()

    course = Course(
        owner=user,
        subject=subject,
        name=u"simple course {}".format(r)

    )
    course.save()
    return course


@pytest.fixture()
def rgb_custom_tags():
    red = Tag(
        value='red',
        type=TagTypeGenericString.get_db_type(),
        project=None
    )
    green = Tag(
        value='green',
        type=TagTypeGenericString.get_db_type(),
        project=None
    )
    blue = Tag(
        value='blue',
        type=TagTypeGenericString.get_db_type(),
        project=None
    )
    red.save()
    green.save()
    blue.save()

    return {
        'red': red,
        'green': green,
        'blue': blue
    }
