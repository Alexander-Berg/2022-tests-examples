import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import Header from '../';

import {
    initialState,
    initialStateWithoutPresets,
    initialStateWithNewPresets,
    initialStateForAuthorizedUser,
    initialStateWithLogoSpecialProjects,
    Gate,
} from './mocks';

const specialProjectsWithLogo = [
    ['NoName', 123456],
    ['legenda', 10174],
    ['suvarstroit', 11119],
    ['stolicaNizhnij', 11079],
    ['unistroy', 463788],
    ['samolet', 102320],
    ['semya', 241299],
    ['komosstroy', 896162],
    ['yugstroyinvest', 230663],
    ['sadovoeKolco', 75122],
    ['4D', 650428],
    ['kronverk', 524385],
];

describe('Header', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(
            <AppProvider initialState={initialState} Gate={Gate}>
                <Header />
            </AppProvider>,
            {
                viewport: { width: 360, height: 80 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии, если пользователь авторизован', async () => {
        await render(
            <AppProvider initialState={initialStateForAuthorizedUser} Gate={Gate}>
                <Header />
            </AppProvider>,
            {
                viewport: { width: 360, height: 80 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('корректно открывает и закрывает меню', async () => {
        await render(
            <AppProvider initialState={initialState} Gate={Gate}>
                <Header config={{ os: 'ios' }} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 800 },
            }
        );

        await page.click('.Header__menu-button');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.HeaderMenu__header > .CloseModalButton');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('меню отрабатывает отсутствие пресетов в сторе', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutPresets} Gate={Gate}>
                <Header config={{ os: 'ios' }} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 800 },
            }
        );

        await page.click('.Header__menu-button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('меню отрабатывает изменение пресетов', async () => {
        class TestContainerComponent extends React.Component {
            state = { ...initialState };

            componentDidMount() {
                this.setState({ ...initialStateWithNewPresets });
            }

            render() {
                return (
                    <AppProvider initialState={this.state} Gate={Gate}>
                        <Header config={{ os: 'ios' }} />
                    </AppProvider>
                );
            }
        }

        await render(<TestContainerComponent />, {
            viewport: { width: 360, height: 800 },
        });

        await page.click('.Header__menu-button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(specialProjectsWithLogo)('рендерится спецпроект %s с логотипом', async (id, geoId) => {
        await render(
            <AppProvider initialState={initialStateWithLogoSpecialProjects[geoId]} Gate={Gate}>
                <Header config={{ os: 'ios' }} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 800 },
            }
        );

        await page.click('.Header__menu-button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
