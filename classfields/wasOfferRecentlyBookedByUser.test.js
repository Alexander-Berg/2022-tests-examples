const wasOfferRecentlyBookedByUser = require('./wasOfferRecentlyBookedByUser');

const cookiesMock = {
    booking_recent_offers: '{"50603092":["1097507660-ed34ec22"],"12345":["123-456", "2-3"]}',
};

it('должен вернуть true', () => {
    const userID = '50603092';
    const offer = {
        id: '1097507660',
        hash: 'ed34ec22',
    };

    expect(wasOfferRecentlyBookedByUser({
        offer,
        userID,
        cookies: cookiesMock,
    })).toEqual(true);
});

it('должен вернуть true, если есть несколько недавних броней у одного юзера', () => {
    const userID = '12345';
    const offer = {
        id: '2',
        hash: '3',
    };

    expect(wasOfferRecentlyBookedByUser({
        offer,
        userID,
        cookies: cookiesMock,
    })).toEqual(true);
});

it('должен вернуть false, если не совпадает userID', () => {
    const userID = '12346';
    const offer = {
        id: '123',
        hash: '456',
    };

    expect(wasOfferRecentlyBookedByUser({
        offer,
        userID,
        cookies: cookiesMock,
    })).toEqual(false);
});

it('должен вернуть false, если не совпадает offerID', () => {
    const userID = '50603092';
    const offer = {
        id: '109750766',
        hash: 'ed34ec2',
    };

    expect(wasOfferRecentlyBookedByUser({
        offer,
        userID,
        cookies: cookiesMock,
    })).toEqual(false);
});
