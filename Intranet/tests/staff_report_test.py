from staff.person.reports.objects import StaffReport


def test_join_data_person_without_department():
    test_persons = [{"id": 1, "employment": 0}]

    result = next(StaffReport().join_data(oebs_full_names=dict(), _populated_departments=dict(), persons=test_persons))

    assert result["oebs_full_name"] == ""
    assert result["department_path"] == ""
    assert result["department_path_en"] == ""
    assert result["employment"] == ""
    assert "id" not in result
    assert "department_id" not in result
