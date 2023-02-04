import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import userEvent from '@testing-library/user-event';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import garagePromoMock from 'auto-core/react/dataDomain/garagePromoAll/mocks';

import GaragePromoAdd from './GaragePromoAdd';

const Context = createContextProvider(contextMock);

const promo = garagePromoMock.value();

declare var global: { location: Record<string, any> };
const { location } = global;

beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = {
        assign: jest.fn(),
    };
});

afterEach(() => {
    global.location = location;
});

describe('метрики', () => {
    const renderComponent = () => {
        return render(
            <Context>
                <Provider store={ mockStore({ vinCheckInput: { value: 'A000AA00' } }) }>
                    <GaragePromoAdd
                        promo={ promo }
                        pageParams={{}}
                        onClose={ jest.fn() }
                        metrikaPage="promo_all"
                    />
                </Provider>
            </Context>,
        );
    };

    it('показ', () => {
        renderComponent();
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'common_promo', 'popup_no_garage', 'show' ]);
    });

    it('клик на линк "войти"', () => {
        renderComponent();

        const link = screen.getByText('Войдите, чтобы увидеть свои автомобили');
        userEvent.click(link);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'common_promo', 'popup_no_garage', 'auth_link' ]);
    });

    it('сабмит', () => {
        renderComponent();

        const button = screen.getByText('Добавить');
        userEvent.click(button);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'promo_all', 'common_promo', 'popup_no_garage', 'submit' ]);
    });
});
