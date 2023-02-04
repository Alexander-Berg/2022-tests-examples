import getRootBodyType from 'auto-core/react/lib/seo/listingCommon/getRootTransmission';

it('Если передана одна трансмиссия - должен вернуть ее же', () => {
    expect(getRootBodyType([ 'ROBOT' ])).toEqual('ROBOT');
});

it('Если передано два типа трансмиссии разных групп - должен вернуть пустую строку', () => {
    expect(getRootBodyType([ 'ROBOT', 'MECHANICAL' ])).toEqual('');
});

it('Если переданы все типы трансмиссии одной группы, должен вернуть название этой группы', () => {
    expect(getRootBodyType([ 'AUTO', 'AUTOMATIC', 'ROBOT', 'VARIATOR' ]))
        .toEqual('AUTOMATIC');
});
