/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import _ from 'lodash';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import { SECOND } from 'auto-core/lib/consts';

import getAllCatalogComplectations from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/selectors/getAllCatalogComplectations';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import catalogSubtreeMock from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/mocks/subtree';
import cardGroupComplectationsMock from 'auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations';

import CardGroupCompare from './CardGroupCompare';
import type { Props } from './CardGroupCompare';

const complectations = getAllCatalogComplectations({
    catalogConfigurationsSubtree: catalogSubtreeMock,
    cardGroupComplectations: cardGroupComplectationsMock,
});

const defaultProps = {
    complectations,
    equipmentDictionary: equipmentDictionaryMock.data,
    showDiff: false,
    isFirstAppearance: true,
    markModelBodyNotice: 'audi a4 sedan',
    closePopup: () => {},
    isPopupClosed: false,
    searchParameters: {},
};

it('по умолчанию все группы открыты', () => {
    const page = shallowRenderComponent({ props: defaultProps });

    const airbagOption = page.find('.CardGroupCompare__optionTitle').findWhere(node => node.text() === 'Подушка безопасности пассажира');
    expect(airbagOption.isEmptyRender()).toBe(false);
});

it('схлопывает открытую группу при клике на неё', () => {
    const page = shallowRenderComponent({ props: defaultProps });

    const airbagOption = page.find('.CardGroupCompare__optionTitle').findWhere(node => node.text() === 'Подушка безопасности пассажира');
    expect(airbagOption.isEmptyRender()).toBe(false);

    const safetyGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Безопасность"]');
    safetyGroup.simulate('click', { currentTarget: { dataset: { groupName: 'Безопасность' } } });

    const airbagOptionUpdated = page.find('.CardGroupCompare__optionTitle').findWhere(node => node.text() === 'Подушка безопасности пассажира');
    expect(airbagOptionUpdated.isEmptyRender()).toBe(true);
});

it('открывает зыкрытую группу при клике на неё', () => {
    const page = shallowRenderComponent({ props: defaultProps });

    const airbagOption = page.find('.CardGroupCompare__optionTitle').findWhere(node => node.text() === 'Подушка безопасности пассажира');
    expect(airbagOption.isEmptyRender()).toBe(false);

    const safetyGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Безопасность"]');
    safetyGroup.simulate('click', { currentTarget: { dataset: { groupName: 'Безопасность' } } });

    const airbagOptionUpdated = page.find('.CardGroupCompare__optionTitle').findWhere(node => node.text() === 'Подушка безопасности пассажира');
    expect(airbagOptionUpdated.isEmptyRender()).toBe(true);

    const safetyGroupUpdated = page.find('.CardGroupCompare__row_type_group[data-group-name="Безопасность"]');
    safetyGroupUpdated.simulate('click', { currentTarget: { dataset: { groupName: 'Безопасность' } } });

    const airbagOptionUpdated2 = page.find('.CardGroupCompare__optionTitle').findWhere(node => node.text() === 'Подушка безопасности пассажира');
    expect(airbagOptionUpdated2.isEmptyRender()).toBe(false);
});

