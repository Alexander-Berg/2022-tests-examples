import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import type { AnonOfferInfo, Event } from '@vertis/schema-registry/ts-types-snake/auto/cabinet/walk_in';
import { Section } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

const ContextProvider = createContextProvider(contextMock);

import walkInMock from 'auto-core/react/dataDomain/walkIn/mocks/withData.mock';

import getWalkInEventsList from 'www-cabinet/react/dataDomain/walkIn/selectors/getWalkInEventsList';

import WalkInEventsListItem from './WalkInEventsListItem';

describe('с несколькими офферами во всех колонках', () => {
    const tree = shallow(
        <ContextProvider>
            <WalkInEventsListItem
                data={ getWalkInEventsList(walkInMock)[0] }
            />
        </ContextProvider>,
    ).dive();

    it('должен рендерить иконку расхлопывания для элемента', () => {
        const icon = tree.find('.WalkInEventsListItem__icon');

        expect((shallowToJson(icon))).toMatchSnapshot();
    });

    it('должен вешать обработчик расхлопывания на контейнер', () => {
        const container = tree.find('.WalkInEventsListItem');

        expect(container.props().onClick).not.toBeUndefined();
    });

    it('должен рендерить контейнеры со скрытыми элементами', () => {
        const content = tree.find('.WalkInEventsListItem__expandableContent');

        expect((shallowToJson(content))).toMatchSnapshot();
    });
});

describe('по одному офферу в колонках', () => {
    const tree = shallow(
        <ContextProvider>
            <WalkInEventsListItem
                data={ getWalkInEventsList(walkInMock)[1] }
            />
        </ContextProvider>,
    ).dive();

    it('не должен рендерить иконку расхлопывания для элемента с несколькими офферами в истории поиска', () => {
        const icon = tree.find('.WalkInEventsListItem__icon');

        expect((shallowToJson(icon))).toBeNull();
    });

    it('не должен вешать обработчик расхлопывания на контейнер', () => {
        const container = tree.find('.WalkInEventsListItem');

        expect(container.props().onClick).toBeUndefined();
    });

    it('не должен рендерить контейнер со скрытыми элементами', () => {
        const content = tree.find('.WalkInEventsListItem__expandableContent');

        expect((shallowToJson(content))).toBeNull();
    });
});

describe('должен считать, что элемент может расхлопнуться, если есть несколько элементов', () => {
    const dealerOffers = getWalkInEventsList(walkInMock)[0].source_offers;

    const TEST_CASES = [
        { input: {
            search_history: [ { section: Section.USED }, { section: Section.NEW } ],
            source_offers: [],
            user_offers: [],
        }, description: 'в истории поиска' },
        { input: { user_offers: [ {}, {} ] as Array<AnonOfferInfo>, source_offers: [], search_history: [] }, description: 'в оффера юзера' },
        { input: { source_offers: dealerOffers }, description: 'в офферах дилера' },
    ] as Array<{ input: Event; description: string }>;

    TEST_CASES.forEach((testCase) => {
        it(testCase.description, () => {
            const tree = shallow(
                <ContextProvider>
                    <WalkInEventsListItem
                        data={{
                            date: '2011-01-12',
                            ...testCase.input,
                        }}
                    />
                </ContextProvider>,
            ).dive();

            const container = tree.find('.WalkInEventsListItem');

            expect(container.props().onClick).not.toBeUndefined();
        });
    });
});
