from decimal import Decimal as D
import factory
import logging
import pytest

from mock import patch, Mock
from staff.lib.testing import get_random_date

from staff.oebs.constants import OEBS_RESOURCE_NAME
from staff.oebs.controllers.datasources import XmlDatasource
from staff.oebs.models import Employee, LeaveBalance, Office, Reward, Review, Job
from staff.oebs.tests.factories import RewardFactory, ReviewFactory, JobFactory

logger = logging.getLogger(__name__)


xml_data = {
    OEBS_RESOURCE_NAME.PERSON: """
<ROWSET>
 <ROW>
  <PERSON_GUID>guid0001</PERSON_GUID>
  <LAST_NAME>Стрелков</LAST_NAME>
  <FIRST_NAME>Дмитрий</FIRST_NAME>
  <MIDDLE_NAMES>Владимирович</MIDDLE_NAMES>
  <DATE_START>2013/11/29 00:00:00</DATE_START>
  <RWN>1</RWN>
  <ACTUAL_TERMINATION_DATE/>
  <CONTRACT_END_DATE/>
  <NDA_END_DATE/>
  <MANAGE_ORG_ID>4713</MANAGE_ORG_ID>
  <LEGAL_ENTITY_ORG_ID>121</LEGAL_ENTITY_ORG_ID>
  <OFFICE_LOC_ID>1711077</OFFICE_LOC_ID>
  <HOME_LOC_ID/>
  <HOME_FLAG>Нет</HOME_FLAG>
  <PERSON_TYPE>Сотрудник</PERSON_TYPE>
  <CONCATENATED_ADDRESS>Russian Federation,Москва г, </CONCATENATED_ADDRESS>
  <MANAGE_ORG_NAME>Отдел геопродуктов</MANAGE_ORG_NAME>
  <JOB_NAME>Менеджер по работе с партнерами</JOB_NAME>
  <BUDGET_MANAGER_GUID/>
  <WORK_COUNTRY>RU</WORK_COUNTRY>
  <VACATION_SIGNATORY_GUID/>
  <CHILD_COUNT>2</CHILD_COUNT>
  <BYOD_ACCESS>Нет</BYOD_ACCESS>
  <HEADCOUNT>0</HEADCOUNT>
  <JOBPRICE>0</JOBPRICE>
  <WIRETAP>Y</WIRETAP>
  <STAFF_AGREEMENT>Y</STAFF_AGREEMENT>
  <STAFF_BIOMETRIC_AGREEMENT>Y</STAFF_BIOMETRIC_AGREEMENT>
 </ROW>
 <ROW>
  <PERSON_GUID>guid0002</PERSON_GUID>
  <LAST_NAME>Куликов</LAST_NAME>
  <FIRST_NAME>Николай</FIRST_NAME>
  <MIDDLE_NAMES>Юрьевич</MIDDLE_NAMES>
  <DATE_START>2010/01/18 00:00:00</DATE_START>
  <RWN>1</RWN>
  <ACTUAL_TERMINATION_DATE/>
  <CONTRACT_END_DATE/>
  <NDA_END_DATE/>
  <MANAGE_ORG_ID>48650</MANAGE_ORG_ID>
  <LEGAL_ENTITY_ORG_ID>121</LEGAL_ENTITY_ORG_ID>
  <OFFICE_LOC_ID>1711077</OFFICE_LOC_ID>
  <HOME_LOC_ID/>
  <HOME_FLAG>Нет</HOME_FLAG>
  <PERSON_TYPE>Сотрудник</PERSON_TYPE>
  <CONCATENATED_ADDRESS>Russian Federation,Московская обл,Люберцы г</CONCATENATED_ADDRESS>
  <MANAGE_ORG_NAME>Группа экспертизы рантайм поиска</MANAGE_ORG_NAME>
  <JOB_NAME>Старший разработчик программного обеспечения</JOB_NAME>
  <BUDGET_MANAGER_GUID/>
  <WORK_COUNTRY>RU</WORK_COUNTRY>
  <VACATION_SIGNATORY_GUID/>
  <CHILD_COUNT/>
  <BYOD_ACCESS>Нет</BYOD_ACCESS>
  <HEADCOUNT>1</HEADCOUNT>
  <JOBPRICE>0</JOBPRICE>
  <WIRETAP>N</WIRETAP>
  <STAFF_AGREEMENT>N</STAFF_AGREEMENT>
  <STAFF_BIOMETRIC_AGREEMENT>N</STAFF_BIOMETRIC_AGREEMENT>
 </ROW>
 <ROW>
  <PERSON_GUID>guid0003</PERSON_GUID>
  <LAST_NAME>Шульгин</LAST_NAME>
  <FIRST_NAME>Константин</FIRST_NAME>
  <MIDDLE_NAMES>Михайлович</MIDDLE_NAMES>
  <DATE_START>2013/08/12 00:00:00</DATE_START>
  <RWN>1</RWN>
  <ACTUAL_TERMINATION_DATE/>
  <CONTRACT_END_DATE/>
  <NDA_END_DATE/>
  <MANAGE_ORG_ID>58474</MANAGE_ORG_ID>
  <LEGAL_ENTITY_ORG_ID>121</LEGAL_ENTITY_ORG_ID>
  <OFFICE_LOC_ID>1711077</OFFICE_LOC_ID>
  <HOME_LOC_ID/>
  <HOME_FLAG>Нет</HOME_FLAG>
  <PERSON_TYPE>Сотрудник</PERSON_TYPE>
  <CONCATENATED_ADDRESS>Russian Federation,Москва г, </CONCATENATED_ADDRESS>
  <MANAGE_ORG_NAME>Группа функциональности</MANAGE_ORG_NAME>
  <JOB_NAME>Разработчик программного обеспечения</JOB_NAME>
  <BUDGET_MANAGER_GUID/>
  <WORK_COUNTRY>RU</WORK_COUNTRY>
  <VACATION_SIGNATORY_GUID/>
  <CHILD_COUNT/>
  <BYOD_ACCESS>Нет</BYOD_ACCESS>
  <HEADCOUNT>.75</HEADCOUNT>
  <JOBPRICE>0</JOBPRICE>
  <WIRETAP>Y</WIRETAP>
  <STAFF_AGREEMENT>N</STAFF_AGREEMENT>
  <STAFF_BIOMETRIC_AGREEMENT>N</STAFF_BIOMETRIC_AGREEMENT>
 </ROW>
 <ROW>
  <PERSON_GUID>guid0004</PERSON_GUID>
  <LAST_NAME>Мельников</LAST_NAME>
  <FIRST_NAME>Андрей</FIRST_NAME>
  <MIDDLE_NAMES>Александрович</MIDDLE_NAMES>
  <DATE_START>2013/08/14 00:00:00</DATE_START>
  <RWN>1</RWN>
  <ACTUAL_TERMINATION_DATE/>
  <CONTRACT_END_DATE/>
  <NDA_END_DATE/>
  <MANAGE_ORG_ID>47281</MANAGE_ORG_ID>
  <LEGAL_ENTITY_ORG_ID>121</LEGAL_ENTITY_ORG_ID>
  <OFFICE_LOC_ID>1711077</OFFICE_LOC_ID>
  <HOME_LOC_ID/>
  <HOME_FLAG>Нет</HOME_FLAG>
  <PERSON_TYPE>Сотрудник</PERSON_TYPE>
  <CONCATENATED_ADDRESS>Russian Federation,Москва г, </CONCATENATED_ADDRESS>
  <MANAGE_ORG_NAME>Группа аналитики Картинок</MANAGE_ORG_NAME>
  <JOB_NAME>Аналитик</JOB_NAME>
  <BUDGET_MANAGER_GUID/>
  <WORK_COUNTRY>RU</WORK_COUNTRY>
  <VACATION_SIGNATORY_GUID/>
  <CHILD_COUNT/>
  <BYOD_ACCESS>Да</BYOD_ACCESS>
  <HEADCOUNT>0.75</HEADCOUNT>
  <JOBPRICE>0</JOBPRICE>
  <WIRETAP>N</WIRETAP>
  <STAFF_AGREEMENT>Y</STAFF_AGREEMENT>
  <STAFF_BIOMETRIC_AGREEMENT>Y</STAFF_BIOMETRIC_AGREEMENT>
 </ROW>
 <ROW>
  <PERSON_GUID>guid0005</PERSON_GUID>
  <LAST_NAME>Рожков</LAST_NAME>
  <FIRST_NAME>Геннадий</FIRST_NAME>
  <MIDDLE_NAMES>Николаевич</MIDDLE_NAMES>
  <DATE_START>2008/02/18 00:00:00</DATE_START>
  <RWN>1</RWN>
  <ACTUAL_TERMINATION_DATE>2013/11/29 00:00:00</ACTUAL_TERMINATION_DATE>
  <CONTRACT_END_DATE>2013/11/29 00:00:00</CONTRACT_END_DATE>
  <NDA_END_DATE/>
  <MANAGE_ORG_ID>3493</MANAGE_ORG_ID>
  <LEGAL_ENTITY_ORG_ID>121</LEGAL_ENTITY_ORG_ID>
  <OFFICE_LOC_ID>1711077</OFFICE_LOC_ID>
  <HOME_LOC_ID/>
  <HOME_FLAG>Нет</HOME_FLAG>
  <PERSON_TYPE>Сотрудник</PERSON_TYPE>
  <CONCATENATED_ADDRESS>Russian Federation,Московская обл, </CONCATENATED_ADDRESS>
  <MANAGE_ORG_NAME>Инженерная группа</MANAGE_ORG_NAME>
  <JOB_NAME>Старший инженер</JOB_NAME>
  <BUDGET_MANAGER_GUID/>
  <WORK_COUNTRY>RU</WORK_COUNTRY>
  <VACATION_SIGNATORY_GUID/>
  <CHILD_COUNT>1</CHILD_COUNT>
  <BYOD_ACCESS>Нет</BYOD_ACCESS>
  <HEADCOUNT>5</HEADCOUNT>
  <JOBPRICE>0</JOBPRICE>
  <WIRETAP>Y</WIRETAP>
 </ROW>
</ROWSET>
        """,

    OEBS_RESOURCE_NAME.LEAVE_BALANCE: """
<ROWSET>
 <ROW>
  <PERSON_GUID>guid0001</PERSON_GUID>
  <LEAVE_BALANCES>
    <LEAVE_BALANCE_DEFAULT>6,3</LEAVE_BALANCE_DEFAULT>
    <LEAVE_BALANCE_COMPANY>3,6</LEAVE_BALANCE_COMPANY>
  </LEAVE_BALANCES>
  <TIME_OFF>1,1</TIME_OFF>
 </ROW>
 <ROW>
  <PERSON_GUID>guid0002</PERSON_GUID>
  <LEAVE_BALANCES>
    <LEAVE_BALANCE_DEFAULT>32,67</LEAVE_BALANCE_DEFAULT>
    <LEAVE_BALANCE_COMPANY>67,32</LEAVE_BALANCE_COMPANY>
  </LEAVE_BALANCES>
  <TIME_OFF>2,2</TIME_OFF>
 </ROW>
 <ROW>
  <PERSON_GUID>GUID0003</PERSON_GUID>
  <LEAVE_BALANCES>
    <LEAVE_BALANCE_DEFAULT>16,32</LEAVE_BALANCE_DEFAULT>
    <LEAVE_BALANCE_COMPANY>32,16</LEAVE_BALANCE_COMPANY>
  </LEAVE_BALANCES>
  <TIME_OFF>3,3</TIME_OFF>
 </ROW>
 <ROW>
  <PERSON_GUID>guid0004</PERSON_GUID>
  <LEAVE_BALANCES>
    <LEAVE_BALANCE_DEFAULT>12,98</LEAVE_BALANCE_DEFAULT>
    <LEAVE_BALANCE_COMPANY>98,12</LEAVE_BALANCE_COMPANY>
  </LEAVE_BALANCES>
  <TIME_OFF>4,4</TIME_OFF>
 </ROW>
</ROWSET>
        """,

    OEBS_RESOURCE_NAME.OFFICE: """
<ROWSET>
 <ROW>
  <LOCATION_ID>1</LOCATION_ID>
  <LOCATION_CODE>The Hague Office, Yandex Europe B.V.</LOCATION_CODE>
  <LOCATION_ADDR>Netherlands,Ln Copes van Cattenburch 52, 2585GB s-Gravenhage, the Netherlands</LOCATION_ADDR>
  <HOME_WORK>НЕТ</HOME_WORK>
  <TAXUNIT_CODE>1</TAXUNIT_CODE>
  <BUSINESSCENTRE_CODE>2</BUSINESSCENTRE_CODE>
  <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
 </ROW>
 <ROW>
  <LOCATION_ID>2</LOCATION_ID>
  <LOCATION_CODE>Офис &quot;Амстердам&quot;, г. Амстердам</LOCATION_CODE>
  <LOCATION_ADDR>Netherlands,Amsterdam, Luttenbergweg, 4</LOCATION_ADDR>
  <HOME_WORK>НЕТ</HOME_WORK>
  <TAXUNIT_CODE>3</TAXUNIT_CODE>
  <BUSINESSCENTRE_CODE>4</BUSINESSCENTRE_CODE>
  <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
 </ROW>
 <ROW>
  <LOCATION_ID>3</LOCATION_ID>
  <LOCATION_CODE>Las Vegas DC, Yandex Inc.</LOCATION_CODE>
  <LOCATION_ADDR>United States,South Decatur Blvd</LOCATION_ADDR>
  <HOME_WORK>НЕТ</HOME_WORK>
  <TAXUNIT_CODE>5</TAXUNIT_CODE>
  <BUSINESSCENTRE_CODE>6</BUSINESSCENTRE_CODE>
  <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
 </ROW>
 <ROW>
  <LOCATION_ID>4</LOCATION_ID>
  <LOCATION_CODE>Офис &quot;Мамонтов&quot;, г.Москва</LOCATION_CODE>
  <LOCATION_ADDR>119034,Москва г, , , ,Тимура Фрунзе ул,11,2</LOCATION_ADDR>
  <HOME_WORK>НЕТ</HOME_WORK>
  <TAXUNIT_CODE>7</TAXUNIT_CODE>
  <BUSINESSCENTRE_CODE>8</BUSINESSCENTRE_CODE>
  <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
 </ROW>
 <ROW>
  <LOCATION_ID>5</LOCATION_ID>
  <LOCATION_CODE>Офис &quot;Строганов&quot;, г. Москва</LOCATION_CODE>
  <LOCATION_ADDR>119034,Москва г, , , ,Тимура Фрунзе ул,11,44</LOCATION_ADDR>
  <HOME_WORK>НЕТ</HOME_WORK>
  <TAXUNIT_CODE>9</TAXUNIT_CODE>
  <BUSINESSCENTRE_CODE>10</BUSINESSCENTRE_CODE>
  <ACTIVE_STATUS>Открыто</ACTIVE_STATUS>
 </ROW>
</ROWSET>
        """,
}