describe('контролы листания', () => {
    it('если листаем вправо, сдвинет страницу вправо', () => {
        const page = shallowRenderComponent({ props: defaultProps });
        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');

        const complectations = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectations).toMatchSnapshot();
    });

    it('знает когда мы достигли правого края', () => {
        const page = shallowRenderComponent({ props: defaultProps });

        const header = page.find('CardGroupCompareHeader');
        expect(header.prop('isEnd')).toBe(false);

        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');

        const headerUpdated = page.find('CardGroupCompareHeader');
        expect(headerUpdated.prop('isEnd')).toBe(true);
    });

    it('если листаем влево, сдвинет страницу влево', () => {
        const page = shallowRenderComponent({ props: defaultProps });
        // тут придется сначала перейти вправо, а потом вернутся назад, чтобы проверить это
        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');
        simulateControlClick(page, 'CardGroupCompareHeader__arrowLeft');

        const complectations = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectations).toMatchSnapshot();
    });

    it('знает когда мы достигли левого края', () => {
        const page = shallowRenderComponent({ props: defaultProps });

        const header = page.find('CardGroupCompareHeader');
        expect(header.prop('isStart')).toBe(true);

        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');

        const headerUpdated = page.find('CardGroupCompareHeader');
        expect(headerUpdated.prop('isStart')).toBe(false);

        simulateControlClick(page, 'CardGroupCompareHeader__arrowLeft');

        const headerUpdated2 = page.find('CardGroupCompareHeader');
        expect(headerUpdated2.prop('isStart')).toBe(true);
    });

    it('ничего не будет делать, если это край', () => {
        const page = shallowRenderComponent({ props: defaultProps });
        const complectationsBefore = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;

        simulateControlClick(page, 'CardGroupCompareHeader__arrow_disabled');

        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;

        expect(complectationsBefore).toEqual(complectationsAfter);
    });

    it('обновляет фильтр "показать только различия" при смене страницы', () => {
        const props = { ...defaultProps, showDiff: true };
        const page = shallowRenderComponent({ props });

        const viewGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Обзор"]');
        viewGroup.simulate('click', { currentTarget: { dataset: { groupName: 'Обзор' } } });

        const optionsBefore = page.find('.CardGroupCompare__optionTitle').map((node) => node.text());

        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');

        const safetyGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Безопасность"]');
        safetyGroup.simulate('click', { currentTarget: { dataset: { groupName: 'Безопасность' } } });

        const optionsAfter = page.find('.CardGroupCompare__optionTitle').map((node) => node.text());

        expect(_.difference(optionsAfter, optionsBefore)).toMatchSnapshot();
    });
});

it('если есть фильтр "показать только различия", не покажет пустые группы', () => {
    const props = { ...defaultProps, showDiff: true };
    const page = shallowRenderComponent({ props });
    const safetyGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Безопасность"]');

    expect(safetyGroup.isEmptyRender()).toBe(true);
});

describe('дрэг энд дроп', () => {
    it('при дропе изменить сортировку комплектаций', () => {
        const page = shallowRenderComponent({ props: defaultProps });

        const complectationsBefore = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsBefore).toMatchSnapshot();

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Active'));
        header.simulate('sortEnd', { newIndex: 0 });

        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toMatchSnapshot();
    });

    it('сохранит новую сортировку при переключении страницы', () => {
        const page = shallowRenderComponent({ props: defaultProps });

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Active'));
        header.simulate('sortEnd', { newIndex: 0 });

        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');

        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toMatchSnapshot();
    });

    it('ничего не будет делать, если порядок не поменялся', () => {
        const page = shallowRenderComponent({ props: defaultProps });

        const complectationsBefore = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Active'));
        header.simulate('sortEnd', { newIndex: 2 });

        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toEqual(complectationsBefore);
    });

    it('лимитирует новый индекс размером страницы', () => {
        const page = shallowRenderComponent({ props: defaultProps });

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Laurin & Klement'));
        header.simulate('sortEnd', { newIndex: 4 });

        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toMatchSnapshot();

        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Laurin & Klement'));
        header.simulate('sortEnd', { newIndex: 4 });

        const complectationsAfter2 = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toEqual(complectationsAfter2);
    });
});

