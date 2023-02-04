import json

from django.core.urlresolvers import reverse
from django.test import override_settings
from django.conf import settings
from django.contrib.auth.models import Permission
from django.contrib.contenttypes.models import ContentType

from intranet.audit.src.users.models import User
from intranet.audit.src.core.models import Control, ControlTest


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_list_view_permissions(db, client, control):
    url = reverse("api_v1:control")
    response = client.get(url)
    assert response.status_code == 403

    permission = Permission.objects.get(name='Can view control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.get(url)
    assert response.status_code == 200


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_detail_view_permissions(db, client, control):
    url = reverse("api_v1:control_detail", kwargs={'pk': control.id})
    response = client.get(url)
    assert response.status_code == 403

    permission = Permission.objects.get(name='Can view control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.get(url)
    assert response.status_code == 200


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_create_view_permissions(db, client,):
    assert Control.objects.count() == 0
    url = reverse("api_v1:control")
    data = {"name": "test",
            "number": "123",
            "control": "some text"}
    response = client.post(url, json.dumps(data), content_type='application/json', )
    assert response.status_code == 403

    permission = Permission.objects.get(name='Can add control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.post(url, json.dumps(data), content_type='application/json', )
    assert response.status_code == 201
    assert Control.objects.count() == 1


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_update_view_permissions(db, client, control):
    data = {"name": "some new name", }
    assert Control.objects.count() == 1
    assert Control.objects.first().name != data['name']
    url = reverse("api_v1:control_detail", kwargs={'pk': control.id})
    response = client.patch(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 403
    permission = Permission.objects.get(name='Can change control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.patch(url, json.dumps(data), content_type='application/json', )
    assert response.status_code == 200
    assert Control.objects.count() == 1
    assert Control.objects.first().name == data['name']


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_count_view_permissions(db, client, control):
    url = reverse("api_v1:count", kwargs={'obj_class': 'control'})
    response = client.get(url)
    assert response.status_code == 403

    permission = Permission.objects.get(name='Can view control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.get(url)
    assert response.status_code == 200


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_run_controlplan_permissions(db, client, control_plan):
    url = reverse("api_v1:controlplan_run")
    assert ControlTest.objects.count() == 0
    response = client.post(url, {'obj_pks': control_plan.id, },)
    assert response.status_code == 403
    assert ControlTest.objects.count() == 0

    permission = Permission.objects.get(name='Can add control test')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.post(url, {'obj_pks': control_plan.id, }, )
    assert response.status_code == 302  # редирект на созданный controltest
    assert ControlTest.objects.count() == 1
    assert ControlTest.objects.first().control_plan_id == control_plan.id


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_export_permissions(db, client, control_plan):
    url = reverse("api_v1:export", kwargs={'obj_class': 'controlplan'})
    response = client.get(url, {'obj_pks': control_plan.id},)
    assert response.status_code == 403

    permission = Permission.objects.create(name='Can export excel',
                                           codename='export_excel',
                                           content_type=ContentType.objects.get(
                                               model='user'
                                           ),
                                           )
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.get(url, {'obj_pks': control_plan.id}, )
    assert response.status_code == 200


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_list_permissions_success(db, client, control, django_assert_num_queries):
    url = reverse("api_v1:permissions", kwargs={'user_data': 'me'})
    response = client.get(url)
    assert response.status_code == 200
    assert response.json() == {}

    permission = Permission.objects.get(name='Can view control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    with django_assert_num_queries(3):
        response = client.get(url)
    assert response.status_code == 200
    assert response.json() == {'core.view_control': True}


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_list_permissions_for_uid_success(db, client, control):
    url = reverse("api_v1:permissions", kwargs={'user_data': settings.TEST_NO_ACCESS_USER_DATA['uid']})
    response = client.get(url)
    assert response.status_code == 200
    assert response.json() == {}

    permission = Permission.objects.get(name='Can view control')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.get(url)
    assert response.status_code == 200
    assert response.json() == {'core.view_control': True}


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_list_permissions_no_access_success(db, client, control):
    url = reverse("api_v1:permissions", kwargs={'user_data': '2222'})
    response = client.get(url)
    assert response.status_code == 403

    permission = Permission.objects.create(name='Can view permissions',
                                           codename='view_permissions',
                                           content_type=ContentType.objects.get(
                                               model='user'
                                           ),
                                           )
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)
    response = client.get(url)
    assert response.status_code == 409


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_files_export_permissions_success(db, client, control_test, ):
    url = reverse("api_v1:export_files", kwargs={'obj_class': 'controltest',
                                                 'pk': control_test.id,
                                                 })
    response = client.get(url)
    assert response.status_code == 403

    permission = Permission.objects.get(name='Can view controltest')
    user = User.objects.get(uid=settings.TEST_NO_ACCESS_USER_DATA['uid'])
    user.user_permissions.add(permission)

    response = client.get(url)
    assert response.status_code == 200
    assert response.json() == {'detail': 'No attached files found'}
