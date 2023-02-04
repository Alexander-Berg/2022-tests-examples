/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/cookie', () => ({
    get: jest.fn((name) => `_cookie_${ name }_value`),
    set: jest.fn(),
    setForever: jest.fn(),
    getCsrfToken: jest.fn(() => '_csrf_token_from_cookie'),
}));

const { shallow } = require('enzyme');
const React = require('react');
const { Provider } = require('react-redux');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const geoEmptyIpNovosibirsk = require('auto-core/react/dataDomain/geo/mocks/geoEmptyIpNovosibirsk.mock');
const geoMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');
const geoMskSpbMock = require('auto-core/react/dataDomain/geo/mocks/geoMskSpb.mock');

const GeoSelectPopup = require('./GeoSelectPopup');
const cookie = require('auto-core/react/lib/cookie');

let Context;
let store;

let _originalLocation;
beforeEach(() => {
    _originalLocation = global.location;
    delete global.location;

    const pageUrl = new URL('https://auto.ru/?foo=bar&bar=baz');
    global.location = {
        hostname: pageUrl.hostname,
        pathname: pageUrl.pathname,
        protocol: pageUrl.protocol,
        search: pageUrl.search,
        href: pageUrl.toString(),
    };
});

afterEach(() => {
    global.location = _originalLocation;
});

beforeEach(() => {
    cookie.get.mockReset();
    Context = createContextProvider(contextMock);
});

describe('Вычисление дефолтного радиуса при выборе регионов из примеров (Мск+Питер+Новосибирск)', () => {
    beforeEach(() => {
        store = mockStore({
            geo: geoEmptyIpNovosibirsk,
        });
    });

    it('должен взять значение из куки, если она есть', () => {
        const wrapper = renderComponent();
        cookie.get.mockImplementation(() => '100');

        let selectedRegion;
        wrapper.find('GeoSelectPopupRegion')
            .filterWhere((item) => {
                const region = item.prop('region');
                if (region.id === 213) {
                    selectedRegion = region;
                    return true;
                }
                return false;
            })
            .simulate('click', selectedRegion, true);

        expect(wrapper.find('GeoSelectSlider')).toHaveProp('value', '100');
    });

    it('должен взять дефолтное значение 200 для Москвы', () => {
        const wrapper = renderComponent();

        let selectedRegion;
        wrapper.find('GeoSelectPopupRegion')
            .filterWhere((item) => {
                const region = item.prop('region');
                if (region.id === 213) {
                    selectedRegion = region;
                    return true;
                }
                return false;
            })
            .simulate('click', selectedRegion, true);

        expect(wrapper.find('GeoSelectSlider')).toHaveProp('value', '200');
    });

    it('должен взять дефолтное значение 500 для Новосибирска', () => {
        const wrapper = renderComponent();

        let selectedRegion;
        wrapper.find('GeoSelectPopupRegion')
            .filterWhere((item) => {
                const region = item.prop('region');
                if (region.id === 65) {
                    selectedRegion = region;
                    return true;
                }
                return false;
            })
            .simulate('click', selectedRegion, true);

        expect(wrapper.find('GeoSelectSlider')).toHaveProp('value', '500');
    });
});

describe('Выбор региона', () => {
    let wrapper;
    beforeEach(() => {
        store = mockStore({
            geo: geoMock,
        });

        wrapper = renderComponent();
        // меняем радиус
        wrapper.find('GeoSelectSlider').simulate('change', '100');
    });

    it('должен сохранить значения в куки и сделать редирект на /georedir/', () => {
        global.location = {
            href: 'https://auto.ru/?foo=bar&bar=baz',
        };

        // кликаем сохранить
        wrapper.find('Button').simulate('click');

        expect(cookie.setForever).toHaveBeenCalledWith('gids', '213');
        expect(cookie.setForever).toHaveBeenCalledWith('gradius', '100');
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'gradius', '100' ]);

        expect(location.href).toEqual(
            'https://autoru_frontend.base_domain/georedir/?_csrf_token=_csrf_token_from_cookie&geo=213&url=https%3A%2F%2Fauto.ru%2F%3Ffoo%3Dbar%26bar%3Dbaz',
        );
    });

    it('должен удалить из урла невалидные параметры', () => {
        global.location = {
            href: 'https://auto.ru/?from=test&lat_from=1&lon_to=1&is_page_redirected=1',
        };

        // кликаем сохранить
        wrapper.find('Button').simulate('click');

        expect(location.href).toEqual(
            'https://autoru_frontend.base_domain/georedir/?_csrf_token=_csrf_token_from_cookie&geo=213&url=https%3A%2F%2Fauto.ru%2F%3Ffrom%3Dtest',
        );
    });

    it('должен сохранить значения в куки не навсегда в экспе SEARCHER_VS_1240_NO_HOME_TIER', () => {
        global.location = {
            href: 'https://auto.ru/?foo=bar&bar=baz',
        };

        const CustomContext = createContextProvider({
            ...contextMock,
            hasExperiment: exp => exp === 'SEARCHER_VS_1240_NO_HOME_TIER',
        });

        const wrapper = renderComponent(CustomContext);
        // меняем радиус
        wrapper.find('GeoSelectSlider').simulate('change', '100');

        // кликаем сохранить
        wrapper.find('Button').simulate('click');

        expect(cookie.set).toHaveBeenCalledWith('gradius_1000km_exp', '100');
    });
});

it('должен построить правильную ссылку на редирект, если выбрано два региона', () => {
    global.location = {
        href: 'https://auto.ru/?foo=bar&bar=baz',
    };

    store = mockStore({
        geo: geoMskSpbMock,
    });

    const wrapper = renderComponent();

    // кликаем сохранить
    wrapper.find('Button').simulate('click');

    const expectedUrl = 'https://autoru_frontend.base_domain/georedir/' +
            '?_csrf_token=_csrf_token_from_cookie&geo=213%2C10174&url=https%3A%2F%2Fauto.ru%2F%3Ffoo%3Dbar%26bar%3Dbaz';
    expect(location.href).toEqual(expectedUrl);
});

function renderComponent(CustomContext) {
    const ContextComponent = CustomContext || Context;
    return shallow(
        <ContextComponent>
            <Provider store={ store }>
                <GeoSelectPopup/>
            </Provider>
        </ContextComponent>,
    ).dive().dive().dive();
}
