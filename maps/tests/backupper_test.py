import datetime
import os
import pytest

import yatest
from maps.infra.ecstatic.backupper.lib.backup_area import BackupArea
from maps.infra.ecstatic.backupper.lib.backupper import Backupper, BackupperException, ContentsInfo, DatasetWithBranches
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from .fake import FakeS3Storage, FakeEcstaticClient, CreateFakeDateTime

tests_data_path = yatest.common.source_path("maps/infra/ecstatic/backupper/tests/data")

hash_a = "e55982996f53c5ec6f691a1ac88b720804b58b44"
hash_b = "60ba60ec9d256588c9acb74e0a9fa44145e91419"
hash_c = "ece63be3e426ec4b5281aa945d254872164321ac"


class TestBackupper:
    def test_get_timestamp(self):
        assert (
            Backupper.create_timestamp(datetime.datetime(year=2019, month=5, day=20, hour=17, minute=43, second=26)) ==
            "2019_05_20_17_43_26")

        assert (
            Backupper.create_timestamp(datetime.datetime(year=2019, month=5, day=20, hour=0, minute=43, second=00)) ==
            "2019_05_20_00_43_00")

    def test_content_size_from_body(self):
        data_path_1 = os.path.join(tests_data_path, hash_b + ".torrent")
        with open(data_path_1, 'rb') as file:
            size_1 = Backupper._content_size_from_body(file.read())

        assert size_1 == 446253163

        data_path_2 = os.path.join(tests_data_path, hash_a + ".torrent")
        with open(data_path_2, 'rb') as file:
            size_2 = Backupper._content_size_from_body(file.read())

        assert size_2 == 12370

        data_path_3 = os.path.join(tests_data_path, hash_c + ".torrent")
        with open(data_path_3, 'rb') as file:
            size_3 = Backupper._content_size_from_body(file.read())

        assert size_3 == 35163760

    def test_init_empty_backup_no_datasets(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_EMPTY))

        assert len(job.datasets_with_branches_by_info_hash) == 0
        assert len(job.contents_info_map) == 0

    def test_init_empty_backup_datasets(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        # Check that the internal structures store expected values.
        assert job.datasets_with_branches_by_info_hash == {
            hash_b: [DatasetWithBranches(Dataset("dataset-number-one", "271"), ["testing"])],
            hash_a: [DatasetWithBranches(Dataset("dataset-number-two", "828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset("dataset-number-three", "182"), ["testing/hold", "stable"]),
                DatasetWithBranches(Dataset("dataset-number-four", "845"), ["testing", "stable"])
            ]
        }

        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 0),
            hash_a: ContentsInfo(12370, 0),
            hash_c: ContentsInfo(35163760, 0)
        }

    def test_init_one_content_already_backed_up(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        backupper.backup_area.store_contents_info_map({
            hash_a: ContentsInfo(12370, 1)
        })

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 0),
            hash_a: ContentsInfo(12370, 1),
            hash_c: ContentsInfo(35163760, 0)
        }

    def test_init_with_input_datasets_one_known_dataset(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        input_datasets = set()
        input_datasets.add(("dataset-number-one", "271"))
        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS), input_datasets)

        assert job.datasets_with_branches_by_info_hash == {
            hash_b: [DatasetWithBranches(Dataset("dataset-number-one", "271"), ["testing"])]
        }

        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 0)
        }

    def test_init_with_input_datasets_two_known_datasets(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        input_datasets = set()
        input_datasets.add(("dataset-number-one", "271"))
        input_datasets.add(("dataset-number-two", "828"))
        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS), input_datasets)

        assert job.datasets_with_branches_by_info_hash == {
            hash_b: [DatasetWithBranches(Dataset("dataset-number-one", "271"), ["testing"])],
            hash_a: [DatasetWithBranches(Dataset("dataset-number-two", "828"), [])]
        }

        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 0),
            hash_a: ContentsInfo(12370, 0)
        }

    def test_init_with_input_datasets_one_known_dataset_one_unkown_dataset(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        input_datasets = set()
        input_datasets.add(("dataset-number-one", "271"))
        input_datasets.add(("dataset-number-nine", "314"))

        with pytest.raises(BackupperException, match="One or more of the specified datasets are incorrect"):
            backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS), input_datasets)

    def test_init_with_one_excluded_dataset(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        excluded_datasets = set()
        excluded_datasets.add("dataset-number-one")

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS),
                                           excluded_datasets=excluded_datasets)

        # Check that the internal structures store expected values.
        assert job.datasets_with_branches_by_info_hash == {
            hash_a: [DatasetWithBranches(Dataset("dataset-number-two", "828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset("dataset-number-three", "182"), ["testing/hold", "stable"]),
                DatasetWithBranches(Dataset("dataset-number-four", "845"), ["testing", "stable"])
            ]
        }

        assert job.contents_info_map == {
            hash_a: ContentsInfo(12370, 0),
            hash_c: ContentsInfo(35163760, 0)
        }

    def test_subtask_input_data_generator_no_content(self):
        # Create an initial environment.

        # 450 Mb
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=450,
            max_stored_snapshots=7
        )

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_EMPTY))

        # Check that we have nothing to back up.
        count = 0
        for _ in backupper.subtask_input_data_generator(job):
            count += 1

        assert count == 0

    def test_subtask_input_data_generator_no_referenced_content(self):
        # Create an initial environment.

        # 450 Mb
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=450,
            max_stored_snapshots=7
        )

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        # Check that we have all the unreferenced content
        # and each generated set of data has a size < max_disk_space

        unreferenced_content_count = 0
        for _, contents_info in job.contents_info_map.items():
            if contents_info.refcount == 0:
                unreferenced_content_count += 1

        count = 0
        for subtask_input_data in backupper.subtask_input_data_generator(job):
            subtask_input_data_size = 0
            count += len(subtask_input_data)
            for info_hash, _ in subtask_input_data.items():
                assert info_hash is not None
                contents_info = job.contents_info_map[info_hash]
                assert contents_info.refcount == 0
                subtask_input_data_size += contents_info.size
            assert subtask_input_data_size <= backupper.max_disk_space

        assert count == unreferenced_content_count

    def test_subtask_input_data_generator_no_referenced_content_unlimited_size(self):
        # Create an initial environment.
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()))

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        count = 0
        for _ in backupper.subtask_input_data_generator(job):
            count += 1
        assert count == 1

    def test_subtask_input_data_generator_one_referenced_content(self):
        # Create an initial environment.

        # 450 Mb
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=450,
            max_stored_snapshots=7
        )

        backupper.backup_area.store_contents_info_map({
            hash_a: ContentsInfo(12370, 1)
        })

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        # Check that we have all the unreferenced content
        # and each generated set of data has a size < max_disk_space

        unreferenced_content_count = 0
        for _, contents_info in job.contents_info_map.items():
            if contents_info.refcount == 0:
                unreferenced_content_count += 1

        count = 0
        for subtask_input_data in backupper.subtask_input_data_generator(job):
            subtask_input_data_size = 0
            count += len(subtask_input_data)
            for info_hash, _ in subtask_input_data.items():
                assert info_hash is not None
                contents_info = job.contents_info_map[info_hash]
                assert contents_info.refcount == 0
                subtask_input_data_size += contents_info.size
            assert subtask_input_data_size <= backupper.max_disk_space

        assert count == unreferenced_content_count

    def test_subtask_input_data_generator_all_content_referenced(self):
        # Create an initial environment.

        # 450 Mb
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=450,
            max_stored_snapshots=7
        )

        # Create an initial environment.
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        # Check that we have nothing to back up.
        count = 0
        for _ in backupper.subtask_input_data_generator(job):
            count += 1

        assert count == 0

    def test_collect_garbage_no_snapshots(self):
        # Create an initial environment.
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        backupper.backup_area.store_contents_info_map({})

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_EMPTY))

        # Check that garbage collections works correctly on empty data.
        backupper._remove_expired_snapshots(job)

        assert len(job.contents_info_map) == 0

        snapshots_list = backupper.backup_area.snapshots_list()
        assert len(snapshots_list) == 0

    def test_collect_garbage_no_snapshots_expired(self):
        # Create an initial environment.

        # 7 snapshots
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        self.setup_contents(backupper.backup_area)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_EMPTY))

        backupper._remove_expired_snapshots(job)

        # Check that in-core information on contents have not changed.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 3),
            hash_a: ContentsInfo(12370, 3),
            hash_c: ContentsInfo(35163760, 3)
        }

        for info_hash in job.contents_info_map.keys():
            file_list = backupper.backup_area.storage.list_objects(BackupArea.Layout.content_storage_path(info_hash))
            assert len(file_list) != 0

        assert (
            backupper.backup_area.snapshots_list() ==
            ["2019_05_18_14_50_10", "2019_05_19_12_30_00", "2019_05_20_12_30_00", "2019_05_21_12_30_00"])

    def test_collect_garbage_some_snapshots_expired(self):
        # Create an initial environment.

        # 2 snapshots
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=2
        )

        # Store contents.
        self.setup_contents(backupper.backup_area)

        # Create a bunch of snapshots.
        self.setup_snapshots(backupper.backup_area)
        backupper.backup_area.store_snapshot({
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), [])]
        }, "2019_05_18_14_50_10")

        backupper.backup_area.store_snapshot({
            hash_b: [
                DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), [])
            ],
            hash_a: [
                DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])
            ],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), [])
            ]
        }, "2019_05_19_12_30_00")

        backupper.backup_area.store_snapshot({
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), []),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), [])
            ]
        }, "2019_05_20_12_30_00")

        backupper.backup_area.store_snapshot({
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), []),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), [])
            ]
        }, "2019_05_21_12_30_00")

        backupper.backup_area.store_contents_info_map({
            hash_b: ContentsInfo(446253163, 2),
            hash_a: ContentsInfo(12370, 3),
            hash_c: ContentsInfo(35163760, 3)
        })

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_EMPTY))

        backupper._remove_expired_snapshots(job)

        # Check that in-core information on contents have been correctly updated.
        assert job.contents_info_map == {
            hash_a: ContentsInfo(12370, 1),
            hash_c: ContentsInfo(35163760, 1)
        }

        # Check that the content whose info hash is 60ba60ec9d256588c9acb74e0a9fa44145e91419
        # has been deleted.
        file_list = backupper.backup_area.storage.list_objects(BackupArea.Layout.content_storage_path(hash_b))
        assert len(file_list) == 0

        # Check that the remaining two contents are still stored.
        file_list = backupper.backup_area.storage.list_objects(BackupArea.Layout.content_storage_path(hash_a))
        assert len(file_list) != 0
        file_list = backupper.backup_area.storage.list_objects(BackupArea.Layout.content_storage_path(hash_c))
        assert len(file_list) != 0

        assert backupper.backup_area.snapshots_list() == ["2019_05_21_12_30_00"]

    def test_create_snapshot_one_new_snapshot_no_snapshots_expired(self):
        # Create an initial environment.

        # 7 snapshots
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=7
        )

        self.setup_contents(backupper.backup_area)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        datetime.datetime = CreateFakeDateTime(2019, 5, 22, 19, 17, 00)

        snapshot_name = backupper.create_and_store_snapshot(job)
        assert snapshot_name == "2019_05_22_19_17_00"

        # Check that in-core information on contents have been updated accordingly.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 4),
            hash_a: ContentsInfo(12370, 4),
            hash_c: ContentsInfo(35163760, 4)
        }

        # Check that all the contents are still stored.
        for info_hash in job.contents_info_map.keys():
            file_list = backupper.backup_area.storage.list_objects(BackupArea.Layout.content_storage_path(info_hash))
            assert len(file_list) != 0

        assert (
            backupper.backup_area.snapshots_list() ==
            ["2019_05_18_14_50_10", "2019_05_19_12_30_00", "2019_05_20_12_30_00",
             "2019_05_21_12_30_00", "2019_05_22_19_17_00"])

        snapshot = backupper.backup_area.load_snapshot("2019_05_22_19_17_00")

        assert snapshot == {
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), ["testing"])],
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), ["testing/hold", "stable"]),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), ["testing", "stable"])
            ]
        }

    def test_create_snapshot_one_new_snapshot_some_snapshots_expired(self):
        # Create an initial environment.

        # 2 snapshots
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=2
        )

        self.setup_contents(backupper.backup_area)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        datetime.datetime = CreateFakeDateTime(2019, 5, 22, 19, 17, 00)

        snapshot_name = backupper.create_and_store_snapshot(job)
        assert snapshot_name == "2019_05_22_19_17_00"

        # Check that in-core information on contents have been updated accordingly.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 1),
            hash_a: ContentsInfo(12370, 2),
            hash_c: ContentsInfo(35163760, 2)
        }

        # Check that all the contents are still stored.
        for info_hash in job.contents_info_map.keys():
            file_list = backupper.backup_area.storage.list_objects(BackupArea.Layout.content_storage_path(info_hash))
            assert len(file_list) != 0

        assert backupper.backup_area.snapshots_list() == ["2019_05_21_12_30_00", "2019_05_22_19_17_00"]

        snapshot = backupper.backup_area.load_snapshot("2019_05_22_19_17_00")
        assert snapshot == {
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), ["testing"])],
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), ["testing/hold", "stable"]),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), ["testing", "stable"])
            ]
        }

    def test_create_snapshot_initial_snapshot(self):
        # Create an initial environment.

        # 2 snapshots
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=2
        )

        backupper.backup_area.store_contents_info_map({})

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        # Simulate contents upload.
        self.setup_contents(backupper.backup_area)

        datetime.datetime = CreateFakeDateTime(2019, 5, 22, 19, 17, 00)

        snapshot_name = backupper.create_and_store_snapshot(job)
        assert snapshot_name == "2019_05_22_19_17_00"

        # Check that in-core information on contents have been updated accordingly.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 1),
            hash_a: ContentsInfo(12370, 1),
            hash_c: ContentsInfo(35163760, 1)
        }

        assert backupper.backup_area.snapshots_list() == ["2019_05_22_19_17_00"]

        snapshot = backupper.backup_area.load_snapshot("2019_05_22_19_17_00")
        assert snapshot == {
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), ["testing"])],
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), ["testing/hold", "stable"]),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), ["testing", "stable"])
            ]
        }

    def test_create_snapshot_initial_snapshot_with_failure(self):
        # Create an initial environment.

        # 2 snapshots
        backupper = Backupper(
            backup_area=BackupArea(FakeS3Storage()),
            max_disk_space=100*1024,
            max_stored_snapshots=2
        )

        backupper.backup_area.store_contents_info_map({})

        job = backupper.prepare_backup_job(FakeEcstaticClient(FakeEcstaticClient.MODE_TORRENTS))

        # Simulate contents upload.
        # We failed to store ece63be3e426ec4b5281aa945d254872164321ac.
        backupper.backup_area.upload_content_by_hash("dataset-number-one_271", hash_b)
        backupper.backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)

        datetime.datetime = CreateFakeDateTime(2019, 5, 22, 19, 17, 00)

        snapshot_name = backupper.create_and_store_snapshot(job)
        assert snapshot_name == "2019_05_22_19_17_00"

        # Check that in-core information on contents have been updated accordingly.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 1),
            hash_a: ContentsInfo(12370, 1),
        }

        assert backupper.backup_area.snapshots_list() == ["2019_05_22_19_17_00"]

        snapshot = backupper.backup_area.load_snapshot("2019_05_22_19_17_00")
        assert snapshot == {
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), ["testing"])],
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])]
        }

    def test_init_restore_empty(self):
        # Create an initial environment.
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()), max_disk_space=100*1024)

        with pytest.raises(BackupperException, match="no snapshot found"):
            backupper.prepare_restore_job("2019_05_18_14_50_10")

    def test_init_restore_with_snapshots(self):
        # Create an initial environment.
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()), max_disk_space=100*1024)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_restore_job("2019_05_21_12_30_00")

        # Check information on contents.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 3),
            hash_a: ContentsInfo(12370, 3),
            hash_c: ContentsInfo(35163760, 3)
        }

        # Check datasets_with_branches_by_info_hash
        assert job.datasets_with_branches_by_info_hash == {
            hash_a: [DatasetWithBranches(Dataset("dataset-number-two", "828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset("dataset-number-three", "182"), []),
                DatasetWithBranches(Dataset("dataset-number-four", "845"), [])
            ]
        }

        assert hash_b not in job.datasets_with_branches_by_info_hash

    def test_init_restore_with_snapshots_2(self):
        # Create an initial environment.
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()), max_disk_space=100*1024)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_restore_job("2019_05_20_12_30_00")

        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 3),
            hash_a: ContentsInfo(12370, 3),
            hash_c: ContentsInfo(35163760, 3)
        }

        assert job.datasets_with_branches_by_info_hash == {
            hash_b: [DatasetWithBranches(Dataset("dataset-number-one", "271"), [])],
            hash_a: [DatasetWithBranches(Dataset("dataset-number-two", "828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset("dataset-number-three", "182"), []),
                DatasetWithBranches(Dataset("dataset-number-four", "845"), [])
            ]
        }

    def test_init_restore_with_good_input_datasets(self):
        # Create an initial environment.
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()), max_disk_space=100*1024)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)
        input_datasets = {("dataset-number-two", "828"), ("dataset-number-three", "182")}

        job = backupper.prepare_restore_job("2019_05_21_12_30_00", input_datasets)

        # Check information on contents.
        assert job.contents_info_map == {
            hash_b: ContentsInfo(446253163, 3),
            hash_a: ContentsInfo(12370, 3),
            hash_c: ContentsInfo(35163760, 3)
        }

        # Check datasets_with_branches_by_info_hash
        assert job.datasets_with_branches_by_info_hash == {
            hash_a: [DatasetWithBranches(Dataset("dataset-number-two", "828"), [])],
            hash_c: [DatasetWithBranches(Dataset("dataset-number-three", "182"), [])]
        }

    def test_init_restore_with_one_bad_input_dataset(self):
        # Create an initial environment.
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()), max_disk_space=100*1024)
        self.setup_snapshots(backupper.backup_area)
        input_datasets = {("dataset-number-two", "828"), ("dataset-nonsensical", "999")}

        with pytest.raises(BackupperException, match="One or more of the specified datasets are incorrect"):
            backupper.prepare_restore_job("2019_05_21_12_30_00", input_datasets)

    def test_restore_subtask_input_data_generator(self):
        # Create an initial environment.

        # 450 Mb
        backupper = Backupper(backup_area=BackupArea(FakeS3Storage()), max_disk_space=450)
        self.setup_snapshots(backupper.backup_area)
        self.setup_contents_info_map(backupper.backup_area)

        job = backupper.prepare_restore_job("2019_05_20_12_30_00")

        # Check that each generated set of data has a size < max_disk_space
        info_hashes = []
        for subtask_input_data in backupper.subtask_input_data_generator(job):
            subtask_input_data_size = 0
            for info_hash, datasets in subtask_input_data.items():
                info_hashes.append(info_hash)
                contents_info = job.contents_info_map.get(info_hash)
                assert contents_info is not None
                subtask_input_data_size += contents_info.size
            assert subtask_input_data_size <= backupper.max_disk_space

        assert list(job.contents_info_map.keys()) == info_hashes

    @staticmethod
    def setup_contents_info_map(backup_area):
        backup_area.store_contents_info_map({
            hash_b: ContentsInfo(446253163, 3),
            hash_a: ContentsInfo(12370, 3),
            hash_c: ContentsInfo(35163760, 3)
        })

    @staticmethod
    def setup_contents(backup_area):
        backup_area.upload_content_by_hash("dataset-number-one_271", hash_b)
        backup_area.upload_content_by_hash("dataset-number-two_828", hash_a)
        backup_area.upload_content_by_hash("dataset-number-three_182", hash_c)

    @staticmethod
    def setup_snapshots(backup_area):
        backup_area.store_snapshot({
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), [])]
        }, "2019_05_18_14_50_10")

        backup_area.store_snapshot({
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), [])],
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), [])
            ]
        }, "2019_05_19_12_30_00")

        backup_area.store_snapshot({
            hash_b: [DatasetWithBranches(Dataset(name="dataset-number-one", version="271"), [])],
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), []),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), [])
            ]
        }, "2019_05_20_12_30_00")

        backup_area.store_snapshot({
            hash_a: [DatasetWithBranches(Dataset(name="dataset-number-two", version="828"), [])],
            hash_c: [
                DatasetWithBranches(Dataset(name="dataset-number-three", version="182"), []),
                DatasetWithBranches(Dataset(name="dataset-number-four", version="845"), [])
            ]
        }, "2019_05_21_12_30_00")
