import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FakeChatWidgetOpenButton } from '../index';

import { IChatView } from '../../types';

const views: IChatView[] = ['dark', 'blue'];

describe('FakeChatWidgetOpenButton', () => {
    views.forEach((view) => {
        it(`Базовая отрисовка ${view}`, async () => {
            await render(
                <FakeChatWidgetOpenButton view={view} page="page" pageType="pageType" onClick={() => null} />,
                {
                    viewport: { width: 100, height: 100 },
                }
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
