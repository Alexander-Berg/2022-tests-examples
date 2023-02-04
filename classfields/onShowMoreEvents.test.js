/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const _ = require('lodash');
const gateApi = require('auto-core/react/lib/gateApi');
const actionTypes = require('../actionTypes');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const walkInMock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const onShowMoreEvents = require('./onShowMoreEvents');

it('должен запрашивать следующую страницу эвентов с теми же параметрами', () => {
    const store = mockStore({
        ...walkInMock,
        config: {
            client: {
                id: '123',
            },
        },
    });

    gateApi.getResource.mockImplementation(jest.fn(() => Promise.resolve()));

    store.dispatch(onShowMoreEvents());

    const expectedRequestParams = {
        category: 'cars',
        dealer_id: '123',
        from: '2019-10-01',
        to: '2019-10-28',
        page: 4,
    };

    expect(gateApi.getResource).toHaveBeenCalledWith('getWalkInEventsList', expectedRequestParams);
});

it('должен диспатчить экшен обновления эвентов со смердженной коллекцией прошлых эвентов и новых', async() => {
    const walkInMockClone = _.cloneDeep(walkInMock);
    const newEvent = walkInMockClone.walkIn.eventsList.events[0];

    newEvent.date = '2012-12-15';

    const store = mockStore(walkInMock);

    gateApi.getResource.mockImplementation(jest.fn(() => Promise.resolve({
        events: [
            newEvent,
        ],
        paging: {},
    })));

    await store.dispatch(onShowMoreEvents());

    const updateAction = store.getActions().find((action) => action.type === actionTypes.UPDATE_EVENTS);

    const events = updateAction.payload.events;

    const expectedEvents = [ ...walkInMock.walkIn.eventsList.events, newEvent ];

    expect(events).toEqual(expectedEvents);
});
