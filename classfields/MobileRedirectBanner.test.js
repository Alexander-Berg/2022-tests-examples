/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const MobileRedirectBanner = require('./MobileRedirectBanner');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const context = {
    ...contextMock,
    router: {
        ...contextMock.router,
        params: {},
    },
};

jest.mock('auto-core/react/dataDomain/cookies/actions/setToRoot');
const setCookie = require('auto-core/react/dataDomain/cookies/actions/setToRoot').default;
setCookie.mockImplementation(() => () => {});

jest.mock('auto-core/react/dataDomain/cookies/actions/remove');
const removeCookie = require('auto-core/react/dataDomain/cookies/actions/remove').default;
removeCookie.mockImplementation(() => () => {});

const mockStore = require('autoru-frontend/mocks/mockStore').default;

let originalWindowLocation;
beforeEach(() => {
    // ДРУГ! Если ты знаешь нормальный способ, как замокать window.location - подскажи!
    // Сделай мир лучше!
    originalWindowLocation = global.window.location;
    delete global.window.location;
    global.window.location = { href: 'https://auto.ru/?nomobile' };
});

afterEach(() => {
    global.window.location = originalWindowLocation;
});

describe('условия показа', () => {
    it('должен показать плашку для тач-устройства с правильными куками', () => {
        const store = mockStore({
            config: { data: { browser: { isTouch: true } } },
            cookies: { nomobile: '1' },
        });

        const wrapper = shallow(
            <MobileRedirectBanner store={ store }/>,
            { context: context },
        ).dive();
        expect(wrapper.isEmptyRender()).toBe(false);
    });

    it('не должен показать плашку не для тач-устройства', () => {
        const store = mockStore({
            config: { data: { browser: { isTouch: false } } },
            cookies: { nomobile: '1' },
        });

        const wrapper = shallow(
            <MobileRedirectBanner store={ store }/>,
            { context: context },
        ).dive();
        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('не должен показать плашку без куки nomobile', () => {
        const store = mockStore({
            config: { data: { browser: { isTouch: true } } },
            cookies: {},
        });

        const wrapper = shallow(
            <MobileRedirectBanner store={ store }/>,
            { context: context },
        ).dive();
        expect(wrapper.isEmptyRender()).toBe(true);
    });

    it('не должен показать плашку с куками nomobile и noad-mobile', () => {
        const store = mockStore({
            config: { data: { browser: { isTouch: true } } },
            cookies: { nomobile: '1', 'noad-mobile': '1' },
        });

        const wrapper = shallow(
            <MobileRedirectBanner store={ store }/>,
            { context: context },
        ).dive();
        expect(wrapper.isEmptyRender()).toBe(true);
    });
});

describe('обработка кликов', () => {
    it('должен удалить куку nomobile и перезагрузить по клику "да"', () => {
        const store = mockStore({
            config: { data: { browser: { isTouch: true } } },
            cookies: { nomobile: '1' },
        });

        const wrapper = shallow(
            <MobileRedirectBanner store={ store }/>,
            { context: context },
        ).dive();
        wrapper.find('Button').at(0).simulate('click');
        expect(removeCookie).toHaveBeenCalledWith('nomobile');
        expect(window.location.href).toBe('https://auto.ru/');
    });

    it('должен поставить куку noad-mobile и скрыть плашку по клику "нет"', () => {
        const store = mockStore({
            config: { data: { browser: { isTouch: true } } },
            cookies: { nomobile: '1' },
        });

        const wrapper = shallow(
            <MobileRedirectBanner store={ store }/>,
            { context: context },
        ).dive();
        wrapper.find('Button').at(1).simulate('click');
        expect(setCookie).toHaveBeenCalledWith('noad-mobile', '1', { expires: 1 });
        expect(wrapper.state().visible).toBe(false);
    });
});
