import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { combineReducers, Middleware, Reducer } from 'redux';
import { Provider } from 'react-redux';
import createSagaMiddleware from 'redux-saga';
import SagaTester, { SagaFunction } from 'redux-saga-tester';
import { createStore } from 'redux-dynamic-modules';
import { getSagaExtension, ISagaModule } from 'redux-dynamic-modules-saga';
import { getThunkExtension } from 'redux-dynamic-modules-thunk';

import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { request } from 'common/utils/request';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { Permissions } from 'common/constants';
import {
    ClientSelectorAction,
    ClientSelectorListAction,
    PersonSelectorAction,
    PersonSelectorListAction
} from 'common/actions';
import * as snoutApi from 'common/api/snout';

import { ModuleSagaTester } from './module-saga-tester';

export interface MockData {
    request: (object | string | boolean | undefined)[] | object;
    response?: object | null;
    error?: any;
}

export type Mocks = {
    fetchGet?: MockData[];
    fetchPost?: MockData[];
    requestGet?: MockData[];
    requestPost?: MockData[];
    snoutApi?: { [key: string]: MockData };
};

export type MocksObject = { [key: string]: jest.Mock };

export type PageProps = {
    withModules?: boolean;

    perms?: string[];
    initialState?: any;
    mocks?: Mocks;
    windowLocationSearch?: string;
    windowLocationHash?: string;
    mockWindowLocation?: boolean;

    rootSaga?: SagaFunction;
    reducers?: { [key: string]: Reducer };
    RootContainer?: React.ElementType;
    module?: ISagaModule<any>;

    isDebugActions?: boolean;
    isDebugStates?: boolean;
};

const actionsLoggerMiddleware = (store: any) => (next: any) => (action: any) => {
    console.dir({ action });
    const result = next(action);
    return result;
};

const statesLoggerMiddleware = (store: any) => (next: any) => (action: any) => {
    const previousState = store.getState();
    const result = next(action);
    console.dir({ previousState, nextState: store.getState() });
    return result;
};

export class Page {
    wrapper: ReactWrapper;
    sagaTester: SagaTester<object> | ModuleSagaTester;
    store: any;
    fetchGet: jest.Mock;
    fetchPost: jest.Mock;
    snoutApi: MocksObject;
    request: { get: jest.Mock; post: jest.Mock };
    mocks?: Mocks;

    static selector: string;

    constructor({
        withModules,
        perms = Object.values(Permissions),
        initialState = {},
        mocks,
        windowLocationSearch,
        windowLocationHash,
        mockWindowLocation,
        rootSaga,
        reducers,
        RootContainer,
        module,
        isDebugActions,
        isDebugStates
    }: PageProps) {
        if (mockWindowLocation || windowLocationHash || windowLocationSearch) {
            //@ts-ignore
            delete window.location;
            //@ts-ignore
            window.location =
                windowLocationSearch || windowLocationHash
                    ? {
                          search: windowLocationSearch || '',
                          hash: windowLocationHash || '',
                          href: ''
                      }
                    : {
                          search: '',
                          hash: '',
                          href: ''
                      };
        }

        this.mocks = mocks;
        this.fetchGet = fetchGet as jest.Mock;
        this.fetchPost = fetchPost as jest.Mock;
        this.snoutApi = (snoutApi as unknown) as MocksObject;
        this.request = { get: request.get as jest.Mock, post: request.post as jest.Mock };

        this.setupMocks();

        const state = Object.assign({ perms }, initialState);
        const reducersMap = {
            ...commonReducers,
            ...reducers
        };
        const rootReducer: Reducer = combineReducers(reducersMap);

        const middlewares = [];
        if (isDebugActions) {
            middlewares.push(actionsLoggerMiddleware);
        }
        if (isDebugStates) {
            middlewares.push(statesLoggerMiddleware);
        }

        if (withModules) {
            this.sagaTester = new ModuleSagaTester();

            middlewares.push(this.sagaTester.getMiddleware());

            this.store = configureStoreWithModules(
                reducersMap,
                state,
                rootSaga,
                middlewares,
                module
            );
        } else {
            this.sagaTester = new SagaTester({
                // @ts-ignore
                initialState: state,
                reducers: rootReducer,
                middlewares
            });

            if (rootSaga) {
                this.sagaTester.start(rootSaga);
            }

            // @ts-ignore
            this.store = this.sagaTester.store;
        }

        const Container = withIntlProvider(() => (
            <Provider store={this.store}>{RootContainer && <RootContainer />}</Provider>
        ));

        this.wrapper = mount(<Container />);
    }

    private setupMocks() {
        if (this.mocks?.fetchGet) {
            for (let i = 0; i < this.mocks.fetchGet.length; i++) {
                this.setupMock(this.fetchGet, this.mocks.fetchGet[i]);
            }
        }

        if (this.mocks?.fetchPost) {
            for (let i = 0; i < this.mocks.fetchPost.length; i++) {
                this.setupMock(this.fetchPost, this.mocks.fetchPost[i]);
            }
        }

        if (this.mocks?.requestGet) {
            for (let i = 0; i < this.mocks.requestGet.length; i++) {
                this.setupMock(this.request.get, this.mocks.requestGet[i]);
            }
        }

        if (this.mocks?.requestPost) {
            for (let i = 0; i < this.mocks.requestPost.length; i++) {
                this.setupMock(this.request.post, this.mocks.requestPost[i]);
            }
        }

        if (this.mocks?.snoutApi) {
            for (let key in this.mocks?.snoutApi) {
                this.setupMock(this.snoutApi[key], this.mocks.snoutApi[key]);
            }
        }
    }

