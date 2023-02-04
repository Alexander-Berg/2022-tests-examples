import calculatePlacementAndServices from 'www-cabinet/react/dataDomain/sales/helpers/calculatePlacementAndServices';

const data = [ {
    metrics: [
        {
            special_shows: '155',
            special_offer: 12000,
            placement: 95000,
            premium: 50000,
            day: '2022-04-25',
        },
        {
            badge: 4000,
            placement: 95000,
            day: '2022-04-22',
        },
        {
            day: '2022-04-20',
        },
        {
            badge: 4000,
            placement: 95000,
            day: '2022-04-24',
        },
        {
            placement: 95000,
            day: '2022-04-21',
        },
        {
            badge: 4000,
            placement: 95000,
            day: '2022-04-23',
        },
        {
            premium: 50000,
            day: '2022-04-27',
        },
        {
            placement: 95000,
            premium: 50000,
            day: '2022-04-26',
        },
    ],
} ];

it('должен посчитать деньги на услуги и размещение', () => {
    expect(calculatePlacementAndServices(data)).toEqual({
        placement: 5700,
        services: 1740,
    });
});
