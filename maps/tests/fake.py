"""This module implements mock objects and utilities for unit tests"""

import yatest
import datetime
import os
from maps.infra.ecstatic.proto import coordinator_pb2
from botocore.exceptions import ClientError


tests_data_path = yatest.common.source_path("maps/infra/ecstatic/backupper/tests/data")


# datetime cannot be mocked since it is a C module.
# https://stackoverflow.com/a/192857
def CreateFakeDateTime(year, month, day, hour, minute, second):
    class FakeDateTime(datetime.datetime):
        @classmethod
        def now(cls):
            return cls(year, month, day, hour, minute, second)
    return FakeDateTime


# Simulate an error from the boto3 library.
def raiseFakeBotoError():
    error_response = {'Error': {'Code': 404}}
    e = ClientError(error_response, 'download_file')
    raise e


# Fake S3 storage implemented as a dictionary filename -> content.
class FakeS3Storage(object):
    def __init__(self):
        self.bucket = "test_bucket"
        self.files = {}

        class FakeLazyDeleter(object):
            def __init__(self, fake_s3_storage):
                self.fake_s3_storage = fake_s3_storage

            def __enter__(self):
                return self

            def __exit__(self, type, value, traceback):
                pass

            def delete(self, filename):
                # We imitate S3's behavior.
                if filename in self.fake_s3_storage.files:
                    del self.fake_s3_storage.files[filename]

        self.deleter = FakeLazyDeleter(self)

    def download_file(self, remote_path, local_path):
        data = self.files.get(remote_path)
        if not data:
            raiseFakeBotoError()

        with open(local_path, 'w') as f:
            f.write(data)

    def upload_file(self, local_path, remote_path):
        with open(local_path) as f:
            self.files[remote_path] = f.read()

    def remove_file(self, remote_file):
        self.deleter.delete(remote_file)

    def download_folder(self, remote_path, local_path):
        found = True
        if os.path.join(remote_path, "dir_1", "file_1") not in self.files:
            found = False
        if os.path.join(remote_path, "dir_2", "file_2") not in self.files:
            found = False
        if os.path.join(remote_path, "dir_2", "subdir_1", "file_3") not in self.files:
            found = False
        if not found:
            raiseFakeBotoError()

    def upload_folder(self, local_path, remote_path):
        self.files[os.path.join(remote_path, "dir_1", "file_1")] = "content_1"
        self.files[os.path.join(remote_path, "dir_2", "file_2")] = "content_2"
        self.files[os.path.join(remote_path, "dir_2", "subdir_1", "file_3")] = "content_3"

    def remove_folder(self, remote_path):
        self.remove_files(self.list_objects(remote_path))

    def remove_files(self, remote_files):
        for f in remote_files:
            self.deleter.delete(f)

    def list_objects(self, remote_path, is_recursive=True):
        def first_component(p):
            out = None
            head, tail = os.path.split(p)
            while len(tail) > 0:
                out = tail
                head, tail = os.path.split(head)
            return out

        objects = [f for f in self.files.keys() if f.startswith(remote_path)]
        if not is_recursive:
            objects = [first_component(os.path.relpath(f, remote_path)) for f in objects]
        return objects

    def destroy_bucket(self):
        pass


class FakeEcstaticClient(object):
    MODE_EMPTY = 0
    MODE_TORRENTS = 1

    def __init__(self, mode):
        if mode == self.MODE_EMPTY:
            self.creator = self._create_empty_response
        elif mode == self.MODE_TORRENTS:
            self.creator = self._create_response_with_torrents
        else:
            raise Exception("invalid mode")

    def _create_empty_response(self):
        blob = coordinator_pb2.TorrentsBackupInfo()
        return blob.SerializeToString()

    def _create_response_with_torrents(self):
        blob = coordinator_pb2.TorrentsBackupInfo()

        # 1st torrent: 1 dataset
        descriptor = blob.torrent_descriptors.add()
        descriptor.info_hash = "60ba60ec9d256588c9acb74e0a9fa44145e91419"

        data_path = os.path.join(tests_data_path, descriptor.info_hash + ".torrent")
        with open(data_path, 'rb') as file:
            descriptor.torrent_body = file.read()

        qs = descriptor.qualified_datasets.add()
        qs.dataset = "dataset-number-one"
        qs.tag = ""
        qs.version = "271"

        # 2nd torrent: 1 dataset
        descriptor_2 = blob.torrent_descriptors.add()
        descriptor_2.info_hash = "e55982996f53c5ec6f691a1ac88b720804b58b44"

        data_path = os.path.join(tests_data_path, descriptor_2.info_hash + ".torrent")
        with open(data_path, 'rb') as file:
            descriptor_2.torrent_body = file.read()

        qs_2 = descriptor_2.qualified_datasets.add()
        qs_2.dataset = "dataset-number-two"
        qs_2.tag = ""
        qs_2.version = "828"

        # 3rd torrent: 2 datasets
        descriptor_3 = blob.torrent_descriptors.add()
        descriptor_3.info_hash = "ece63be3e426ec4b5281aa945d254872164321ac"

        data_path = os.path.join(tests_data_path, descriptor_3.info_hash + ".torrent")
        with open(data_path, 'rb') as file:
            descriptor_3.torrent_body = file.read()

        qs_3 = descriptor_3.qualified_datasets.add()
        qs_3.dataset = "dataset-number-three"
        qs_3.tag = ""
        qs_3.version = "182"

        qs_4 = descriptor_3.qualified_datasets.add()
        qs_4.dataset = "dataset-number-four"
        qs_4.tag = ""
        qs_4.version = "845"

        # Describe in which branches some datasets are to be found.
        entry_1 = blob.branches_by_versioned_dataset.add()
        entry_1.dataset = "dataset-number-one"
        entry_1.version = "271"

        branch_hold = entry_1.branches_hold.add()
        branch_hold.name = "testing"
        branch_hold.on_hold = False

        entry_3 = blob.branches_by_versioned_dataset.add()
        entry_3.dataset = "dataset-number-three"
        entry_3.version = "182"

        branch_hold = entry_3.branches_hold.add()
        branch_hold.name = "testing"
        branch_hold.on_hold = True

        branch_hold = entry_3.branches_hold.add()
        branch_hold.name = "stable"
        branch_hold.on_hold = False

        entry_4 = blob.branches_by_versioned_dataset.add()
        entry_4.dataset = "dataset-number-four"
        entry_4.version = "845"

        branch_hold = entry_4.branches_hold.add()
        branch_hold.name = "testing"
        branch_hold.on_hold = False

        branch_hold = entry_4.branches_hold.add()
        branch_hold.name = "stable"
        branch_hold.on_hold = False

        return blob.SerializeToString()

    def all_torrents(self):
        return self.creator()
