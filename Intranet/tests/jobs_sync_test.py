import json

import pytest
from mock import patch, Mock

from datetime import date, timedelta

from staff.oebs.controllers.datasources.jobs_datasource import JobsDatasource
from staff.oebs.models import Job


json_data = '''
{
    "jobs": [{
            "code": 82906,
            "jobNameRus": "\xd0\x97\xd0\xb0\xd0\xbc\xd0\xb5\xd1\x81\xd1\x82\xd0\xb8\xd1\x82\xd0\xb5\xd0\xbb\xd1\x8c",
            "jobNameEng": "Deputy Head of IT System Maintenance in Data Center",
            "startDate": "2014-03-30",
            "endDate": "2018-06-30"
        }, {
            "code": 71906,
            "jobNameRus": "\xd0\xa1\xd1\x82\xd0\xb0\xd0\xb6\xd0\xb5\xd1\x80-\xd0\xbc\xd0\xbe\xd0\xb4\xd0\xb5\xd1",
            "jobNameEng": "\xd0\xa1\xd1\x82\xd0\xb0\xd0\xb6\xd0\xb5\xd1\x80-\xd0\xbc\xd0\xbe\xd0\xb4\xd0\xb5\xd1",
            "startDate": "2014-03-12",
            "endDate": null
        }, {
            "code": 55906,
            "jobNameRus": "\xd0\xa0\xd0\xb5\xd0\xb4\xd0\xb0\xd0\xba\xd1\x82\xd0\xbe\xd1\x80 \xd0\xb1\xd0\xb0\xd0",
            "jobNameEng": "Database Editor",
            "startDate": "2013-10-01",
            "endDate": null
        }
    ]
}
'''


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_jobs_sync(_save_mock, build_updater):
    datasource = JobsDatasource(Job.oebs_type, Job.method, Mock())
    datasource._data = json.loads(json_data)

    updater = build_updater(model=Job, datasource=datasource)
    updater.run_sync()
    assert Job.objects.count() == 3

    job1 = Job.objects.get(code=82906)
    assert job1.start_date == date(year=2014, month=3, day=30)
    assert job1.end_date == date(year=2018, month=6, day=30)
    assert job1.name_en == 'Deputy Head of IT System Maintenance in Data Center'

    job2 = Job.objects.get(code=71906)
    assert job2.start_date == date(year=2014, month=3, day=12)
    assert job2.end_date is None

    job3 = Job.objects.get(code=55906)
    assert job3.start_date == date(year=2013, month=10, day=1)
    assert job3.end_date is None
    assert job3.name_en == 'Database Editor'


@pytest.mark.django_db
@patch('staff.oebs.controllers.datasources.oebs_datasource.OEBSDatasource._save_to_s3')
def test_jobs_update(_save_mock, build_updater):
    Job.objects.create(
        code=1,
        name_en='11',
        name='11',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=1),
    )
    Job.objects.create(
        code=2,
        name_en='22',
        name='22',
        start_date=date.today(),
        end_date=date.today() + timedelta(days=2),
    )
    json_data = '''
    {
        "jobs": [{
                "code": 2,
                "jobNameRus": "222",
                "jobNameEng": "222",
                "startDate": "2014-03-30",
                "endDate": "2018-06-30"
            }, {
                "code": 3,
                "jobNameRus": "3",
                "jobNameEng": "3",
                "startDate": "2014-03-12",
                "endDate": null
            }
        ]
    }
    '''
    datasource = JobsDatasource(Job.oebs_type, Job.method, Mock())
    datasource._data = json.loads(json_data)
    updater = build_updater(model=Job, datasource=datasource)
    updater.run_sync()
    assert Job.objects.count() == 3

    job1 = Job.objects.get(code=1)
    assert job1.is_deleted_from_oebs is True

    job2 = Job.objects.get(code=2)
    assert job2.is_deleted_from_oebs is False
    assert job2.name == '222'
    assert job2.name_en == '222'
    assert job2.start_date == date(year=2014, month=3, day=30)
    assert job2.end_date == date(year=2018, month=6, day=30)

    job3 = Job.objects.get(code=3)
    assert job3.is_deleted_from_oebs is False
    assert job3.name == '3'
    assert job3.name_en == '3'
    assert job3.start_date == date(year=2014, month=3, day=12)
    assert job3.end_date is None
