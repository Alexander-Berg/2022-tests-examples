jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import VinCheckSnippetMobile from './VinCheckSnippetMobile';

const store = mockStore({
    vinCheckInput: { value: 'Z8NAJL01054212789' }, state: {},
});

const VALID_PARAMS = {
    product: 'REPORTS',
    context_page: 'PAGE_PROAUTO',
    context_block: 'BLOCK_REPORT_PROMO_HORIZONTAL',
    context_service: 'SERVICE_AUTORU',
};

const Context = createContextProvider(contextMock);

it('отправляет метрики на показ для промо-блока с вводом VIN', () => {
    render(
        <Context>
            <Provider store={ store }>
                <VinCheckSnippetMobile
                    showVinHelpModal={ jest.fn() }
                    onInputSubmit={ jest.fn() }
                />
            </Provider>
        </Context>,
    );
    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({ vas_show_event: VALID_PARAMS });
});

it('отправляет метрики на клик по кнопке "Проверить"', () => {
    const { container } = render(
        <Context>
            <Provider store={ store }>
                <VinCheckSnippetMobile
                    showVinHelpModal={ jest.fn() }
                    onInputSubmit={ jest.fn() }
                />
            </Provider>
        </Context>,
    );

    userEvent.click(container.getElementsByClassName('Button')[0]);

    expect(statApi.log).toHaveBeenCalledTimes(1);
    expect(statApi.log).toHaveBeenNthCalledWith(1, {
        vas_click_navig_event: {
            ...VALID_PARAMS,
            vin_or_gosnumber: 'Z8NAJL01054212789',
        },
    });
});