describe('листание при дрэг-энд-дропе', () => {
    beforeEach(() => {
        //@see https://github.com/facebook/jest/issues/11551
        jest.useFakeTimers('legacy');
    });

    it('при задержке над последним элементом перелистнет страницу вперед', () => {
        const complectations = defaultProps.complectations.map(({ name }) => name);
        const page = shallowRenderComponent({ props: defaultProps });

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Laurin & Klement', complectations));

        jest.advanceTimersByTime(SECOND);

        // проверяем смену страницы когда дрэг еще активен
        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toMatchSnapshot();

        // проверяем сортировку уже после дропа в конкретное место
        header.simulate('sortEnd', { newIndex: 0 });
        const complectationsAfter2 = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter2).toMatchSnapshot();
    });

    it('при задержке над первым элементом перелистнет страницу назад', () => {
        const complectations = defaultProps.complectations.map(({ name }) => name);
        const page = shallowRenderComponent({ props: defaultProps });

        simulateControlClick(page, 'CardGroupCompareHeader__arrowRight');
        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Style', 'Ambition', complectations));

        jest.advanceTimersByTime(SECOND * 1.5);

        // проверяем смену страницы когда дрэг еще активен
        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toMatchSnapshot();

        // проверяем что сбросили интервал, когда достигли края
        const instance = page.instance() as CardGroupCompare;

        expect(clearInterval).toHaveBeenCalledTimes(1);
        expect(clearInterval).toHaveBeenCalledWith(0);

        jest.advanceTimersByTime(SECOND * 0.5);

        expect(clearInterval).toHaveBeenCalledTimes(2);
        expect(clearInterval).toHaveBeenCalledWith(instance._scrollOnDragInterval);

        // проверяем сортировку уже после дропа в конкретное место
        header.simulate('sortEnd', { newIndex: 0 });
        const complectationsAfter2 = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter2).toMatchSnapshot();
    });

    it('после перелистывания будет правильно обновлять порядок элементов', () => {
        const complectations = defaultProps.complectations.map(({ name }) => name);
        const page = shallowRenderComponent({ props: defaultProps });

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Laurin & Klement', complectations));
        jest.advanceTimersByTime(SECOND);
        header.simulate('sortEnd', { newIndex: 0 });

        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Ambition', complectations));
        header.simulate('sortEnd', { newIndex: 1 });

        const complectationsAfter = page.find('CardGroupCompareHeader').prop('visibleItems') as Array<string>;
        expect(complectationsAfter).toMatchSnapshot();
    });

    it('обновляет фильтр "показать только различия" при смене страницы', () => {
        const complectations = defaultProps.complectations.map(({ name }) => name);
        const props = { ...defaultProps, showDiff: true };
        const page = shallowRenderComponent({ props });

        const viewGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Обзор"]');
        viewGroup.simulate('click', { currentTarget: { dataset: { groupName: 'Обзор' } } });

        const optionsBefore = page.find('.CardGroupCompare__optionTitle').map((node) => node.text());

        const header = page.find('CardGroupCompareHeader');
        header.simulate('dragMove', getDragMoveEvent('Hockey Edition', 'Laurin & Klement', complectations));
        jest.advanceTimersByTime(SECOND);
        header.simulate('sortEnd', { newIndex: 0 });

        const safetyGroup = page.find('.CardGroupCompare__row_type_group[data-group-name="Безопасность"]');
        safetyGroup.simulate('click', { currentTarget: { dataset: { groupName: 'Безопасность' } } });

        const optionsAfter = page.find('.CardGroupCompare__optionTitle').map((node) => node.text());

        expect(_.difference(optionsAfter, optionsBefore)).toMatchSnapshot();
    });
});

function simulateControlClick(page: ShallowWrapper, className: string) {
    const header = page.find('CardGroupCompareHeader');
    header.simulate('controlClick', { currentTarget: { classList: {
        contains: (classNameToCheck: string) => classNameToCheck === className,
    } } });
}

function getDragMoveEvent(draggedItem: string, hoveredItem: string, complectations: Array<string> = []) {
    return {
        related: {
            dataset: { id: hoveredItem },
        },
        dragged: {
            dataset: { id: draggedItem },
        },
        relatedRect: { x: complectations.indexOf(hoveredItem) },
        draggedRect: { x: complectations.indexOf(draggedItem) },
    };
}

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <CardGroupCompare { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