json_data = {
    OEBS_RESOURCE_NAME.MANAGEMENT_ORG: '''{"departments": [{"active": true,
   "category": "200",
   "chief": null,
   "id": 1,
   "kind": "31",
   "name": "Группа счастья магазинов",
   "name_en": null,
   "parent_id": 4431,
   "short_name": null,
   "short_name_en": null},
  {"active": true,
   "category": "100",
   "chief": "chief_with_guid0001",
   "id": 2,
   "kind": "51",
   "name": "Группа аналитики Яндекс.Картинок",
   "name_en": null,
   "parent_id": 4280,
   "short_name": null,
   "short_name_en": null},
  {"active": false,
   "category": "200",
   "chief": null,
   "id": 3,
   "kind": "51",
   "name": "Группа Медиалаба и экспериментальных спецпроектов",
   "name_en": "Medialab and experimental special projects group",
   "parent_id": 3218,
   "short_name": "Группа Медиалаба и экспериментальных спецпроектов",
   "short_name_en": "Medialab and experimental special projects group"}]}''',

    OEBS_RESOURCE_NAME.REWARD: """{
    "rewardSchemes": [{
        "schemeID": 1,
        "schemeName": "HC1",
        "description": "С численностью",
        "startDate": "1998-01-01",
        "endDate": null,
        "schemesLineID": 1,
        "hcCategory": "T",
        "food": "Не определено",
        "dms": [{"name": "АльфаСтрахование (полная)", "typeInsurance": "Полная", "yaInsurance": "Да"}],
        "dmsGroup": "Стандартный",
        "ai": [{"name": "АльфаСтрахование (полная)", "typeInsurance": "Полная", "yaInsurance": "Да"}],
        "equipment": null,
        "mob": null,
        "bankCards": ["Райффайзен (з/п)"]
    }]
}""",

    OEBS_RESOURCE_NAME.REVIEW: """{
    "reviewSchemes": [{
        "schemeID": 1,
        "schemeName": "DEFAULT",
        "description": "Схема",
        "frequency": null,
        "first_month_period": null,
        "startDate": "2000-01-01",
        "endDate": "2020-08-31",
        "schemesLineID": 1,
        "schemesLineDesc": null,
        "targetBonus": 0,
        "grantType": "grantType",
        "grantTypeDesc": "grantTypeDesc"
    }, {
        "schemeID": 1,
        "schemeName": "DEFAULT",
        "description": "Схема",
        "frequency": null,
        "first_month_period": null,
        "startDate": "2020-09-01",
        "endDate": "4712-12-31",
        "schemesLineID": 2,
        "schemesLineDesc": null,
        "targetBonus": 0,
        "grantType": "New grantType",
        "grantTypeDesc": "New grantTypeDesc"
    }]
}""",

    OEBS_RESOURCE_NAME.JOB: """{"jobs": []}""",
}


