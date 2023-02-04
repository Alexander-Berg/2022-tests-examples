const getBadgesForSearchApp = require('./getBadgesForSearchApp');
const offerMock = require('autoru-frontend/mockData/responses/offer.mock.json');

it('Должен отдать бейджи для оффера', () => {
    expect(Object.values(getBadgesForSearchApp(offerMock))
        .filter(badge => badge.condition)).toEqual([ {
        background: '#F2F2F7',
        color: '#007AFF',
        condition: 'OK',
        text: 'Отчёт по VIN',
    } ]);
});
