from builtins import object, str

import pytest

from django.core.urlresolvers import reverse

from kelvin.scorm.models import Scorm, ScormResourceUserData


def get_scorm_data(score_raw, lesson_status):
    return {
        'data': {
            'cmi': {
                "completion_status": "",
                "objectives": {
                    "_count": 0,
                    "_children": {}
                },
                "comments_from_learner": {
                    "_count": 0,
                    "_children": ""
                },
                "score": {
                    "raw": 0,
                    "max": 0,
                    "_children": "scaled,raw,min,max",
                    "scaled": 0,
                    "min": 0
                },
                "exit": "",
                "location": "",
                "learner_id": "",
                "time_limit_action": "",
                "learner_preference": {
                    "audio_captioning": 0,
                    "delivery_speed": 0,
                    "audio_level": 0,
                    "language": "",
                    "_children": "audio_level,language,delivery_speed,audio_captioning"
                },
                "launch_data": "",
                "total_time": 0,
                "core": {
                    "lesson_status": lesson_status,
                    "score": {
                        "raw": str(score_raw),
                    },
                    "session_time": "00:02:19",
                    "lesson_location": "15;1"
                },
                "max_time_allowed": "",
                "interactions": {
                    "_count": 0,
                    "_children": ""
                },
                "success_status": 0,
                "comments_from_lms": {
                    "_count": 0,
                    "_children": ""
                },
                "entry": "",
                "scaled_passing_score": 0,
                "learner_name": "",
                "session_time": 0,
                "_version": "1484.11",
                "suspend_data": "lesson_progress=1;101;00;0000;00;00;00;00;0000;00;00;00;00;00;0000;01",
                "credit": "",
                "mode": "",
                "progress_measure": 0,
                "completion_threshold": 0
            }
        }
    }


