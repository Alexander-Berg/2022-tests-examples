import os


class TestEnv():
    def test_env(self):
        assert os.uname().release[:3] == '5.4'
