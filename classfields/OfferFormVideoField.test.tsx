import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import gateApi from 'auto-core/react/lib/gateApi';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import type { TOfferImage } from 'auto-core/types/proto/auto/api/api_offer_model';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormVideoField,
{
    INVALID_LINK_ERROR,
    ADD_BUTTON_TEXT,
    REMOVE_BUTTON_TEXT,
    PLACEHOLDER,
}
    from './OfferFormVideoField';

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const videoId = '9fGYGSfQVJg';
const validYoutubeUrl = `//www.youtube.com/watch?v=${ videoId }`;

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const errorPromise = Promise.resolve({
    error: 'Не могу распознать провайдера видео',
    result: false,
});

const validFieldValue = {
    id: videoId,
    url: `//www.youtube.com/embed/${ videoId }`,
    userPreviewLink: `www.youtube.com/embed/${ videoId }`,
    previews: { small: `//img.youtube.com/vi/${ videoId }/default.jpg` },
};

const successPromise = Promise.resolve({
    response: {
        result: {
            embed_url: `//www.youtube.com/embed/${ videoId }`,
            status: null,
            thumbs: {
                '120x90': `//img.youtube.com/vi/${ videoId }/default.jpg`,
            },
        },
    },
});

const defaultInitialValues = {
    [OfferFormFieldNames.PHOTOS]: offerMock.state?.image_urls as unknown as [TOfferImage],
};

it('не показываем, когда нет фоточек', async() => {
    const { queryByText } = await renderComponent(<OfferFormVideoField/>);

    const submitButton = queryByText('ADD_BUTTON_TEXT');
    expect(submitButton).toBeNull();
});

it('дизэйблим кнопку Добавить, когда ничего не ввели в инпут', async() => {
    await renderComponent(<OfferFormVideoField/>, { initialValues: defaultInitialValues });

    const input = await screen.getByRole('textbox') as HTMLInputElement;
    const addButton = await screen.getByRole('button', { name: ADD_BUTTON_TEXT });

    expect(addButton.className).toContain('Button_disabled');

    userEvent.type(input, validYoutubeUrl);

    expect(addButton.className).not.toContain('Button_disabled');
});

it('если ввели невалидную ссылку, то показываем ошибку и отправляем метрики с ошибкой, состояние компонента не меняем', async() => {
    getResource.mockImplementation(() => errorPromise);
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    await renderComponent(<OfferFormVideoField/>, { formApi, initialValues: defaultInitialValues });
    await errorPromise;

    const input = await screen.getByRole('textbox') as HTMLInputElement;
    const addButton = await screen.getByRole('button', { name: ADD_BUTTON_TEXT });

    userEvent.type(input, 'www.invalidlink.com');
    userEvent.click(addButton);

    const error = await screen.queryByText(INVALID_LINK_ERROR);
    expect(error).toBeInTheDocument();

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.VIDEO)).toEqual(undefined);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: `${ OfferFormFieldNames.VIDEO }_add`, event: 'error' });
});

it('если ввели невалидную ссылку, при повторном вводе в инпут значения, ошибку скрываем, до клика на кнопку Добавить', async() => {
    getResource.mockImplementation(() => errorPromise);
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    await renderComponent(<OfferFormVideoField/>, { formApi, initialValues: defaultInitialValues });
    await errorPromise;

    const input = await screen.getByRole('textbox') as HTMLInputElement;
    const addButton = await screen.getByRole('button', { name: ADD_BUTTON_TEXT });
    //ищем через querySelector, потому что placeholder у инпута есть всегда, просто меняет свое значение
    // было бы некорретно искать через queryByText
    const placeholder = document.querySelector('.TextInput__placeholder');

    userEvent.type(input, 'www.invalidlink.com');
    userEvent.click(addButton);

    expect(placeholder?.innerHTML).toBe(INVALID_LINK_ERROR);

    userEvent.type(input, 'www.invalidlink2.com');

    expect(placeholder?.innerHTML).toBe(PLACEHOLDER);

});

it('если ввели валидную ссылку, то отправляем успешные метрики и показываем превью-картинку с кнопкой удалить', async() => {
    getResource.mockImplementation(() => successPromise);
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    await renderComponent(<OfferFormVideoField/>, { formApi, initialValues: defaultInitialValues });
    await successPromise;

    const input = screen.getByRole('textbox') as HTMLInputElement;
    const addButton = screen.getByRole('button', { name: ADD_BUTTON_TEXT });

    userEvent.type(input, validYoutubeUrl);

    userEvent.click(addButton);

    await waitFor(() => {
        const image = screen.queryByRole('img');
        expect(image).toBeInTheDocument();

    });
    const removeButton = screen.queryByRole('button', { name: REMOVE_BUTTON_TEXT });

    expect(removeButton).toBeInTheDocument();

    const error = await screen.queryByText(INVALID_LINK_ERROR);

    expect(error).not.toBeInTheDocument();
    expect(input).not.toBeInTheDocument();
    expect(addButton).not.toBeInTheDocument();

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.VIDEO)).toEqual(validFieldValue);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: `${ OfferFormFieldNames.VIDEO }_add`, event: 'success' });
});

it('при нажатии на кнопку Удалить, в форме значение поля станет пустым и мы показываем первоначальное состояние', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const initialValues = {
        ...defaultInitialValues,
        [OfferFormFieldNames.VIDEO]: {
            ...validFieldValue,
        },
    };

    await renderComponent(<OfferFormVideoField/>, { formApi, initialValues });

    const removeButton = screen.getByRole('button', { name: REMOVE_BUTTON_TEXT });

    userEvent.click(removeButton);

    const input = screen.queryByRole('textbox') as HTMLInputElement;
    const addButton = screen.queryByRole('button', { name: ADD_BUTTON_TEXT });

    expect(input).toBeInTheDocument();
    expect(input.value).toBe('');
    expect(addButton).toBeInTheDocument();

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.VIDEO)).toEqual(null);
});