@pytest.mark.django_db(transaction=True)
class TestScormViewSet(object):
    def test_course_not_passed(self, mocker, jclient, student, student_in_scorm_course):
        course = student_in_scorm_course['course']
        clesson = student_in_scorm_course['clesson']

        jclient.login(student)

        scorm_url = reverse('v2:scorm-detail', args=(clesson.id,))

        clesson_result_url = reverse('v2:course_lesson_result-list')
        response = jclient.post(clesson_result_url, {
            'completed': False,
            'clesson': clesson.id
        })
        assert response.status_code == 201, response.content

        jclient.put(scorm_url, get_scorm_data(score_raw=65, lesson_status='incomplete'))

        course_url = reverse('v2:sirius-courses-strict', args=(course.id,))

        response = jclient.get(course_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert len(content['clessons']) == 1

        (lesson,) = content['clessons']

        assert lesson['points'] == 65
        assert lesson['max_points'] == 100
        assert lesson['progress'] == 0

    def test_course_passed(self, mocker, jclient, student, student_in_scorm_course):
        course = student_in_scorm_course['course']
        clesson = student_in_scorm_course['clesson']

        jclient.login(student)

        scorm_url = reverse('v2:scorm-detail', args=(clesson.id,))

        clesson_result_url = reverse('v2:course_lesson_result-list')
        response = jclient.post(clesson_result_url, {
            'completed': False,
            'clesson': clesson.id
        })
        assert response.status_code == 201, response.content

        response = jclient.put(scorm_url, get_scorm_data(score_raw=76, lesson_status='passed'))
        assert response.status_code == 200, response.content

        course_url = reverse('v2:sirius-courses-strict', args=(course.id,))

        response = jclient.get(course_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert len(content['clessons']) == 1

        (lesson,) = content['clessons']

        assert lesson['points'] == 76
        assert lesson['max_points'] == 100
        assert lesson['progress'] == 100

    def test_course_restart(self, mocker, jclient, student, student_in_scorm_course):
        course = student_in_scorm_course['course']
        clesson = student_in_scorm_course['clesson']

        jclient.login(student)

        scorm_url = reverse('v2:scorm-detail', args=(clesson.id,))

        clesson_result_url = reverse('v2:course_lesson_result-list')
        response = jclient.post(clesson_result_url, {
            'completed': False,
            'clesson': clesson.id
        })
        assert response.status_code == 201, response.content

        jclient.put(scorm_url, get_scorm_data(score_raw=68, lesson_status='passed'))
        jclient.put(scorm_url, get_scorm_data(score_raw=0, lesson_status='incomplete'))

        course_url = reverse('v2:sirius-courses-strict', args=(course.id,))

        response = jclient.get(course_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert len(content['clessons']) == 1

        (lesson,) = content['clessons']

        assert lesson['points'] == 68
        assert lesson['max_points'] == 100
        assert lesson['progress'] == 100


@pytest.mark.django_db
class TestScormResourceUserDataViewSet(object):
    def test_course_not_passed(self, mocker, jclient, student, student_in_scorm_course):
        course = student_in_scorm_course['course']
        clesson = student_in_scorm_course['clesson']
        resource_ref = u'qwerty'

        jclient.force_authenticate(user=student)

        scorm_url = reverse('v3-scorm-package:resources-detail', args=(clesson.id, resource_ref))

        clesson_result_url = reverse('v2:course_lesson_result-list')
        response = jclient.post(clesson_result_url, {
            'completed': False,
            'clesson': clesson.id
        })
        assert response.status_code == 201, response.content

        scorm_data = get_scorm_data(score_raw=65, lesson_status='incomplete')
        jclient.put(scorm_url, scorm_data)

        course_url = reverse('v2:sirius-courses-strict', args=(course.id,))

        response = jclient.get(course_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert len(content['clessons']) == 1

        (lesson,) = content['clessons']

        assert lesson['points'] == 65
        assert lesson['max_points'] == 100
        assert lesson['progress'] == 0

        scorm_resource_user_data = ScormResourceUserData.objects.get(
            clesson=clesson,
            student=student,
            resource_ref=resource_ref,
        )
        assert scorm_resource_user_data.data == scorm_data.get('data', {})

    def test_course_passed(self, mocker, jclient, student, student_in_scorm_course):
        course = student_in_scorm_course['course']
        clesson = student_in_scorm_course['clesson']
        resource_ref = u'qwerty'

        jclient.force_authenticate(user=student)

        scorm_url = reverse('v3-scorm-package:resources-detail', args=(clesson.id, resource_ref))

        clesson_result_url = reverse('v2:course_lesson_result-list')
        response = jclient.post(clesson_result_url, {
            'completed': False,
            'clesson': clesson.id
        })
        assert response.status_code == 201, response.content

        scorm_data = get_scorm_data(score_raw=76, lesson_status='passed')
        jclient.put(scorm_url, scorm_data)

        course_url = reverse('v2:sirius-courses-strict', args=(course.id,))

        response = jclient.get(course_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert len(content['clessons']) == 1

        (lesson,) = content['clessons']

        assert lesson['points'] == 76
        assert lesson['max_points'] == 100
        assert lesson['progress'] == 100

        scorm_resource_user_data = ScormResourceUserData.objects.get(
            clesson=clesson,
            student=student,
            resource_ref=resource_ref,
        )
        assert scorm_resource_user_data.data == scorm_data.get('data', {})

    def test_course_restart(self, mocker, jclient, student, student_in_scorm_course):
        course = student_in_scorm_course['course']
        clesson = student_in_scorm_course['clesson']
        resource_ref = u'qwerty'

        jclient.force_authenticate(user=student)

        scorm_url = reverse('v3-scorm-package:resources-detail', args=(clesson.id, resource_ref))

        clesson_result_url = reverse('v2:course_lesson_result-list')
        response = jclient.post(clesson_result_url, {
            'completed': False,
            'clesson': clesson.id
        })
        assert response.status_code == 201, response.content

        scorm_data1 = get_scorm_data(score_raw=68, lesson_status='passed')
        scorm_data2 = get_scorm_data(score_raw=0, lesson_status='incomplete')
        jclient.put(scorm_url, scorm_data1)
        jclient.put(scorm_url, scorm_data2)

        course_url = reverse('v2:sirius-courses-strict', args=(course.id,))

        response = jclient.get(course_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert len(content['clessons']) == 1

        (lesson,) = content['clessons']

        assert lesson['points'] == 68
        assert lesson['max_points'] == 100
        assert lesson['progress'] == 100

        scorm_resource_user_data = ScormResourceUserData.objects.get(
            clesson=clesson,
            student=student,
            resource_ref=resource_ref,
        )
        assert scorm_resource_user_data.data == scorm_data2.get('data', {})


# @pytest.mark.django_db(transaction=True)
# class TestScormISpringSuiteViewSet:
#     def test_put(self, mocker, jclient, student, student_in_scorm_course):
#         clesson = student_in_scorm_course['clesson']
#
#         jclient.force_authenticate(user=student)
#
#         scorm = Scorm.objects.create(clesson=clesson, student=student)
#
#         scorm_url = reverse('v2:scorm_ispring_suite-detail', args=(clesson.id,))
#
#         response = jclient.put(
#             scorm_url,
#             {
#                 'ispring_suite_data': {
#                     'dr': "<qwerty id=\"1\">ext<nested>inner</nested></qwerty>",
#                 },
#                 'qt': "test",
#             },
#         )
#
#         assert response.status_code == 200
#
#         scorm.refresh_from_db()
#
#         assert scorm.ispring_suite_data == {
#             "test": {
#                 "dr": {
#                     "qwerty": {
#                         "@id": "1",
#                         "#text": "ext",
#                         "nested": "inner"
#                     }
#                 },
#                 "qt": "test",
#             }
#         }
