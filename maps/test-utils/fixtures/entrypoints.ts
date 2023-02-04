import {
    EntrypointEnrichedData, EntrypointData
} from '../../@types/navi';
import {
    EntrypointType
} from '../../common/navi/constants';

export const testBrandedRouteNoActions: EntrypointData = {
    data: {
        bgColor: 'fff',
        textColor: '000000',
        disclamerColor: '000000',
        iconUrl: 'https://spec-admin.s3.mds.yandex.net/1608821327714.png',
        text: 'some text'
    },
    id: 'test-branded-route',
    entrypointName: 'test branded route',
    isActive: true,
    type: EntrypointType.BRANDED_ROUTE,
    dateStart: '2021-01-01',
    dateFinish: '2021-01-30',
    viewsLimit: 100,
    geography: '',
    yqlViews: '',
    yqlClicks: '',
    actionIds: []
};

export const enrichedTestBrandedRouteNoActions: EntrypointEnrichedData = {
    ...testBrandedRouteNoActions,
    actions: []
};

export const testKorzhEntrypoint: EntrypointData = {
    data: {
        frequency: 100,
        trafficJam: 10,
        cooldown: 10,
        text: 'test korzh text',
        imageKorzhPlaceholderUrl: 'https://test-image-korzh-placeholder-url.ru',
        imageKorzhLogoUrl: 'test-image-korzh-logo-url',
        logoPlacement: 'at_top',
        textColor: '000',
        verticalBias: 0.3
    },
    id: 'test-korzh-entrypoint',
    entrypointName: 'test korzh',
    isActive: true,
    type: EntrypointType.KORZH,
    dateStart: '2021-01-01',
    dateFinish: '2021-01-30',
    viewsLimit: 100,
    geography: '',
    yqlViews: '',
    yqlClicks: '',
    actionIds: []
};

export const enrichedTestKorzhEntrypoint: EntrypointEnrichedData = {
    ...testKorzhEntrypoint,
    actions: []
};

export const testKorzhJsonStructure = {
    reporting_id: 'sp-corg_test-project_test-korzh-entrypoint',
    logo: 'test-image-korzh-logo-url',
    placeholder: 'test-image-korzh-placeholder-url.ru',
    logo_placement: 'at_top',
    vertical_bias: 0.3,
    text_color: 'ff000000',
    actions: [],
    messages: ['test korzh text']
};
