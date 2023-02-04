jest.mock('auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen', () => ({ 'default': jest.fn(() => {}) }));
jest.mock('auto-core/react/dataDomain/state/actions/gusModalOpen', () => ({ 'default': jest.fn(() => {}) }));
jest.mock('auto-core/react/lib/phones/getPhonesWithProofOfWork');
jest.mock('auto-core/react/dataDomain/offerPhones/lib/sendMetricsAfterPhonesFetch');
jest.mock('auto-core/react/dataDomain/offerPhones/lib/sendRetargetingAfterPhonesFetch');
jest.mock('auto-core/react/dataDomain/offerPhones/lib/sendFrontLogAfterPhonesFetch');

const getPhonesWithProofOfWork = require('auto-core/react/lib/phones/getPhonesWithProofOfWork').default;
const authModalWithCallbackOpen = require('auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen').default;
const gusModalOpen = require('auto-core/react/dataDomain/state/actions/gusModalOpen').default;
const sendMetricsAfterPhonesFetch = require('auto-core/react/dataDomain/offerPhones/lib/sendMetricsAfterPhonesFetch');
const sendRetargetingAfterPhonesFetch = require('auto-core/react/dataDomain/offerPhones/lib/sendRetargetingAfterPhonesFetch');
const sendFrontLogAfterPhonesFetch = require('auto-core/react/dataDomain/offerPhones/lib/sendFrontLogAfterPhonesFetch');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const fetchPhones = require('./fetch');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

let store;

beforeEach(() => {
    store = mockStore({ bunker: {} });
});

it('должен вызвать экшн открытия авторизации с параметром isCallback, если getPhonesWithProofOfWork вернул соответствующую ошибку', () => {
    const error = {
        error: 'ACTION_REQUIRED',
        action_kind: 'NEED_AUTH',
    };
    const pr = Promise.reject(error);
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock, authCallback: () => {} })).then(() => {}, () => {
        return pr.then(() => {}, () => {
            expect(authModalWithCallbackOpen.mock.calls[0][0].callbackParams).toHaveProperty('isCallback', true);
        });
    });
});

it('не должен вызвать экшн открытия авторизации при успешном ответе getPhonesWithProofOfWork', () => {
    const pr = Promise.resolve();
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock, authCallback: () => {} })).then(() => {
        return pr.then(() => expect(authModalWithCallbackOpen).not.toHaveBeenCalled());
    });
});

it('не должен вызвать экшн открытия авторизации, если getPhonesWithProofOfWork вернул другую ошибку', () => {
    const error = { error: 'SOME ERROR' };
    const pr = Promise.reject(error);
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock, authCallback: () => {} })).then(() => {}, () => {
        return pr.then(() => {}, () => expect(authModalWithCallbackOpen).not.toHaveBeenCalled());
    });
});

it('должен вызвать sendMetricsAfterPhonesFetch, sendRetargetingAfterPhonesFetch и sendFrontLogAfterPhonesFetch', () => {
    const pr = Promise.resolve();
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock, authCallback: () => {} }, {})).then(() => {
        return pr.then(() => {
            expect(sendMetricsAfterPhonesFetch).toHaveBeenCalled();
            expect(sendRetargetingAfterPhonesFetch).toHaveBeenCalled();
            expect(sendFrontLogAfterPhonesFetch).toHaveBeenCalled();
        });
    });
});

it('не должен вызвать sendMetricsAfterPhonesFetch и sendFrontLogAfterPhonesFetch с параметром isCallback', () => {
    const pr = Promise.resolve();
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock, authCallback: () => {}, isCallback: true }, {})).then(() => {
        return pr.then(() => {
            expect(sendMetricsAfterPhonesFetch).not.toHaveBeenCalled();
            expect(sendRetargetingAfterPhonesFetch).not.toHaveBeenCalled();
        });
    });
});

it('не должен вызывать sendFrontLogAfterPhonesFetch, если прокидывается опция disableEventsLog', () => {
    const pr = Promise.resolve();
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock, authCallback: () => {}, isCallback: true }, { disableEventsLog: true })).then(() => {
        return pr.then(() => {
            expect(sendFrontLogAfterPhonesFetch).not.toHaveBeenCalled();
        });
    });
});

it('должен вызвать gusModalOpen при соответствующей ошибке', () => {
    const error = {
        error: 'ACTION_REQUIRED',
        action_kind: 'SHOW_URL',
        action_tags: [ 'GOSUSLUGI_REQUIRED' ],
    };
    const pr = Promise.reject(error);
    getPhonesWithProofOfWork.mockImplementation(() => pr);
    return store.dispatch(fetchPhones({ offer: cardMock })).then(() => {}, () => {
        return pr.then(() => {}, () => {
            expect(gusModalOpen).toHaveBeenCalled();
        });
    });
});
