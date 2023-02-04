import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { AppProvider } from 'view/libs/test-helpers';
import i18n from 'view/components/Selection/i18n';

import { SelectionCardContainer } from '../container';
import { SelectionCardType } from '../types';

const renderOptions = [{ viewport: { width: 450, height: 250 } }, { viewport: { width: 650, height: 400 } }];

const getStore = (isSkeleton?: boolean) => {
    if (isSkeleton) {
        return {
            spa: {
                status: RequestStatus.PENDING,
            },
        };
    }

    return {
        spa: {
            status: RequestStatus.LOADED,
        },
    };
};

const Component: React.FunctionComponent<{
    title: string;
    type: SelectionCardType;
    disabled?: boolean;
    isSoon?: boolean;
    isSkeleton?: boolean;
}> = ({ title, type, disabled, isSoon, isSkeleton }) => (
    <AppProvider initialState={getStore(isSkeleton)}>
        <SelectionCardContainer title={title} type={type} disabled={disabled} isSoon={isSoon} targetUrl="selection" />
    </AppProvider>
);

describe('SelectionCard', () => {
    renderOptions.forEach((renderOption) => {
        it(`Сдать ${renderOption.viewport.width} px`, async () => {
            await render(<Component title={i18n('title.LEND')} type={SelectionCardType.LEND} />, renderOption);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Снять ${renderOption.viewport.width} px`, async () => {
            await render(<Component title={i18n('title.RENT')} type={SelectionCardType.RENT} />, renderOption);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Сдать disabled ${renderOption.viewport.width} px`, async () => {
            await render(<Component title={i18n('title.LEND')} type={SelectionCardType.LEND} disabled />, renderOption);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Сдать disabled с бэйджиком ${renderOption.viewport.width} px`, async () => {
            await render(
                <Component title={i18n('title.LEND')} type={SelectionCardType.LEND} disabled isSoon />,
                renderOption
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Скелетон ${renderOption.viewport.width} px`, async () => {
            await render(
                <Component title={i18n('title.LEND')} type={SelectionCardType.LEND} isSkeleton />,
                renderOption
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
