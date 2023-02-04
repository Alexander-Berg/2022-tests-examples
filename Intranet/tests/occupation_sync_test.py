import json
from mock import patch, Mock

import pytest

from staff.oebs.controllers.datasources import OccupationDatasource
from staff.oebs.models import Occupation


json_data = '''
{"scales": [{
        "scaleName": "DutyNetworkEngineer",
        "scaleDescription": "NOA~AIT040",
        "endDate": null,
        "scaleDescriptionRUS": "\xd0\x94\xd0\xb5\xd0\xb6\xd1\x83\xd1\x80\xd0\xbd\xd1\x8b\xd0\xb9",
        "scaleDescriptionENG": "Duty Network Engineer",
        "transferToStaff": "Y",
        "scaleGroupFemida": "Редакция и тексты",
        "scaleGroupReview": "Other_review",
        "scaleGroupBonus": null,
        "scaleGroupReward": "Other_benefits"
    }, {
        "scaleName": "NetworkEngineer",
        "scaleDescription": "NOA~AID080",
        "endDate": null,
        "scaleDescriptionRUS": "\xd0\xa1\xd0\xb5\xd1\x82\xd0\xb5\xd0\xb2\xd0\xbe\xd0\xb9",
        "scaleDescriptionENG": "Network Engineer",
        "transferToStaff": "Y",
        "scaleGroupFemida": "Развитие бизнеса и продажи",
        "scaleGroupReview": "Sales_review",
        "scaleGroupBonus": null,
        "scaleGroupReward": "Other_benefits"
    }, {
        "scaleName": "HelpDesk",
        "scaleDescription": "ITC~AIT020",
        "endDate": null,
        "scaleDescriptionRUS": "\xd0\xa1\xd0\xbf\xd0\xb5\xd1\x86\xd0\xb8\xd0\xb0\xd0\xbb\xd0\xb8\xd1\x81\xd1\x82",
        "scaleDescriptionENG": "HelpDesk Specialist",
        "transferToStaff": "Y",
        "scaleGroupFemida": "Развитие бизнеса и продажи",
        "scaleGroupReview": "Sales_review",
        "scaleGroupBonus": null,
        "scaleGroupReward": "Realtor_benefits"
    }, {
        "scaleName": "ItAssetManager",
        "scaleDescription": "NOA~AIT000",
        "endDate": null,
        "scaleDescriptionRUS": "\xd0\xa1\xd0\xbf\xd0\xb5\xd1\x86\xd0\xb8\xd0\xb0\xd0\xbb\xd0\xb8\xd1\x81\xd1\x82",
        "scaleDescriptionENG": "IT Asset Manager",
        "transferToStaff": "Y",
        "scaleGroupFemida": "HR",
        "scaleGroupReview": "Other_review",
        "scaleGroupBonus": null,
        "scaleGroupReward": "Other_benefits"
    }, {
        "scaleName": "EngSysDesigner",
        "scaleDescription": "EGA~TPD270",
        "endDate": null,
        "scaleDescriptionRUS": "\xd0\x9f\xd1\x80\xd0\xbe\xd0\xb5\xd0\xba\xd1\x82\xd0\xb8\xd1\x80\xd0\xbe\xd0\xb2",
        "scaleDescriptionENG": "Engineering Systems Designer",
        "transferToStaff": "Y",
        "scaleGroupFemida": "HR",
        "scaleGroupReview": "Other_review",
        "scaleGroupBonus": null,
        "scaleGroupReward": "Other_benefits"
    }
]}
'''


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_occupation_sync(_save_mock, build_updater):
    datasource = OccupationDatasource(Occupation.oebs_type, Occupation.method, Mock())
    datasource._data = json.loads(json_data)

    updater = build_updater(model=Occupation, datasource=datasource)
    updater.run_sync()
    assert Occupation.objects.count() == 5

    occupation1 = Occupation.objects.get(scale_name='DutyNetworkEngineer')
    assert occupation1.scale_code == 'NOA~AIT040'
    assert occupation1.scale_description_en == 'Duty Network Engineer'

    occupation2 = Occupation.objects.get(scale_name='NetworkEngineer')
    assert occupation2.scale_code == 'NOA~AID080'
    assert occupation2.scale_description_en == 'Network Engineer'

    occupation3 = Occupation.objects.get(scale_name='HelpDesk')
    assert occupation3.scale_code == 'ITC~AIT020'
    assert occupation3.scale_description_en == 'HelpDesk Specialist'

    occupation4 = Occupation.objects.get(scale_name='ItAssetManager')
    assert occupation4.scale_code == 'NOA~AIT000'
    assert occupation4.scale_description_en == 'IT Asset Manager'

    occupation5 = Occupation.objects.get(scale_name='EngSysDesigner')
    assert occupation5.scale_code == 'EGA~TPD270'

    assert occupation5.scale_description_en == 'Engineering Systems Designer'


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_occupation_sync_update(_save_mock, build_updater):
    update_json_data = '''
    {"scales": [{
        "scaleName": "DutyNetworkEngineer",
        "scaleDescription": "NOA~AIT040",
        "endDate": null,
        "scaleDescriptionRUS": "\xd0\x94\xd0\xb5\xd0\xb6\xd1\x83\xd1\x80\xd0\xbd\xd1\x8b\xd0\xb9",
        "scaleDescriptionENG": "Duty Network Engineer",
        "transferToStaff": "Y",
        "scaleGroupFemida": "HR",
        "scaleGroupReview": "Other_review",
        "scaleGroupBonus": null,
        "scaleGroupReward": "Other_benefits"
    }]}'''

    Occupation.objects.create(
        scale_name='DutyNetworkEngineer',
        scale_code='1',
        scale_description='222',
        scale_description_en='333',
    )
    datasource = OccupationDatasource(Occupation.oebs_type, Occupation.method, Mock())
    datasource._data = json.loads(update_json_data)

    updater = build_updater(model=Occupation, datasource=datasource)
    updater.run_sync()
    assert Occupation.objects.count() == 1

    o = Occupation.objects.get(scale_name='DutyNetworkEngineer')
    assert o.scale_code == 'NOA~AIT040'
    assert o.scale_description_en == 'Duty Network Engineer'
    assert o.scale_group_reward == 'Other_benefits'
