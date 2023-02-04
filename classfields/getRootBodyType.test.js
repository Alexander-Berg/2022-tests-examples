const getRootBodyType = require('auto-core/react/lib/seo/listingCommon/getRootBodyType');

it('Если передан один тип кузова - должен вернуть его же', () => {
    expect(getRootBodyType([ 'SEDAN' ])).toEqual('SEDAN');
});

it('Если передано два типа кузова разных групп - должен вернуть пустую строку', () => {
    expect(getRootBodyType([ 'SEDAN', 'HATCHBACK' ])).toEqual('');
});

it('Если переданы все типы кузова одной группы, должен вернуть название этой группы', () => {
    expect(getRootBodyType([ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ]))
        .toEqual('HATCHBACK');
});
