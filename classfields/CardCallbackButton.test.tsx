/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/dealerCallback/helpers/sendFrontLogAfterCallback', () => jest.fn());

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import mockOffer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import sendFrontLogAfterCallback from 'auto-core/react/dataDomain/dealerCallback/helpers/sendFrontLogAfterCallback';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';

import type { ReduxState } from './CardCallbackButton';
import CardCallbackButton from './CardCallbackButton';

const Context = createContextProvider(contextMock);
let store: ThunkMockStore<ReduxState>;
beforeEach(() => {
    store = mockStore({
        config: configStateMock.withPageType('card').value(),
        dealerCallback: { isSuccess: false, isVisible: false, pending: false, place: undefined },
        bunker: getBunkerMock([ 'common/callback_callcenter' ]),
        user: userStateMock.value(),
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    });
});

it('при клике на кнопку должен отправить событие во фронт-лог', async() => {
    const offer = cloneOfferWithHelpers(mockOffer).withSection('new').value();
    const page = shallow(
        <Context>
            <Provider store={ store }>
                <CardCallbackButton
                    offer={ offer }
                    place="card"
                    contextBlock={ ContextBlock.BLOCK_CARD }
                    contextPage={ ContextPage.PAGE_CARD }
                />
            </Provider>
        </Context>,
    ).dive().dive().dive();

    const button = page.find('Button');
    button.simulate('click');

    expect(sendFrontLogAfterCallback).toHaveBeenCalled();
});
