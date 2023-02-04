import React from 'react';
import { expect } from 'chai';
import { render, mount } from 'enzyme';
import { Provider } from 'react-redux';

import configureStore from 'configureStore';
import { constants as commonConstants } from 'modules/app';

import DebugPanel from '../index';
import UserComponent from '../containers/user';

describe('DebugPanel', () => {
    const commonData = {
        app: {
            version: 'development',
            env: 'development',
        },
        user: {
            uid: '42',
            login: 'vasya.pupkin',
            balanceClientId: 42,
        },
        info: {
            projectId: '42',
        },
    };
    let store;
    let renderedDebugPanel;
    let mountedDebugPanel;

    const renderDebugPanel = () => {
        renderedDebugPanel = render(<Provider store={store}><DebugPanel /></Provider>);

        return renderedDebugPanel;
    };

    const mountDebugPanel = () => {
        mountedDebugPanel = mount(<Provider store={store}><DebugPanel /></Provider>);

        return mountedDebugPanel;
    };

    beforeEach(() => {
        store = configureStore();
        renderedDebugPanel = undefined;
        mountedDebugPanel = undefined;

        store.dispatch({ type: commonConstants.ACTION_TYPES.getCommonSuccess, data: commonData });
    });

    it('should be selectable by class "debug-panel"', () => {
        expect(renderDebugPanel().is('.debug-panel'))
            .to.equal(true);
    });

    it('should have 5 elements of class "debug-panel__item"', () => {
        expect(renderDebugPanel().find('.debug-panel__item'))
            .to.have.length(5);
    });

    describe('when "debugPanelUser" is not defined in fetched data', () => {
        it('should not render UserComponent', async () => {
            const currentDebugPanel = mountDebugPanel();

            await currentDebugPanel;

            expect(currentDebugPanel.update().find(UserComponent).length)
                .to.equal(0);
        });
    });

    describe('when "debugPanelUser" is true in fetched data', () => {
        it('should render UserComponent', async () => {
            const data = {
                ...commonData,
                info: {
                    ...commonData.info,
                    debugPanelUser: true,
                },
            };

            store.dispatch({ type: commonConstants.ACTION_TYPES.getCommonSuccess, data });

            const currentDebugPanel = mountDebugPanel();

            await currentDebugPanel;

            expect(currentDebugPanel.update().find(UserComponent).length)
                .to.equal(1);
        });
    });
});
