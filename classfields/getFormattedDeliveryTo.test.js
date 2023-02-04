const getFormattedDeliveryTo = require('./getFormattedDeliveryTo');

it('должен правильно форматировать одиночный город', () => {
    const offer = {
        delivery_info: {
            delivery_regions: [
                {
                    location: { region_info: { accusative: 'Москву' } },
                },
            ],
        },
    };

    expect(getFormattedDeliveryTo(offer)).toEqual([ 'Москву' ]);
});

it('должен правильно форматировать несколько городов', () => {
    const offer = {
        delivery_info: {
            delivery_regions: [
                {
                    location: { region_info: { accusative: 'Москву' } },
                },
                {
                    location: { region_info: { accusative: 'Александрию' } },
                },
                {
                    location: { region_info: { accusative: 'Воркуту' } },
                },
            ],
        },
    };

    expect(getFormattedDeliveryTo(offer)).toEqual([ 'Москву', 'Александрию', 'Воркуту' ]);
});
