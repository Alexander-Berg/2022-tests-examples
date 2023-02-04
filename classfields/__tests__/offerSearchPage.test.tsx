import React from 'react';
import { shallow } from 'enzyme';

import { OffersSearchPage } from '../index';

import { initialStore } from './mock';

describe('В разметке h1 и seoText рендерятся над другими элементами страницы', () => {
    it('рендрер для типы страницы "Самолет"', async () => {
        const wrapper = shallow(<OffersSearchPage {...initialStore} />).find(
            '.OffersSearchPage__specialProjectContent'
        );

        expect(wrapper.childAt(1).is('h1')).toBe(true);
        expect(wrapper.childAt(2).is('.OffersSearchPage__content')).toBe(true);

        const adsWrapper = wrapper.find('.OffersSearchPage__content').dive();
        expect(adsWrapper.childAt(0).is('AsideAds')).toBe(true);
    });

    it('рендер для страницы без промо', () => {
        const store = {
            ...initialStore,
            siteSpecialProjectSecondPackage: null,
        };

        const wrapper = shallow(<OffersSearchPage {...store} />)
            .find('.OffersSearchPage')
            .find('ContentWrapper');

        expect(wrapper.childAt(0).is('ContentCol')).toBe(true);

        const adsWrapper = wrapper.find('ContentCol').dive();
        expect(adsWrapper.childAt(0).is('AsideAds')).toBe(true);
    });
});
