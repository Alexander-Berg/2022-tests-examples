import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import VasServiceBuyButton from '../index';

describe('VasServiceBuyButton', () => {
    it('должен нарисовать кнопку покупки поднятия со скидкой', async() => {
        await render(
            <VasServiceBuyButton
                type='raising'
                size='xl'
                view='yellow'
                isActive={false}
                price={999}
                basePrice={1300}
            />,
            { viewport: { width: 300, height: 90 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
