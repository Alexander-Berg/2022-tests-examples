import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AudioPlayerPopup } from '../index';

const Component = (props: Partial<React.ComponentProps<typeof AudioPlayerPopup>>) => (
    <div style={{ padding: '20px' }}>
        <AudioPlayerPopup fileUrl="/123.mp3" fileId="1" {...props} />
    </div>
);

const PLAY_BUTTON_SELECTOR = '[data-test="audio-player-popup-play-button"]';

describe('AudioPlayerPopup', () => {
    it('Рендерится в закрытом состоянии', async () => {
        const component = <Component />;

        await render(component, { viewport: { width: 130, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится в открытом состоянии после клика на Play', async () => {
        const component = <Component />;

        await render(component, { viewport: { width: 500, height: 100 } });

        await page.click(PLAY_BUTTON_SELECTOR);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
