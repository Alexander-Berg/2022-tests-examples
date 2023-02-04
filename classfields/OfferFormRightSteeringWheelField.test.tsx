jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { Car_SteeringWheel } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import catalogSuggestStateMock from 'auto-core/react/dataDomain/catalogSuggest/mock';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import catalogSuggestMocks from 'auto-core/models/catalogSuggest/mocks';

import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormRightSteeringWheelField from './OfferFormRightSteeringWheelField';

let defaultState: Partial<AppState>;

beforeEach(() => {
    defaultState = {
        catalogSuggest: catalogSuggestStateMock.value(),
    };
});

it('в дефолтном состоянии чекбокс будет отжат', async() => {
    const { findByLabelText } = await renderComponent(<OfferFormRightSteeringWheelField/>, { state: defaultState });
    const checkbox = await findByLabelText('Правый руль') as HTMLInputElement;

    expect(checkbox.checked).toBe(false);
});

it('выберет правый руль, если он один и еще не выбран', async() => {
    const state = {
        ...defaultState,
        catalogSuggest: catalogSuggestStateMock.withData(
            catalogSuggestMocks.withSteeringWheel([ Car_SteeringWheel.RIGHT ]).value(),
        ).value(),
    };
    const { findByLabelText } = await renderComponent(<OfferFormRightSteeringWheelField/>, { state });
    const checkbox = await findByLabelText('Правый руль') as HTMLInputElement;

    expect(checkbox.checked).toBe(true);
    expect(checkbox.disabled).toBe(true);
});

describe('видимость', () => {
    it('скроет чекбокс, если правый руль не доступен', async() => {
        await renderComponent(<OfferFormRightSteeringWheelField/>, { state: defaultState });
        const field = document.querySelector('.OfferFormRightSteeringWheelField');

        expect(field?.className).toContain('OfferFormRightSteeringWheelField_hidden');
    });

    it('покажет, если правый руль был выбран ранее, но сейчас он не доступен', async() => {
        const initialValues = {
            [FieldNames.RIGHT_STEERING_WHEEL]: true,
        };
        await renderComponent(<OfferFormRightSteeringWheelField/>, { state: defaultState, initialValues });
        const field = document.querySelector('.OfferFormRightSteeringWheelField');

        expect(field?.className).not.toContain('OfferFormRightSteeringWheelField_hidden');
    });

    it('покажет, если нет информации у рулях в каталоге', async() => {
        const state = {
            ...defaultState,
            catalogSuggest: catalogSuggestStateMock.withData(
                catalogSuggestMocks.withSteeringWheel([]).value(),
            ).value(),
        };
        await renderComponent(<OfferFormRightSteeringWheelField/>, { state });
        const field = document.querySelector('.OfferFormRightSteeringWheelField');

        expect(field?.className).not.toContain('OfferFormRightSteeringWheelField_hidden');
    });

    it('покажет, если доступны оба варианта рулей', async() => {
        const state = {
            ...defaultState,
            catalogSuggest: catalogSuggestStateMock.withData(
                catalogSuggestMocks.withSteeringWheel([ Car_SteeringWheel.LEFT, Car_SteeringWheel.RIGHT ]).value(),
            ).value(),
        };
        await renderComponent(<OfferFormRightSteeringWheelField/>, { state });
        const field = document.querySelector('.OfferFormRightSteeringWheelField');

        expect(field?.className).not.toContain('OfferFormRightSteeringWheelField_hidden');
    });
});

it('отправит метрику при нажатии на чекбокс', async() => {
    const { findByLabelText } = await renderComponent(<OfferFormRightSteeringWheelField/>, { state: defaultState });
    const checkbox = await findByLabelText('Правый руль') as HTMLInputElement;

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ event: 'click', field: FieldNames.RIGHT_STEERING_WHEEL });
});
