import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IStore } from 'view/react/deskpad/reducers/roots/metro-stations';

import { MetroStationContainer } from '../container';

import {
    baseInitialState,
    initialStateWithoutLinks,
    initialStateWithoutNewbuildings,
    initialStateWithoutNewbuildingsAndWithLowLinks,
    initialStateWitOneNewbuilding,
} from './mocks';
import styles from './styles.module.css';

const Component: React.FC<{ state: Partial<IStore> }> = ({ state }) => (
    <div className={styles.wrapper}>
        <AppProvider initialState={state}>
            <MetroStationContainer />
        </AppProvider>
    </div>
);

describe('MetroStation', function () {
    it('рисует полностью заполненный компонент на широком экране', async () => {
        await render(<Component state={baseInitialState} />, { viewport: { width: 1280, height: 3000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует полностью заполненный компонент на узком экране', async () => {
        await render(<Component state={baseInitialState} />, { viewport: { width: 1000, height: 3000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async () => {
        await render(<Component state={initialStateWithoutNewbuildings} />, {
            viewport: { width: 1000, height: 2800 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async () => {
        await render(<Component state={initialStateWitOneNewbuilding} />, { viewport: { width: 1000, height: 3000 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async () => {
        await render(<Component state={initialStateWithoutNewbuildingsAndWithLowLinks} />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует нет ссылок', async () => {
        await render(<Component state={initialStateWithoutLinks} />, { viewport: { width: 1000, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
