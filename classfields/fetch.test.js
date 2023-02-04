jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getResource = require('auto-core/react/lib/gateApi').getResource;

const fetch = require('./fetch');

it('не должен ничего менять в параметрах запроса и в ответе, если нет excluded_offers', () => {
    const dispatch = jest.fn();

    const responseMock = Promise.resolve([ { radius: 300, count: 1, offers: [ { id: 1 } ] } ]);

    getResource.mockReturnValue(responseMock);

    return fetch({})(dispatch).then(() => {
        expect(getResource).toHaveBeenCalledWith('getListingLocatorCounters', { exclude_offer_id: [] });
        expect(dispatch.mock.calls).toEqual([
            [ { type: 'LISTING_LOCATOR_COUNTERS_PENDING' } ],
            [ {
                type: 'LISTING_LOCATOR_COUNTERS_RESOLVED',
                payload: [
                    { radius: 300, count: 1, offers: [ { id: 1 } ] },
                ],
            } ],
        ]);
    });
});

it('отправляет в параметрах запроса excluded_offers_id и shouldGroup=false', () => {
    const dispatch = jest.fn();

    const responseMock = Promise.resolve([ { radius: 300, count: 1, offers: [ { id: 1 } ] } ]);

    getResource.mockReturnValue(responseMock);

    return fetch({}, [ 4 ])(dispatch).then(() => {
        expect(getResource).toHaveBeenCalledWith('getListingLocatorCounters', { exclude_offer_id: [ 4 ] });
        expect(dispatch.mock.calls).toEqual([
            [ { type: 'LISTING_LOCATOR_COUNTERS_PENDING' } ],
            [ {
                type: 'LISTING_LOCATOR_COUNTERS_RESOLVED',
                payload: [
                    { radius: 300, count: 1, offers: [ { id: 1 } ] },
                ],
            } ],
        ]);
    });
});

it('добавляет в параметрах запроса группировку и отсекает ненужные офферы, если shouldGroup=false и массив exluded_offer_id есть', () => {
    const dispatch = jest.fn();

    const responseMock = Promise.resolve([
        { radius: 300, count: 1, offers: [ { id: 1 } ] },
        { radius: 500, count: 1, offers: [ { id: 2 }, { id: 3 } ] },
    ]);

    getResource.mockReturnValue(responseMock);

    return fetch({}, [ 3 ], true)(dispatch).then(() => {
        expect(getResource).toHaveBeenCalledWith('getListingLocatorCounters', { group_by: [ 'CONFIGURATION' ] });
        expect(dispatch.mock.calls).toEqual([
            [ { type: 'LISTING_LOCATOR_COUNTERS_PENDING' } ],
            [ {
                type: 'LISTING_LOCATOR_COUNTERS_RESOLVED',
                payload: [
                    { radius: 300, count: 1, offers: [ { id: 1 } ] },
                    { radius: 500, count: 1, offers: [ { id: 2 } ] },
                ],
            } ],
        ]);
    });
});
