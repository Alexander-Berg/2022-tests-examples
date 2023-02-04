import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import * as h from './common';

describe('ClientProfileLinks', () => {
    it('correct draw links', async() => {
        await render(
            <h.Component links={h.links} mock={h.defaultStoreMock} />,
            { viewport: { width: 600, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('always draw pd link with edit button', async() => {
        const mock = {
            client: {
                common: {},
                profile: {
                    data: { links: {} }
                }
            }
        };

        const emptyLinks = mock.client.profile.data.links;

        await render(<h.Component links={emptyLinks} mock={mock} />, { viewport: { width: 160, height: 60 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw typed text and reset it to defaults in pd modal', async() => {
        await render(
            <h.Component links={h.links} mock={h.defaultStoreMock} />,
            { viewport: { width: 580, height: 250 } }
        );

        await page.click(h.pdEditButtonSelector);

        await page.type(h.pdInputSelector, h.typedText);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(h.pdCancelButtonSelector);

        await page.click(h.pdEditButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw pd popup', async() => {
        await render(
            <h.Component links={h.links} mock={h.defaultStoreMock} />,
            { viewport: { width: 580, height: 250 } }
        );

        await page.click(h.pdEditButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw error message when failed', async() => {
        await render(
            <h.Component links={h.links} mock={h.defaultStoreMock} />,
            { viewport: { width: 580, height: 250 } }
        );

        await page.click(h.pdEditButtonSelector);

        await page.type(h.pdInputSelector, h.typedText);

        await page.click(h.pdSaveButtonSelector);

        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    // todo кейс, когда успешно сохранили, проверить, повторно открыв попап, и посмотрев в нем информацию
});
