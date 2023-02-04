import type { RouteData, StateRouter } from '../redux/StateRouter';
import type { MockStore } from 'redux-mock-store';
import type { MockResponseInit } from 'jest-fetch-mock';
import type SusaninRouter from '@vertis/susanin';

import fetchMock from 'jest-fetch-mock';
import React from 'react';
import { Provider } from 'react-redux';
import { mount, shallow } from 'enzyme';
import configureStore from 'redux-mock-store';

import Router from './Router';

import susanin from './mocks/susanin';
import components from './mocks/components';

interface State {
    router: StateRouter;
}

const mockStore = configureStore<State>();

let susaninFindFirstSpy: jest.SpyInstance;
let consoleErrorSpy: jest.SpyInstance;
let store: MockStore<State>;
beforeEach(() => {
    store = getStateForUrl('/?foo=bar');
    susaninFindFirstSpy = jest.spyOn(susanin, 'findFirst');
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
    fetchMock.resetMocks();
    susaninFindFirstSpy.mockRestore();
    consoleErrorSpy.mockRestore();
});

describe('render()', () => {
    it('должен нарисовать Router -> App -> Index для известного роута', () => {
        const wrapper = shallow(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        ).dive().dive();

        const router = wrapper.find('.SusaninReact');
        expect(wrapper.find('.SusaninReact')).toHaveLength(1);

        const app = router.find('App');
        expect(app).toHaveLength(1);
        expect(app.prop('params')).toEqual({ foo: 'bar' });

        const index = app.find('Index');
        expect(index).toHaveLength(1);
        expect(index.prop('params')).toEqual({ foo: 'bar' });
    });

    it('должен выбросить ошибки, если компоненты для роута не найдены', () => {
        store = getStateForUrl('/500/?from=anywhere');

        expect(renderBadComponent).toThrow('Unknown component "undefined". Did you forget to add it to dictionary?');

        function renderBadComponent() {
            return shallow(
                <Provider store={ store }>
                    <Router
                        components={ components }
                        susanin={ susanin }
                    />
                </Provider>,
            ).dive().dive();
        }
    });
});

