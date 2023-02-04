from source.vpnrobot import DataCollect
import pytest

class TestDataCollect:

    def test_type_error_1(self, typeerror_resource):
        dc = DataCollect()
        with pytest.raises(TypeError):
            dc.get_chef(typeerror_resource[1],typeerror_resource[2])

    def test_key_error_1(self):
        dc = DataCollect()
        with pytest.raises(KeyError):
            dc.get_chef('foo', {'bar': [], 'foobar': {}})

    def test_testcases(self, staff_resource):
        dc = DataCollect()
        assert(dc.get_chef(staff_resource[0], staff_resource[2]) == staff_resource[1])

    def test_vpn_group_search_testcases(self, group_resource):
        dc = DataCollect()
        assert (dc.vpn_group_search(group_resource[1]) == group_resource[0])

