jest.mock('auto-core/lib/core/isMobileApp');

const preparer = require('./index');

describe('offer.services', () => {
    it('должен вернуть купленные услуги, если они есть в ответе', () => {
        const result = preparer({
            additional_info: {},
            id: '123-abc',
            services: [ { service: 'all_sale_activate', is_active: true } ],
        });

        expect(result).toMatchObject({
            services: [ { service: 'all_sale_activate', is_active: true } ],
        });
    });

    it('должен вернуть пустой массив, если в ответ нет услуг', () => {
        const result = preparer({
            additional_info: {},
            id: '123-abc',
        });

        expect(result).toMatchObject({
            services: [],
        });
    });
});
