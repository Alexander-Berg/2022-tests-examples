from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


DEFAULT_APPLICATION_ID = None
DEFAULT_DATASET_ID = None
DEFAULT_COLLECTION_ID = None


def get_datasync_data(user, application_id=None, dataset_id=None, collection_id=None):
    application_id = application_id or DEFAULT_APPLICATION_ID
    dataset_id = dataset_id or DEFAULT_DATASET_ID
    collection_id = collection_id or DEFAULT_COLLECTION_ID
    return http_request_json(
        'GET', get_url() + '/datasync',
        params={
            'application_id': application_id,
            'dataset_id': dataset_id,
            'collection_id': collection_id,
            'uid': user.uid,
        }) >> 200


def add_collection(application_id, dataset_id, collection_id):
    return http_request_json(
        'PUT', get_url() + '/datasync/fake/collection',
        json={
            'application_id': application_id,
            'dataset_id': dataset_id,
            'collection_id': collection_id,
        }) >> 200


def set_default_collection(application_id, dataset_id, collection_id):
    global DEFAULT_APPLICATION_ID
    global DEFAULT_DATASET_ID
    global DEFAULT_COLLECTION_ID
    DEFAULT_APPLICATION_ID = application_id
    DEFAULT_DATASET_ID = dataset_id
    DEFAULT_COLLECTION_ID = collection_id


def pause():
    return http_request_json(
        'PUT', get_url() + '/datasync/fake/paused',
        json={'paused': True}) >> 200


def resume():
    return http_request_json(
        'PUT', get_url() + '/datasync/fake/paused',
        json={'paused': False}) >> 200
