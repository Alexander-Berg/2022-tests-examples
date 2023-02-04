/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { Props, State } from './HeaderMainNav';
import HeaderMainNav from './HeaderMainNav';

let props: Props;

beforeEach(() => {
    props = {
        cookies: {},
        isWideLayout: false,
        pageParams: {
            foo: 'bar',
            mark: 'ferrari',
        },
        pageType: 'index',
        setCookieForever: () => {},
    };

    jest.useFakeTimers();
});

it('правильно формирует линки в меню легковых на обычной странице', () => {
    const page = shallowRenderComponent({ props });
    page.find('HeaderDropdownMenu').simulate('mouseenter', { currentTarget: { getAttribute: () => 'cars' } });
    jest.advanceTimersByTime(300);

    const links = page.find('Link');
    expect(links).toMatchSnapshot();
});

it('правильно формирует линки в меню легковых на обычной странице с авторизацией под лилером', () => {
    const page = shallowRenderComponent({
        props: {
            ...props,
            isDealerUser: true,
        },
    });
    page.find('HeaderDropdownMenu').simulate('mouseenter', { currentTarget: { getAttribute: () => 'cars' } });
    jest.advanceTimersByTime(300);

    const links = page.find('Link');
    expect(links).toMatchSnapshot();
});

describe('Ссылка на Выкуп', () => {
    it('Рендерится, если передан флаг', () => {
        const page = shallowRenderComponent({ props: { ...props, isBuyoutLinkVisible: true } });

        const garageItem = page.find('li').get(7);
        const c2bItem = page.find('li').get(8);
        const magItem = page.find('li').get(9);

        expect(garageItem.props['data-id']).toBe('garage-new');
        expect(c2bItem.props['data-id']).toBe('c2b-promo');
        expect(magItem.props['data-id']).toBe('mag');
    });

    it('Не рендерится, если не передан соответствующий флаг', () => {
        const page = shallowRenderComponent({ props });

        const garageItem = page.find('li').get(7);
        const magItem = page.find('li').get(8);

        expect(garageItem.props['data-id']).toBe('garage-new');
        expect(magItem.props['data-id']).toBe('mag');
    });

    it('Рендерится с точкой', () => {
        const page = shallowRenderComponent({ props: { ...props, isBuyoutLinkVisible: true } });

        const c2bItem = page.find('li').get(7);
        expect(c2bItem.props.className).toBe('HeaderMainNav__item HeaderMainNav__item_dot');
    });

    it('Выставляет куку при клике на ссылку', () => {
        const setCookieForeverMock = jest.fn();
        const page = shallowRenderComponent({ props: { ...props, isBuyoutLinkVisible: true, setCookieForever: setCookieForeverMock } });

        const c2bItem = page.findWhere((node) => node.name() === 'Link' && node.prop('children') === 'Выкуп');

        c2bItem.simulate('click');

        expect(setCookieForeverMock).toHaveBeenCalledTimes(1);
        expect(setCookieForeverMock).toHaveBeenLastCalledWith('navigation_dot_seen-c2b-promo-dot', 'true');
    });

    it('Рендерится без точки при наличии куки', () => {
        const newProps = {
            ...props,
            isBuyoutLinkVisible: true,
            cookies: {
                'navigation_dot_seen-c2b-promo-dot': 'true',
            },
        };

        const page = shallowRenderComponent({ props: newProps });

        const c2bItem = page.find('li').get(9);
        expect(c2bItem.props.className).toBe('HeaderMainNav__item');
    });
});

