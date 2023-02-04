import pandas as pd
import pytest
from dwh.grocery.targets import ShareableS3Target


@pytest.mark.with_cert
class TestS3:
    def setup(self):
        self.target = ShareableS3Target("s3://dwh/test")

    def test_remove(self):
        with self.target.open('w') as target:
            target.write(b"OLOLO")
        self.target.remove()
        assert not self.target.exists()

    def test_write(self):
        with self.target.open('w') as target:
            target.write(b"OLOLO")
        assert self.target.exists()

    def test_rewrite(self):
        with self.target.open('w') as target:
            target.write(b"OLOLO")
        with self.target.open('w') as target:
            target.write(b"KEKEKE")
        with self.target.open('r') as target:
            s = target.read()
        assert s == b"KEKEKE"

    def test_read(self):
        with self.target.open('w') as target:
            target.write(b"OLOLO")
        with self.target.open('r') as target:
            s = target.read()
        assert s == b"OLOLO"

    def test_get_fileref(self):
        with self.target.open('w') as target:
            target.write(b"OLOLO")
        assert self.target.get_url() == "https://dwh.s3.mdst.yandex.net/test"

    def test_excel(self):
        df = pd.DataFrame({'a': [1, 2, 3], 'b': [3, 4, 5]})
        with self.target.open('w') as target:
            w = pd.ExcelWriter(target)
            df.to_excel(w)
            w.save()
        assert self.target.exists()

    def teardown(self):
        self.target.remove()
