import {URL} from 'url';
import * as qs from 'qs';
import {getDeployStandUrl} from '@yandex-int/maps-stand-tools';
import {UiTheme} from 'types/theme';

const LOCAL_MOCKS_BACKENDS = ['storiesInt', 'spravApi', 'bookingInt'];
type LocalMocksBackendType = typeof LOCAL_MOCKS_BACKENDS[number];
type MockBackends = Record<LocalMocksBackendType, Record<string, string | number | true> | string | number | true>;

const defaultParams = [
    'no-tests',
    'no-daas',
    'no-mooa',
    'no-counters',
    'no-mail',
    'no-cache',
    'no-animation',
    'no-interstitial',
    'no-lazy-render'
];

const standUrlOptions = {
    standName: process.env.STAND_NAME!,
    tld: '{tld}'
};

const HOSTS = {
    deploy: getDeployStandUrl(standUrlOptions) + '{baseUrl}',
    testing: 'https://maps-rc.l7test.yandex.{tld}/{baseUrl}',
    tunneler: `https://${process.env.USER}-${process.env.MAPS_NODEJS_PORT}-maps.ldev.yandex.{tld}/{baseUrl}`
};

type Host = keyof typeof HOSTS;
type Tld = 'ru' | 'com' | 'tr' | 'by' | 'ua' | 'kz';
type Application = 'maps' | 'metro' | 'profile' | 'map-widget' | 'transport-spb' | 'constructor-api';
type TldsPresets = Record<Tld, string>;
type HostsPresets = Record<Host, TldsPresets>;
type ApplicationsPresets = Record<Application, HostsPresets>;

const APPLICATIONS: Application[] = ['maps', 'metro', 'profile', 'map-widget', 'transport-spb', 'constructor-api'];
const BASE_URLS: Record<Application, Partial<Record<Tld, string>>> = {
    maps: {ru: 'maps', com: 'maps', tr: 'harita', by: 'maps', ua: 'maps', kz: 'maps'},
    metro: {ru: 'metro', com: 'metro', tr: 'metro', by: 'metro', ua: 'metro', kz: 'maps'},
    profile: {ru: 'profile'},
    'map-widget': {ru: 'map-widget/v1', tr: 'map-widget/v1'},
    'transport-spb': {ru: 'maps/spb-transport'},
    'constructor-api': {ru: 'maps/constructor-api'}
};

const TLDS: Record<Tld, string> = {ru: 'ru', com: 'com', tr: 'com.tr', by: 'by', ua: 'ua', kz: 'kz'};

const HOSTS_PRESETS = APPLICATIONS.reduce<ApplicationsPresets>((applications, app) => {
    applications[app] = Object.entries(HOSTS).reduce<HostsPresets>((presets, [key, host]: [Host, string]) => {
        presets[key] = Object.entries(TLDS).reduce<TldsPresets>((result, [tldKey, tld]: [Tld, string]) => {
            const baseUrl = BASE_URLS[app][tldKey];
            if (!baseUrl) {
                return result;
            }
            result[tldKey] = host.replace(/\{tld\}/, tld).replace(/\{baseUrl\}/, baseUrl);
            return result;
        }, {} as TldsPresets);
        return presets;
    }, {} as HostsPresets);
    return applications;
}, {} as ApplicationsPresets);

interface GetHostOptions {
    tld?: Tld;
    application?: Application;
}

/**
 * Возвращает хост.
 */
function getHost(options: GetHostOptions = {}): string {
    const hostPreset = process.env.HOSTS || 'deploy';
    return HOSTS_PRESETS[options.application || 'maps'][hostPreset as Host][options.tld || 'ru'];
}

interface GetUrlOptions {
    tld?: Tld;
    // Не добавлять no-interstitial и no-daas
    allowDistribution?: boolean;
    // Не добавлять no-lazy-render
    allowLazyRender?: boolean;
    // Добавляет debug=mobile для немобильных браузеров.
    isMobile?: boolean;
    // Параметр pathname содержит host.
    withHost?: boolean;
    // Устанавливает дефолтную зафиксированную дату "сегодня".
    mockToday?: string;
    // Добавляет дополнительный параметр для переиспользования старых данных в новых моках.
    mockVersion?: string;
    mockBunkerVersion?: string;
    // Устанавливает пользовательские координаты через куку `gpauto` в контейнере `yp`.
    // Значение `none` эмулирует отсутствие `gpauto` в контейнере `yp`.
    // Для того, чтобы замокать позицию геолокации на фронтенде смотри опцию `geolocation`.
    mockGpauto?: [lat: number, lon: number] | 'none';
    // Подменяет пользовательский регион
    mockRegion?: number;
    // Позволяет замокать клиентскую геолокацию
    mockGeolocation?: [lat: number, lon: number, accuracy?: number] | 'denied' | 'unavailable';
    // Настройки моков для конкретных бекендов
    mockBackends?: MockBackends;
    withTooltip?: boolean;
    fakeLogin?: boolean | string;
    fakePhone?: boolean | string;
    fakeOrganizationOwner?: boolean;
    havePlus?: boolean;
    disableJavaScript?: boolean;
    enableVector?: boolean;
    application?: Application;
    // Мгновенный зум к маршруту в метро.
    instantZoom?: boolean;
    debugMocks?: boolean;
    // Не добавлять no-counters
    allowCounters?: boolean;
    logAnalytics?: boolean;
    theme?: UiTheme;
}

