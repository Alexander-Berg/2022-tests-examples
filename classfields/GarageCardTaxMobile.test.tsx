import React from 'react';
import { Provider } from 'react-redux';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import type { Tax } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { BlockState_Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import fillFormFieldsByCard from 'auto-core/react/dataDomain/garageCard/actions/fillFormFieldsByCard';
import updateCardWithPayload from 'auto-core/react/dataDomain/garageCard/actions/updateCardWithPayload';
import clearFormFields from 'auto-core/react/dataDomain/formFields/actions/clearFormFields';

import mockGarageCard from 'auto-core/server/resources/publicApiGarage/methods/getCard.fixtures';

import GarageCardTaxMobile from './GarageCardTaxMobile';

jest.mock('auto-core/react/dataDomain/garageCard/actions/fillFormFieldsByCard', () => jest.fn());
jest.mock('auto-core/react/dataDomain/garageCard/actions/updateCardWithPayload', () => jest.fn());
jest.mock('auto-core/react/dataDomain/formFields/actions/clearFormFields', () => jest.fn());

const Context = createContextProvider(contextMock);

const fillFormFieldsByCardMocked = fillFormFieldsByCard as jest.MockedFunction<typeof fillFormFieldsByCard>;
const updateCardWithPayloadMocked = updateCardWithPayload as jest.MockedFunction<typeof updateCardWithPayload>;
const clearFormFieldsMocked = clearFormFields as jest.MockedFunction<typeof clearFormFields>;

clearFormFieldsMocked.mockImplementation(() => ({ type: 'CLEAR_FORM_FIELDS' }));

type Params = { taxStatus?: BlockState_Status; tax?: number | null };

const TAX = 1688;
const DEFAULT_ARGS = { taxStatus: BlockState_Status.CAN_BE_CLARIFIED };

const renderComponent = ({ taxStatus = BlockState_Status.CAN_BE_CLARIFIED }: Params = DEFAULT_ARGS) => {
    const card = mockGarageCard.response200WithPriceStats().card;
    card.tax = {
        tax: TAX,
        year: 2022,
        block_state: {
            status: taxStatus,
        },
    } as Tax;

    const store = mockStore({
        carsTechOptions: {
            data: {
                body_type: [ 'SEDAN' ],
                year: [ 1996, 1995, 1994, 1993 ],
                engine_type: [ 'GASOLINE', 'DIESEL' ],
                gear_type: [ 'FORWARD_CONTROL', 'ALL_WHEEL_DRIVE' ],
                transmission_full: [ 'MECHANICAL', 'AUTOMATIC' ],
                tech_param: [
                    {
                        id: '20388931',
                        tech_params: {
                            engine_type: 'GASOLINE',
                            displacement: 1595,
                            gear_type: 'FORWARD_CONTROL',
                            transmission: 'MECHANICAL',
                            power: 101,
                            power_kvt: '74.0',
                            year_start: 1993,
                            year_stop: 1994,
                        },
                        no_listing: false,
                        configuration_id: '7878116',
                    },
                    {
                        id: '7879217',
                        tech_params: {
                            engine_type: 'GASOLINE',
                            displacement: 2771,
                            gear_type: 'FORWARD_CONTROL',
                            transmission: 'AUTOMATIC',
                            power: 174,
                            power_kvt: '128.0',
                            year_start: 1991,
                            year_stop: 1994,
                        },
                        no_listing: false,
                        configuration_id: '7878116',
                    },
                ],
            },
        },
    });

    return render(
        <Context>
            <Provider store={ store }>
                <GarageCardTaxMobile garageCard={ card }/>
            </Provider>
        </Context>,
    );
};

beforeEach(() => {
    jest.clearAllMocks();
});

it('должен запросить данные при клике на модификацию', async() => {
    fillFormFieldsByCardMocked.mockImplementation(() => jest.fn().mockResolvedValue({}));
    const { queryByRole } = await renderComponent();

    expect(fillFormFieldsByCardMocked).toHaveBeenCalledTimes(0);
    const link = queryByRole('button', { name: /дизель/i });
    link && userEvent.click(link);
    expect(fillFormFieldsByCardMocked).toHaveBeenCalledTimes(1);
});

it('должен вызвать экшн при сохранении модификации', async() => {
    fillFormFieldsByCardMocked.mockImplementation(() => jest.fn().mockResolvedValue({}));
    updateCardWithPayloadMocked.mockImplementation(() => jest.fn().mockResolvedValue({}));

    const { queryByRole } = await renderComponent();

    // 1. открываем попап
    const link = queryByRole('button', { name: /дизель/i });
    link && userEvent.click(link);

    expect(updateCardWithPayloadMocked).toHaveBeenCalledTimes(0);

    // 2. тыкаем во вторую модификацию
    const item = screen.getAllByText(/1.6/)[1];
    item && userEvent.click(item);

    expect(updateCardWithPayloadMocked).toHaveBeenCalledTimes(1);
});

it('должен открыть регионовыбиралку', async() => {
    const { queryByRole } = await renderComponent();

    // 1. открываем попап
    const link = queryByRole('button', { name: /регион учёта/i });
    link && userEvent.click(link);

    // 2. смотрим на попап
    const popup = screen.getByRole('dialog');
    expect(popup).toBeInTheDocument();
    expect(popup.classList.contains('FiltersSuggestPopup')).toBe(true);
});

it('при закрытии попапа должен сбросить поля формы', async() => {
    const { queryByRole } = await renderComponent();
    fillFormFieldsByCardMocked.mockImplementation(() => jest.fn().mockResolvedValue({}));

    // 1. открываем попап
    const link = queryByRole('button', { name: /дизель/i });
    link && userEvent.click(link);

    // 2. закрываем попап
    const popup = screen.getByRole('dialog');
    const closer = popup.getElementsByClassName('Modal__closer')[0];

    expect(clearFormFieldsMocked).toHaveBeenCalledTimes(0);
    userEvent.click(closer);
    expect(clearFormFieldsMocked).toHaveBeenCalledTimes(1);
});

it('если статус равен CAN_BE_CLARIFIED, то рендерит компонент', async() => {
    await renderComponent();

    const element = document.getElementById('block-tax');

    expect(element).not.toBeNull();
});

it('если статус равен NOT_ENOUGH_DATA, то не рендерит компонент', async() => {
    await renderComponent({ taxStatus: BlockState_Status.NOT_ENOUGH_DATA });

    const element = document.getElementById('block-tax');

    expect(element).toBeNull();
});
