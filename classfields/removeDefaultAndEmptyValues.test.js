const removeDefaultAndEmptyValues = require('./removeDefaultAndEmptyValues');

const params = {
    currency: 'RUR',
    has_image: true,
    in_stock: 'ANY_STOCK',
    exchange_group: 'NO_EXCHANGE',
    seller_group: 'ANY_SELLER',
    damage_group: 'NOT_BEATEN',
    owners_count_group: 'ANY_COUNT',
    customs_state_group: 'CLEARED',
    body_type_group: [],
    engine_group: [ 'ANY_ENGINE' ],
    section: 'all',
    rid: '',
    with_autoru_expert: 'BOTH',
    on_credit: false,
};

const expectedResult = { section: 'all', rid: '' };

it('должен правильно удалить пустые параметры и дефолты', () => {
    const result = removeDefaultAndEmptyValues(params);
    expect(result).toEqual(expectedResult);
});
