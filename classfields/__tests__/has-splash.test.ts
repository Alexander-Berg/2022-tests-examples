import { AnyObject } from 'realty-core/types/utils';

import hasSplash from '../has-splash';

function getDefaultReq() {
    return {
        query: {},
        uatraits: {
            isRobot: () => false,
            getOSFamily: () => 'android',
        },
        cookies: {
            from: 'direct',
        },
        experimentsData: {
            has: () => false,
        },
        headers: {},
    };
}

function hasExpMock(key: string) {
    return key === 'REALTYFRONT-11965_disable_splash_touch' ? true : false;
}

describe('есть сплеш если', () => {
    it('нет эксперимента', () => {
        const result = hasSplash(getDefaultReq());

        expect(result).toBe(true);
    });
});

describe('нет сплеша если', () => {
    let expReq: AnyObject;

    beforeEach(() => {
        expReq = getDefaultReq();
    });

    it('платформа не ios или android', () => {
        const req = { ...expReq, uatraits: { ...expReq.uatrairs, getOSFamily: () => '' } };

        const result = hasSplash(req);

        expect(result).toBe(false);
    });

    it('запрос от робота', () => {
        expReq.uatraits.isRobot = () => true;

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('роут с отключенным сплешом', () => {
        expReq.router = {
            route: { getData: () => ({ disableSplash: true }) },
        };

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('запрос nosplash', () => {
        expReq.query.nosplash = true;

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('запрос на only-content', () => {
        expReq.query['only-content'] = true;

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('кука на отключения баннера', () => {
        expReq.cookies.splash_banner_closed = '1';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('эксперимент и пришли из Яндекса (morda)', () => {
        expReq.experimentsData.has = hasExpMock;
        expReq.cookies.from = 'morda';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('эксперимент и пришли из Яндекса (yandex)', () => {
        expReq.experimentsData.has = hasExpMock;
        expReq.cookies.from = 'yandex';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('эксперимент и пришли из Яндекса (search)', () => {
        expReq.experimentsData.has = hasExpMock;
        expReq.cookies.from = 'search';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('эксперимент и пришли из Яндекса (wizard)', () => {
        expReq.experimentsData.has = hasExpMock;
        expReq.cookies.from = 'wizard';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('пришли из гугла (referer)', () => {
        expReq.headers.referer = 'https://google.com/';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });

    it('пришли из гугла (cookie)', () => {
        expReq.cookies.from = 'google-search';

        const result = hasSplash(expReq);

        expect(result).toBe(false);
    });
});
