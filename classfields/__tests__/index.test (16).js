import { shallow } from 'enzyme';
import Link from 'vertis-react/components/Link';
import { withContext } from 'view/lib/test-helpers';
import { CallsHistoryComponent } from '../';

const hasGraph = wrapper => wrapper.find('.calls-history__graph').exists();
const hasTotal = wrapper => wrapper.find('.calls-history__item_header').exists();
const hasTable = wrapper => wrapper.find('.calls-history__toggle').exists();
const hasError = wrapper => wrapper.find('.calls-history__error').exists();

const onRetry = jest.fn();

const commonProps = {
    periodFilters: {},
    calltrackingFilters: {},
    hasRedirectPhones: true,
    periodToShow: {},
    isTuzAvailable: false,
    isTuzEnabled: false,
    onOpenOfferCallComplaintPopup: () => {},
    mandatoryPaymentRgidMap: {},
    onRetry
};

const callsHistorySuccess = {
    status: 'success',
    statsByDay: [ {
        interval: {
            from: '2019-06-01T21:00:00Z',
            to: '2019-06-02T21:00:00Z'
        },
        stat: {}
    } ]
};

const callsHistoryErrored = {
    status: 'errored',
    statsByDay: []
};

const viewsDataSuccess = {
    status: 'success',
    totalShows: {},
    aggregatedShows: []
};

const viewsDataErrored = {
    status: 'errored',
    totalShows: {},
    aggregatedShows: []
};

describe('CallsHistory', () => {
    afterEach(() => {
        onRetry.mockReset();
    });

    it('renders graph and table on successful fetch', () => {
        const wrapper = withContext(shallow,
            <CallsHistoryComponent
                {...commonProps}
                callsHistory={callsHistorySuccess}
                viewsData={viewsDataSuccess}
            />
        );

        expect(hasGraph(wrapper)).toBe(true);
        expect(hasTotal(wrapper)).toBe(true);
        expect(hasTable(wrapper)).toBe(true);
        expect(hasError(wrapper)).toBe(false);
    });

    it('renders error on calls and views fetch fail', () => {
        const wrapper = withContext(shallow,
            <CallsHistoryComponent
                {...commonProps}
                callsHistory={callsHistoryErrored}
                viewsData={viewsDataErrored}
            />
        );

        expect(hasGraph(wrapper)).toBe(false);
        expect(hasTotal(wrapper)).toBe(false);
        expect(hasTable(wrapper)).toBe(false);
        expect(hasError(wrapper)).toBe(true);

        wrapper.find('.calls-history__error').find(Link).simulate('click');

        expect(onRetry).toBeCalled();
    });

    it('renders error on calls fetch fail', () => {
        const wrapper = withContext(shallow,
            <CallsHistoryComponent
                {...commonProps}
                callsHistory={callsHistoryErrored}
                viewsData={viewsDataSuccess}
            />
        );

        expect(hasGraph(wrapper)).toBe(false);
        expect(hasTotal(wrapper)).toBe(false);
        expect(hasTable(wrapper)).toBe(false);
        expect(hasError(wrapper)).toBe(true);
    });

    it('renders error and table on views fetch fail', () => {
        const wrapper = withContext(shallow,
            <CallsHistoryComponent
                {...commonProps}
                callsHistory={callsHistorySuccess}
                viewsData={viewsDataErrored}
            />
        );

        expect(hasGraph(wrapper)).toBe(false);
        expect(hasTotal(wrapper)).toBe(true);
        expect(hasTable(wrapper)).toBe(true);
        expect(hasError(wrapper)).toBe(true);
    });
});
