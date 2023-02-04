import React, { createRef } from 'react';
import { screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import descSuggestions from 'www-poffer/data/dicts/desc_suggestions_new.json';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { AppState } from 'www-poffer/react/store/AppState';

import OfferFormDescriptionField from './OfferFormDescriptionField';

type State = Partial<AppState>

let state: State;

beforeEach(() => {
    state = {
        equipmentDictionary: equipmentDictionaryMock,
        offerDraft: offerDraftMock.value(),
        parsedOptions: parsedOptionsMock.value(),
        catalogOptions: catalogOptionsMock.value(),
    };
});

it('показывает саджесты на фокусе в textarea', async() => {
    await renderComponent(<OfferFormDescriptionField/>, { state });

    const textarea = screen.getByRole<HTMLTextAreaElement>('textbox');
    let suggests = screen.queryAllByRole<HTMLDivElement>('button');

    userEvent.type(textarea, '{arrowleft}');

    suggests = screen.getAllByRole<HTMLDivElement>('button');
    expect(suggests).not.toHaveLength(0);
});

it('добавляет в textarea текст саджеста при клике на него', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.DESCRIPTION]: 'Кусочек текста в начале',
    };
    await renderComponent(<OfferFormDescriptionField/>, { state, formApi, initialValues });

    const textarea = screen.getByRole<HTMLTextAreaElement>('textbox');
    userEvent.type(textarea, '{arrowleft}');
    const suggest = screen.getByText<HTMLDivElement>('Комплект зимних шин в подарок');

    await act(async() => {
        userEvent.click(suggest);
    });

    expect(textarea.value).toEqual('Кусочек текста в начале Комплект зимних шин в подарок.');
});

it('удаляет из списка саджестов уже добавленные в текст', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.DESCRIPTION]: `${ descSuggestions[0].text }Кусочек текста в начале`,
    };
    await renderComponent(<OfferFormDescriptionField/>, { state, formApi, initialValues });

    const textarea = screen.getByRole<HTMLTextAreaElement>('textbox');
    userEvent.type(textarea, '{arrowleft}');
    let suggest: HTMLDivElement | null = screen.getByText<HTMLDivElement>('Комплект зимних шин в подарок');

    await act(async() => {
        userEvent.click(suggest!);
    });

    suggest = screen.queryByText<HTMLDivElement>('Комплект зимних шин в подарок');

    expect(suggest).toBeNull();
});

it('на блуре кладет в лог инфу об ошибке, если есть ошибка', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.DESCRIPTION]: `Кусочек текста в начале`.repeat(2000),
    };
    await renderComponent(<OfferFormDescriptionField/>, { state, formApi, initialValues });

    const textarea = screen.getByRole<HTMLTextAreaElement>('textbox');

    userEvent.type(textarea, '{arrowleft}');
    userEvent.tab();
    await flushPromises();

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.DESCRIPTION, event: 'error' });
});

it('на блуре кладет в лог инфу об успехе, если данные изменены', async() => {
    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        [OfferFormFieldNames.DESCRIPTION]: `Кусочек текста в начале`,
    };
    await renderComponent(<OfferFormDescriptionField/>, { state, formApi, initialValues });

    const textarea = screen.getByRole<HTMLTextAreaElement>('textbox');

    userEvent.type(textarea, '{arrowleft}');
    userEvent.tab();

    expect(offerFormPageContextMock.sendFormLog).not.toHaveBeenCalled();
    userEvent.type(textarea, textarea.value);
    userEvent.tab();

    await flushPromises();

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: OfferFormFieldNames.DESCRIPTION, event: 'success' });
});
