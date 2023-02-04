from hamcrest import assert_that, instance_of

from billing.yandex_pay.yandex_pay.file_storage import FileStorageBase
from billing.yandex_pay.yandex_pay.file_storage.base import Resource


class ExampleResource(Resource):
    @classmethod
    def create(cls):
        return cls()


class ExampleFileStorage(FileStorageBase):
    example_resource: ExampleResource
    not_a_resource: int


def test_example_file_storage_initialization():
    file_storage = ExampleFileStorage()

    assert_that(file_storage.example_resource, instance_of(ExampleResource))


def test_initializes_only_resources():
    file_storage = ExampleFileStorage()

    assert not hasattr(file_storage, 'not_a_resource')
