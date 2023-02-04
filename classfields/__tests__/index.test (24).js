import * as React from 'react';
import { shallow } from 'enzyme';

import { OffersListEmpty } from 'view/deskpad/components/common/offers-list-empty';

import { OffersListError } from '../../offers-list-error';
import AgencyOffer from '../../agency-offer';
import { AgencyOffersListComponent } from '../';

jest.mock('view/lib/link');

const commonProps = {
    reasons: {},
    isFilesDragging: false,
    onDrop: jest.fn(),
    onDragLeave: jest.fn(),
    onRetry: jest.fn(),
    clearFilters: jest.fn()
};

const offers = [ { id: '1' }, { id: '2' }, { id: '3' } ];

const loadedProps = {
    ...commonProps,
    status: 'loaded',
    offers,
    offersFound: offers.length
};

describe('AgencyOffersList', () => {
    it('renders error state', () => {
        const wrapper = shallow(
            <AgencyOffersListComponent
                {...commonProps}
                status='errored'
                offers={[]}
                offersFound={0}
                isManualAddingAvailable
            />
        );

        expect(wrapper.find(OffersListError).length).toBe(1);
    });

    it('renders empty state', () => {
        const wrapper = shallow(
            <AgencyOffersListComponent
                {...commonProps}
                status='loaded'
                offers={[]}
                offersFound={0}
                isManualAddingAvailable
            />
        );

        expect(wrapper.find(OffersListEmpty).length).toBe(1);
    });

    it('renders offers', () => {
        const wrapper = shallow(
            <AgencyOffersListComponent
                {...loadedProps}
                isManualAddingAvailable
            />
        );

        expect(wrapper.find(AgencyOffer).length).toBe(3);
    });

    describe('add new offer button', () => {
        it('is visible', () => {
            const wrapper = shallow(
                <AgencyOffersListComponent
                    {...loadedProps}
                    isManualAddingAvailable
                />
            );

            expect(wrapper.find('.agency-offers-list__add-new').length).toBe(1);
        });

        it('is hidden', () => {
            const wrapper = shallow(
                <AgencyOffersListComponent
                    {...loadedProps}
                    isManualAddingAvailable={false}
                />
            );

            expect(wrapper.find('.agency-offers-list__add-new').length).toBe(0);
        });
    });
});
