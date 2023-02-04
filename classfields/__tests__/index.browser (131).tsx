import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import forwardIcon from '@realty-front/icons/common/longforward-24.svg';

import IconSvg from 'vertis-react/components/IconSvg';

import { CircleButtonWithShadow } from '../index';

describe('CircleButtonWithShadow', () => {
    it('Базовая отрисовка', async () => {
        await render(<CircleButtonWithShadow />, { viewport: { width: 80, height: 80 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка в активном состоянии', async () => {
        await render(<CircleButtonWithShadow isActive />, { viewport: { width: 80, height: 80 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с содержимым', async () => {
        await render(
            <CircleButtonWithShadow>
                <IconSvg id={forwardIcon} size={IconSvg.SIZES.SIZE_20} />
            </CircleButtonWithShadow>,
            { viewport: { width: 80, height: 80 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
