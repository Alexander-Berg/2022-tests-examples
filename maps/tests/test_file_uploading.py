import pytest

from .basic_test import BasicTest

from data_types.anonymous_file import AnonymousFile

import lib.scheduler as scheduler
import lib.manager as manager


class TestFileUploading(BasicTest):
    def test_can_upload_file_and_get_it(self, test_user):
        anonymous_file = AnonymousFile()

        manager.upload_file(anonymous_file) >> 200
        contents = scheduler.get_file(file_id=anonymous_file.file_id, user=test_user) >> 200

        assert contents == anonymous_file.contents

        another_file = AnonymousFile(file_id=anonymous_file.file_id)
        manager.upload_file(another_file) >> 200

        contents = scheduler.get_file(
            file_id=anonymous_file.file_id,
            head_id=anonymous_file.head_unit.head_id,
            user=test_user
        ) >> 200
        assert contents == anonymous_file.contents

        another_contents = scheduler.get_file(
            file_id=another_file.file_id,
            head_id=another_file.head_unit.head_id,
            user=test_user
        ) >> 200
        assert another_contents == another_file.contents
