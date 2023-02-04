import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/search/search_model';
import { TradeInType } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import mockStore from 'autoru-frontend/mocks/mockStore';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import configMock from 'auto-core/react/dataDomain/config/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

import type { Props } from './OfferFormTradeInField';
import OfferFormTradeInField from './OfferFormTradeInField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        predictPrice: {
            tradein_dealer_matrix_new: { from: 1249000, to: 1365000, currency: Currency.RUR, orig_price: 1500000 },
            tradein_dealer_matrix_used: { from: 1279000, to: 1395000, currency: Currency.RUR, orig_price: 1500000 },
            tradein_dealer_matrix_buyout: { from: 1339000, to: 1456000, currency: Currency.RUR, orig_price: 1500000 },
        },
        isVisible: true,
        isInitiallyFetched: true,
    };
});

it('выбирает / сбрасывает значение в списке', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { findAllByRole, findByLabelText } = await renderComponent(<OfferFormTradeInField { ...defaultProps }/>, { formApi });

    const checkboxes = await findAllByRole('checkbox') as Array<HTMLInputElement>;
    expect(checkboxes.map(({ checked }) => checked)).toEqual([ false, false, false ]);

    // выбираем первое значение
    const forNewCheckbox = await findByLabelText(/при покупке нового автомобиля/i) as HTMLInputElement;
    userEvent.click(forNewCheckbox);
    let formValue = formApi.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

    expect(forNewCheckbox.checked).toBe(true);
    expect(formValue).toEqual({
        trade_in_price_range: {
            currency: 'RUR',
            from: '1249000',
            to: '1365000',
        },
        trade_in_type: TradeInType.FOR_NEW,
    });
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1,
        { field: OfferFormFieldNames.TRADE_IN + '_option', event: 'click', level_6: TradeInType.FOR_NEW });

    // меняем на второе
    const forUsedCheckbox = await findByLabelText(/при покупке автомобиля с пробегом/i) as HTMLInputElement;
    userEvent.click(forUsedCheckbox);
    formValue = formApi.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

    expect(forNewCheckbox.checked).toBe(false);
    expect(forUsedCheckbox.checked).toBe(true);
    expect(formValue).toEqual({
        trade_in_price_range: {
            currency: 'RUR',
            from: '1279000',
            to: '1395000',
        },
        trade_in_type: TradeInType.FOR_USED,
    });
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2,
        { field: OfferFormFieldNames.TRADE_IN + '_option', event: 'click', level_6: TradeInType.FOR_USED });

    // кликаем опять на второе, проверяем что все сбросилось
    userEvent.click(forUsedCheckbox);
    formValue = formApi.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

    expect(forUsedCheckbox.checked).toBe(false);
    expect(formValue).toBe(null);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
});

it('показывает дисклеймер когда выбрана одна из опций', async() => {
    const { findByLabelText, queryByLabelText } = await renderComponent(<OfferFormTradeInField { ...defaultProps }/>);

    let disclaimer = await queryByLabelText(/получать предложения по выкупу/i) as HTMLInputElement;
    expect(disclaimer).toBe(null);

    const forNewCheckbox = await findByLabelText(/при покупке нового автомобиля/i) as HTMLInputElement;
    userEvent.click(forNewCheckbox);

    disclaimer = await queryByLabelText(/получать предложения по выкупу/i) as HTMLInputElement;
    expect(disclaimer).not.toBe(null);
    expect(disclaimer.checked).toBe(true);
});

it('сразу покажет дисклеймер если в форме уже было что-то предзаполнено', async() => {
    const initialValues = {
        [OfferFormFieldNames.TRADE_IN]: {
            trade_in_price_range: {
                currency: 'RUR',
                from: '1279000',
                to: '1395000',
            },
            trade_in_type: TradeInType.FOR_USED,
        },
    };
    const { queryByLabelText } = await renderComponent(<OfferFormTradeInField { ...defaultProps }/>, { initialValues });

    const disclaimer = await queryByLabelText(/получать предложения по выкупу/i) as HTMLInputElement;
    expect(disclaimer).not.toBe(null);
    expect(disclaimer.checked).toBe(true);
});

