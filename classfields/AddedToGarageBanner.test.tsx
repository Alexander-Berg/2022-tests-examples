/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
import React from 'react';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import contextMock from 'autoru-frontend/mocks/contextMock';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import sendPageMetrikaEventMocked from 'auto-core/react/dataDomain/garageCard/actions/sendPageMetrikaEvent';
import setToRootMocked from 'auto-core/react/dataDomain/cookies/actions/setToRoot';
import makeGarageCardMocked from 'auto-core/react/dataDomain/garageCard/actions/makeGarageCard';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import AddedToGarageBanner, { GARAGE_BANNER_CLOSED_COOKIE_NAME } from './AddedToGarageBanner';

jest.mock('auto-core/react/dataDomain/garageCard/actions/sendPageMetrikaEvent');
jest.mock('auto-core/react/dataDomain/cookies/actions/setToRoot');
jest.mock('auto-core/react/dataDomain/garageCard/actions/makeGarageCard');

const sendPageMetrikaEvent = sendPageMetrikaEventMocked as jest.MockedFunction<typeof sendPageMetrikaEventMocked>;
const setToRoot = setToRootMocked as jest.MockedFunction<typeof setToRootMocked>;
const makeGarageCard = makeGarageCardMocked as jest.MockedFunction<typeof makeGarageCardMocked>;

const store = mockStore({
    bunker: getBunkerMock([ 'garage/added-to-garage-banner' ]),
});

const offer = {
    id: 'abc hahaha',
} as unknown as Offer;

const MONTH = 30;

sendPageMetrikaEvent.mockImplementation(() => jest.fn());
setToRoot.mockImplementation(() => ({ type: 'COOKIES_CHANGE', payload: {} }));

beforeEach(() => {
    sendPageMetrikaEvent.mockClear();
    setToRoot.mockClear();
});

describe('успешно обработано нажатие на кнопку "в гараж"', () => {
    makeGarageCard.mockImplementation(() => jest.fn().mockResolvedValue({}));

    const render = () => {
        return shallow(
            <AddedToGarageBanner
                offer={ offer }
            />,
            { context: { ...contextMock, store } },
        );
    };

    it('поставит куку', async() => {
        const wrapper = await render();

        const button = wrapper.dive();
        button.simulate('buttonClick');

        await flushPromises();

        expect(setToRoot).toHaveBeenCalledTimes(1);
        expect(setToRoot).toHaveBeenLastCalledWith(GARAGE_BANNER_CLOSED_COOKIE_NAME, 'redirected', { expires: MONTH });
    });

    it('отправит метрику', async() => {
        const wrapper = await render();

        const button = wrapper.dive();
        button.simulate('buttonClick');

        await flushPromises();

        expect(sendPageMetrikaEvent).toHaveBeenCalledTimes(1);
        expect(sendPageMetrikaEvent).toHaveBeenLastCalledWith([ 'addedToGarageBanner', 'redirect' ]);
    });

    it('заблокирует кнопку', async() => {
        const wrapper = await render();

        const button = wrapper.dive();
        button.simulate('buttonClick');

        await flushPromises();

        expect(button.prop('isLoading')).toBe(true);
    });

    it('разблокирует кнопку, если случилась ошибка', async() => {
        makeGarageCard.mockImplementationOnce(() => jest.fn().mockResolvedValue({ error: '...' }));

        const wrapper = await render();

        const button = wrapper.dive();
        button.simulate('buttonClick');

        expect(button.prop('isLoading')).toBe(true);
        await flushPromises();
        expect(button.prop('isLoading')).toBe(false);
    });
});

describe('успешно обработано нажатие на кнопку "закрыть"', () => {
    makeGarageCard.mockImplementation(() => jest.fn().mockResolvedValue({}));

    const render = () => {
        return shallow(
            <AddedToGarageBanner
                offer={ offer }
            />,
            { context: { ...contextMock, store } },
        );
    };

    it('поставит куку', async() => {
        const wrapper = await render();

        const button = wrapper.dive();
        button.simulate('closeClick');

        await flushPromises();

        expect(setToRoot).toHaveBeenCalledTimes(1);
        expect(setToRoot).toHaveBeenLastCalledWith(GARAGE_BANNER_CLOSED_COOKIE_NAME, 'closed', { expires: MONTH });
    });

    it('отправит метрику', async() => {
        const wrapper = await render();

        const button = wrapper.dive();
        button.simulate('closeClick');

        await flushPromises();

        expect(sendPageMetrikaEvent).toHaveBeenCalledTimes(1);
        expect(sendPageMetrikaEvent).toHaveBeenLastCalledWith([ 'addedToGarageBanner', 'close' ]);
    });
});

it('не отрендерит, если есть кука', async() => {
    const storeWithCookies = mockStore({
        bunker: getBunkerMock([ 'garage/added-to-garage-banner' ]),
        cookies: {
            [ GARAGE_BANNER_CLOSED_COOKIE_NAME ]: 'closed',
        },
    });
    const wrapper = shallow(
        <AddedToGarageBanner
            offer={ offer }
        />,
        { context: { ...contextMock, store: storeWithCookies } },
    );

    expect(wrapper.dive().isEmptyRender()).toBe(true);
});
