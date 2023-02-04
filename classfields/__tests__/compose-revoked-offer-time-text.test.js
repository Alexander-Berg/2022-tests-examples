import composeText from '../';

describe('compose-revoked-offer-time-text', () => {
    it('returns correct string if revokedDate is not passed', () => {
        expect(composeText()).toEqual('Объявление устарело');
    });

    it('returns correct string if revokedDate is not passed and the func is called from offer page', () => {
        expect(composeText(undefined, 'offer'))
            .toEqual('Объявление устарело или снято с публикации');
    });

    it('returns correct string if date is passed as utc string', () => {
        const revokeDate = '2019-07-27T23:56:10Z';
        const currentDate = new Date('2019-07-29T23:58:10Z');

        expect(composeText(revokeDate, 'serp', currentDate))
            .toEqual('Снято 2 дня назад');
    });

    it('returns correct string if diff is less than an hour', () => {
        const revokeDate = '2019-07-27T23:16:10Z';
        const currentDate = new Date('2019-07-27T23:46:10Z');

        expect(composeText(revokeDate, 'serp', currentDate))
            .toEqual('Снято менее часа назад');
    });

    it('returns correct string if diff is less than a day', () => {
        const revokeDate = '2019-07-27T13:56:10Z';
        const currentDate = new Date('2019-07-27T23:57:10Z');

        expect(composeText(revokeDate, 'serp', currentDate))
            .toEqual('Снято 10 часов назад');
    });

    it('returns correct string if diff is more than 7 days', () => {
        const revokeDate = '2019-07-27T13:56:10Z';
        const currentDate = new Date('2019-08-27T23:57:10Z');

        expect(composeText(revokeDate, 'serp', currentDate))
            .toEqual('Объявление устарело');
    });

    it('returns correct string if diff is more than 7 days and the func is called from offer page', () => {
        const revokeDate = '2019-07-27T13:56:10Z';
        const currentDate = new Date('2019-08-27T23:57:10Z');

        expect(composeText(revokeDate, 'offer', currentDate))
            .toEqual('Объявление устарело или снято с публикации');
    });

    it('returns correct string if diff is less than an hour and the func is called from offer page', () => {
        const revokeDate = '2019-07-27T23:16:10Z';
        const currentDate = new Date('2019-07-27T23:46:10Z');

        expect(composeText(revokeDate, 'offer', currentDate))
            .toEqual('Объявление устарело или снято с публикации менее часа назад');
    });
});
