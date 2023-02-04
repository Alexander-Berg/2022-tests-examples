import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ClusterNotificationPopup } from '../index';

describe('ClusterNotificationPopup', () => {
    it('default render', async() => {
        const comp = (
            <ClusterNotificationPopup
                rulesUrl='https://realty.yandex.ru'
                priceUrl='https://realty.yandex.ru'
                visible
                onOpen={() => {}}
                onConfirm={() => {}}
                onClose={() => {}}
                onLinkClick={() => {}}
            />
        );

        await render(comp, { viewport: { width: 700, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
