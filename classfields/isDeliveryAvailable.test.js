const isDeliveryAvailable = require('./isDeliveryAvailable');

it('должен возвращать false, если нет регионов доставки', () => {
    const offer = {
        delivery_info: {
            delivery_regions: [],
        },
    };

    expect(isDeliveryAvailable(offer)).toBe(false);
});

it('должен возвращать true, если есть хотя бы один регион доставки', () => {
    const offer = {
        delivery_info: {
            delivery_regions: [
                {
                    location: { region_info: { accusative: 'Москву' } },
                },
            ],
        },
    };

    expect(isDeliveryAvailable(offer)).toBe(true);
});
