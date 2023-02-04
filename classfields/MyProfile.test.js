/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return { getResource: jest.fn(() => Promise.resolve({})) };
});

const _ = require('lodash');
const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { render, screen } = require('@testing-library/react');
const userEvent = require('@testing-library/user-event').default;

const MyProfile = require('./MyProfile');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const getResource = require('auto-core/react/lib/gateApi').getResource;
const geoStateMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');
const userWithPhonesMock = require('auto-core/react/dataDomain/user/mocks/withPhones.mock');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const userMock = require('auto-core/react/dataDomain/user/mocks').default;
const serverConfig = require('auto-core/react/dataDomain/serverConfig/mocks/serverConfig.mock').default;
const { SocialProvider } = require('@vertis/schema-registry/ts-types-snake/vertis/common');

const alertMock = jest.fn();

let initialState;
let originalCreateObjectUrl;

beforeEach(() => {
    initialState = {
        user: _.cloneDeep(userWithPhonesMock),
        geo: _.cloneDeep(geoStateMock),
        config: configStateMock.value(),
        notifier: {},
        serverConfig,
    };

    jest.spyOn(global, 'alert').mockImplementation(alertMock);

    originalCreateObjectUrl = global.URL.createObjectURL;
    global.URL.createObjectURL = jest.fn(() => 'image-preview');
});

afterEach(() => {
    jest.restoreAllMocks();
    global.URL.createObjectURL = originalCreateObjectUrl;
});

describe('поставит иконку авторизации mos.ru на первое место', () => {
    it('если у пользователя выбран регион и это Москва и МО', () => {
        initialState.user.data.profile.autoru.geo_id = 213;
        initialState.user.data.social_profiles = [];
        initialState.geo.gids = [];
        const page = shallowRenderComponent({ initialState });
        const socialProfiles = page.find('SocialIcon');

        expect(socialProfiles.at(0).prop('id')).toBe('mosru');
    });

    it('если у пользователя не выбран регион, но ip московский', () => {
        initialState.user.data.profile.autoru.geo_id = undefined;
        initialState.user.data.social_profiles = [];
        initialState.geo.gids = [ 1 ];
        const page = shallowRenderComponent({ initialState });
        const socialProfiles = page.find('SocialIcon');

        expect(socialProfiles.at(0).prop('id')).toBe('mosru');
    });
});

describe('поставит иконку авторизации mos.ru на последнее место', () => {
    it('если у пользователя выбран регион и это не Москва', () => {
        initialState.user.data.profile.autoru.geo_id = 42;
        initialState.user.data.social_profiles = [];
        initialState.geo.gids = [];
        const page = shallowRenderComponent({ initialState });
        const socialProfiles = page.find('SocialIcon');

        expect(socialProfiles.at(socialProfiles.length - 1).prop('id')).toBe('mosru');
    });

    it('если у пользователя не выбран регион, и ip не московский', () => {
        initialState.user.data.profile.autoru.geo_id = undefined;
        initialState.user.data.social_profiles = [];
        initialState.geo.gids = [ 42 ];
        const page = shallowRenderComponent({ initialState });
        const socialProfiles = page.find('SocialIcon');

        expect(socialProfiles.at(socialProfiles.length - 1).prop('id')).toBe('mosru');
    });
});

describe('добавит иконку госуслуг', () => {
    it('в экспе', () => {
        initialState.user = userMock.withAuth(true).withSocialProfiles([]).value();
        initialState.geo.gids = [];

        const page = shallowRenderComponent({ initialState, contextMock });
        const gosUslugiIcon = page.find('SocialIcon#gosuslugi');

        expect(gosUslugiIcon.isEmptyRender()).toBe(false);
    });

    it('вне экспа если есть привязанный аккаунт госуслуг', () => {
        initialState.user = userMock.withAuth(true).withSocialProfiles([ {
            provider: SocialProvider.GOSUSLUGI,
            trusted: true,
        } ]).value();
        initialState.geo.gids = [];
        const page = shallowRenderComponent({ initialState });
        const gosUslugiIcon = page.find('SocialIcon#gosuslugi');

        expect(gosUslugiIcon.isEmptyRender()).toBe(false);
    });
});

