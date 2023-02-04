import type { RegionInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { RegionWithLinguistics } from 'auto-core/react/dataDomain/geo/StateGeo';
import getDistanceToSelectedCity from 'auto-core/react/lib/offer/getDistanceToSelectedCity';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';

let regionInfo: RegionInfo;
beforeEach(() => {
    regionInfo = {
        latitude: 0,
        longitude: 0,
        id: '10758',
        name: 'Химки',
        genitive: '',
        dative: '',
        accusative: '',
        instrumental: '',
        prepositional: '',
        preposition: '',
        sub_title: '',
        supports_geo_radius: true,
        default_radius: 200,
        children: [],
        parent_ids: [],
    };
});

it('должен выдать дистанцию', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 19,
                    geobase_id: '213',
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, geoMock)).toMatchSnapshot();
});

it('должен выдать дистанцию более георадиусса', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 219,
                    geobase_id: '213',
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, geoMock)).toMatchSnapshot();
});

it('должен выдать дистанцию, если георадиус 0', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 19,
                    geobase_id: '213',
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, {
        ...geoMock,
        radius: 0,
    })).toMatchSnapshot();
});

it('должен выдать дистанцию, если выбрана область и прилетает дистанция до столицы', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 101,
                    geobase_id: '213',
                    region_info: {
                        id: '213',
                        name: 'Москва',
                        genitive: 'Москвы',
                        dative: 'Москве',
                        accusative: 'Москву',
                        prepositional: 'Москве',
                        instrumental: 'Москвой',
                        preposition: 'в',
                        latitude: 55.753215,
                        longitude: 37.622504,
                        supports_geo_radius: true,
                        sub_title: '',
                        default_radius: 200,
                        children: [],
                        parent_ids: [],
                    },
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, {
        ...geoMock,
        gidsInfo: [
            {
                id: 1,
                latitude: 55.815792,
                longitude: 37.380031,
                name: 'Москва и Московская область',
                parent_id: 3,
                type: 5,
                linguistics: {
                    ablative: '',
                    accusative: 'Москву и Московскую область',
                    dative: 'Москве и Московской области',
                    directional: '',
                    genitive: 'Москвы и Московской области',
                    instrumental: 'Москвой и Московской областью',
                    locative: '',
                    nominative: 'Москва и Московская область',
                    preposition: 'в',
                    prepositional: 'Москве и Московской области',
                },
            } as RegionWithLinguistics,
        ],
    })).toMatchSnapshot();
});

it('должен выдать дистанцию до совпадающего, если выбрана область и прилетает два региона в distance_to_selected_geo', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    coord: {
                        latitude: 44.608865,
                        longitude: 40.098548,
                    },
                    geobase_id: '11004',
                    distance: 173,
                    region_info: {
                        id: '1093',
                        name: 'Майкоп',
                        genitive: 'Майкопа',
                        dative: 'Майкопу',
                        accusative: 'Майкоп',
                        instrumental: 'Майкопом',
                        prepositional: 'Майкопе',
                        preposition: 'в',
                        latitude: 44.608865,
                        longitude: 40.098548,
                        supports_geo_radius: true,
                        sub_title: '',
                        default_radius: 200,
                        children: [],
                        parent_ids: [],
                    },
                },
                {
                    coord: {
                        latitude: 45.03547,
                        longitude: 38.975313,
                    },
                    geobase_id: '10995',
                    distance: 187,
                    region_info: {
                        id: '35',
                        name: 'Краснодар',
                        genitive: 'Краснодара',
                        dative: 'Краснодару',
                        accusative: 'Краснодар',
                        instrumental: 'Краснодаром',
                        prepositional: 'Краснодаре',
                        preposition: 'в',
                        latitude: 45.03547,
                        longitude: 38.975313,
                        supports_geo_radius: true,
                        sub_title: '',
                        default_radius: 200,
                        children: [],
                        parent_ids: [],
                    },
                },
            ],
            region_info: {
                id: '100630',
                name: 'Белая Глина',
                genitive: 'Белой Глины',
                dative: 'Белой Глине',
                accusative: 'Белую Глину',
                instrumental: 'Белой Глиной',
                prepositional: 'Белой Глине',
                preposition: 'в',
                latitude: 46.073665,
                longitude: 40.871324,
                parent_ids: [
                    '100630',
                    '176022',
                    '99268',
                    '10995',
                    '26',
                    '225',
                    '10001',
                    '10000',
                ],
                sub_title: '',
                default_radius: 200,
                children: [],
                supports_geo_radius: true,
            },
        })
        .value();

    expect(getDistanceToSelectedCity(offer, {
        ...geoMock,
        gidsInfo: [
            {
                id: 10995,
                latitude: 45.272365,
                longitude: 38.951409,
                linguistics: {
                    ablative: '',
                    accusative: 'Краснодарский край',
                    dative: 'Краснодарскому краю',
                    directional: '',
                    genitive: 'Краснодарского края',
                    instrumental: 'Краснодарским краем',
                    locative: '',
                    nominative: 'Краснодарский край',
                    preposition: 'в',
                    prepositional: 'Краснодарском крае',
                },
                name: 'Краснодарский край',
                parent_id: 26,
                type: 5,
            } as RegionWithLinguistics,
        ],
    })).toMatchSnapshot();
});