class EmployeeFactory(factory.DjangoModelFactory):
    class Meta:
        model = Employee


def get_patcher(datasource, model):
    content = xml_data.get(model.oebs_type, json_data.get(model.oebs_type, None))
    return (
        datasource,
        '_oebs_request',
        Mock(return_value=content),
     )


@pytest.mark.django_db
def test_employee(build_updater):
    datasource = XmlDatasource(object_type=Employee.oebs_type, method=Employee.method, oebs_session=Mock())
    with patch.object(*get_patcher(datasource, Employee)):
        updater = build_updater(model=Employee, custom_logger=logger, datasource=datasource)

        updater.run_sync()

        employee1 = Employee.objects.get(person_guid='guid0001')
        assert employee1.last_name == 'Стрелков'
        assert employee1.first_name == 'Дмитрий'
        assert employee1.middle_names == 'Владимирович'
        assert employee1.date_start == '2013/11/29 00:00:00'
        assert employee1.actual_termination_date == ''
        assert employee1.manage_org_id == 4713
        assert employee1.legal_entity_org_id == 121
        assert employee1.office_loc_id == 1711077
        assert employee1.home_loc_id is None
        assert employee1.home_flag == 'Нет'
        assert employee1.person_type == 'Сотрудник'
        assert employee1.concatenated_address == 'Russian Federation,Москва г, '
        assert employee1.manage_org_name == 'Отдел геопродуктов'
        assert employee1.job_name == 'Менеджер по работе с партнерами'
        assert employee1.child_count == 2
        assert employee1.nda_end_date == ''
        assert employee1.contract_end_date == ''
        assert employee1.byod_access == 'Нет'
        assert employee1.headcount == D('0')
        assert employee1.wiretap == 'Y'
        assert employee1.staff_agreement == 'Y'
        assert employee1.staff_biometric_agreement == 'Y'

        employee2 = Employee.objects.get(person_guid='guid0002')
        assert employee2.last_name == 'Куликов'
        assert employee2.first_name == 'Николай'
        assert employee2.middle_names == 'Юрьевич'
        assert employee2.date_start == '2010/01/18 00:00:00'
        assert employee2.actual_termination_date == ''
        assert employee2.manage_org_id == 48650
        assert employee2.legal_entity_org_id == 121
        assert employee2.office_loc_id == 1711077
        assert employee2.home_loc_id is None
        assert employee2.home_flag == 'Нет'
        assert employee2.person_type == 'Сотрудник'
        assert employee2.concatenated_address == 'Russian Federation,Московская обл,Люберцы г'
        assert employee2.manage_org_name == 'Группа экспертизы рантайм поиска'
        assert employee2.job_name == 'Старший разработчик программного обеспечения'
        assert employee2.child_count is None
        assert employee2.nda_end_date == ''
        assert employee2.contract_end_date == ''
        assert employee2.byod_access == 'Нет'
        assert employee2.headcount == D('1')
        assert employee2.wiretap == 'N'
        assert employee2.staff_agreement == 'N'
        assert employee2.staff_biometric_agreement == 'N'

        employee3 = Employee.objects.get(person_guid='guid0003')
        assert employee3.last_name == 'Шульгин'
        assert employee3.first_name == 'Константин'
        assert employee3.middle_names == 'Михайлович'
        assert employee3.date_start == '2013/08/12 00:00:00'
        assert employee3.actual_termination_date == ''
        assert employee3.manage_org_id == 58474
        assert employee3.legal_entity_org_id == 121
        assert employee3.office_loc_id == 1711077
        assert employee3.home_loc_id is None
        assert employee3.home_flag == 'Нет'
        assert employee3.person_type == 'Сотрудник'
        assert employee3.concatenated_address == 'Russian Federation,Москва г, '
        assert employee3.manage_org_name == 'Группа функциональности'
        assert employee3.job_name == 'Разработчик программного обеспечения'
        assert employee3.child_count is None
        assert employee3.nda_end_date == ''
        assert employee3.contract_end_date == ''
        assert employee3.byod_access == 'Нет'
        assert employee3.headcount == D('.75')
        assert employee3.wiretap == 'Y'
        assert employee3.staff_agreement == 'N'
        assert employee3.staff_biometric_agreement == 'N'

        employee4 = Employee.objects.get(person_guid='guid0004')
        assert employee4.last_name == 'Мельников'
        assert employee4.first_name == 'Андрей'
        assert employee4.middle_names == 'Александрович'
        assert employee4.date_start == '2013/08/14 00:00:00'
        assert employee4.actual_termination_date == ''
        assert employee4.manage_org_id == 47281
        assert employee4.legal_entity_org_id == 121
        assert employee4.office_loc_id == 1711077
        assert employee4.home_loc_id is None
        assert employee4.home_flag == 'Нет'
        assert employee4.person_type == 'Сотрудник'
        assert employee4.concatenated_address == 'Russian Federation,Москва г, '
        assert employee4.manage_org_name == 'Группа аналитики Картинок'
        assert employee4.job_name == 'Аналитик'
        assert employee4.child_count is None
        assert employee4.nda_end_date == ''
        assert employee4.contract_end_date == ''
        assert employee4.byod_access == 'Да'
        assert employee4.headcount == D('0.75')
        assert employee4.wiretap == 'N'
        assert employee4.staff_agreement == 'Y'
        assert employee4.staff_biometric_agreement == 'Y'

        employee5 = Employee.objects.get(person_guid='guid0005')
        assert employee5.last_name == 'Рожков'
        assert employee5.first_name == 'Геннадий'
        assert employee5.middle_names == 'Николаевич'
        assert employee5.date_start == '2008/02/18 00:00:00'
        assert employee5.actual_termination_date == '2013/11/29 00:00:00'
        assert employee5.manage_org_id == 3493
        assert employee5.legal_entity_org_id == 121
        assert employee5.office_loc_id == 1711077
        assert employee5.home_loc_id is None
        assert employee5.home_flag == 'Нет'
        assert employee5.person_type == 'Сотрудник'
        assert employee5.concatenated_address == 'Russian Federation,Московская обл, '
        assert employee5.manage_org_name == 'Инженерная группа'
        assert employee5.job_name == 'Старший инженер'
        assert employee5.child_count == 1
        assert employee5.nda_end_date == ''
        assert employee5.contract_end_date == '2013/11/29 00:00:00'
        assert employee5.byod_access == 'Нет'
        assert employee5.headcount == D('5')
        assert employee5.wiretap == 'Y'
        assert employee5.staff_agreement == ''
        assert employee5.staff_biometric_agreement == ''


