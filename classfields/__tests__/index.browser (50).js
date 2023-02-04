import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { LicenseBlock } from '../index';

const Component = (props = {}) => {
    return (
        <LicenseBlock
            link={() => {}}
            {...props}
        />
    );
};

describe('LicenseBlock', () => {
    it('natural edit', async() => {
        const component = <Component isEdit />;

        await render(component, { viewport: { width: 700, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('natural publish', async() => {
        const component = <Component isEdit={false} />;

        await render(component, { viewport: { width: 700, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('natural publish without vas selected', async() => {
        const component = <Component withoutVas />;

        await render(component, { viewport: { width: 700, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('juridical edit', async() => {
        const component = <Component isJuridical isEdit />;

        await render(component, { viewport: { width: 700, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('juridical publish', async() => {
        const component = <Component isJuridical isEdit={false} />;

        await render(component, { viewport: { width: 700, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
