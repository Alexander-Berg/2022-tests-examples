import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { LicenseText } from '../index';

describe('LicenseText', () => {
    it('default', async() => {
        const component = (
            <LicenseText />
        );

        await render(component, { viewport: { width: 500, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with promocodes', async() => {
        const component = (
            <LicenseText withPromocodes />
        );

        await render(component, { viewport: { width: 500, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with promocodes centered', async() => {
        const component = (
            <div style={{ textAlign: 'center' }}>
                <LicenseText withPromocodes />
            </div>
        );

        await render(component, { viewport: { width: 500, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
