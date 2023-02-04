import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

const imageUrl = generateImageUrl({ width: 400, height: 400, size: 20 });

import { NotificationCard } from '../index';

const renderOptions = [
    { viewport: { width: 320, height: 568 } },
    { viewport: { width: 375, height: 812 } },
    { viewport: { width: 1024, height: 812 } },
];

const propsMap = [
    { testTitle: 'С картинкой, isMobile - true', image: true, isMobile: true },
    { testTitle: 'Без картинки, isMobile - true', image: false, isMobile: true },
    { testTitle: 'С картинкой, isMobile - false', image: true, isMobile: false },
    { testTitle: 'Без картинки, isMobile - false', image: false, isMobile: false },
];

const title = 'Анкета почти готова';
const subtitle = 'Осталось заполнить её до конца, чтобы мы приняли вашу заявку на сдачу квартиры';

describe('NotificationCard', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            propsMap.forEach((prop) => {
                it(`${prop.testTitle} ${renderOption.viewport.width}`, async () => {
                    await render(
                        <div style={{ backgroundColor: 'grey', padding: '10px' }}>
                            <NotificationCard
                                title={title}
                                subtitle={subtitle}
                                image={prop.image ? imageUrl : undefined}
                                isMobile={prop.isMobile}
                            />
                        </div>,
                        renderOption
                    );

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });
});
