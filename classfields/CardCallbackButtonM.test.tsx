/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/dealerCallback/helpers/sendFrontLogAfterCallback', () => jest.fn());

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import user from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import sendFrontLogAfterCallback from 'auto-core/react/dataDomain/dealerCallback/helpers/sendFrontLogAfterCallback';

import CardCallbackButtonM from './CardCallbackButtonM';

const Context = createContextProvider(contextMock);
const store = mockStore({
    config: { data: { pageType: 'card' } },
    dealerCallback: {},
    user,
    bunker: getBunkerMock([ 'common/telepony' ]),
});

it('при клике на кнопку должен отправить событие во фронт-лог', async() => {
    const page = shallow(
        <Context>
            <Provider store={ store }>
                <CardCallbackButtonM offer={{ ...offer, section: 'new' }}/>
            </Provider>
        </Context >,
    ).dive().dive().dive();

    const button = page.find('Button');
    button.simulate('click');

    expect(sendFrontLogAfterCallback).toHaveBeenCalled();
});
