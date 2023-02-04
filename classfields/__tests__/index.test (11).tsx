import React from 'react';

import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import GeoLinks, { IGeoLinksProps } from '../';

import { offerWithHome, offerWithoutPosibilityToBuildLinks } from './mocks';

describe('GeoLinks', () => {
    const component = (props: IGeoLinksProps) =>
        mount(
            <AppProvider>
                <GeoLinks {...props} />
            </AppProvider>
        );

    it('Должен разбить адрес на составные части ( Город, улица, дом... ), когда есть данные', () => {
        const wrapper = component({ offer: offerWithHome });

        expect(wrapper).toMatchSnapshot();
    });

    it('Должен оставить адрес как есть, если не совпадают geocoder и address', () => {
        const wrapper = component({ offer: offerWithoutPosibilityToBuildLinks });

        expect(wrapper).toMatchSnapshot();
    });
});
