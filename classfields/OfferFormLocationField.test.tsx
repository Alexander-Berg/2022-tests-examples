import React, { createRef } from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import _ from 'lodash';

import MockInputComponentGenerator, { runEventPropCallbackOn } from 'autoru-frontend/jest/unit/MockInputComponentGenerator';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import typeInRichInput from 'autoru-frontend/jest/unit/typeInRichInput';

import sleep from 'auto-core/lib/sleep';

import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import type { FormContext, FormValidationResult } from 'auto-core/react/components/common/Form/types';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import gateApi from 'auto-core/react/lib/gateApi';

import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';

import OfferFormLocationField, { streetPlacholders } from './OfferFormLocationField';

const mockCitySelectItem = {
    id: 2,
    name: 'Токио',
    latitude: '111111',
    longitude: '22222',
};

const mockStreet = {
    geoObjects: {
        get: () => ({
            geometry: {
                getCoordinates: () => ([ 55.800656, 37.540317 ]),
            },
            properties: {
                get: () => ('Васечкина'),
            },
        }),
    },
};

const mockStreetSelectItem = {
    title: { text: 'Московская улица' },
    pos: '37.542824,55.749451',
};

const INITIAL_VALUES = {
    coord: {
        latitude: 3456786,
        longitude: 111111,
    },
    streetName: 'Васечкина-Петрова улица',
    geobaseId: '1',
    cityName: 'Москва',
};
const mockMapLocationPofferLabel = 'MapLocationPoffer';

type HandlerTypes = 'change' | 'select';
const source = 'placemark';

jest.mock('www-poffer/react/components/desktop/MapLocationPoffer/MapLocationPoffer',
    () => MockInputComponentGenerator<HandlerTypes>('MapLocationPoffer'));

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const mockgeocode = jest.fn(() => Promise.resolve(mockStreet));

jest.mock('react-yandex-maps/main/util/api', () => ({
    'default': {
        get: () => Promise.resolve({ geocode: mockgeocode }),
    },
}));

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

describe('тесты для карты', () => {

    it('при изменении положения метки на карте посылает стату', async() => {
        await renderMockComponent();

        const mapLocationPoffer = screen.getByLabelText(mockMapLocationPofferLabel);

        const values = {
            lat: 111111,
            lon: 222222,
            name: 'Петрова-Васечкина улица',
        };

        const params = { source };

        await act(async() => {
            runEventPropCallbackOn({ eventType: 'change', node: mapLocationPoffer, values, params });
        });

        expect(offerFormPageContextMock.sendFormLog)
            .toHaveBeenCalledWith({ field: OfferFormFieldNames.LOCATION + '_map', event: 'click', level_6: 'placemark' });

    });

    it('при выборе местоположения из карты новые значения улицы и координат берутся из переданного value', async() => {
        const { formApi } = await renderMockComponent();

        const mapLocationPoffer = screen.getByLabelText(mockMapLocationPofferLabel);

        const values = {
            lat: 111111,
            lon: 222222,
            name: 'Петрова-Васечкина улица',
        };

        const params = { source };

        const expectValue = {
            ...INITIAL_VALUES,
            coord: {
                latitude: 111111,
                longitude: 222222,
            },
            streetName: 'Петрова-Васечкина улица',
        };

        await act(async() => {
            runEventPropCallbackOn({ eventType: 'change', node: mapLocationPoffer, values, params });
        });

        expect(formApi.current?.getFieldValue(OfferFormFieldNames.LOCATION)).toEqual(expectValue);

    });

});

describe('события на blur города', () => {
    const promise = Promise.resolve([ mockCitySelectItem ]);

    beforeAll(() => {
        getResource.mockImplementation(() => promise);
    });

    it('при блюре из города отсылает успешную стату, если поменяли значения', async() => {

        await renderMockComponent();

        const input = await screen.getByLabelText('Город') as HTMLInputElement;

        await act(async() => {
            await typeInRichInput(input, 'Токио');
        });

        await promise;

        expect(getResource).toHaveBeenCalledTimes(1);

        const item = await screen.getByRole('button', { name: 'Токио' });

        await act(async() => {
            await userEvent.click(item);
            await sleep(200);
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: `${ OfferFormFieldNames.LOCATION }_city`, event: 'success' });
    });

    it('при блюре из города не отсылает стату, если не поменялись значения', async() => {
        await renderMockComponent();
        const input = await screen.getByLabelText('Город') as HTMLInputElement;

        userEvent.type(input, '{arrowleft}');
        userEvent.tab();

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });

    it('если есть ошибка, при блюре отсылает стату с ошибкой', async() => {
        await renderMockComponent();

        const input = await screen.getByLabelText('Город') as HTMLInputElement;

        await act(async() => {
            await userEvent.clear(input);
            await userEvent.tab();
            await sleep(200);
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ field: `${ OfferFormFieldNames.LOCATION }_city`, event: 'error' });
    });
});