it('не должен выдать дистанцию, если выбрано два региона', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 19,
                    geobase_id: '213',
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, {
        ...geoMock,
        gidsInfo: geoMock.gidsInfo.concat(geoMock.gidsInfo),
    })).toBeNull();
});

it('не должен выдать дистанцию, если оффер из выбранного региона', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 19,
                    geobase_id: '213',
                },
            ],
            region_info: {
                latitude: 0,
                longitude: 0,
                id: '213',
                name: 'Москва',
                genitive: '',
                dative: '',
                accusative: '',
                instrumental: '',
                prepositional: '',
                preposition: '',
                sub_title: '',
                default_radius: 200,
                children: [],
                parent_ids: [],
                supports_geo_radius: true,
            },
        })
        .value();

    expect(getDistanceToSelectedCity(offer, geoMock)).toBeNull();
});

it('не должен выдать дистанцию, если выбрана страна', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 19,
                    geobase_id: '213',
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, {
        ...geoMock,
        gidsInfo: [
            {
                id: 225,
                latitude: 61.698653,
                longitude: 99.505405,
                name: 'Россия',
                parent_id: 10001,
                type: 3,
                linguistics: {
                    ablative: '',
                    accusative: 'Россию',
                    dative: 'России',
                    directional: '',
                    genitive: 'России',
                    instrumental: 'Россией',
                    locative: '',
                    nominative: 'Россия',
                    preposition: 'в',
                    prepositional: 'России',
                },
            } as RegionWithLinguistics,
        ],
    })).toBeNull();
});

it('не должен выдать дистанцию, если выбрана область', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerLocation({
            distance_to_selected_geo: [
                {
                    distance: 19,
                    geobase_id: '213',
                },
            ],
            region_info: regionInfo,
        })
        .value();

    expect(getDistanceToSelectedCity(offer, {
        ...geoMock,
        gidsInfo: [
            {
                id: 1,
                latitude: 55.815792,
                longitude: 37.380031,
                name: 'Москва и Московская область',
                parent_id: 3,
                type: 5,
                linguistics: {
                    ablative: '',
                    accusative: 'Москву и Московскую область',
                    dative: 'Москве и Московской области',
                    directional: '',
                    genitive: 'Москвы и Московской области',
                    instrumental: 'Москвой и Московской областью',
                    locative: '',
                    nominative: 'Москва и Московская область',
                    preposition: 'в',
                    prepositional: 'Москве и Московской области',
                },
            } as RegionWithLinguistics,
        ],
    })).toBeNull();
});
