import pytest
from kelvin.accounts.factories import UserFactory
from kelvin.common.utils_for_tests import CaptureQueriesContext
from kelvin.player.models import PlayerData


@pytest.mark.django_db
class TestPlayerData:
    url = '/api/v3/player/'

    def test_anonymous(self, jclient):
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url)
        assert response.status_code == 401
        assert len(queries_context) == 0

    def test_invalid_data(self, jclient):
        user = UserFactory()
        jclient.login(user=user)

        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url)
        assert response.status_code == 400
        assert len(queries_context) == 0

        response_json = response.json()
        assert response_json['errors'][0]['code'] == 'required'
        assert response_json['errors'][0]['source'] == 'vsid'

        assert response_json['errors'][1]['code'] == 'required'
        assert response_json['errors'][1]['source'] == 'video_id'

        assert response_json['errors'][2]['code'] == 'required'
        assert response_json['errors'][2]['source'] == 'course_lesson_id'

        assert response_json['errors'][3]['code'] == 'required'
        assert response_json['errors'][3]['source'] == 'lesson_problem_id'

    def test_ok(self, jclient):
        user = UserFactory()
        jclient.login(user=user)

        vsid = 'dkfjvkdfjhvdfhviefhbv'
        video_id = 'bfbefbtbrgbrgbnsgr'
        course_lesson_id = 12345
        lesson_problem_id = 7890
        data = {
            'vsid': vsid,
            'video_id': video_id,
            'course_lesson_id': course_lesson_id,
            'lesson_problem_id': lesson_problem_id,
        }
        with CaptureQueriesContext() as queries_context:
            response = jclient.post(self.url, data=data)
        assert response.status_code == 201
        assert len(queries_context) == 1

        assert PlayerData.objects.filter(
            vsid=vsid,
            video_id=video_id,
            course_lesson_id=course_lesson_id,
            lesson_problem_id=lesson_problem_id,
        ).count() > 0
