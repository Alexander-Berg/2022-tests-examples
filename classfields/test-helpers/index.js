import * as enzyme from 'enzyme';
import { createMemoryHistory } from 'history';
import { createPath } from 'history';
import * as most from 'most';
import * as React from 'react';
import { configureFela } from 'view/libs/configure-fela';
import { AppContextProvider } from 'view/libs/context';
import { createRouteLocator, createRouter } from 'view/router';
import { createGateFromContext } from './gate';
import { createAppStoreProxy, runAppStore } from './store';

export { createBlackboxMock } from './blackbox';
export { createBackendMock, createGateMock } from './gate';
export { createStoreMock } from './store';

function createContext(options = {}) {
    const routeLocator = createRouteLocator();

    const defaultRoute = { page: 'clients', params: {} };
    const locationEntries = options.router && options.router.entries || [ defaultRoute ];
    const history = createMemoryHistory({
        initialEntries: locationEntries.map(routeLocator.getLocationByDescriptor).map(createPath)
    });
    const router = createRouter(history, routeLocator);

    const storeProxy = createAppStoreProxy();

    const appContext = {
        router,
        store: storeProxy.store,
        gate: createGateFromContext(options)
    };

    runAppStore(storeProxy, appContext, options.store && options.store.initialState);

    return appContext;
}

const { FelaProvider } = configureFela();

function Providers(props) {
    return (
        <AppContextProvider value={props.ctx}>
            <FelaProvider>
                {props.children}
            </FelaProvider>
        </AppContextProvider>
    );
}

function isStreamBasedComponent(element) {
    return element.length === 1 &&
        element.instance() &&
        element.instance().isStreamBasedComponent;
}

export function mount(node, ctx = {}) {
    const context = createContext(ctx);
    const dom = enzyme.mount(
        <Providers ctx={context}>
            {node}
        </Providers>
    );

    const time = {
        async tick(ms) {
            const promises = dom.findWhere(isStreamBasedComponent).map(element => {
                return element.instance().tick(ms);
            });

            await Promise.all(promises);
            dom.update();
        }
    };

    const gate = {
        ...context.gate,
        async waitForPendingRequests() {
            await context.gate.waitForPendingRequests();
            await time.tick();
        }
    };

    return { dom, time, gate, router: context.router };
}

export function simulateRIButtonClick(button) {
    button.simulate('mousedown', { button: 0 });
    button.simulate('mouseup');
}

export function getCurrentStreamValue(stream) {
    return stream
        .takeUntil(most.of())
        .reduce((acc, x) => x, undefined);
}
