import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardPromo from '../';

const developer = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    video: {
        id: '1',
        name: 'Видео',
        description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
            'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.'
    }
};

describe('DeveloperCardPromo', () => {
    it('рисует превью видео с заголовком и описанием', async() => {
        await render(<DeveloperCardPromo developer={developer} />,
            { viewport: { width: 600, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
