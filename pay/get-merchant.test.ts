import { getMerchant } from './get-merchant';

describe('getMerchant', function () {
    beforeEach(() => {
        // @ts-ignore
        delete window.location;
    });

    it('should return test merchant', () => {
        // @ts-ignore
        window.location = new URL('https://test.pay.yandex.ru');

        expect(getMerchant()).toStrictEqual({
            id: '1003b7ce-f225-4675-bf4e-e49c3ac5747c',
            name: 'testing-merchant',
            url: '',
        });
    });

    it('should return empty merchant if not defined', () => {
        // @ts-ignore
        window.location = new URL('http://localhost');

        expect(getMerchant()).toStrictEqual({
            id: '',
            name: '',
            url: '',
        });
    });
});
