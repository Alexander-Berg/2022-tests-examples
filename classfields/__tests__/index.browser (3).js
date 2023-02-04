import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Gate from 'realty-core/view/react/libs/gate';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import REASONS from '../../__tests__/reasons.json';
import { ComplainReasonsTreeModal } from '../index';

import listItemStyles from '../../ListItem/styles.module.css';

const [ WIDTH, HEIGHT ] = [ 1000, 800 ];

const state = {
    user: { crc: '' }
};

Gate.create = () => Promise.resolve();

const Component = props => (
    <AppProvider initialState={state}>
        <ComplainReasonsTreeModal
            visible
            onModalOpen={() => {}}
            onModalClose={() => {}}
            data={REASONS}
            offerId='777'
            {...props}
        />
    </AppProvider>
);

describe('ComplainReasonsTreeModal', () => {
    it('default', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen height', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('long reason text', async() => {
        const data = [
            ...REASONS
        ];

        data[0] = {
            reason: 'SOLD',
            // eslint-disable-next-line max-len
            text: 'Не снимают трубку или сбрасывают звонок. Не понятно что случилось, вроде нормально общались. Попробую еще позвонить пару раз, а там видно будет'
        };

        const component = <Component data={data} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('click with no subreasons', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });
        await page.click(`.${listItemStyles.container}:nth-child(1)`);
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('click with subreasons', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });
        await page.click(`.${listItemStyles.container}:nth-child(4)`);
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('click on ANOTHER resason', async() => {
        const anotherReason = REASONS.find(item => item.reason === 'ANOTHER');
        const index = REASONS.indexOf(anotherReason);

        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });
        await page.click(`.${listItemStyles.container}:nth-child(${index + 1})`);
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
