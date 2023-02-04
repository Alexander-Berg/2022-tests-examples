import React from 'react';
import { shallow } from 'enzyme';
import type { DebouncedFunc } from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { SalesMarkModels, SalesModel } from 'auto-core/react/dataDomain/salesMarkModels/TStateSalesMarkModels';

import type { Props } from './SalesFiltersNewDesign';
import SalesFiltersNewDesign from './SalesFiltersNewDesign';

const salesMarkModels = [ { mark: 'MINI', models: [ { model: 'COOPER', offers_count: 1 } as SalesModel ] } as SalesMarkModels ];

let originalAbortController: typeof AbortController;
const abortMock = jest.fn();
class AbortControllerMock {
    signal = 'abort-signal'

    abort = abortMock
}

beforeEach(() => {
    originalAbortController = global.AbortController;
    global.AbortController = AbortControllerMock as unknown as typeof AbortController;
});

afterEach(() => {
    global.AbortController = originalAbortController;
});

describe('filterSubmit', () => {
    const submitFiltersPromiseSuccess = Promise.resolve();

    function getProps() {
        return {
            isStatsExpanded: false,
            pageParams: {},
            salesMarkModels: salesMarkModels,
            onFiltersSubmit: jest.fn(() => submitFiltersPromiseSuccess),
            onExpandStatsChange: jest.fn(),
            replaceUrl: jest.fn(),
            shouldShowExpandTooltip: false,
            isResellerSales: true,
        };
    }

    it('должен отправить запрос, если изменился фильтр', () => {
        const props = getProps();
        const tree = shallowRenderComponent({ props });

        tree.find('SalesFilterMark').simulate('change', [ 'MINI' ], { name: 'marks' });

        expect(props.onFiltersSubmit).toHaveBeenCalledTimes(1);
        expect(props.onFiltersSubmit).toHaveBeenCalledWith({ mark_model: [ 'MINI' ] }, { signal: 'abort-signal' });
    });

    it('не должен отправлять запрос, если изменился вин, но он не валидный', () => {
        const props = getProps();
        const tree = shallowRenderComponent({ props });

        tree.find('SalesFilterVin').simulate('change', 'AAA', { name: 'vin ' });

        expect(props.onFiltersSubmit).toHaveBeenCalledTimes(0);
    });

    it('должен отправлять запрос, если изменился вин и он валидный', () => {
        const props = getProps();
        const tree = shallowRenderComponent({ props });

        tree.find('SalesFilterVin').simulate('change', 'Z8TND5FS9DM047548', { name: 'vin ' });

        expect(props.onFiltersSubmit).toHaveBeenCalledTimes(1);
        expect(props.onFiltersSubmit).toHaveBeenCalledWith({ vin: 'Z8TND5FS9DM047548' }, { signal: 'abort-signal' });
    });

    it('должен отработать аборт запроса при последовательных изменениях фильтров', () => {
        const abortedPromise1 = Promise.reject({ name: 'AbortError' });
        const abortedPromise2 = Promise.reject({ name: 'AbortError' });

        const props = getProps();
        props.onFiltersSubmit.mockImplementationOnce(() => abortedPromise1);
        props.onFiltersSubmit.mockImplementationOnce(() => abortedPromise2);
        const tree = shallowRenderComponent({ props });

        tree.find('SalesFilterMark').simulate('change', [ 'MINI' ], { name: 'marks' });
        tree.find('SalesFilterVin').simulate('change', 'Z8TND5FS9DM047548', { name: 'vin ' });

        expect(abortMock).toHaveBeenCalledTimes(1);

        return abortedPromise1
            .catch(() => { })
            .then(() => {
                tree.find('PriceFromToFilter').simulate('change', 777, { name: 'price_from ' });
                expect(abortMock).toHaveBeenCalledTimes(2);

                return abortedPromise2
                    .catch(() => { })
                    .then(() => {
                        expect(props.onFiltersSubmit).toHaveBeenCalledTimes(3);
                    });
            });
    });
});

it('выставляет куку при клике на тоггл', () => {
    const expandStatsChange = jest.fn();

    const tree = shallow(
        <SalesFiltersNewDesign
            pageParams={{ category: 'cars' }}
            salesMarkModels={ salesMarkModels }
            onFiltersSubmit={ jest.fn() }
            onExpandStatsChange={ expandStatsChange }
            replaceUrl={ jest.fn() }
            isStatsExpanded={ false }
            shouldShowExpandTooltip={ false }
        />,
        { context: { ...contextMock } },
    );

    tree.find('Toggle').simulate('check', true);

    expect(expandStatsChange).toHaveBeenCalled();
});

describe('при клике на тоггл', () => {
    it('дергает проп', () => {
        const expandStatsChange = jest.fn();

        const tree = shallow(
            <SalesFiltersNewDesign
                pageParams={{ category: 'cars' }}
                salesMarkModels={ salesMarkModels }
                onFiltersSubmit={ jest.fn() }
                onExpandStatsChange={ expandStatsChange }
                replaceUrl={ jest.fn() }
                isStatsExpanded={ false }
                shouldShowExpandTooltip={ false }
            />,
            { context: { ...contextMock } },
        );

        tree.find('Toggle').simulate('check', true);

        expect(expandStatsChange).toHaveBeenCalled();
    });

    it('отправляет корректную метрику если значение true', () => {
        const expandStatsChange = jest.fn();

        const tree = shallow(
            <SalesFiltersNewDesign
                pageParams={{ category: 'cars' }}
                salesMarkModels={ salesMarkModels }
                onFiltersSubmit={ jest.fn() }
                onExpandStatsChange={ expandStatsChange }
                replaceUrl={ jest.fn() }
                isStatsExpanded={ false }
                shouldShowExpandTooltip={ false }
            />,
            { context: { ...contextMock } },
        );

        tree.find('Toggle').simulate('check', true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'everyoffer-stats', 'show' ]);
    });

    it('отправляет корректную метрику если значение false', () => {
        const expandStatsChange = jest.fn();

        const tree = shallow(
            <SalesFiltersNewDesign
                pageParams={{ category: 'cars' }}
                salesMarkModels={ salesMarkModels }
                onFiltersSubmit={ jest.fn() }
                onExpandStatsChange={ expandStatsChange }
                replaceUrl={ jest.fn() }
                isStatsExpanded={ false }
                shouldShowExpandTooltip={ false }
            />,
            { context: { ...contextMock } },
        );

        tree.find('Toggle').simulate('check', false);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'everyoffer-stats', 'close' ]);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    class NonThrottledComponent extends SalesFiltersNewDesign {
        debouncedFiltersSubmit = this.handleFiltersSubmit as DebouncedFunc<() => void>;
    }

    const tree = shallow(
        <NonThrottledComponent { ...props }/>,
        { context: contextMock },
    );

    return tree;
}
