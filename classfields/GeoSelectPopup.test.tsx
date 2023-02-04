/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => new Promise(() => {})),
    };
});

import { noop } from 'lodash';
import React from 'react';
import { mount } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import geoWithAliasMock from 'autoru-frontend/mockData/state/geoWithAlias.mock';

import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import gateApi from 'auto-core/react/lib/gateApi';

import GeoSelectPopup from './GeoSelectPopup';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const moscowGroup = [
    {
        id: '1',
        name: 'Москва и Московская область',
        geoAlias: 'moskovskaya_oblast',
        originalLength: 2,
        children: [
            {
                id: '213',
                name: 'Москва',
                supports_geo_radius: true,
                default_radius: 200,
                geoAlias: 'moskva',
            },
        ],
    },
];

const pechenga = [
    {
        id: '10900',
        name: 'Печенга',
        latitude: 69.55418,
        longitude: 31.218819,
        sub_title: 'Мурманская область, Печенгский район',
        supports_geo_radius: true,
        default_radius: 200,
        geoAlias: 'pechenga',
    },
];

const russia = [
    {
        id: '225',
        name: 'Россия',
        genitive: 'России',
        dative: 'России',
        accusative: 'Россию',
        prepositional: 'России',
        preposition: 'в',
        latitude: 61.698653,
        longitude: 99.505405,
        geoAlias: 'rossiya',
    },
];

beforeEach(() => {
    getResource.mockClear();
});

it('должен запросить популярные регионы при маунте', () => {
    const wrapper = mount(
        <GeoSelectPopup
            onChange={ noop }
            onRequestHide={ noop }
        />, { context: { store: mockStore({ geo: geoMock }) } },
    );
    expect(getResource).toHaveBeenCalledWith('getGeoRegionsWithAlias');
    expect(wrapper.find('GeoSelectPopup').state().geoRegionsPending).toBe(true);
});

it('должен обновить стейт и отрисовать регионы после успешной загрузки', () => {
    const p = Promise.resolve(geoWithAliasMock);
    getResource.mockImplementation(() => p);
    const wrapper = mount(
        <GeoSelectPopup
            onChange={ noop }
            onRequestHide={ noop }
        />, { context: { store: mockStore({ geo: geoMock }) } },
    );
    return p
        .then(() => {
            expect(wrapper.find('GeoSelectPopup').state().geoRegions).toEqual(geoWithAliasMock.regions);
        })
        .finally(() => {
            wrapper.update();
            expect(wrapper.find('GeoSelectPopup').state().geoRegionsPending).toBe(false);
            expect(wrapper.find('GeoSelectPopup').state().selection).toEqual([ '213' ]);
            expect(wrapper.find('.GeoSelectPopup__selected-regions').find('CheckboxTree').prop('groups')).toEqual(moscowGroup);
        });
});

it('не должен обновлять отображаемые регионы после выбора из списка', () => {
    const p = Promise.resolve(geoWithAliasMock);
    getResource.mockImplementation(() => p);
    const wrapper = mount(
        <GeoSelectPopup
            onChange={ noop }
            onRequestHide={ noop }
        />, { context: { store: mockStore({ geo: geoMock }) } },
    );
    return p
        .then(() => {})
        .finally(() => {
            wrapper.update();
            const spb = wrapper.find('input[name="10174"]');
            spb.simulate('change');
            wrapper.update();
            expect(wrapper.find('GeoSelectPopup').state().selection).toEqual([ '213', '10174' ]);
            expect(wrapper.find('.GeoSelectPopup__selected-regions').find('CheckboxTree').prop('groups')).toEqual(moscowGroup);
        });
});

it('должен обновлять отображаемые регионы после выбора из саджеста', () => {
    const p1 = Promise.resolve(geoWithAliasMock);
    const p2 = Promise.resolve(pechenga);
    getResource.mockImplementation((method, options) => {
        if (options && options.letters === 'печенга') {
            return p2;
        }
        return p1;
    });

    const wrapper = mount(
        <GeoSelectPopup
            onChange={ noop }
            onRequestHide={ noop }
        />, { context: { store: mockStore({ geo: geoMock }) } },
    );
    return p1
        .then(() => {})
        .finally(() => {
            wrapper.update();
            wrapper.find('.GeoSelectPopup__search-input').simulate('mouseDown');
            wrapper.update();
            wrapper.find('.FiltersSuggestPopup input').simulate('change', { target: { value: 'печенга' } });
            return p2.then(() => {
                wrapper.update();
                wrapper.find('.FiltersSuggestPopup__item').simulate('click');
                wrapper.update();
                expect(wrapper.find('GeoSelectPopup').state().selection).toEqual([ '213', '10900' ]);
                expect(wrapper.find('.GeoSelectPopup__selected-regions').find('CheckboxTree').prop('groups')).toEqual([
                    {
                        id: '10900',
                        name: 'Печенга',
                    },
                    ...moscowGroup,
                ]);
            });
        });
});

it('должен отобразить регион в выбранных, если его нет в ответе geoWithAliasMock', () => {
    const p = Promise.resolve({ regions: pechenga });
    getResource.mockImplementation(() => p);
    const wrapper = mount(
        <GeoSelectPopup
            onChange={ noop }
            onRequestHide={ noop }
        />, { context: { store: mockStore({ geo: geoMock }) } },
    );
    return p
        .then(() => {})
        .finally(() => {
            wrapper.update();
            expect(wrapper.find('GeoSelectPopup').state().geoRegionsPending).toBe(false);
            expect(wrapper.find('GeoSelectPopup').state().selection).toEqual([ '213' ]);
            expect(wrapper.find('.GeoSelectPopup__selected-regions').find('CheckboxTree').prop('groups')).toEqual([
                {
                    id: '213',
                    name: 'Москва',
                },
            ]);
        });
});

it('должен обновлять отображаемые регионы после выбора из саджеста, если это Россия', () => {
    const p1 = Promise.resolve(geoWithAliasMock);
    const p2 = Promise.resolve(russia);
    getResource.mockImplementation((method, options) => {
        if (options && options.letters === 'россия') {
            return p2;
        }
        return p1;
    });

    const wrapper = mount(
        <GeoSelectPopup
            onChange={ noop }
            onRequestHide={ noop }
        />, { context: { store: mockStore({ geo: geoMock }) } },
    );
    return p1
        .then(() => {})
        .finally(() => {
            wrapper.update();
            wrapper.find('.GeoSelectPopup__search-input').simulate('mouseDown');
            wrapper.update();
            wrapper.find('.FiltersSuggestPopup input').simulate('change', { target: { value: 'россия' } });
            return p2.then(() => {
                wrapper.update();
                wrapper.find('.FiltersSuggestPopup__item').simulate('click');
                wrapper.update();

                // вот тут осталась только россия!
                expect(wrapper.find('GeoSelectPopup').state().selection).toEqual([ '225' ]);

                expect(wrapper.find('.GeoSelectPopup__selected-regions').find('CheckboxTree').prop('groups')).toEqual([
                    {
                        geoAlias: 'rossiya',
                        id: '225',
                        name: 'Россия',
                    },
                    ...moscowGroup,
                ]);
            });
        });
});
