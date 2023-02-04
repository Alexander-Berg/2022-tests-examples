import getOfferIdFromUrl from './getOfferIdFromUrl';

describe('getOfferIdFromUrl получает id оффера из ссылки', () => {
    it('ссылка со слэшом на конце', () => {
        const mockUrl = 'https://auto.ru/cars/used/sale/audi/a3/1114610398-20e1336a/';

        expect(getOfferIdFromUrl(mockUrl)).toBe('1114610398-20e1336a');
    });

    it('ссылка без слэша на конце', () => {
        const mockUrl = 'https://auto.ru/cars/used/sale/audi/a3/1114610398-20e1336a';

        expect(getOfferIdFromUrl(mockUrl)).toBe('1114610398-20e1336a');
    });

    it('тестовая ссылка', () => {
        const mockUrl = 'https://test.avto.ru/cars/used/sale/audi/a3/1114610398-20e1336a/';

        expect(getOfferIdFromUrl(mockUrl)).toBe('1114610398-20e1336a');
    });

    it('урл не страницы оффера', () => {
        const mockUrl = 'https://auto.ru/moskva/cars/vaz/all/';

        expect(getOfferIdFromUrl(mockUrl)).toBe('');
    });
});
