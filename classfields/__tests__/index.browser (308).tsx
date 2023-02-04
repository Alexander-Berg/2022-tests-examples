import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansFiltersModal, ISitePlansFiltersModalProps } from '..';

const COMMON_PROPS = {
    submitButtonText: 'Текст на кнопке фильтра',
    isVisible: true,
    turnoverOccurrence: [],
};

describe('SitePlansFiltersModal', () => {
    it('рисует фильтр', async () => {
        const props = (COMMON_PROPS as unknown) as ISitePlansFiltersModalProps;

        await render(
            <AppProvider>
                <SitePlansFiltersModal {...props} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует фильтр в состоянии загрузки', async () => {
        const props = ({ ...COMMON_PROPS, isLoading: true } as unknown) as ISitePlansFiltersModalProps;

        await render(
            <AppProvider>
                <SitePlansFiltersModal {...props} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует фильтр в c задизейбленной кнопкой', async () => {
        const props = ({ ...COMMON_PROPS, isSubmitButtonDisabled: true } as unknown) as ISitePlansFiltersModalProps;

        await render(
            <AppProvider>
                <SitePlansFiltersModal {...props} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
