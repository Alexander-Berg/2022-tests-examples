/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import VIN_REPORT from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-nissan.mock';
import type { ReportContentItem } from 'auto-core/react/dataDomain/vinReport/types';

import CardVinReportCompactContent from './CardVinReportCompactContent';
import type { Props } from './CardVinReportCompactContent';

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
const ITEMS = _.cloneDeep(VIN_REPORT.report!.content!.items).slice(0, 21) as Array<ReportContentItem>;

const onItemClick = jest.fn();
const onButtonClick = jest.fn();

const shallowRenderComponent = ({ showAmountHiddenItems, showFreeBlock = true, items = ITEMS }: Partial<Props>) => {
    return shallow(
        <CardVinReportCompactContent
            isOwner={ false }
            showFreeBlock={ showFreeBlock }
            items={ items }
            onItemClick={ onItemClick }
            onShowReportButtonClick={ onButtonClick }
            showAmountHiddenItems={ showAmountHiddenItems }
        />,
    );
};

describe('CardVinReportCompactContent обычный', () => {
    it('правильно обрабатывает нажатие кнопки', () => {
        const wrap = shallowRenderComponent({});
        wrap.find('Button').simulate('click');

        expect(onButtonClick).toHaveBeenCalledTimes(1);
    });

    it('правильно обрабатывает нажатие на пункт бесплатного отчета', () => {
        const wrap = shallowRenderComponent({});
        const item = wrap.find('VinReportFreeBlockItem').first();
        item.simulate('click', 'pts_info');

        expect(onItemClick).toHaveBeenCalledTimes(1);
        expect(window.location.hash).toBe('#block-pts_info');
    });

    it('правильно рендерит "Еще Х пунктов проверки"', () => {
        const wrap = shallowRenderComponent({});
        const item = wrap.find('.CardVinReportCompactContent__item').last()
            .find('.CardVinReportCompactContent__itemText');
        expect(item.children().text()).toBe('Еще 12 пунктов проверки');
    });

    it('правильно кликает на "Еще Х пунктов проверки"', () => {
        const wrap = shallowRenderComponent({});
        const item = wrap.find('.CardVinReportCompactContent__item').last()
            .find('.CardVinReportCompactContent__itemText');
        item.simulate('click');
        expect(wrap.state()).toEqual({ showAllItems: true });
    });

    it('правильно рендерит развернутое оглавление', () => {
        const wrap = shallowRenderComponent({});
        const itemsElements = wrap.find('GridLikeColumn').dive().find('.CardVinReportCompactContent__item');
        expect(itemsElements).toHaveLength(5);
        wrap.setState({ showAllItems: true });
        wrap.update();
        const itemsElementsAll = wrap.find('GridLikeColumn').dive().find('.CardVinReportCompactContent__item');
        expect(itemsElementsAll).toHaveLength(17);
    });
});

describe('CardVinReportCompactContent скрыта бесплатная часть', () => {
    it('правильно рендерит "Еще Х пунктов проверки"', () => {
        const wrap = shallowRenderComponent({ showFreeBlock: false });
        const item = wrap.find('.CardVinReportCompactContent__item').last()
            .find('.CardVinReportCompactContent__itemText');

        expect(item.children().text()).toBe('Еще 16 пунктов проверки');
    });

    it('скрывает бесплатные пункты', () => {
        const wrap = shallowRenderComponent({ showFreeBlock: false });
        const item = wrap.find('VinReportFreeBlockItem');

        expect(item.exists()).toBe(false);
    });
});

describe('CardVinReportCompactContent мало пунктов', () => {
    it('не рендерит "Еще Х пунктов проверки", если пунктов впритык', () => {
        const items = _.cloneDeep(ITEMS).filter(i => !i.available_for_free).slice(0, 5);
        const wrap = shallowRenderComponent({ items });
        const itemsElements = wrap.find('GridLikeColumn').dive().find('.CardVinReportCompactContent__item');

        expect(itemsElements).toHaveLength(5);
    });

    it('не рендерит "Еще Х пунктов проверки", если пунктов откровенно мало', () => {
        const items = _.cloneDeep(ITEMS).filter(i => !i.available_for_free).slice(0, 2);
        const wrap = shallowRenderComponent({ items });
        const itemsElements = wrap.find('GridLikeColumn').dive().find('.CardVinReportCompactContent__item');

        expect(itemsElements).toHaveLength(2);
    });
});
