import React from 'react';
import { Provider } from 'react-redux';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '@testing-library/react';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import AddOfferModal from 'www-mobile/react/components/AddOfferModal/AddOfferModal';

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const initialState = {
    addOfferModal: {
        isVisible: true,
        hasAnimation: false,
    },
    user: {
        data: {
            auth: true,
            phones: [ {
                phone: '79991112233',
            } ],
        },
    },
};

const Container = () => {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    const store = mockStore(initialState);
    const Context = createContextProvider(contextMock);

    mockUseSelector(initialState);
    mockUseDispatch(store);

    return (
        <Provider store={ store }>
            <Context>
                <AddOfferModal/>
            </Context>
        </Provider>
    );
};

it('должен отрисовать модальное окно и отправить метрику', () => {
    const metrikaParams = [ 'add-form-lite', 'show' ];
    render(<Container/>);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith(metrikaParams);
});

it('должен закрыть модальное окно при клике на "X" и отправить метрику при клике', async() => {
    const metrikaParams = [ 'add-form-lite', 'close' ];
    render(<Container/>);

    const closeButton = screen.getByLabelText('close');

    userEvent.click(closeButton);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith(metrikaParams);
});

it('должен отрисовать кнопку "Продолжить в приложении" и отправить метрику при клике', () => {
    const metrikaParams = [ 'add-form-lite', 'install_app' ];
    render(<Container/>);

    userEvent.click(screen.getByText(/Продолжить в приложении/i));

    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith(metrikaParams);
});

it('должен отрисовать кнопку "Разместить с оператором" и отправить метрику при клике', async() => {
    const metrikaParams = [ 'add-form-lite', 'add_with_operator' ];
    render(<Container/>);

    userEvent.click(screen.getByText(/Разместить с оператором/i));

    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith(metrikaParams);
});

it('после выбора "Разместить с оператором" должен закрыть модальное окно при клике на "X" и отправить метрику при клике', async() => {
    const metrikaParams = [ 'add-form-lite', 'add_with_operator', 'close' ];
    render(<Container/>);

    userEvent.click(screen.getByText(/Разместить с оператором/i));

    const closeButton = screen.getByLabelText('close');

    userEvent.click(closeButton);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith(metrikaParams);
});

it('должен отрисовать кнопку "Подтвердить номер" и отправить метрику при клике', () => {
    const metrikaParams = [ 'add-form-lite', 'add_with_operator', 'add_number' ];
    const metrikaSuccessParams = [ 'add-form-lite', 'add_with_operator', 'add_number', 'submit', 'success' ];
    render(<Container/>);

    userEvent.click(screen.getByText(/Разместить с оператором/i));
    userEvent.click(screen.getByText(/Подтвердить номер/i));

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(4);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(3, metrikaParams);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(4, metrikaSuccessParams);
});

it('должен отрисовать кнопку "Свяжитесь со мной" и отправить метрику при клике', async() => {
    const metrikaParams = [ 'add-form-lite', 'add_with_operator', 'send_draft' ];
    render(<Container/>);

    userEvent.click(screen.getByText(/Разместить с оператором/i));
    userEvent.click(screen.getByText(/Подтвердить номер/i));
    userEvent.click(screen.getByText(/Свяжитесь со мной/i));

    await waitFor(() => screen.getByText(/Хорошо/i));

    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith(metrikaParams);
});
