import { prepareUrlForLog } from './prepareUrlForLog';

describe('common', () => {
    it.each([
        [
            '',
            '',
        ],
        [
            '/test?sessionid=3%3A1610989805.5.0.1610989805688%3AAQABAAAAAAATZYCwuAYCKg%3A9.1|4041549780.0.2|342174.167460.PsczHYrRm9exf97D5YNh0xWY8Mo',
            '/test?sessionid=3%3A1610989805.5.0.1610989805688%3AAQABAAAAAAATZYCwuAYCKg%3A9.1XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX',
        ],
    ])(`должен преобразовать %s %s`, (url, result) => {
        expect(prepareUrlForLog(url)).toEqual(result);
    });
});

describe('auto.ru', () => {
    it.each([
        [
            'http://backend/test?token=123456789012345678901234567890',
            'http://backend/test?token=123456789012345XXXXXXXXXXXXXXX',
        ],
        [
            'http://backend/test?sid=123456789012345678901234567890',
            'http://backend/test?sid=123456789012345XXXXXXXXXXXXXXX',
        ],
        [
            'http://backend/test?session_id=123456789012345678901234567890',
            'http://backend/test?session_id=123456789012345XXXXXXXXXXXXXXX',
        ],
        [
            'http://login:password@84.201.128.30/article/fordexcursiondubai/',
            'http://login:pXXXXXXX@84.201.128.30/article/fordexcursiondubai/',
        ],
    ])(`должен преобразовать %s %s`, (url, result) => {
        expect(prepareUrlForLog(url)).toEqual(result);
    });
});
