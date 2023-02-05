import * as React from 'react';
import {Provider} from 'mobx-react';
import {BrowserRouter} from 'react-router-dom';
import App from 'app/app';
import stores from 'stores/init';
import {setDefaultConfig} from 'common/view/config/config';
import * as renderer from 'react-test-renderer';
import {configMock} from 'mocks/config-mock';
import {IAPIResultResponse} from '../../@types/api';

const mockEmptyResponse: IAPIResultResponse<any> = {
    items: []
};

describe('Route', () => {
    const originalHref = location.href;
    const originalTitle = document.title;
    setDefaultConfig(configMock);

    function matchRoute(url: string): void {
        history.replaceState({}, originalTitle, url);

        const tree = renderer.create(
            <Provider {...stores()}>
                <BrowserRouter basename="/">
                    <App />
                </BrowserRouter>
            </Provider>
        ).toJSON();

        expect(tree).toMatchSnapshot();
    }

    beforeAll(() => {
        global.fetch.mockResponse(JSON.stringify({data: mockEmptyResponse}));
    });

    afterAll(() => {
        history.replaceState({}, originalTitle, originalHref);
        global.fetch.resetMocks();
    });

    [
        ['/', '<PageSources />'],
        ['/tasks', '<PageTasks />'],
        ['/shipments', '<PageShipments />'],
        ['/map', '<PageMap />'],
        ['/foo', '<PageNotFound />']
    ].forEach(([url, pageName]) => {
        it(`should render ${pageName}`, () => {
            matchRoute(url);
        });
    });
});
