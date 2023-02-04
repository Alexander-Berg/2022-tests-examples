from paysys.sre.tools.monitorings.lib.util.helpers import update_checks


class TestUpdateChecks:
    def test_update_checks_empty1(self):
        assert update_checks({}, {}) == {}

    def test_update_checks_empty2(self):
        assert update_checks({}, {"b": True}) == {}

    def test_update_checks_one_check1(self):
        assert update_checks({"a": {}}, {"b": True}) == {"a": {"b": True}}

    def test_update_checks_one_check2(self):
        assert update_checks({"a": {}, "c": {"b": False}}, {"b": True}) == \
            {"a": {"b": True}, "c": {"b": True}}
