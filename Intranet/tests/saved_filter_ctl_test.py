from staff.person_filter.saved_filter_ctl import SavedFilterCtl


def test_create_will_use_existing_filter(company, mocked_mongo):
    person = company['persons']['dep1-chief']
    person_for_filter = company['persons']['dep11-person']

    filter_data = {
        'person': person_for_filter,
        'department': None,
        'filter_id': None,
    }

    saved_filter = SavedFilterCtl(person).create(data=filter_data, with_task=False)
    assert saved_filter.id == SavedFilterCtl(person).create(data=filter_data, with_task=False).id
