import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { FakeChatWidgetContent } from '../index';

import { IChatView } from '../../types';

import { presets } from './mocks';

const views: IChatView[] = ['dark', 'blue'];

const logo = generateImageUrl({ width: 72, height: 32 });

describe('FakeChatWidgetContent', () => {
    views.forEach((view) => {
        it(`Базовая отрисовка ${view}`, async () => {
            await render(
                <FakeChatWidgetContent
                    presets={presets}
                    title="название ЖК"
                    view={view}
                    headerLogo={logo}
                    onCloseWidget={() => null}
                    onOpenChat={() => null}
                    page="page"
                    pageType="pageType"
                />,
                {
                    viewport: { width: 400, height: 600 },
                }
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
