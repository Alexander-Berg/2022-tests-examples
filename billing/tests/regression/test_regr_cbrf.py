import pytest


@pytest.mark.regression
def test_get_fields(refs_get):

    expected_fields = [
        'bic', 'nameFull', 'nameFullEng', 'regionCode', 'countryCode', 'zip', 'place', 'placeType', 'address',
        'regnum', 'type', 'dateAdded', 'corr', 'swift', 'restricted', 'restrictions', 'archived', 'accounts', 'checksum'
    ]
    result = refs_get('/api/cbrf/?query={__type(name: "Bank") {description fields {name description}}}')
    result_fields = [field['name'] for field in result['data']['__type']['fields']]
    assert set(expected_fields) == set(result_fields)

    query_fields = result_fields.copy()
    query_fields.remove('accounts')
    query_fields.remove('restrictions')

    query_fields.append('accounts {number}')
    query_fields.append('restrictions {date}')

    result = refs_get(
        '/api/cbrf/?query={banks(bic:["044525728", "046311904"]) {%(fields)s}}',
        fields=query_fields)
    assert not result.get('errors', None)

    bank_data = result['data']['banks'][0]
    assert set(bank_data.keys()) == set(result_fields)
    assert 'number' in bank_data['accounts'][0]

@pytest.mark.regression
def test_get_without_fields(refs_get):
    result = refs_get('/api/cbrf/?query={banks(bic:["044525728", "046311904"]) {}}')
    assert 'Syntax Error GraphQL' in result['errors'][0]['message']


@pytest.mark.regression
def test_get_incorrect_query(refs_get):
    result = refs_get('/api/cbrf/?query={tests(bic:["044525728", "046311904"]) {nameFull zip}}')
    assert 'Cannot query field' in result['errors'][0]['message']


@pytest.mark.regression
def test_get_incorrect_filter(refs_get):
    result = refs_get('/api/cbrf/?query={banks(bic:["11123111111"]) {nameFull zip}}')
    assert not result.get('errors', None)
    assert result['data']['banks'] == []


@pytest.mark.regression
def test_get_with_incorrect_fields(refs_get):
    result = refs_get('/api/cbrf/?query={banks(bic:["044525728", "046311904"]) {testField}}')
    assert 'Cannot query field' in result['errors'][0]['message']


@pytest.mark.regression
def test_result(refs_get):
    expected_result = [
        {
            "bic": "044525728", "nameFull": "ООО \"СПЕЦСТРОЙБАНК\"", "zip": "109004"
        },
        {
            "bic": "044525745",
            "nameFull": "ФИЛИАЛ № 7701 БАНКА ВТБ (ПАО)",
            "zip": "107031"
        }
    ]
    result = refs_get('/api/cbrf/?query={banks(bic:["044525728", "044525745"]) {bic nameFull zip}}')
    assert result['data']['banks'] == expected_result