describe('при выборе файла', () => {
    it('покажет алерт если изображение не jpeg или png', () => {
        const page = shallowRenderComponent({ initialState });
        const input = page.find('.MyProfile__avatar-change-control');
        input.simulate('change', { target: { files: [
            { name: 'foo', type: 'image/heic' },
        ] } });

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock).toHaveBeenCalledWith('Вы пытаетесь загрузить файл некорректного типа. Допустимы только файлы изображений .jpeg или .png');
    });

    it('покажет превью файла если это jpeg или png', () => {
        const page = shallowRenderComponent({ initialState });
        const input = page.find('.MyProfile__avatar-change-control');
        input.simulate('change', {
            target: {
                files: [
                    { name: 'foo', type: 'image/png' },
                ],
            },
        });

        const crop = page.find('ReactCrop');
        expect(crop.prop('src')).toBe('image-preview');
    });
});

describe('allow_offers_show', () => {
    it('отправит запрос с allow_offers_show=true, если активировали чекбокс', () => {
        const page = shallowRenderComponent({
            initialState: {
                ...initialState,
                user: userMock.withAuth(true).withReseller().value(),
            },
        });
        const checkbox = page.find('Checkbox');
        expect(checkbox).toHaveProp('checked', false);
        checkbox.simulate('check', true, { name: 'allow_offers_show' });
        page.find('.MyProfile__save').simulate('click');

        expect(getResource).toHaveBeenCalledWith(
            'updateProfile',
            // eslint-disable-next-line max-len
            { body: '{"alias":"J.DOE","full_name":"кукуруз","driving_year":1963,"birthday":"1899-11-26","about":"привет!","geo_id":213,"allow_offers_show":true}' },
        );
    });

    it('отправит запрос с allow_offers_show=false, если дезактивировали чекбокс', () => {
        const page = shallowRenderComponent({
            initialState: {
                ...initialState,
                user: userMock.withAuth(true).withReseller().withAllowOffersShow().value(),
            },
        });
        const checkbox = page.find('Checkbox');
        expect(checkbox).toHaveProp('checked', true);
        checkbox.simulate('check', false, { name: 'allow_offers_show' });
        page.find('.MyProfile__save').simulate('click');

        expect(getResource).toHaveBeenCalledWith(
            'updateProfile',
            // eslint-disable-next-line max-len
            { body: '{"alias":"J.DOE","full_name":"кукуруз","driving_year":1963,"birthday":"1899-11-26","about":"привет!","geo_id":213,"allow_offers_show":false}' },
        );
    });

    describe('метрики', () => {
        const INITIAL_STATE = {
            geo: _.cloneDeep(geoStateMock),
            config: configStateMock.value(),
            notifier: {},
            serverConfig,
            user: userMock.withAuth(true).withReseller().value(),
        };

        it('отправит метрику на показ чекбокса', async() => {
            render(getComponentWithWrapper({
                initialState: INITIAL_STATE,
            }));

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
                'public_profile',
                'show',
            ]);
        });

        it('отправит метрику на клик по чекбоксу', async() => {
            render(getComponentWithWrapper({
                initialState: INITIAL_STATE,
            }));

            const checkbox = await screen.findByRole('checkbox');
            userEvent.click(checkbox);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
                'public_profile',
                'click',
            ]);
        });

        it('отправит метрику на клик по чекбоксу когда он уже активен', async() => {
            render(getComponentWithWrapper({
                initialState: {
                    ...INITIAL_STATE,
                    user: userMock.withAuth(true).withReseller().withAllowOffersShow(true).value(),
                },
            }));

            const checkbox = await screen.findByRole('checkbox');
            userEvent.click(checkbox);

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
                'public_profile',
                'click',
                'remove',
            ]);
        });
    });
});

function getComponentWithWrapper({ context, initialState }) {
    const ContextProvider = createContextProvider(context || contextMock);
    const store = mockStore(initialState);

    return (
        <Provider store={ store }>
            <ContextProvider>
                <MyProfile/>
            </ContextProvider>
        </Provider>
    );
}

function shallowRenderComponent({ context, initialState, props }) {
    const ContextProvider = createContextProvider(context || contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <MyProfile { ...props } store={ store }/>
        </ContextProvider>,
    ).dive().dive().dive();
}