    private setupMock(mock: jest.Mock, data: MockData) {
        if (data.response) {
            mock.mockResolvedValueOnce(data.response);
        } else if (data.error) {
            mock.mockRejectedValueOnce(data.error);
        }
    }

    protected forceRerender() {
        this.wrapper.setProps({});
    }

    protected async nextTick() {
        await new Promise(resolve => setTimeout(resolve, 0));
    }

    findElement(element: string, selector: string) {
        return this.wrapper.find(
            `${(this.constructor as typeof Page).selector}__${element} ${selector}`
        );
    }

    fillDateField(title: string, value?: string) {
        const dateField = this.wrapper.find(`FormField[title="${title}"]`).find('DatePicker').at(0);

        (dateField.prop('onChange') as Function)(value);
    }

    fillSelect(title: string, value?: string | number | null) {
        (this.getSelect(title).prop('onChange') as Function)(value);
    }

    fillTextField(title: string, value?: string | number) {
        let textInput = this.wrapper.find(`FormField[title="${title}"]`).find('input');

        textInput.simulate('change', { target: { value } });
    }

    fillTextareaField(title: string, value?: string) {
        let textInput = this.wrapper.find(`FormField[title="${title}"]`).find('textarea');

        textInput.simulate('change', { target: { value } });
    }

    fillCheckboxField(title: string, checked: boolean) {
        let checkboxInput = this.wrapper.find(`FormField[title="${title}"]`).find('input');

        checkboxInput.simulate('change', { target: { checked } });
    }

    submitFilter() {
        const submitButton = this.wrapper.find('SearchFilter').find('[type="submit"]').at(1);

        submitButton.simulate('submit');
    }

    getListItems() {
        return this.wrapper.find('SearchList').find('tbody').find('tr');
    }

    getSelect(title: string) {
        return this.wrapper.find(`FormField[title="${title}"]`).find('Select');
    }

    async fillClientSelector(title: string, value: string, clientId: string = '') {
        let clientSelector = this.wrapper
            .find(`FormField[title="${title}"]`)
            .find('ModalSelector')
            .at(0);

        // @ts-ignore
        clientSelector.prop('onClick')();
        this.forceRerender();
        this.wrapper.update();

        const clientSelectorLogin = this.wrapper.find('FormField[title="Логин"]').find('input');
        clientSelectorLogin.simulate('change', { target: { value } });

        const clientSelectorSubmit = this.wrapper
            .find('ClientSelector')
            .find('[type="submit"]')
            .at(1);
        clientSelectorSubmit.simulate('submit');
        await this.sagaTester.waitFor(ClientSelectorListAction.RECEIVE, true);
        this.wrapper.update();

        const selectClientLink = this.wrapper
            .find('ClientList')
            .find(`a[id^="link-select-client-${clientId}"]`)
            .at(0);
        selectClientLink.simulate('click');
        await this.sagaTester.waitFor(ClientSelectorAction.SELECT);

        this.wrapper.update();
    }

    async fillPersonSelector(title: string, value: string) {
        let personSelector = this.wrapper
            .find(`FormField[title="${title}"]`)
            .find('ModalSelector')
            .at(0);

        // @ts-ignore
        personSelector.prop('onClick')();
        this.forceRerender();
        this.wrapper.update();

        const personSelectorId = this.wrapper.find('FormField[title="ID"]').find('input');
        personSelectorId.simulate('change', { target: { value } });

        const personSelectorSubmit = this.wrapper
            .find('PersonSelector')
            .find('[type="submit"]')
            .at(0);
        personSelectorSubmit.simulate('submit');
        await this.sagaTester.waitFor(PersonSelectorListAction.RECEIVE, true);
        this.wrapper.update();

        const selectPersonLink = this.wrapper
            .find('PersonSelector SearchList')
            .find('a')
            .find({ children: 'Выбрать' })
            .last();
        selectPersonLink.simulate('click');
        await this.sagaTester.waitFor(PersonSelectorAction.SELECT);

        this.wrapper.update();
    }

    async debugCalls() {
        await new Promise(res => setTimeout(res, 2000));
        console.log(
            JSON.stringify({
                fetchGet: this.fetchGet.mock.calls,
                fetchPost: this.fetchPost.mock.calls,
                requestGet: this.request.get.mock.calls,
                requestPost: this.request.post.mock.calls
            })
        );
    }
}

function configureStoreWithModules(
    reducers: PageProps['reducers'],
    initialState: PageProps['initialState'],
    rootSaga: PageProps['rootSaga'],
    middlewares: Middleware[],
    module: PageProps['module']
) {
    let sagaMiddleware;

    if (rootSaga) {
        sagaMiddleware = createSagaMiddleware();
        middlewares.push(sagaMiddleware);
    }

    const store = createStore(
        {
            initialState,
            enhancers: [],
            extensions: [getSagaExtension({}), getThunkExtension()]
        },
        module ? { ...module, middlewares } : getRootModule(reducers, rootSaga, middlewares)
    );

    return store;
}

function getRootModule(reducers: any, rootSaga: any, middlewares: any) {
    return {
        id: 'root',
        middlewares,
        reducerMap: reducers,
        sagas: rootSaga ? [rootSaga] : []
    };
}
