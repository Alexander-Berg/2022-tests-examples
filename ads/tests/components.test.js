import { Provider } from 'react-redux';
import React from 'React';
import thunk from 'redux-thunk'
import { createStore, applyMiddleware, compose } from 'redux';

import sinon from 'sinon';
import { mount } from 'enzyme';
import { createWaitForElement } from 'enzyme-wait';

import Updates, { UpdatesBase } from '../src/Updates';

import MainReducer from '../src/reducers';

import * as Util from "../src/Util";

import PhfApiMock from './fixtures';
import * as fixtures from './fixtures';


describe('fully mounted components', () => {
    let api;
    let store;
    const sandbox = sinon.sandbox.create();
    const waitForSample = createWaitForElement('.LayoutFinish');

    beforeEach(() => {
        api = new PhfApiMock();
        store = createStore(MainReducer, compose(applyMiddleware(thunk)));
        sandbox.spy(Util, 'onError');
        sandbox.spy(UpdatesBase.prototype, 'renderData');
        sandbox.spy(UpdatesBase.prototype, 'renderTable');
        sandbox.spy(UpdatesBase.prototype, 'renderCalendar');

    });

    afterEach(() => {
        sandbox.restore();
    });

    test('App should call renderCommonError() after initialization failed with status 500', () => {
        api.setRejectGetAllUpdates(fixtures.API_5xx_ERROR());

        const component = mount(
            <Provider store={store}>
                <Updates api={ api } selectedDate={ null }/>
            </Provider>
        );

        return expect(waitForSample(component)).resolves.toBeDefined().then(() => {
            expect(UpdatesBase.prototype.renderData.called).toBe(true);
            expect(UpdatesBase.prototype.renderTable.called).toBe(false);
            expect(UpdatesBase.prototype.renderCalendar.called).toBe(true);
            expect(Util.onError.called).toBe(true);
        })
    });

    test('App should call renderAuthorizationError() after initialization failed with status 403', () => {
        api.setRejectGetAllUpdates(fixtures.API_403_ERROR());

        const component = mount(
            <Provider store={store}>
                <Updates api={ api } selectedDate={ null }/>
            </Provider>
        );

        return expect(waitForSample(component)).resolves.toBeDefined().then(() => {
            expect(UpdatesBase.prototype.renderData.called).toBe(true);
            expect(UpdatesBase.prototype.renderTable.called).toBe(false);
            expect(UpdatesBase.prototype.renderCalendar.called).toBe(true);
            expect(Util.onError.called).toBe(true);
        })
    });

    test('App should call renderComponents() after success initialization', () => {
        api.setResolveGetAllUpdates(fixtures.API_ALL_UPDATES());

        const component = mount(
            <Provider store={store}>
                <Updates api={ api } selectedDate={ null }/>
            </Provider>
        );

        return expect(waitForSample(component)).resolves.toBeDefined().then(() => {
            expect(UpdatesBase.prototype.renderData.called).toBe(true);
            expect(UpdatesBase.prototype.renderTable.called).toBe(true);
            expect(UpdatesBase.prototype.renderCalendar.called).toBe(true);
            expect(Util.onError.called).toBe(false);
        })
    });
});
