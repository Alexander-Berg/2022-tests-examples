import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import HeaderSitemap from './HeaderSitemap';

it('правильно формирует линки сайтмапа', () => {
    const page = shallow(
        <HeaderSitemap pageType="index" url="/" rel="nofollow" className="HeaderSitemap"/>
        ,
        { context: contextMock },
    );
    const links = page.find('.HeaderSitemap__itemLink');
    expect(links).toMatchSnapshot();
});

describe('Ссылка на Выкуп', () => {
    it('Рендерится, если передан флаг', () => {
        const page = shallow(
            <HeaderSitemap pageType="index" url="/" rel="nofollow" className="HeaderSitemap" isBuyoutLinkVisible={ true }/>
            ,
            { context: contextMock },
        );

        const links = page.find('.HeaderSitemap__itemLink');
        const safeDealLink = links.get(7);
        const nextToSafeDealLink = links.get(8);

        expect(safeDealLink.props.children).toBe('Безопасная сделка');
        expect(nextToSafeDealLink.props.children).toBe('Выкуп');

        page.find('.HeaderSitemap__itemLink[href="link/c2b-auction-landing/?"]').simulate('click');

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenLastCalledWith([ 'header', 'buter-menu', 'buyout', 'click' ]);
    });

    it('Не рендерится, если не передан соответствующий флаг', () => {
        const page = shallow(
            <HeaderSitemap pageType="index" url="/" rel="nofollow" className="HeaderSitemap"/>
            ,
            { context: contextMock },
        );

        const links = page.find('.HeaderSitemap__itemLink');
        const safeDealLink = links.get(7);
        const nextToSafeDealLink = links.get(8);

        expect(safeDealLink.props.children).toBe('Безопасная сделка');
        expect(nextToSafeDealLink.props.children).not.toBe('Выкуп');
    });
});

describe('Моя тачка', () => {
    it('должен добавить ссылку "Моя тачка" на карточке легковых', () => {
        const wrapper = shallow(
            <HeaderSitemap
                className=""
                pageType="card"
                url="/cars/used/sale/vaz/2170/1113786050-e92ae383/?foo=bar"
            />,
            { context: { ...contextMock } },
        );
        const myCarLink = wrapper.find('.HeaderSitemap__itemLink').filterWhere((item) => item.prop('href')!.startsWith('/login/'));

        expect(myCarLink).toHaveProp('children', 'Моя тачка!');
        expect(myCarLink).toHaveProp(
            'href', '/login/cars/1113786050/?r=%2Fcars%2Fused%2Fsale%2Fvaz%2F2170%2F1113786050-e92ae383%2F%3Ffoo%3Dbar',
        );
    });

    it('должен добавить ссылку "Моя тачка" на карточке LCV', () => {
        const wrapper = shallow(
            <HeaderSitemap
                className=""
                pageType="card"
                url="/lcv/used/sale/mazda/bongo/19224629-8b5459b4/?foo=bar"
            />,
            { context: { ...contextMock } },
        );
        const myCarLink = wrapper.find('.HeaderSitemap__itemLink').filterWhere((item) => item.prop('href')!.startsWith('/login/'));

        expect(myCarLink).toHaveProp('children', 'Моя тачка!');
        expect(myCarLink).toHaveProp(
            'href', '/login/trucks/19224629/?r=%2Flcv%2Fused%2Fsale%2Fmazda%2Fbongo%2F19224629-8b5459b4%2F%3Ffoo%3Dbar',
        );
    });

    it('не должен добавить ссылку "Моя тачка" на карточке новых', () => {
        const wrapper = shallow(
            <HeaderSitemap
                className=""
                pageType="card-new"
                url="/cars/new/group/bmw/x5/21718431/21935248/1098091482-55e74d29/?foo=bar"
            />,
            { context: { ...contextMock } },
        );
        const myCarLink = wrapper.find('.HeaderSitemap__itemLink').filterWhere((item) => item.prop('href')!.startsWith('/login/'));

        expect(myCarLink).not.toExist();
    });
});

describe('ссылка на отчёты с метриками', () => {
    it('рендерится как ПроАвто', () => {
        const page = shallow(
            <HeaderSitemap pageType="index" url="/" rel="nofollow" className="HeaderSitemap"/>,
            { context: contextMock },
        );

        const link = page.find('.HeaderSitemap__itemLink[href="link/proauto-landing/?"]');
        expect(link.text()).toEqual('ПроАвто');

        link.simulate('click');

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'history-click', 'burger' ]);
    });

    it('в экспе рендерится как Отчёты', () => {
        const page = shallow(
            <HeaderSitemap pageType="index" url="/" rel="nofollow" className="HeaderSitemap"/>,
            {
                context: {
                    ...contextMock,
                    hasExperiment: (flag: string) => flag === 'AUTORUFRONT-21544_rename_proauto',
                },
            },
        );

        const link = page.find('.HeaderSitemap__itemLink[href="link/proauto-landing/?"]');
        expect(link.text()).toEqual('Отчёты');

        link.simulate('click');

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'history-click', 'burger', 'reports' ]);
    });
});
