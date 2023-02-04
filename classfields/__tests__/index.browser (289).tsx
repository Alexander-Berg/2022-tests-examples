import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import { SamoletFiltersContainer } from '../container';

import { state, filledState } from './mocks';

const AutoOpenChildrenComponent: React.FC<{ handleFiltersShow(): void }> = ({ handleFiltersShow }) => {
    React.useEffect(() => {
        handleFiltersShow();
    }, [handleFiltersShow]);

    return null;
};

const render = (props: IAppProviderProps) =>
    _render(
        <AppProvider
            {...props}
            fakeTimers={{
                now: new Date('2021-06-02T03:00:00.111Z').getTime(),
            }}
        >
            <SamoletFiltersContainer>{(props) => <AutoOpenChildrenComponent {...props} />}</SamoletFiltersContainer>
        </AppProvider>,
        { viewport: { width: 320, height: 700 } }
    );

describe('SamoletFilters', () => {
    it('рендерится корректно', async () => {
        await render({ initialState: state });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно большой экран', async () => {
        await _render(
            <AppProvider initialState={state}>
                <SamoletFiltersContainer>{(props) => <AutoOpenChildrenComponent {...props} />}</SamoletFiltersContainer>
            </AppProvider>,
            { viewport: { width: 375, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно в заполненном состоянии', async () => {
        await render({ initialState: filledState });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('переключение фильтров', async () => {
        await render({ initialState: state });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.RadioGroup .Radio`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.CheckboxGroup:nth-child(1) .Checkbox:first-child`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.CheckboxGroup:nth-child(1) .Checkbox:last-child`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.focus(`.NumberRange:nth-child(2) .TextInput:first-of-type input`);
        await page.keyboard.type('100000');
        await page.focus(`.NumberRange:nth-child(2) .TextInput:last-of-type input`);
        await page.keyboard.type('20000000');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.focus(`.NumberRange:nth-child(3) .TextInput:first-of-type input`);
        await page.keyboard.type('33');
        await page.focus(`.NumberRange:nth-child(3) .TextInput:last-of-type input`);
        await page.keyboard.type('87');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.Select:nth-child(4)`);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
