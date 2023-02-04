import pytest
from source.cmdb.cmdb import (CertificatorYTCollector,
                  OebsNotebooksYTCollector)
from source.tests.testdata import (CMDB_NOTEBOOKS_STATUSES_TESTDATA,
                            CMDB_NOTEBOOKS_OS_TESTDATA)

@pytest.fixture()
def mock_find_inv(monkeypatch):
    def mockreturn(self, sn):
        if sn == 'C02V51GEHV2R':
            return '100100100'
        else:
            return None

    monkeypatch.setattr(CertificatorYTCollector, '_find_inv_by_sn', mockreturn)

test_data = [
    ({"certificates": [], "username":"orange13"},
     {"username": "orange13", "type": None, "instance_number": None}),
    ({"certificates":[
        {
            "issued": "2018-11-08T14:26:43+03:00",
            "id": 447390,
            "type": "pc",
            "pc_serial_number": "C02V51GEHV2R",
            "pc_mac": "001c422078fc",
            "pc_inum": "101468465"
        },
        {
            "issued": "2018-11-08T13:42:48+03:00",
            "id": 447212,
            "type": "pc",
            "pc_serial_number": "C02V51GEHV2R",
            "pc_mac": "001c422078fc",
            "pc_inum": "101468465"
        }],
    "username":"orange13"},
     {'username': 'orange13', 'type': 'pc', 'instance_number': '101468465'}),
    ({"certificates":[
            {
                "issued": "2018-11-08T14:26:43+03:00",
                "id": 447390,
                "type": "pc",
                "pc_serial_number": "C02V51GEHV2R",
                "pc_mac": "001c422078fc",
            },
            {
                "issued": "2018-11-08T13:42:48+03:00",
                "id": 447212,
                "type": "pc",
                "pc_serial_number": "C02V51GEHV2R",
                "pc_mac": "001c422078fc",
            }],
        "username":"orange13"},
         {'username': 'orange13', 'type': 'pc', 'instance_number': '100100100'}),
    ({"certificates":[
                {
                    "issued": "2018-11-08T14:26:43+03:00",
                    "id": 447390,
                    "type": "pc",
                    "pc_serial_number": "invalid_serial",
                    "pc_mac": "001c422078fc",
                },
                {
                    "issued": "2018-11-08T13:42:48+03:00",
                    "id": 447212,
                    "type": "pc",
                    "pc_serial_number": "C02V51GEHV2R",
                    "pc_mac": "001c422078fc",
                }],
            "username":"orange13"},
             {'username': 'orange13', 'type': 'pc', 'instance_number': None})
]

@pytest.mark.parametrize("input_data, ex_result", test_data)
def test_certificator_collection(mock_find_inv, input_data, ex_result):
    print(input_data)
    certificates = input_data["certificates"]
    username = input_data["username"]

    collector = CertificatorYTCollector()
    result = collector._get_full_data_by_login(username,certificates)
    assert result == ex_result

@pytest.mark.parametrize("input_data, ex_result", CMDB_NOTEBOOKS_STATUSES_TESTDATA)
def test_cmdb_notebook_collector_os_parsing(input_data, ex_result):
    assert OebsNotebooksYTCollector()._analyze_eq_statuses_data(input_data) == ex_result

@pytest.mark.parametrize("input_data, ex_result", CMDB_NOTEBOOKS_OS_TESTDATA)
def test_cmdb_notebook_collector_os_parsing(input_data, ex_result):
    assert OebsNotebooksYTCollector()._analyze_os_data(input_data) == ex_result