import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, proxyGate } from 'realty-core/view/react/libs/test-helpers';
import actions from 'realty-core/view/react/common/actions/page';
import { AnyObject, AnyFunction } from 'realty-core/types/utils';

import { mortgageReducer, IMortgageStore } from 'view/react/deskpad/reducers/roots/mortgage';
import filtersStyles from 'view/react/deskpad/components/MortgageFilters/styles.module.css';
import serpStyles from 'view/react/deskpad/components/serps/MortgageProgramsSerp/styles.module.css';

import { MortgageSearchContainer } from '../container';

import { getMockGate, getStore, fullSearchQuery } from './mocks';

async function clearSliderInputAndType(selector: string, text: string) {
    const inputValue = await page.$eval(selector, (el) => (el as HTMLInputElement).value);

    await page.focus(selector);
    await Promise.all(inputValue.split('').map(() => page.keyboard.press('Backspace')));

    return page.keyboard.type(text);
}

const selectors = {
    outside: `body`,
    resetButton: `.${filtersStyles.resetButton}`,
    moreButton: `.${filtersStyles.actions} button:first-of-type`,
    submitButton: `.${filtersStyles.submitButton}`,
    filters: {
        selectFlatType: `.${filtersStyles.selectRow}:first-child .Select:first-child`,
        selectMortgageType: `.${filtersStyles.selectRow}:first-child .Select:nth-child(2)`,
        getSliderInputSelector: (n: number) =>
            `.${filtersStyles.sliderRow} .SliderInput2:nth-child(${n}) .SliderInput2__input`,
        rateFrom: `.${filtersStyles.rate} .NumberRange__input:first-of-type input`,
        rateTo: `.${filtersStyles.rate} .NumberRange__input:last-of-type input`,
        selectBank: `.${filtersStyles.banks}`,
        getBorrowerChips: (n: number) => `.${filtersStyles.chipsRow}:nth-child(4) .Checkbox:nth-child(${n})`,
        getIncomeChips: (n: number) => `.${filtersStyles.chipsRow}:nth-child(5) .Radio:nth-child(${n})`,
        maternityCapitalChips: `.${filtersStyles.chipsRow}:nth-child(6) .Checkbox`,
    },
    getSelectMenuItem: (n: number) => `.Popup_visible .Menu .Menu__item:nth-of-type(${n})`,
    showMoreButton: `.${serpStyles.showMore}`,
};

const withTestContext = () => (WrappedComponent: React.ComponentType) => {
    class WithTestContext extends React.PureComponent<{ load: AnyFunction; Gate: AnyObject }> {
        static childContextTypes = {
            router: PropTypes.object.isRequired,
            load: PropTypes.func.isRequired,
        };

        getChildContext() {
            return {
                load: (...args: unknown[]) => this.props.load('', ...args, undefined, undefined, this.props.Gate),
                router: {
                    params: {},
                    createHref: () => undefined,
                    push: (args: string) => {
                        const [type, params]: [type: string, params: Record<string, string>] = JSON.parse(args);

                        if (type !== 'mortgage-search') {
                            return;
                        }

                        this.props.load(type, params, ['mortgage-programs'], undefined, undefined, this.props.Gate);
                    },
                    replace: () => undefined,
                    go: () => undefined,
                    goBack: () => undefined,
                    goForward: () => undefined,
                    setRouteLeaveHook: () => undefined,
                    getCurrentLocation: () => ({}),
                    isActive: () => false,
                    location: {
                        pathname: 'pathname',
                        search: '',
                        query: {},
                        hash: '',
                    },
                },
            };
        }

        render() {
            return <WrappedComponent {...this.props} />;
        }
    }

    return connect(null, {
        load: actions.load,
    })(WithTestContext);
};

const MortgageSearchContainerWithTestContext = withTestContext()(MortgageSearchContainer);

const Component: React.FunctionComponent<{
    store?: DeepPartial<IMortgageStore>;
    Gate?: AnyObject;
}> = ({ store, Gate = getMockGate() }) => (
    <AppProvider
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        rootReducer={mortgageReducer as any}
        initialState={store || getStore()}
        context={{
            link: (...args: unknown[]) => JSON.stringify(args),
        }}
        Gate={Gate}
    >
        <MortgageSearchContainerWithTestContext Gate={proxyGate(Gate)} />
    </AppProvider>
);

const viewport = { width: 1400, height: 1500 };

describe('MortgageSearch', () => {
    it('Базовое состояние', async () => {
        await render(<Component />, { viewport });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    describe('Фильтры', () => {
        it('сброс', async () => {
            await render(
                <Component
                    store={getStore({
                        searchQuery: fullSearchQuery,
                        programsAmount: 7,
                    })}
                    Gate={getMockGate([25])}
                />,
                {
                    viewport,
                }
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.resetButton);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('смена (обычные)', async () => {
            await render(<Component Gate={getMockGate([23, 7, 14, 16, 10, 8])} />, {
                viewport,
            });

            await page.click(selectors.filters.selectFlatType);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.getSelectMenuItem(1));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.selectMortgageType);
            await page.click(selectors.getSelectMenuItem(5));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.selectMortgageType);
            await page.click(selectors.getSelectMenuItem(2));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await clearSliderInputAndType(selectors.filters.getSliderInputSelector(1), '15530000');
            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await clearSliderInputAndType(selectors.filters.getSliderInputSelector(2), '5000100');
            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await clearSliderInputAndType(selectors.filters.getSliderInputSelector(3), '25');
            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('смена (расширенные)', async () => {
            await render(<Component Gate={getMockGate([3], [23, 19, 7, 12, 10, 11, 8, 3])} />, {
                viewport,
            });

            await page.click(selectors.moreButton);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.focus(selectors.filters.rateFrom);
            await page.keyboard.type('2');
            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.focus(selectors.filters.rateTo);
            await page.keyboard.type('7');
            await page.click(selectors.outside);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.selectBank);
            await page.click(selectors.getSelectMenuItem(3));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.getSelectMenuItem(12));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.getBorrowerChips(2));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.getBorrowerChips(3));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.getIncomeChips(4));
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.filters.maternityCapitalChips);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.submitButton);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('подскролл', async () => {
            await render(
                <Component
                    store={getStore({
                        searchQuery: fullSearchQuery,
                        programsAmount: 7,
                    })}
                />,
                {
                    viewport: { width: 1050, height: 1000 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.submitButton);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('Подгрузка', async () => {
        await render(<Component Gate={getMockGate([5])} />, { viewport });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(selectors.showMoreButton);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
