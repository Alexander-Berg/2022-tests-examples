import router from 'realty-core/view/react/libs/router';

router.findFirst = () => true;

import { isValidRealtyUrl } from '../index';

const VALID_URLS = [
    ['https://realty.yandex.ru/management-new'],
    ['https://realty.test.vertis.yandex.ru/'],
    ['//realty.test.vertis.yandex.ru/'],
    ['%2Fmanagement-new%2Ffeeds%2Fadd%2F'],
    ['/management-new/feeds/add/'],
];

const INVALID_URLS = [
    ['http://realty.yandex.ru/'],
    ['https://evil.com/'],
    ['//evil.com/'],
    ['////evil.com/'],
    ['https://yandex.ru.attacker.com'],
    ['javascript%3Aalert(document.location)'],
    ['javascript:alert(document.location)'],
];

describe('Корректные адреса', () => {
    it.each(VALID_URLS)('Валиадный адрес: %s', (url) => {
        expect(isValidRealtyUrl(url)).toEqual(true);
    });
});

describe('Некорректные адреса', () => {
    it.each(INVALID_URLS)('Не валиадный адрес: %s', (url) => {
        expect(isValidRealtyUrl(url)).toEqual(false);
    });
});
