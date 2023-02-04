import pytest
from mock import patch, Mock

from staff.oebs.constants import ACTIVE_STATUS
from staff.oebs.controllers.datagenerators import OEBSOfficeDataGenerator
from staff.oebs.controllers.updaters import OEBSPlacementDataDiffMerger, OEBSUpdater
from staff.oebs.models import Office


def _build_updater(datasource):
    datagenerator = OEBSOfficeDataGenerator(datasource)
    data_diff_merger = OEBSPlacementDataDiffMerger(datagenerator, None)
    result = OEBSUpdater(data_diff_merger, None)
    return result


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_office_sync_update(_save_mock, build_updater):
    update_xml_data = '''
    <ROWSET>
     <ROW>
      <LOCATION_ID>1</LOCATION_ID>
      <LOCATION_CODE>11changed</LOCATION_CODE>
      <LOCATION_ADDR></LOCATION_ADDR>
      <TAXUNIT_CODE>204</TAXUNIT_CODE>
      <BUSINESSCENTRE_CODE>65</BUSINESSCENTRE_CODE>
      <STAFF_USAGE>Да</STAFF_USAGE>
      <HOME_WORK>Нет</HOME_WORK>
      <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
     </ROW>
     <ROW>
      <LOCATION_ID>3</LOCATION_ID>
      <LOCATION_CODE>33</LOCATION_CODE>
      <LOCATION_ADDR></LOCATION_ADDR>
      <TAXUNIT_CODE>121</TAXUNIT_CODE>
      <BUSINESSCENTRE_CODE>13</BUSINESSCENTRE_CODE>
      <STAFF_USAGE>Да</STAFF_USAGE>
      <HOME_WORK>Нет</HOME_WORK>
      <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
     </ROW>
    </ROWSET>
    '''
    mock = Mock(return_value=Mock(content=update_xml_data, status_code=200))

    Office.objects.create(location_id=1, active_status=ACTIVE_STATUS[1], location_code='11')
    Office.objects.create(location_id=2, active_status=ACTIVE_STATUS[1], location_code='22')

    datasource = Office.datasource_class(
        object_type=Office.oebs_type,
        method=Office.method,
        oebs_session=Mock(post=mock),
    )
    updater = _build_updater(datasource)
    updater.run_sync()
    assert Office.objects.count() == 3

    office = Office.objects.get(location_id=1)
    assert office.active_status == ACTIVE_STATUS[1]
    assert office.location_code == '11changed'

    office = Office.objects.get(location_id=2)
    assert office.active_status == ACTIVE_STATUS[0]
    assert office.location_code == '22'

    office = Office.objects.get(location_id=3)
    assert office.active_status == ACTIVE_STATUS[1]
    assert office.location_code == '33'
