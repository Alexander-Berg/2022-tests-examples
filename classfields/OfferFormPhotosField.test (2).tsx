import React from 'react';

import '@testing-library/jest-dom';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

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
        <OfferFormPhotosField/>, { state: defaultState, storeMock },
    );

    const offerWithRecognizedPlate = offerDraftMock
        .withOfferMock(cloneOfferWithHelpers(offerMock).withRecognizedLicensePlate()).value();

    (storeMock.getState as jest.MockedFunction<typeof storeMock.getState>).mockImplementation(() => ({
        ...defaultState,
        offerDraft: offerWithRecognizedPlate,
    }));

    await rerender(
        <OfferFormPhotosField/>,
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
        const { queryByText } = await renderComponent(<OfferFormPhotosField/>, { state, initialValues });
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
        const { queryByText } = await renderComponent(<OfferFormPhotosField/>, { state, initialValues });
        const warning = await queryByText(/Мы не распознали госномер на фото/i);
        expect(warning).toBeNull();
    });
});