/**
 * Подставляет дефолтные параметры в URL, передает их браузеру и проверяет загрузку страницы.
 */
function getUrl(pathname = '', options: GetUrlOptions = {}): string {
    let host = getHost(options);
    host = host + (pathname.startsWith('/') ? '' : '/213/moscow');

    const urlObj = new URL((!options.withHost ? host : '') + pathname);

    const queryObject: Record<string, unknown> = qs.parse(urlObj.search, {ignoreQueryPrefix: true});
    defaultParams.forEach((key) => (queryObject[key] = '1'));

    const mocksParams: Record<string, string | number | object> = {};
    if (options.mockToday) {
        mocksParams.today = options.mockToday;
    }
    if (options.mockVersion) {
        mocksParams.version = options.mockVersion;
    }
    if (options.mockBunkerVersion) {
        mocksParams.bunkerVersion = options.mockBunkerVersion;
    }
    if (options.mockRegion) {
        mocksParams.region = options.mockRegion;
    }
    if (options.mockGpauto) {
        mocksParams.gpauto = Array.isArray(options.mockGpauto) ? options.mockGpauto.join() : options.mockGpauto;
    }
    if (options.mockBackends) {
        mocksParams.backends = options.mockBackends;
    }
    if (options.mockGeolocation) {
        mocksParams.geolocation = Array.isArray(options.mockGeolocation)
            ? options.mockGeolocation.join()
            : options.mockGeolocation;
    }

    const debugParams = (Array.isArray(queryObject.debug)
        ? queryObject.debug.map(String)
        : [String(queryObject.debug || '')]
    )
        .map((debugString) => (debugString ? debugString.split(',') : []))
        .reduce((memo, el) => memo.concat(el), [])
        .filter(Boolean);

    debugParams.push('requestId');
    if (options.debugMocks) {
        debugParams.push('mock');
    }
    if (options.fakeLogin) {
        debugParams.push('login');

        if (typeof options.fakeLogin === 'string') {
            debugParams.push(`login:${options.fakeLogin}`);
        }
    }
    if (options.fakePhone) {
        debugParams.push('phone');

        if (typeof options.fakePhone === 'string') {
            debugParams.push(`phone:${options.fakePhone}`);
        }
    }
    if (options.fakeOrganizationOwner) {
        debugParams.push('owner');
    }
    if (options.isMobile) {
        debugParams.push('mobile');
    }
    if (options.havePlus) {
        debugParams.push('have-plus');
    }

    if (options.allowDistribution) {
        delete queryObject['no-daas'];
        delete queryObject['no-interstitial'];
    }
    if (options.allowLazyRender) {
        delete queryObject['no-lazy-render'];
    }
    if (options.disableJavaScript) {
        queryObject['no-js'] = '1';
    }
    if (!options.enableVector) {
        queryObject['no-vector'] = '1';
    }
    if (options.instantZoom) {
        queryObject['instant-zoom'] = '1';
    }
    if (options.withTooltip) {
        queryObject['with-tooltip'] = '1';
    }
    if (options.allowCounters) {
        delete queryObject['no-counters'];
    }
    if (options.logAnalytics) {
        queryObject.loggers = 'analytics';
    }
    if (options.theme === 'dark') {
        queryObject.theme = 'dark';
    }

    if (debugParams.length === 0) {
        delete queryObject.debug;
    } else {
        queryObject.debug = debugParams;
    }

    queryObject.mocks = Object.keys(mocksParams).length === 0 ? '1' : mocksParams;

    const loggers = typeof queryObject.loggers === 'string' ? queryObject.loggers.split(',') : [];
    loggers.push('requestId');
    queryObject.loggers = loggers.join(',');

    urlObj.search = qs.stringify(queryObject, {addQueryPrefix: true, indices: false});

    return urlObj.toString();
}

export {
    getUrl,
    GetUrlOptions,
    getHost,
    defaultParams,
    Tld,
    Host,
    TLDS,
    Application,
    MockBackends,
    LocalMocksBackendType,
    LOCAL_MOCKS_BACKENDS
};
