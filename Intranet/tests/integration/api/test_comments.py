import pytest

from django.urls.base import reverse

from intranet.femida.src.candidates.choices import (
    CHALLENGE_RESOLUTIONS,
    CHALLENGE_TYPES,
    CHALLENGE_STATUSES,
)
from intranet.femida.src.interviews.models import Interview

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


create_interview = lambda: f.create_interview(state=Interview.STATES.finished)
create_challenge = lambda: f.ChallengeFactory(
    resolution=CHALLENGE_RESOLUTIONS.hire,
    type=CHALLENGE_TYPES.quiz,
    status=CHALLENGE_STATUSES.finished,
)


related_objects_data = (
    (create_interview, 'api:interviews:comments'),
    (create_challenge, 'api:challenges:comments'),
    (f.ComplaintFactory, 'api:complaints:comments'),
)


@pytest.mark.parametrize('related_object_builder, view_name', related_objects_data)
def test_comment_list(su_client, related_object_builder, view_name):
    related_object = related_object_builder()
    f.CommentFactory.create(related_object=related_object)
    url = reverse(view_name, kwargs={'pk': related_object.id})
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('related_object_builder, view_name', related_objects_data)
def test_comment_create(su_client, related_object_builder, view_name):
    related_object = related_object_builder()
    url = reverse(view_name, kwargs={'pk': related_object.id})
    data = {
        'text': 'text',
    }
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_comment_update(su_client):
    user = f.get_superuser()
    interview = f.create_interview()
    comment = f.CommentFactory.create(related_object=interview, created_by=user)
    url = reverse('api:comments:detail', kwargs={'pk': comment.id})
    data = {
        'text': 'text',
    }
    response = su_client.put(url, data)
    assert response.status_code == 200


def test_comment_remove(su_client):
    user = f.get_superuser()
    interview = f.create_interview()
    comment = f.CommentFactory.create(related_object=interview, created_by=user)
    url = reverse('api:comments:detail', kwargs={'pk': comment.id})
    response = su_client.delete(url)
    assert response.status_code == 204
