/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

jest.mock('auto-core/lib/event-log/statApi');
jest.mock('auto-core/react/lib/localStatData');
import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';

import statApi from 'auto-core/lib/event-log/statApi';

import localStatData from 'auto-core/react/lib/localStatData';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Props } from './ListingItemStatLogger';
import ListingItemStatLogger from './ListingItemStatLogger';

const logImmediately = statApi.logImmediately as jest.MockedFunction<typeof statApi.logImmediately>;
const log = statApi.log as jest.MockedFunction<typeof statApi.log>;
const saveSearchClickInfo = localStatData.saveSearchClickInfo as jest.MockedFunction<typeof localStatData.saveSearchClickInfo>;

class TestListingItemStatLogger extends ListingItemStatLogger<Props> {
    render() {
        return null;
    }
}

beforeEach(() => {
    logImmediately.mockClear();
    log.mockClear();
});

it('на клик отправляет page, page_size и index правильно и записывает данные в ls', () => {
    const wrapper = shallow<TestListingItemStatLogger>(
        <TestListingItemStatLogger
            contextBlock={ ContextBlock.BLOCK_LISTING }
            contextPage={ ContextPage.PAGE_LISTING }
            index={ 4 }
            offer={{ category: 'cars' } as Offer}
            searchID="search_id"
            page={ 10 }
            pageSize={ 37 }
        />,
        { context: contextMock },
    );
    wrapper.instance().sendStatOnClick();
    expect(logImmediately).toHaveBeenCalledTimes(1);
    expect(logImmediately.mock.calls[0][0]).toMatchSnapshot();
    expect(saveSearchClickInfo)
        .toHaveBeenCalledWith('search_id', { category: 'cars' }, { block: 'BLOCK_LISTING', page: 'PAGE_LISTING', self_type: 'TYPE_SINGLE' });
});

it('должен отправить price_from и price_to в sendStatOnIntersectionChange', () => {
    const wrapper = shallow<TestListingItemStatLogger>(
        <TestListingItemStatLogger
            contextBlock={ ContextBlock.BLOCK_LISTING }
            contextPage={ ContextPage.PAGE_LISTING }
            index={ 4 }
            offer={{
                category: 'cars',
                groupping_info: {
                    price_from: { rur_price: 100 },
                    price_to: { rur_price: 200 },
                },
            } as Partial<Offer> as Offer}
            page={ 10 }
            pageSize={ 37 }
        />,
        { context: contextMock },
    );
    wrapper.instance().sendStatOnIntersectionChange(true);
    expect(log).toHaveBeenCalledTimes(1);
    expect(log.mock.calls[0][0].card_show_event?.price_from).toBe('100');
    expect(log.mock.calls[0][0].card_show_event?.price_to).toBe('200');
});

it('ничего не отправляет и не записывает в ls, если передан disableEventsLog', () => {
    const wrapper = shallow<TestListingItemStatLogger>(
        <TestListingItemStatLogger
            contextBlock={ ContextBlock.BLOCK_LISTING }
            contextPage={ ContextPage.PAGE_LISTING }
            index={ 4 }
            offer={{ category: 'cars' } as Offer}
            page={ 10 }
            pageSize={ 37 }
            disableEventsLog
        />,
        { context: contextMock },
    );
    wrapper.instance().sendStatOnClick();
    expect(logImmediately).not.toHaveBeenCalled();
    expect(saveSearchClickInfo).not.toHaveBeenCalled();
});