describe('события на blur улицы', () => {
    const promise = Promise.resolve([ mockCitySelectItem ]);

    beforeAll(() => {
        getResource.mockImplementation(() => promise);
    });

    it('при блюре отсылает успешную стату, если поменяли значения', async() => {
        await renderMockComponent();

        const input = await screen.getByLabelText(streetPlacholders.withSubway);

        await act(async() => {
            await userEvent.clear(input);
            await userEvent.tab();
            await sleep(200);
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    });

    it('при блюре не отсылает стату, если не поменялись значения', async() => {
        await renderMockComponent();
        const input = await screen.getByLabelText(streetPlacholders.withSubway) as HTMLInputElement;

        await act(async() => {
            userEvent.type(input, '{arrowleft}');
            userEvent.tab();
            await sleep(200);
        });

        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });

});

describe('тесты для города', () => {
    const promise = Promise.resolve([ mockCitySelectItem ]);

    beforeAll(() => {
        getResource.mockImplementation(() => promise);
    });

    it('при выборе города из садджеста значения города и координат берутся из переданного value', async() => {
        await promise;

        const { formApi } = await renderMockComponent();

        const input = await screen.getByLabelText('Город') as HTMLInputElement;

        await act(async() => {
            await typeInRichInput(input, 'Т');
        });

        const expectValue = {
            ...INITIAL_VALUES,
            coord: {
                latitude: '111111',
                longitude: '22222',
            },
            geobaseId: '2',
            cityName: 'Токио',
        };

        const item = await screen.getByRole('button', { name: 'Токио' });

        await act(async() => {
            await userEvent.click(item);
        });

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(formApi.current?.getFieldValue(OfferFormFieldNames.LOCATION)).toEqual(expectValue);

    });

    it('при изменении значения в инпуте города geobaseId должен стать undefined', async() => {
        const { formApi } = await renderMockComponent();

        const input = await screen.getByLabelText('Город') as HTMLInputElement;

        const expectValue = _.omit({
            ...INITIAL_VALUES,
            cityName: 'То',
        }, [ 'geobaseId' ]);

        await act(async() => {
            await userEvent.clear(input);

            await userEvent.type(input, 'То');
        });

        expect(formApi.current?.getFieldValue(OfferFormFieldNames.LOCATION)).toEqual(expectValue);

    });

    it('disabled город, если это форма для редактирования текущего объявления', async() => {
        const editPageConfig = {
            config: configStateMock.withPageParams({ form_type: 'edit' }).value(),
        };

        await renderMockComponent({ initinalValues: { geobaseId: '1111' }, mockStoreData: editPageConfig });

        const input = await screen.getByLabelText('Город') as HTMLInputElement;

        expect(input.disabled).toEqual(true);
    });

    it('если не заполнен geobaseId, в поле приходит ошибка с типом required', async() => {
        const { formApi } = await renderMockComponent({ initinalValues: { geobaseId: undefined } });

        let error: FormValidationResult<FieldErrors> | Record<string, FormValidationResult<FieldErrors>> | undefined;

        await act(async() => {
            error = await formApi.current?.validateField(OfferFormFieldNames.LOCATION);
        });

        expect(error?.type).toEqual(FieldErrors.REQUIRED);
    });
});

describe('тесты для улицы', () => {
    const promise = Promise.resolve({ results: [ mockStreetSelectItem ] });

    beforeAll(() => {
        getResource.mockImplementation(() => promise);
    });

    it('если в городе есть метро, то в placeholder улицы добавляется "метро"', async() => {
        await renderMockComponent({ initinalValues: { geobaseId: '1' } });

        const input = await screen.getByLabelText(streetPlacholders.withSubway);
        expect(input).not.toBeUndefined();
    });

    it('если в городе нет метро, то в placeholder улицы нет "метро"', async() => {
        await renderMockComponent({ initinalValues: { geobaseId: '1549' } });

        const input = await screen.getByLabelText(streetPlacholders.standard);
        expect(input).not.toBeUndefined();
    });

    it('при выборе улицы из садджеста новые значения улицы и координат берутся из переданного value', async() => {
        const { formApi } = await renderMockComponent();

        const input = await screen.getByLabelText(streetPlacholders.withSubway) as HTMLInputElement;

        await act(async() => {
            await typeInRichInput(input, 'М');
        });

        await mockgeocode;
        await promise;

        const item = await screen.getByRole('button', { name: 'Московская улица' });

        await act(async() => {
            await userEvent.click(item);
        });

        const expectValue = {
            ...INITIAL_VALUES,
            coord: {
                latitude: 55.800656,
                longitude: 37.540317,
            },
            streetName: 'Васечкина',
        };

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(formApi.current?.getFieldValue(OfferFormFieldNames.LOCATION)).toEqual(expectValue);
    });

    it('при изменении значения в инпуте улицы координаты берется из региона', async() => {
        const { formApi } = await renderMockComponent();

        const input = await screen.getByLabelText(streetPlacholders.withSubway) as HTMLInputElement;

        const expectValue = {
            ...INITIAL_VALUES,
            streetName: '',
            coord: {
                latitude: offerMock?.seller?.location?.region_info?.latitude,
                longitude: offerMock?.seller?.location?.region_info?.longitude,
            },
        };

        await act(async() => {
            await userEvent.clear(input);
        });

        expect(formApi.current?.getFieldValue(OfferFormFieldNames.LOCATION)).toEqual(expectValue);

    });
});

async function renderMockComponent({ initinalValues = {}, mockStoreData = {} } = {}) {
    const state = {
        offerDraft: offerDraftMock.withOfferMock(cloneOfferWithHelpers(offerMock).withSellerGeoId('117587')).value(),
        config: configStateMock.withPageParams({ form_type: 'add' }).value(),
        ...mockStoreData,
    };

    const formApi = createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const props = {
        initialValues: {
            [ OfferFormFieldNames.LOCATION ]: {
                ...INITIAL_VALUES,
                ...initinalValues,
            },
        },
        state,
        formApi,
    };

    await renderComponent(<OfferFormLocationField/>, props);

    return { formApi };
}