@pytest.mark.django_db
def test_leave_balance(build_updater):
    datasource = XmlDatasource(object_type=LeaveBalance.oebs_type, method=LeaveBalance.method, oebs_session=Mock())
    with patch.object(*get_patcher(datasource, LeaveBalance)):
        updater = build_updater(model=LeaveBalance, datasource=datasource)

        updater.run_sync()

        lb1 = LeaveBalance.objects.get(person_guid='guid0001')
        assert lb1.leave_balance_default == '6,3'
        assert lb1.leave_balance_company == '3,6'
        assert lb1.time_off == '1,1'

        lb2 = LeaveBalance.objects.get(person_guid='guid0002')
        assert lb2.leave_balance_default == '32,67'
        assert lb2.leave_balance_company == '67,32'
        assert lb2.time_off == '2,2'

        lb3 = LeaveBalance.objects.get(person_guid='guid0003')
        assert lb3.leave_balance_default == '16,32'
        assert lb3.leave_balance_company == '32,16'
        assert lb3.time_off == '3,3'

        lb4 = LeaveBalance.objects.get(person_guid='guid0004')
        assert lb4.leave_balance_default == '12,98'
        assert lb4.leave_balance_company == '98,12'
        assert lb4.time_off == '4,4'


@pytest.mark.django_db
def test_office(build_updater):
    datasource = XmlDatasource(object_type=Office.oebs_type, method=Office.method, oebs_session=Mock())
    with patch.object(*get_patcher(datasource, Office)):
        updater = build_updater(model=Office, datasource=datasource)

        updater.run_sync()

        o1 = Office.objects.get(location_id=1)
        assert o1.location_code == 'The Hague Office, Yandex Europe B.V.'
        assert o1.location_addr == 'Netherlands,Ln Copes van Cattenburch 52, 2585GB' ' s-Gravenhage, the Netherlands'
        assert o1.taxunit_code == 1
        assert o1.businesscentre_code == '2'
        assert o1.home_work == 'НЕТ'
        assert o1.active_status == 'Открыто'

        o2 = Office.objects.get(location_id=2)
        assert o2.location_code == 'Офис "Амстердам", г. Амстердам'
        assert o2.location_addr == 'Netherlands,Amsterdam, Luttenbergweg, 4'
        assert o2.taxunit_code == 3
        assert o2.businesscentre_code == '4'
        assert o2.home_work == 'НЕТ'
        assert o2.active_status == 'Открыто'

        o3 = Office.objects.get(location_id=3)
        assert o3.location_code == 'Las Vegas DC, Yandex Inc.'
        assert o3.location_addr == 'United States,South Decatur Blvd'
        assert o3.taxunit_code == 5
        assert o3.businesscentre_code == '6'
        assert o3.home_work == 'НЕТ'
        assert o3.active_status == 'Открыто'

        o4 = Office.objects.get(location_id=4)
        assert o4.location_code == 'Офис "Мамонтов", г.Москва'
        assert o4.location_addr == '119034,Москва г, , , ,Тимура Фрунзе ул,11,2'
        assert o4.taxunit_code == 7
        assert o4.businesscentre_code == '8'
        assert o4.home_work == 'НЕТ'
        assert o4.active_status == 'Открыто'

        o5 = Office.objects.get(location_id=5)
        assert o5.location_code == 'Офис "Строганов", г. Москва'
        assert o5.location_addr == '119034,Москва г, , , ,Тимура Фрунзе ул,11,44'
        assert o5.taxunit_code == 9
        assert o5.businesscentre_code == '10'
        assert o5.home_work == 'НЕТ'
        assert o5.active_status == 'Открыто'