describe('промо-прыщи', () => {
    class HeaderMainNavWithPromo extends HeaderMainNav {
        getNavigationItems() {
            const items = super.getNavigationItems();
            items[0].hasPromoDot = true;
            items[0].id = 'cars';
            items[0].isActive = (pageType, pageParams) => (pageType === 'listing' && pageParams.category === 'cars');

            return items;
        }
    }

    it('должен отрисовать ссылку с прыщом, если нет куки', () => {
        const page = shallow(
            <HeaderMainNavWithPromo { ...props }/>,
            { context: contextMock },
        );
        const carsItem = page.find('li').get(0);
        expect(carsItem.props.className).toBe('HeaderMainNav__item HeaderMainNav__item_dot');
    });

    it('должен отрисовать ссылку без прыща, если есть кука', () => {
        const newProps = {
            ...props,
            cookies: {
                'navigation_dot_seen-cars-dot': 'true',
            },
        };
        const page = shallow(
            <HeaderMainNavWithPromo { ...newProps }/>,
            { context: contextMock },
        );
        const carsItem = page.find('li').get(0);
        expect(carsItem.props.className).toBe('HeaderMainNav__item');
    });

    it('должен отрисовать ссылку активной страницы без прыща', () => {
        const newProps = {
            ...props,
            cookies: {},
            pageType: 'listing',
            pageParams: {
                category: 'cars',
            },
        };
        const page = shallow(
            <HeaderMainNavWithPromo { ...newProps }/>,
            { context: contextMock },
        );
        const carsItem = page.find('li').get(0);
        expect(carsItem.props.className).toBe('HeaderMainNav__item');
    });

    it('должен выставить куку при заходе на страницу, у неё есть прыщ', () => {
        const setCookieForever = jest.fn();

        const newProps = {
            ...props,
            pageParams: {
                category: 'cars',
            },
            pageType: 'listing',
            setCookieForever,
        };

        shallow(
            <HeaderMainNavWithPromo { ...newProps }/>,
            { context: contextMock },
        );

        expect(setCookieForever).toHaveBeenCalledWith('navigation_dot_seen-cars-dot', 'true');
        expect(setCookieForever).toHaveBeenCalledTimes(1);
    });

    it('не должен выставлять куку при заходе на страницу, у неё нет прыща', () => {
        const setCookieForever = jest.fn();

        const newProps = {
            ...props,
            pageType: 'history',
            setCookieForever,
        };

        shallow(
            <HeaderMainNavWithPromo { ...newProps }/>,
            { context: contextMock },
        );

        expect(setCookieForever).toHaveBeenCalledTimes(0);
    });

});

describe('должен закрыть меню после клика', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    it('на кузов тачки', () => {
        const page = shallowRenderComponent({ props });
        page.setState({
            isMenuOpened: true,
            menuId: 'cars',
        });
        page.find('.HeaderMainNav__subLink').first().simulate('click');
        jest.runAllTimers();
        const state = page.state() as State;
        expect(state.isMenuOpened).toEqual(false);
    });

    it('на ссылку', () => {
        const page = shallowRenderComponent({ props });
        page.setState({
            isMenuOpened: true,
            menuId: 'cars',
        });
        page.find('.HeaderMainNav__sideNavLink').first().simulate('click');
        jest.runAllTimers();
        const state = page.state() as State;
        expect(state.isMenuOpened).toEqual(false);
    });
});

describe('ссылка на отчёты с метриками', () => {
    it('рендерится как ПроАвто', () => {
        const page = shallow(
            <HeaderMainNav { ...props }/>,
            { context: contextMock },
        );

        const link = page.find('[data-id="history"] .HeaderMainNav__itemLink').dive().find('a');
        expect(link.text()).toEqual('ПроАвто');

        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tabs', 'history' ]);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'history-click', 'title' ]);
    });

    it('в экспе рендерится как Отчёты', () => {
        const page = shallow(
            <HeaderMainNav { ...props }/>,
            {
                context: {
                    ...contextMock,
                    hasExperiment: (flag: string) => flag === 'AUTORUFRONT-21544_rename_proauto',
                },
            },
        );

        const link = page.find('[data-id="history"] .HeaderMainNav__itemLink').dive().find('a');
        expect(link.text()).toEqual('Отчёты');

        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'tabs', 'history' ]);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'history-click', 'title', 'reports' ]);
    });
});

it('должен отправить метрику показа тултипа у электромобилей', () => {
    const wrapper = shallowRenderComponent({ props });
    wrapper.find('HoveredTooltip').simulate('open');
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'electro-tooltip', 'open' ]);
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <HeaderMainNav { ...props }/>
        ,
        { context: contextMock },
    );

    return page;
}