it('дизейблит блок и сбрасывает значение если отжали галку дисклеймера', async() => {
    const { findAllByRole, findByLabelText, queryByLabelText } = await renderComponent(<OfferFormTradeInField { ...defaultProps }/>);

    let checkboxes = await findAllByRole('checkbox') as Array<HTMLInputElement>;
    expect(checkboxes.map(({ disabled }) => disabled)).toEqual([ false, false, false ]);

    const forNewCheckbox = await findByLabelText(/при покупке нового автомобиля/i) as HTMLInputElement;
    userEvent.click(forNewCheckbox);

    const disclaimer = await queryByLabelText(/получать предложения по выкупу/i) as HTMLInputElement;
    userEvent.click(disclaimer);

    checkboxes = await findAllByRole('checkbox') as Array<HTMLInputElement>;
    expect(checkboxes.map(({ disabled }) => disabled)).toEqual([ true, true, true, false ]);

    expect(forNewCheckbox.checked).toBe(false);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { field: OfferFormFieldNames.TRADE_IN + '_disclaimer', event: 'click' });
});

it('когда блок скрывается, сбрасывает значение в форме и прячет дисклеймер', async() => {
    const initialValues = {
        [OfferFormFieldNames.TRADE_IN]: {
            trade_in_price_range: {
                currency: 'RUR',
                from: '1279000',
                to: '1395000',
            },
            trade_in_type: TradeInType.FOR_USED,
        },
    };
    const state = {
        offerDraft: offerDraftMock.value(),
        config: configMock.value(),
    };
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const storeMock = mockStore(state);

    const { findByLabelText, queryByLabelText, rerender } = await renderComponent(
        <OfferFormTradeInField { ...defaultProps }/>,
        { initialValues, storeMock, formApi },
    );

    await rerender(<OfferFormTradeInField { ...defaultProps } isVisible={ false }/>, { initialValues, storeMock, formApi });

    const forUsedCheckbox = await findByLabelText(/при покупке автомобиля с пробегом/i) as HTMLInputElement;
    const disclaimer = await queryByLabelText(/получать предложения по выкупу/i) as HTMLInputElement;
    const formValue = formApi.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

    expect(forUsedCheckbox.checked).toBe(false);
    expect(disclaimer).toBe(null);
    expect(formValue).toBe(null);
});

describe('prop isInitiallyFetched', () => {
    const initialValues = {
        [OfferFormFieldNames.TRADE_IN]: {
            trade_in_price_range: {
                currency: 'RUR',
                from: '1279000',
                to: '1395000',
            },
            trade_in_type: TradeInType.FOR_USED,
        },
    };
    const state = {
        offerDraft: offerDraftMock.value(),
        config: configMock.value(),
    };
    const storeMock = mockStore(state);

    it('если уже запросили обе ручки и по результатам блок не должен показываться, сбросит значение в форме', async() => {
        const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

        const { findByLabelText, rerender } = await renderComponent(
            <OfferFormTradeInField { ...defaultProps } isInitiallyFetched={ false } isVisible={ false }/>,
            { initialValues, storeMock, formApi },
        );

        await rerender(<OfferFormTradeInField { ...defaultProps } isInitiallyFetched={ true } isVisible={ false }/>, { initialValues, storeMock, formApi });

        const forUsedCheckbox = await findByLabelText(/при покупке автомобиля с пробегом/i) as HTMLInputElement;
        const formValue = formApi.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

        expect(forUsedCheckbox.checked).toBe(false);
        expect(formValue).toBe(null);
    });

    it('если еще не запросили обе ручки и блок пока не должен показываться, не будет сбрасывать значения в форме', async() => {
        const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

        const { findByLabelText, rerender } = await renderComponent(
            <OfferFormTradeInField { ...defaultProps } isInitiallyFetched={ false } isVisible={ false }/>,
            { initialValues, storeMock, formApi },
        );

        await rerender(<OfferFormTradeInField { ...defaultProps } isInitiallyFetched={ false } isVisible={ false }/>, { initialValues, storeMock, formApi });

        const forUsedCheckbox = await findByLabelText(/при покупке автомобиля с пробегом/i) as HTMLInputElement;
        const formValue = formApi.current?.getFieldValue(OfferFormFieldNames.TRADE_IN);

        expect(forUsedCheckbox.checked).toBe(true);
        expect(formValue).toBe(initialValues[OfferFormFieldNames.TRADE_IN]);
    });
});
