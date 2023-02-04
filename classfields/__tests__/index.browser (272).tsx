import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FunctionReturnAny } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';

import OffersSearchResultsSortControl from '../';

import { params, ParamsType, sortDecl } from './mocks';

describe('OffersSearchResultsSortControl', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(
            <AppProvider>
                <OffersSearchResultsSortControl params={params} sortDecl={sortDecl} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 85 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Выбор нового типа сортировки', async () => {
        // было необходимо добавить компонент-обёртку чтобы замокать смену типа сортировки
        class WrappedControl extends React.Component {
            state: ParamsType = params;

            render() {
                return (
                    <div key={Math.random()}>
                        <AppProvider context={{ navigate: this.navigateMockHandler }}>
                            <OffersSearchResultsSortControl params={{ ...this.state }} sortDecl={sortDecl} />
                        </AppProvider>
                    </div>
                );
            }

            navigateMockHandler: FunctionReturnAny<[{ params: ParamsType }]> = ({ params }) => {
                this.setState((prevState) => ({ ...prevState, sort: params.sort }));
            };
        }

        await render(<WrappedControl />, {
            viewport: { width: 360, height: 300 },
        });

        await page.click(`.SearchResultsSortControl`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        const options = await page.$$('.SearchResultsSortControl__item');
        await options[2].click();
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
