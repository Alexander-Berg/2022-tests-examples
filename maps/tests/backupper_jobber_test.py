import yatest
import json
import os
from mock import patch

from maps.infra.ecstatic.backupper.lib.backupper_jobber import BackupperJobber
from maps.infra.ecstatic.backupper.lib.backupper import ContentsInfo, DatasetWithBranches
from maps.infra.ecstatic.backupper.lib.backup_area import BackupArea
from maps.infra.ecstatic.backupper.lib.s3_storage import S3Config
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from maps.pylibs.utils.lib.filesystem import temporary_directory

from .fake import FakeS3Storage, FakeEcstaticClient

tests_data_path = yatest.common.source_path("maps/infra/ecstatic/backupper/tests/data")

dummy_s3_config = S3Config(bucket="", tvm_id=1, tvm_secret="", endpoint_url="", max_attempts=10)

hash_a = "e55982996f53c5ec6f691a1ac88b720804b58b44"
hash_b = "60ba60ec9d256588c9acb74e0a9fa44145e91419"
hash_c = "ece63be3e426ec4b5281aa945d254872164321ac"


class TestBackupperJobber:
    @patch("maps.infra.ecstatic.backupper.lib.backupper_jobber.S3Storage")
    def test_make_backup_schedule(self, mock_s3_storage):
        s3_storage = FakeS3Storage()
        mock_s3_storage.return_value = s3_storage

        # Create an initial environment.
        backup_area = BackupArea(s3_storage)

        backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)
        backup_area.store_contents_info_map({
            hash_a: ContentsInfo(12370, 1)
        })
        backup_area.store_snapshot({
            hash_a: [
                DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])
            ]
        }, "2019_05_18_14_50_10")

        backupper_jobber = BackupperJobber(dummy_s3_config)
        schedule = json.loads(backupper_jobber.make_backup_schedule(
            input_datasets=None,
            excluded_datasets=None,
            max_disk_space=450,
            ecstatic_api=FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS)
        ))

        expected = {
            "datasets_with_branches_by_info_hash": {
                hash_b: [{"version": "271", "name": "dataset-number-one", "branches": ["testing"]}],
                hash_c: [
                    {"version": "182", "name": "dataset-number-three", "branches": ["testing/hold", "stable"]},
                    {"version": "845", "name": "dataset-number-four", "branches": ["testing", "stable"]}
                ],
                hash_a: [{"version": "828", "name": "dataset-number-two", "branches": []}]
            },

            "subtasks_input_data": [
                {hash_b: ["dataset-number-one=271"]},
                {hash_c: ["dataset-number-three=182", "dataset-number-four=845"]}
            ],

            "contents_info_map": {
                hash_b: {"refcount": 0, "size": 446253163},
                hash_c: {"refcount": 0, "size": 35163760},
                hash_a: {"refcount": 1, "size": 12370}
            }
        }

        assert schedule == expected

    @patch("maps.infra.ecstatic.backupper.lib.backupper_jobber.S3Storage")
    def test_make_restore_schedule(self, mock_s3_storage):
        s3_storage = FakeS3Storage()
        mock_s3_storage.return_value = s3_storage

        # Create an initial environment.
        backup_area = BackupArea(s3_storage)

        backup_area.upload_content_by_hash("dataset-number-one_271", hash_b)
        backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)
        backup_area.upload_content_by_hash("dataset-number-three_182", hash_c)

        backup_area.store_snapshot({
            hash_b: [
                DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), [])
            ]
        }, "2019_05_18_14_50_10")

        backup_area.store_snapshot({
            hash_a: [
                DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])
            ],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), []),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), [])
            ]
        }, "2019_05_21_12_30_00")

        backup_area.store_contents_info_map({
            hash_b: ContentsInfo(446253163, 1),
            hash_a: ContentsInfo(12370, 1),
            hash_c: ContentsInfo(35163760, 1)
        })

        backupper_jobber = BackupperJobber(dummy_s3_config)
        schedule = json.loads(backupper_jobber.make_restore_schedule(
            snapshot_name="2019_05_21_12_30_00",
            input_datasets=None,
            max_disk_space=450
        ))

        expected = {
            "subtasks_input_data": [
                {
                    hash_c: ["dataset-number-three=182", "dataset-number-four=845"],
                    hash_a: ["dataset-number-two=828"]
                }
            ]
        }

        assert schedule == expected

    @patch("maps.infra.ecstatic.backupper.lib.backupper_jobber.S3Storage")
    def test_commit_backup(self, mock_s3_storage):
        s3_storage = FakeS3Storage()
        mock_s3_storage.return_value = s3_storage

        # Create an initial environment.
        backup_area = BackupArea(s3_storage)
        backup_area.store_contents_info_map({
            hash_a: ContentsInfo(12370, 1)
        })

        # Assumed initial state of the contents storage area.
        backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)

        # Corresponding schedule.
        schedule = {
            "datasets_with_branches_by_info_hash": {
                hash_b: [{"version": "271", "name": "dataset-number-one", "branches": ["stable"]}],
                hash_c: [
                    {"version": "182", "name": "dataset-number-three", "branches": []},
                    {"version": "845", "name": "dataset-number-four", "branches": ["testing/hold", "stable"]}
                ],
                hash_a: [{"version": "828", "name": "dataset-number-two", "branches": ["testing"]}]
            },

            "subtasks_input_data": [
                {hash_b: ["dataset-number-one=271"]},
                {hash_c: ["dataset-number-three=182", "dataset-number-four=845"]}
            ],

            "contents_info_map": {
                hash_b: {"refcount": 0, "size": 446253163},
                hash_c: {"refcount": 0, "size": 35163760},
                hash_a: {"refcount": 1, "size": 12370}
            }
        }

        # Assumed uploads during backup.
        backup_area.upload_content_by_hash("dataset-number-one_271", hash_b)
        backup_area.upload_content_by_hash("dataset-number-three_182", hash_c)

        backupper_jobber = BackupperJobber(dummy_s3_config)

        with temporary_directory() as tmp_dir:
            local_file = os.path.join(tmp_dir, "schedule")
            with open(local_file, 'w') as file:
                json.dump(schedule, file)
            backupper_jobber.commit_backup(local_file, 7)

        contents_info_map = backup_area.load_contents_info_map()

        expected = {
            hash_b: ContentsInfo(refcount=1, size=446253163),
            hash_c: ContentsInfo(refcount=1, size=35163760),
            hash_a: ContentsInfo(refcount=2, size=12370)
        }

        assert contents_info_map == expected

        info_hashes = backup_area.contents_info_hashes()
        assert info_hashes == {hash_b, hash_c, hash_a}

    @patch("maps.infra.ecstatic.backupper.lib.backupper_jobber.S3Storage")
    def test_rollback_backup(self, mock_s3_storage):
        s3_storage = FakeS3Storage()
        mock_s3_storage.return_value = s3_storage

        # Create an initial environment.
        backup_area = BackupArea(s3_storage)
        backup_area.store_contents_info_map({
            hash_a: ContentsInfo(12370, 1)
        })

        # Assumed initial state of the contents storage area.
        backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)

        # Corresponding schedule.
        schedule = {
            "datasets_with_branches_by_info_hash": {
                hash_b: [{"version": "271", "name": "dataset-number-one", "branches": ["stable"]}],
                hash_c: [
                    {"version": "182", "name": "dataset-number-three", "branches": []},
                    {"version": "845", "name": "dataset-number-four", "branches": ["testing/hold", "stable"]}
                ],
                hash_a: [{"version": "828", "name": "dataset-number-two", "branches": ["testing"]}]
            },

            "subtasks_input_data": [
                {hash_b: ["dataset-number-one=271"]},
                {hash_c: ["dataset-number-three=182", "dataset-number-four=845"]}
            ],

            "contents_info_map": {
                hash_b: {"refcount": 0, "size": 446253163},
                hash_c: {"refcount": 0, "size": 35163760},
                hash_a: {"refcount": 1, "size": 12370}
            }
        }

        # Assumed uploads during backup.
        backup_area.upload_content_by_hash("dataset-number-one_271", hash_b)
        backup_area.upload_content_by_hash("dataset-number-three_182", hash_c)

        backupper_jobber = BackupperJobber(dummy_s3_config)

        with temporary_directory() as tmp_dir:
            local_file = os.path.join(tmp_dir, "schedule")
            with open(local_file, 'w') as file:
                json.dump(schedule, file)
            backupper_jobber.rollback_backup(local_file)

        # Check that we have stepped back into the initial state.
        assert (
            backup_area.load_contents_info_map() ==
            {hash_a: ContentsInfo(refcount=1, size=12370)}
        )

        assert backup_area.contents_info_hashes() == {hash_a}

    @patch("maps.infra.ecstatic.backupper.lib.backupper_jobber.S3Storage")
    def test_rollback_backup_with_failed_content_upload(self, mock_s3_storage):
        s3_storage = FakeS3Storage()
        mock_s3_storage.return_value = s3_storage

        # Create an initial environment.
        backup_area = BackupArea(s3_storage)
        backup_area.store_contents_info_map({
            hash_a: ContentsInfo(12370, 1)
        })

        # Assumed initial state of the contents storage area.
        backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)

        # Corresponding schedule.
        schedule = {
            "datasets_with_branches_by_info_hash": {
                hash_b: [{"version": "271", "name": "dataset-number-one", "branches": ["stable"]}],
                hash_c: [
                    {"version": "182", "name": "dataset-number-three", "branches": []},
                    {"version": "845", "name": "dataset-number-four", "branches": ["testing/hold", "stable"]}
                ],
                hash_a: [{"version": "828", "name": "dataset-number-two", "branches": ["testing"]}]
            },

            "subtasks_input_data": [
                {hash_b: ["dataset-number-one=271"]},
                {hash_c: ["dataset-number-three=182", "dataset-number-four=845"]}
            ],

            "contents_info_map": {
                hash_b: {"refcount": 0, "size": 446253163},
                hash_c: {"refcount": 0, "size": 35163760},
                hash_a: {"refcount": 1, "size": 12370}
            }
        }

        # Assumed uploads during backup.
        # The content ece63be3e426ec4b5281aa945d254872164321ac could not be uploaded.
        backup_area.upload_content_by_hash("dataset-number-one_271", hash_b)

        backupper_jobber = BackupperJobber(dummy_s3_config)

        with temporary_directory() as tmp_dir:
            local_file = os.path.join(tmp_dir, "schedule")
            with open(local_file, 'w') as file:
                json.dump(schedule, file)
            backupper_jobber.rollback_backup(local_file)

        # Check that we have stepped back into the initial state.
        assert backup_area.load_contents_info_map() == {hash_a: ContentsInfo(refcount=1, size=12370)}
        assert backup_area.contents_info_hashes(), {hash_a}
