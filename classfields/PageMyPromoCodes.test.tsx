jest.mock('auto-core/react/dataDomain/promoFeatures/actions/apply');
jest.mock('auto-core/react/dataDomain/promoFeatures/actions/fetchAll');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { promoFeaturesStateMock, promoFeatureMock } from 'auto-core/react/dataDomain/promoFeatures/mocks';
import applyPromoCode from 'auto-core/react/dataDomain/promoFeatures/actions/apply';
import fetchAllPromoCodes from 'auto-core/react/dataDomain/promoFeatures/actions/fetchAll';
import { showAutoclosableMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import PageMyPromoCodes from './PageMyPromoCodes';

let initialState: Partial<MobileAppState>;

const applyPromoCodeMock = applyPromoCode as jest.MockedFunction<typeof applyPromoCode>;
const applyPromoCodePromise = Promise.resolve();
applyPromoCodeMock.mockImplementation(() => () => applyPromoCodePromise);

const fetchAllPromoCodesMock = fetchAllPromoCodes as jest.MockedFunction<typeof fetchAllPromoCodes>;
const fetchAllPromoCodesPromise = Promise.resolve([
    promoFeatureMock.withTag('color').value(),
    promoFeatureMock.withTag('placement').value(),
]);
fetchAllPromoCodesMock.mockImplementation(() => () => fetchAllPromoCodesPromise);

const showAutoclosableMessageMock = showAutoclosableMessage as jest.MockedFunction<typeof showAutoclosableMessage>;

beforeEach(() => {
    initialState = {
        promoFeatures: promoFeaturesStateMock.withFeatures([]).value(),
    };

    contextMock.logVasEvent.mockClear();
});

describe('при применении промокода', () => {
    it('при успехе запросит все промокоды и отправит корректную метрику', () => {
        const page = shallowRenderComponent({ initialState });
        const input = page.find('PromoCodeInput');
        input.simulate('apply', 'foo');

        return applyPromoCodePromise
            .then(() => {})
            .then(() => {
                expect(fetchAllPromoCodesMock).toHaveBeenCalledTimes(1);

                expect(showAutoclosableMessageMock).toHaveBeenCalledTimes(1);
                expect(showAutoclosableMessageMock.mock.calls[0]).toMatchSnapshot();

                return fetchAllPromoCodesPromise
                    .then(() => {
                        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(3);
                        expect(contextMock.metrika.sendParams.mock.calls).toMatchSnapshot();
                    });
            });
    });

    it('при не успехе не будет запрашивать все промокоды и отправит корректную метрику', async() => {
        const failedRequestPromise = Promise.reject();
        applyPromoCodeMock.mockImplementationOnce(() => () => failedRequestPromise);
        const page = shallowRenderComponent({ initialState });
        const input = page.find('PromoCodeInput');
        input.simulate('apply', 'foo');

        return failedRequestPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                expect(fetchAllPromoCodesMock).toHaveBeenCalledTimes(0);
                expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(3);
                expect(contextMock.metrika.sendParams.mock.calls).toMatchSnapshot();
            },
        );
    });
});

function shallowRenderComponent({ initialState }: { initialState: Partial<MobileAppState> }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PageMyPromoCodes/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
