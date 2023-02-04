import React from 'react';
import _ from 'lodash';
import userEvent from '@testing-library/user-event';
import { fireEvent } from '@testing-library/react';
import flushPromises from 'jest/unit/flushPromises';

import '@testing-library/jest-dom';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import mockStore from 'autoru-frontend/mocks/mockStore';
import carImage from 'autoru-frontend/mockData/ferrari_832x624.jpg';
import carImage320 from 'autoru-frontend/mockData/mustang_320x240.jpg';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import type { TOfferImage } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { OfferFormFieldNamesType, OfferFormFields } from 'www-poffer/react/types/offerForm';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import OfferFormPhotosField from './OfferFormPhotosField';

const defaultOfferDraft = offerDraftMock.value();

const defaultState = {
    config: configStateMock.withPageParams({ form_type: 'add' }).value(),
    offerDraft: defaultOfferDraft,
};
const offerWithRecognizedPlate = offerDraftMock
    .withOfferMock(
        cloneOfferWithHelpers(offerMock)
            .withRecognizedLicensePlate('')
            .withImages([ { name: 'kotya' } ]),
    ).value();

let originalCreateObjectUrl: any;

beforeAll(() => {
    originalCreateObjectUrl = global.URL.createObjectURL;
    global.URL.createObjectURL = jest.fn(() => 'image-preview');
});

afterAll(() => {
    jest.restoreAllMocks();
    global.URL.createObjectURL = originalCreateObjectUrl;
});

it('отправляем метрику при загрузке блока на странице, если прислали recognized_license_plate', async() => {

    const storeMock = mockStore(defaultState);

    const { rerender } = await renderComponent(
        <OfferFormPhotosField
            scrollToEndSection={ _.noop }
        />, { state: defaultState, storeMock },
    );

    const offerWithRecognizedPlate = offerDraftMock
        .withOfferMock(cloneOfferWithHelpers(offerMock).withRecognizedLicensePlate()).value();

    (storeMock.getState as jest.MockedFunction<typeof storeMock.getState>).mockImplementation(() => ({
        ...defaultState,
        offerDraft: offerWithRecognizedPlate,
    }));

    await rerender(
        <OfferFormPhotosField
            scrollToEndSection={ _.noop }
        />,
        { state: defaultState, storeMock });

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});

describe('предупреждение об отсутствии ГРЗ', () => {
    it('покажется, если бэк прислал флаг и есть загруженные фотки', async() => {
        const state = {
            ...defaultState,
            offerDraft: offerWithRecognizedPlate,
        };
        const initialValues = {
            [OfferFormFieldNames.PHOTOS]: [
                { sizes: { small: 'kot' }, name: 'kot' },
            ],
        };
        const { queryByText } = await renderComponent(<OfferFormPhotosField scrollToEndSection={ _.noop }/>, { state, initialValues });
        const warning = await queryByText(/Мы не распознали госномер на фото/i);
        expect(warning).toBeInTheDocument();
    });

    it('не покажется, если бэк прислал флаг, но авто не на учёте', async() => {
        const state = {
            ...defaultState,
            offerDraft: offerWithRecognizedPlate,
        };
        const initialValues = {
            [OfferFormFieldNames.NOT_REGISTERED_IN_RUSSIA]: true,
            [OfferFormFieldNames.PHOTOS]: [
                { sizes: { small: 'kot' }, name: 'kot' },
            ],
        };
        const { queryByText } = await renderComponent(<OfferFormPhotosField scrollToEndSection={ _.noop }/>, { state, initialValues });
        const warning = await queryByText(/Мы не распознали госномер на фото/i);
        expect(warning).toBeNull();
    });
});

it('если стоит чекбокс Загрузить фото позже и мы загрузили фото, то сбрасываем чекбокс', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const file = new File([ 'hello' ], 'hello.png', { type: 'image/png' });

    const offerWithPhotos = offerDraftMock
        .withOfferMock(
            cloneOfferWithHelpers(offerMock)
                .withImages([ { sizes: { '320x240': carImage }, name: 'carImage' }, { sizes: { '320x240': carImage320 }, name: 'carImage320' } ])).value();

    const values = {
        [OfferFormFieldNames.PHOTOS]: offerWithPhotos.data.offer.state?.image_urls as Array<TOfferImage>,
        [OfferFormFieldNames.WITHOUT_PHOTOS]: true,
    };

    const { getByRole } = await renderComponent(
        <OfferFormPhotosField
            scrollToEndSection={ _.noop }
        />, { state: defaultState, initialValues: values, formApi },
    );

    const addInput = getByRole('file-input');

    await userEvent.upload(addInput, file);

    const photosContainer = document.querySelector('.MdsPhotosList__sortable');

    if (photosContainer) {
        fireEvent.change(photosContainer);
    }

    await flushPromises();

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.WITHOUT_PHOTOS)).toEqual(false);
});
