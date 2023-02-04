import pytest
import zipfile
import io

from django.core.urlresolvers import reverse
from django.core.files.uploadedfile import SimpleUploadedFile
from freezegun import freeze_time

from intranet.audit.src.files.models import File


@pytest.mark.skip(reason=("VCR умеет по отдельности aiohttp и boto3, но "
                          "вместе нет, нужно лезть и править, но пока нет времени"))
def test_export_files_controltest_success(db, client, test_vcr, control_test,
                                          controltestipe, ipe, control_step, author, ):
    url = reverse("api_v1:export_files", kwargs={'obj_class': 'controltest',
                                                 'pk': control_test.id,
                                                 })
    with freeze_time('2018-07-05 11:56:58'):
        with test_vcr.use_cassette('test_export_files_create_files'):
            file_one = File.objects.create(
                file=SimpleUploadedFile('some_name.txt', b'some text data'),
                author=author,
            )
            control_test.evidence.add(file_one)

            file_two = File.objects.create(
                file=SimpleUploadedFile('some_name_two.txt', b'some other text data'),
                author=author,
            )
            controltestipe.attach.add(file_two)
            file_three = File.objects.create(
                file=SimpleUploadedFile('some_name_three.txt', b'some another text data'),
                author=author,
            )
            control_step.file.add(file_three)
            file_four = File.objects.create(
                file=SimpleUploadedFile('some_name_four.txt', b'some new text data'),
                author=author,
            )
            control_step.file.add(file_four)
            file_five = File.objects.create(
                file=SimpleUploadedFile('some_name_five.txt', b'some five text data'),
                author=author,
            )
            ipe.evidence.add(file_five)
    with test_vcr.use_cassette('test_export_files_controltest_success'):
        response = client.get(url)
    assert response.status_code == 200

    file_like_object = io.BytesIO(response.content)
    zipfile_ob = zipfile.ZipFile(file_like_object)
    assert sorted(zipfile_ob.namelist()) == sorted([
        'some_name_five.txt', 'some_name_four.txt',
        'some_name.txt', 'some_name_three.txt',
        'some_name_two.txt'
    ])
    file_three = zipfile_ob.open('some_name_three.txt')
    assert file_three.read() == b'some another text data'


def test_export_file_fail_wrong_object_class(db, client, ):
    url = reverse("api_v1:export_files", kwargs={'obj_class': 'controltest1',
                                                 'pk': 1,
                                                 })
    response = client.get(url)
    assert response.status_code == 409
    assert response.json()['message'] == ['Bad obj_class was passed']


def test_export_file_fail_wrong_object_id(db, client, ):
    url = reverse("api_v1:export_files", kwargs={'obj_class': 'controltest',
                                                 'pk': 10,
                                                 })
    response = client.get(url)
    assert response.status_code == 404
    assert response.json()['debug_message'] == 'Request with invalid pk was made'


def test_export_file_success_no_data(db, client, control_test):
    url = reverse("api_v1:export_files", kwargs={'obj_class': 'controltest',
                                                 'pk': control_test.id,
                                                 })
    response = client.get(url)
    assert response.status_code == 200
    assert response.json() == {'detail': 'No attached files found'}
