import os


class TestEnv():
    def test_env(self):
        assert os.uname().release[:4] == '4.19'
