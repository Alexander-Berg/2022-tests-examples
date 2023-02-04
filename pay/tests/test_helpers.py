import os
import pytest
from mongo_idm import helpers


class TestHelpers:
    def test_generate_password(self):
        assert len(helpers.generate_password()) == 64
        assert len(helpers.generate_password(40)) == 40
        with pytest.raises(Exception):
            len(helpers.generate_password(1))
        with pytest.raises(Exception):
            len(helpers.generate_password(0))

    @pytest.mark.skipif("DATABASES" in os.environ, reason="DATABASES environment variable set")
    def test_get_databases(self):
        os.environ["DATABASES"] = "TEST1,TEST-2,TEST_3"
        assert helpers.get_databases() == ["TEST1", "TEST-2", "TEST_3"]
        del os.environ["DATABASES"]
