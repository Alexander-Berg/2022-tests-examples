const getPersonalizedOffersFeedHeaders = require('./getPersonalizedOffersFeedHeaders');

describe('заголовок x-mimic-user-id', () => {
    it('добавится если есть params.debug_yuid и мы во внутренней сети', () => {
        const params = {
            context: {
                req: {
                    isInternalNetwork: true,
                },
            },
            headers: {},
            params: {
                debug_yuid: '12345',
            },
        };

        const headers = getPersonalizedOffersFeedHeaders(params);

        expect(headers).toEqual({
            'x-mimic-user-id': '12345',
        });
    });

    it('не добавится если есть params.debug_yuid и мы не во внутренней сети', () => {
        const params = {
            context: {
                req: {
                    isInternalNetwork: false,
                },
            },
            headers: {},
            params: {
                debug_yuid: '12345',
            },
        };

        const headers = getPersonalizedOffersFeedHeaders(params);

        expect(headers).toEqual({});
    });

    it('не добавится если нет params.debug_yuid', () => {
        const params = {
            context: {
                req: {
                    isInternalNetwork: true,
                },
            },
            headers: {},
            params: {},
        };

        const headers = getPersonalizedOffersFeedHeaders(params);

        expect(headers).toEqual({});
    });
});
