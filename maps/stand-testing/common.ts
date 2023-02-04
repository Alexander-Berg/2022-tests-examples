import {CommonConfig} from 'server/config/types';

const standName = process.env.STAND_NAME;

const common: DeepPartial<CommonConfig> = {
    server: {
        httpTimeout: 60000,
        baseUrls: [
            '/maps',
            '/harita',
            '/web-maps',
            '/maps-[^/]+/maps',
            '/maps-[^/]+/harita',
            '/maps-[^/]+/web-maps',
            '/maps-[^/]+/navi',
            '/maps-[^/]+/profile',
            '/navi',
            '/profile'
        ],
        metroBaseUrls: ['/metro', '/metro/:alias', '/maps-[^/]+/metro'],
        mapWidgetBaseUrls: ['/map-widget/v1', '/maps-[^/]+/map-widget/v1'],
        transportSpbBaseUrls: ['/maps/spb-transport', '/web-maps/spb-transport', '/maps-[^/]+/spb-transport'],
        constructorApiUrls: ['/maps/constructor-api']
    },
    assets: {
        host: (standName ? `/${standName.replace(/maps-trunk-\d/, 'maps-trunk')}` : '') + '/maps/'
    },
    rumSuffix: '_test',
    showApiErrors: true,
    bunker: {
        version: 'stable',
        useTesting: true
    },
    userSplit: {
        environment: 'testing'
    },
    showDevtools: true
};

export default common;
