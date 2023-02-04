import React from 'react';
import _ from 'lodash';
import userEvent from '@testing-library/user-event';
import { screen } from '@testing-library/react';

import '@testing-library/jest-dom';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { AppState } from 'www-poffer/react/store/AppState';

import type { Props } from './OfferFormBadgesField';
import OfferFormBadgesField, { MAX_DISPLAYED_BADGES_NUM } from './OfferFormBadgesField';

type State = Partial<AppState>

let defaultState: State;
let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        dict: [ 'Срочно', 'Один владелец', 'Возможен обмен', 'На гарантии ', 'Автозапуск' ],
    };
    defaultState = {
        offerDraft: offerDraftMock.value(),
    };

    (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();
});

it('при клике выбирает бейдж и отправляет лог', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { findByText } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, state: defaultState });

    const item = await findByText(/Срочно/i);
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.BADGES);
    expect(value).toEqual([ 'Срочно' ]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.BADGES, event: 'click', level_6: 'pre_made' });
});

it('при клике не позволяет выбирать бейдж, если уже выбрали макс кол-во', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.BADGES]: [ 'Один владелец', 'Возможен обмен', 'На гарантии ' ],
    };
    const { findByText } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, initialValues, state: defaultState });

    const item = await findByText(/Срочно/i);
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.BADGES);
    expect(value).toEqual([ 'Один владелец', 'Возможен обмен', 'На гарантии ' ]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
});

it('делеселектит бейдж если он был выбран ранее', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.BADGES]: [ 'Срочно' ],
    };
    const { findByText } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, initialValues, state: defaultState });

    const item = await findByText(/Срочно/i);
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.BADGES);
    expect(value).toEqual([ ]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.BADGES, event: 'deselect', level_6: 'pre_made' });
});

it('должен отрисовать кол-во бейджей из словаря равное MAX_DISPLAYED_BADGES_NUM', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const props = {
        dict: _.range(0, MAX_DISPLAYED_BADGES_NUM + 10).map((num) => `badge #${ num }`),
    };
    const { findAllByRole } = await renderComponent(<OfferFormBadgesField { ...props }/>, { formApi, state: defaultState });

    const items = await findAllByRole('listitem');

    expect(items).toHaveLength(MAX_DISPLAYED_BADGES_NUM + 1); // + 1 кнопка "добавить свой"
});

it('если ранее был выбран кастомный бейдж, добавит его к списку', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.BADGES]: [ 'badge #1', 'Супер-тачка!' ],
    };
    const props = {
        dict: _.range(0, MAX_DISPLAYED_BADGES_NUM).map((num) => `badge #${ num }`),
    };
    const { findAllByRole } = await renderComponent(<OfferFormBadgesField { ...props }/>, { formApi, initialValues, state: defaultState });

    const items = await findAllByRole('listitem');

    expect(items).toHaveLength(MAX_DISPLAYED_BADGES_NUM + 2); // + 2 кастомный бейдж и кнопка "добавить свой"
    expect(items[items.length - 2]?.textContent).toBe('Супер-тачка!');
});

it('должен удалять свой бейдж при клике на "х" и отправить лог', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const initialValues = {
        [OfferFormFieldNames.BADGES]: [ 'my badge' ],
    };

    const { container } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, initialValues, state: defaultState });

    const removeButton = container.querySelector('.OfferFormBadgesField__removeBadgeIcon');

    removeButton && userEvent.click(removeButton);

    const items = screen.getAllByRole('listitem');

    expect(items).toHaveLength(defaultProps.dict.length + 1); // + 1  это кнопка "добавить свой"

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.BADGES, event: 'deselect', level_6: 'custom' });
});

it('должен добавить свой бейдж при клике на кнопку "Добавить" и отправить лог', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { container, findByText } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, state: defaultState });
    const CUSTOM_BADGE = 'my badge';

    const item = container.querySelector('.OfferFormBadgesField__button');

    item && userEvent.click(item);

    const input = screen.getByRole('textbox');
    const button = await findByText('Добавить');

    userEvent.type(input, CUSTOM_BADGE);

    userEvent.click(button);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.BADGES);
    expect(value).toEqual([ CUSTOM_BADGE ]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.BADGES, event: 'add', level_6: 'custom' });
});

it('должен добавить свой бейдж при нажатии на Enter', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { container } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, state: defaultState });
    const CUSTOM_BADGE = 'my badge';

    const item = container.querySelector('.OfferFormBadgesField__button');

    item && userEvent.click(item);

    const input = screen.getByRole('textbox');

    userEvent.type(input, CUSTOM_BADGE);
    userEvent.keyboard('{Enter}');

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.BADGES);
    expect(value).toEqual([ CUSTOM_BADGE ]);
});

it('должен НЕ показывать инпут, если уже выбрали макс кол-во', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.BADGES]: [ 'Один владелец', 'Возможен обмен', 'На гарантии ' ],
    };
    const { container } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, initialValues, state: defaultState });

    const item = container.querySelector('.OfferFormBadgesField__button');

    item && userEvent.click(item);

    const input = screen.queryByRole('textbox');

    expect(input).toBeNull();
});

it('должен дезейблить инпут и кнопку, если уже выбрали макс кол-во', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.BADGES]: [ 'Один владелец', 'Возможен обмен' ],
    };

    const { container } = await renderComponent(<OfferFormBadgesField { ...defaultProps }/>, { formApi, initialValues, state: defaultState });
    const CUSTOM_BADGE = 'my badge';

    const item = container.querySelector('.OfferFormBadgesField__button');

    item && userEvent.click(item);

    const input = screen.getByRole('textbox');
    const button = screen.getByRole('button');

    userEvent.type(input, CUSTOM_BADGE);
    userEvent.click(button);

    expect(input).toHaveAttribute('disabled');
    expect(button).toHaveAttribute('disabled');
});
