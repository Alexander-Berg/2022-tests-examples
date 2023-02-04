import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
// eslint-disable-next-line max-len
import { mortgageApplicationFormReducer } from 'realty-core/view/react/modules/mortgage/mortgage-application-form/redux/reducer';

import { MortgageProgramsSerp, IMortgageProgramsSerpProps } from '../';

import { getPrograms } from './mocks';

const commonProps = {
    propertyCost: 0,
    downPaymentSum: 0,
    periodYears: 0,
    user: {} as never,
    subjectFederationRgid: 1,
    pageType: 'mortgage-search',
    pageParams: {},
    isMoreLoading: false,
};

const Component = (props: IMortgageProgramsSerpProps) => (
    <AppProvider rootReducer={createRootReducer({ mortgageApplicationForm: mortgageApplicationFormReducer })}>
        <MortgageProgramsSerp {...props} />
    </AppProvider>
);

describe('MortgageProgramsSerp', () => {
    it('Рисует выдачу', async () => {
        await render(<Component mortgagePrograms={getPrograms(2)} totalPrograms={2} pageSize={2} {...commonProps} />, {
            viewport: { width: 360, height: 900 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует выдачу без сортировки', async () => {
        await render(
            <Component mortgagePrograms={getPrograms(2)} totalPrograms={2} pageSize={2} disableSort {...commonProps} />,
            { viewport: { width: 360, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует выдачу с открытой сортировкой', async () => {
        await render(<Component mortgagePrograms={getPrograms(1)} totalPrograms={1} pageSize={1} {...commonProps} />, {
            viewport: { width: 360, height: 500 },
        });

        await page.click('.Select__button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует выдачу с кнопкой "Показать еще"', async () => {
        await render(<Component mortgagePrograms={getPrograms(2)} totalPrograms={3} pageSize={2} {...commonProps} />, {
            viewport: { width: 360, height: 960 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует пустую выдачу', async () => {
        await render(<Component mortgagePrograms={[]} totalPrograms={0} pageSize={2} {...commonProps} />, {
            viewport: { width: 360, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует выдачу в состоянии загрузки', async () => {
        await render(
            <Component
                mortgagePrograms={getPrograms(2)}
                totalPrograms={3}
                pageSize={2}
                {...commonProps}
                isMoreLoading
            />,
            { viewport: { width: 360, height: 960 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
