const prepareTags = require('./index');

const GROUPED_OFFERS = [
    {
        tags: [ '1', '2', '3' ],
    },
    {
        tags: [ '1', '2', '3', '4', '5' ],
    },
    {
        tags: [ '4', '5', '6' ],
    },
];

it('должен вернуть массив уникальных тегов из списка офферов', () => {
    expect(
        prepareTags({
            groupedOffers: GROUPED_OFFERS,
        }),
    ).toEqual([ '1', '2', '3', '4', '5', '6' ]);
});
