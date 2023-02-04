import random

from kelvin.accounts.models import User
from kelvin.courses.models import AssignmentRule, Course, CourseLessonLink, Criterion
from kelvin.lessons.models import Lesson
from kelvin.subjects.models import Subject


def create_random_user():
    """ creates simple user with random username """
    user = User(
        username=u"tagtester {}".format(random.randrange(1, 32767)),
    )
    user.save()
    return user


def create_random_course():
    """ creates simple course with random fields: owner, subject, name """
    r = random.randrange(1, 32767)
    user = create_random_user()

    subject = Subject(
        name=u"generic subject {}".format(r),
        slug=u"generic-subject-{}".format(r),
    )
    subject.save()

    course = Course(
        owner=user,
        subject=subject,
        name=u"simple course {}".format(r)
    )

    course.save()
    return course


def create_lesson():
    return Lesson.objects.create(
        owner=create_random_user()
    )


def create_clesson(course, lesson):
    return CourseLessonLink.objects.create(
        course=course,
        lesson=lesson,
    )


def create_clesson_criterion(clesson, assignment_rule):
    return Criterion.objects.create(
        clesson=clesson,
        assignment_rule=assignment_rule,
        priority=0,
        formula=[],
    )


def create_assignment_rule_for_course(course):
    return AssignmentRule.objects.create(
        course=course,
        formula=[]
    )