describe('обработка onClick', () => {
    it('должен обработать клик с обычным переходом', () => {
        let promise: Promise<string> = Promise.resolve('');
        fetchMock.mockResponse((req) => {
            if (req.url === 'http://localhost/offer/123/?from=index') {
                promise = Promise.resolve(
                    JSON.stringify({ response: 'url http://localhost/offer/123/?from=index' }),
                );
                return promise;
            }

            return Promise.reject(new Error('bad url'));
        });


        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.card').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(1);
        return promise
            // ждем внутренние промисы
            .then(() => new Promise((resolve) => {
                setTimeout(resolve, 100);
            }))
            .then(() => {
                expect(store.getActions()).toEqual([
                    {
                        payload: {
                            router: {
                                data: { components: [ 'App', 'Card' ] },
                                name: 'card',
                                params: { id: '123', from: 'index' },
                                url: 'http://localhost/offer/123/?from=index',
                            },
                        },
                        type: 'PAGE_LOADING',
                    },
                    {
                        payload: {
                            response: 'url http://localhost/offer/123/?from=index',
                        },
                        type: 'PAGE_LOADING_SUCCESS',
                    },
                ]);
            });
    });

    it('должен обработать ошибку, если сервер ответил не 200', () => {
        let promise: Promise<MockResponseInit> = Promise.resolve({ status: 500 });
        fetchMock.mockResponse((req) => {
            if (req.url === 'http://localhost/offer/123/?from=index') {
                promise = Promise.resolve({
                    body: JSON.stringify({ error: true }),
                    status: 500,
                });
                return promise;
            }

            return Promise.reject(new Error('bad url'));
        });


        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.card').simulate('click');

        return promise
            // ждем внутренние промисы
            .then(() => new Promise((resolve) => {
                setTimeout(resolve, 100);
            }))
            .then(() => {
                expect(store.getActions()).toEqual([
                    {
                        payload: {
                            router: {
                                data: { components: [ 'App', 'Card' ] },
                                name: 'card',
                                params: { id: '123', from: 'index' },
                                url: 'http://localhost/offer/123/?from=index',
                            },
                        },
                        type: 'PAGE_LOADING',
                    },
                    {
                        type: 'PAGE_LOADING_FAILED',
                    },
                ]);
            });
    });

    it('должен обработать клик с кастомным susaninFindFirst', () => {
        const susaninFindFirst = (susanin: SusaninRouter, urlWithoutHost: string) => {
            let match = susanin.findFirst(urlWithoutHost);
            if (!match) {
                const RE_REGION = /^\/([-_a-z0-9]+)(\/.*)$/;
                const MAX_GEO_URL_LENGTH = 65;
                // Тащить на клиент всю базу геоалиасов не хочется.
                // А провалидированный геоалиас хранится в state, который тоже не хочется сюда привязывать.
                // Поэтому делаем тупо: матчим весь урл, если не получается, то урл без первой части pathname.
                const regionParsedUrl = RE_REGION.exec(urlWithoutHost);
                if (regionParsedUrl && regionParsedUrl[1].length < MAX_GEO_URL_LENGTH) {
                    match = susanin.findFirst(regionParsedUrl[2]);
                }
            }

            return match;
        };

        let promise: Promise<string> = Promise.resolve('');
        fetchMock.mockResponse((req) => {
            if (req.url === 'http://localhost/moscow/?region=moscow') {
                promise = Promise.resolve(
                    JSON.stringify({ response: '/moscow/?region=moscow' }),
                );
                return promise;
            }

            return Promise.reject(new Error('bad url'));
        });


        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                    susaninFindFirst={ susaninFindFirst }
                />
            </Provider>,
        );

        wrapper.find('.url-with-custom-matcher').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(2);
        return promise
            // ждем внутренние промисы
            .then(() => new Promise((resolve) => {
                setTimeout(resolve, 100);
            }))
            .then(() => {
                expect(store.getActions()).toEqual([
                    {
                        payload: {
                            router: {
                                data: { components: [ 'App', 'Index' ] },
                                name: 'index',
                                params: { region: 'moscow' },
                                url: 'http://localhost/moscow/?region=moscow',
                            },
                        },
                        type: 'PAGE_LOADING',
                    },
                    {
                        payload: {
                            response: '/moscow/?region=moscow',
                        },
                        type: 'PAGE_LOADING_SUCCESS',
                    },
                ]);
            });
    });

    it('не должен обработать клик, если он defaultPrevented', () => {
        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.preventDefault').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(0);
        expect(store.getActions()).toEqual([]);
    });

    it('не должен обработать клик, если href из другого домена', () => {
        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.another-hostname').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(0);
        expect(store.getActions()).toEqual([]);
    });

    it('не должен обработать клик, если у ссылки есть target', () => {
        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.target-blank').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(0);
        expect(store.getActions()).toEqual([]);
    });

    it('не должен обработать клик, если у ссылки есть data-spa=false', () => {
        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.target-spa-false').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(0);
        expect(store.getActions()).toEqual([]);
    });

    it('не должен обработать клик, если нажата кнопка ctrl или cmd', () => {
        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.card').simulate('click', { metaKey: true });
        wrapper.find('.card').simulate('click', { ctrlKey: true });

        expect(susanin.findFirst).toHaveBeenCalledTimes(0);
        expect(store.getActions()).toEqual([]);
    });

    it('не должен обработать клик, если не удалось сматчить урл на сусанин', () => {
        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                />
            </Provider>,
        );

        wrapper.find('.unknown-route').simulate('click');

        expect(susanin.findFirst).toHaveBeenCalledTimes(1);
        expect(store.getActions()).toEqual([]);
    });

    it('должен добавить заголовки из пропа getRequestHeaders', () => {
        let promise: Promise<string> = Promise.resolve('');
        fetchMock.mockResponse((req) => {
            if (req.url === 'http://localhost/offer/123/?from=index') {
                promise = Promise.resolve(
                    JSON.stringify({ response: 'url http://localhost/offer/123/?from=index' }),
                );
                return promise;
            }

            return Promise.reject(new Error('bad url'));
        });

        const wrapper = mount(
            <Provider store={ store }>
                <Router
                    components={ components }
                    susanin={ susanin }
                    getRequestHeaders={ () => ({ testHeader: 'test_header_1' }) }
                />
            </Provider>,
        );

        wrapper.find('.card').simulate('click');

        const [ firstCall ] = fetchMock.mock.calls;
        expect(firstCall[1] && firstCall[1].headers).toMatchObject({ testHeader: 'test_header_1' });
    });
});

function getStateForUrl(url: string): MockStore<State> {
    const match = susanin.findFirst(url);
    if (!match) {
        throw new Error('Bad susanin');
    }

    const [ route, params ] = match;
    return mockStore({
        router: {
            current: {
                data: route.getData() as RouteData,
                name: route.getName(),
                params,
                url: '/',
            },
            state: 'LOADED',
        },
    });
}