@pytest.mark.django_db
def test_reward_drop_end_date(build_updater):
    datasource = Reward.datasource_class(object_type=Reward.oebs_type, method=Reward.method, oebs_session=Mock())
    RewardFactory(scheme_id=1, end_date=get_random_date())

    with patch.object(*get_patcher(datasource, Reward)):
        updater = build_updater(model=Reward, datasource=datasource)

        updater.run_sync()

        reward1 = Reward.objects.get(scheme_id=1)
        assert reward1.end_date is None


@pytest.mark.django_db
def test_review_last_row_wins(build_updater):
    datasource = Review.datasource_class(object_type=Review.oebs_type, method=Review.method, oebs_session=Mock())
    ReviewFactory(scheme_id=1, end_date=get_random_date())

    with patch.object(*get_patcher(datasource, Review)):
        updater = build_updater(model=Review, datasource=datasource)

        updater.run_sync()

        review1 = Review.objects.get(scheme_id=1)
        assert review1.grant_type == "New grantType"


@pytest.mark.django_db
def test_job_removal(build_updater):
    datasource = Job.datasource_class(object_type=Job.oebs_type, method=Job.method, oebs_session=Mock())
    job = JobFactory(is_deleted_from_oebs=False)

    with patch.object(*get_patcher(datasource, Job)):
        updater = build_updater(model=Job, datasource=datasource)

        updater.run_sync()

        assert Job.objects.get(code=job.code).is_deleted_from_oebs is True
