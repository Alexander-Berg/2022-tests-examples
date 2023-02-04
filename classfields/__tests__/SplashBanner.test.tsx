import React from 'react';
import { shallow } from 'enzyme';

import { PageName } from 'realty-core/types/router';
import { reachGoal } from 'realty-core/view/common/libs/metrika';
jest.mock('realty-core/view/common/libs/metrika', () => ({
    reachGoal: jest.fn(),
}));

import { SplashBanner } from '../index';

const CONFIG_FIELD_MOCK = {
    rootUrl: '',
    retpath: '',
};

describe('SplashBanner', () => {
    const page: PageName = 'index';

    it('Правильно строится ссылка в сплеше', () => {
        const pageParams: Record<string, string> = {
            appBannerType: 'houseWhite',
        };
        const wrapper = shallow(
            <SplashBanner
                config={{
                    serverTimeStamp: 1,
                    ...CONFIG_FIELD_MOCK,
                }}
                pageName={page}
                pageParams={pageParams}
            />
        );

        const link = wrapper.find('.SplashBanner__footer').find('.SplashBanner__button').first().prop('url');

        expect(link).toBe(
            'https://bzfk.adj.st/?adjust_t=wsudr1e_e8tc29m&adjust_campaign=touch&adjust_adgroup=house_white&adjust_creative=splash_banner_install_button&adjust_fallback='
        );
    });
});

describe('Правильно отправляет метрику', () => {
    const pageParams: Record<string, string> = {
        appBannerType: 'houseWhite',
    };

    const wrapper = shallow(
        <SplashBanner
            config={{
                serverTimeStamp: 1,
                ...CONFIG_FIELD_MOCK,
            }}
            pageName="index"
            pageParams={pageParams}
        />
    );

    it('При первом рендере компонента отправляется цель show', () => {
        expect(reachGoal).toHaveBeenCalledWith(
            'promo_banner',
            expect.objectContaining({
                bannerName: 'houseWhite',
                event: 'show',
                pageName: 'index',
                type: 'splash',
            })
        );
    });

    it('При переходе в маркет отправляется цель click', () => {
        const submitBtn = wrapper.find('.SplashBanner__footer').find('.SplashBanner__button').first();
        submitBtn.simulate('click');

        expect(reachGoal).toHaveBeenLastCalledWith(
            'promo_banner',
            expect.objectContaining({
                bannerName: 'houseWhite',
                event: 'click',
                pageName: 'index',
                type: 'splash',
            })
        );
    });

    it('При закрытии окна отправляется цель close', () => {
        const closeBtn = wrapper.find('.SplashBanner__footer').find('.SplashBanner__button').at(1);
        closeBtn.simulate('click');

        expect(reachGoal).toHaveBeenLastCalledWith(
            'promo_banner',
            expect.objectContaining({
                bannerName: 'houseWhite',
                event: 'close',
                pageName: 'index',
                type: 'splash',
            })
        );
    });
});
