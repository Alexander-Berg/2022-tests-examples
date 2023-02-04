import * as React from 'react';
import { shallow } from 'enzyme';

import Spinner from 'vertis-react/components/Spinner';
import { routerProps, loginPropsMock } from 'view/lib/test-helpers';
import OwnerOffersList from 'view/deskpad/components/page/type/management/components/owner-offers-list';
import { AgencyOffersList } from 'view/deskpad/components/page/type/management/components/agency-offers-list';
import { OffersEmpty } from
    'view/deskpad/components/page/type/management/components/offers-new-container/offers-empty';

import { OfferListComponent } from '../';

jest.mock('view/lib/api/stat');

const filters = { fields: {} };

const commonProps = {
    ...routerProps,
    ...loginPropsMock,
    dispatch: jest.fn(),
    setCookie: jest.fn(),
    cookies: {},
    isManualAddingAvailable: true,
    reasons: {},
    filters,
    hasFeedOffers: false,
    isTuzLoaded: false,
    isTuzEnabled: false,
    isTuzAvailable: false,
    user: {}
};

// eslint-disable-next-line no-undef
global.fetch = () => Promise.resolve();

describe('OfferList', () => {
    describe('error state', () => {
        const errorProps = {
            ...commonProps,
            status: 'errored',
            isAuth: true,
            isVosUser: true,
            offers: [],
            offersFound: 0,
            hasAnyOffers: false,
            isJuridical: false
        };

        it('renders offers list for owner', () => {
            const wrapper = shallow(
                <OfferListComponent
                    {...errorProps}
                    userType='OWNER'
                />
            );

            expect(wrapper.find(OwnerOffersList).length).toBe(1);
        });

        it('renders offers list for agency', () => {
            const wrapper = shallow(
                <OfferListComponent
                    {...errorProps}
                    userType='AGENCY'
                />
            );

            expect(wrapper.find(AgencyOffersList).length).toBe(1);
        });
    });

    it('renders spinner and offers list in loading state', () => {
        const wrapper = shallow(
            <OfferListComponent
                {...commonProps}
                status='pending'
                isAuth
                isVosUser
                userType='OWNER'
                offers={[]}
                offersFound={0}
                hasAnyOffers={false}
                isJuridical={false}
            />
        );

        expect(wrapper.find(Spinner).length).toBe(1);
        expect(wrapper.find(OwnerOffersList).length).toBe(1);
    });

    describe('empty state', () => {
        const emptyProps = {
            ...commonProps,
            status: 'loaded',
            offers: [],
            offersFound: 0,
            hasAnyOffers: false,
            isJuridical: false
        };

        it('renders no offers message for owner', () => {
            const wrapper = shallow(
                <OfferListComponent
                    {...emptyProps}
                    isAuth
                    isVosUser
                    userType='OWNER'
                />
            );

            expect(wrapper.find(OffersEmpty).length).toBe(1);
        });

        it('renders no offers message for agency', () => {
            const wrapper = shallow(
                <OfferListComponent
                    {...emptyProps}
                    isAuth
                    isVosUser
                    userType='AGENCY'
                />
            );

            expect(wrapper.find(OffersEmpty).length).toBe(1);
        });

        it('renders no offers message for unauthorized user', () => {
            const wrapper = shallow(
                <OfferListComponent
                    {...emptyProps}
                    status='errored'
                    isAuth={false}
                    isVosUser={false}
                    userType='OWNER'
                />
            );

            expect(wrapper.find(OffersEmpty).length).toBe(1);
        });

        it('renders no offers message for user without vos', () => {
            const wrapper = shallow(
                <OfferListComponent
                    {...emptyProps}
                    status='errored'
                    isAuth
                    isVosUser={false}
                    userType='OWNER'
                />
            );

            expect(wrapper.find(OffersEmpty).length).toBe(1);
        });
    });

    it('renders offers list if all offers are deleted', () => {
        const offers = [ { id: '1' } ];
        const wrapper = shallow(
            <OfferListComponent
                {...commonProps}
                userType='OWNER'
                status='loaded'
                isAuth
                isVosUser
                offers={offers}
                offersFound={0}
                hasAnyOffers={false}
                isJuridical={false}
            />
        );

        expect(wrapper.find(OwnerOffersList).length).toBe(1);
    });
});
