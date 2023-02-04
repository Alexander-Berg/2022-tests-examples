import pytest

from intranet.femida.src.notifications import comments as n

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('related_class', (f.InterviewFactory, f.ChallengeFactory))
def test_assessment_comment_created(related_class):
    related_object = related_class(created_by=f.UserFactory())
    instance = f.CommentFactory(related_object=related_object)
    instance.formatted_text = 'Текст комментария'
    n.notify_about_comment_create(instance, f.UserFactory())
