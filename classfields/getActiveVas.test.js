const getActiveVas = require('./getActiveVas');

const offer = {
    service_prices: [
        {
            service: 'package_turbo',
            package_services: [
                { service: 'all_sale_color' },
                { service: 'all_sale_special' },
                { service: 'all_sale_toplist' },
            ],
        },
    ],
};

it('возвращает подключеные к оферу васы', () => {
    offer.services = [
        { service: 'all_sale_toplist', is_active: true },
        { service: 'all_sale_special', is_active: false },
    ];
    expect(getActiveVas(offer)).toEqual([ 'all_sale_toplist' ]);
});

it('не учитывает активацию объявления, так как она не является васом', () => {
    offer.services = [
        { service: 'all_sale_toplist', is_active: true },
        { service: 'all_sale_activate', is_active: true },
    ];
    expect(getActiveVas(offer)).toEqual([ 'all_sale_toplist' ]);
});

it('если подключен пакет, убирает васы которые входят в пакет', () => {
    offer.services = [
        { service: 'package_turbo', is_active: true },
        { service: 'all_sale_color', is_active: true },
        { service: 'all_sale_special', is_active: true },
        { service: 'all_sale_toplist', is_active: true },
        { service: 'all_sale_fresh', is_active: true },
    ];
    expect(getActiveVas(offer)).toEqual([ 'package_turbo', 'all_sale_fresh' ]);
});
