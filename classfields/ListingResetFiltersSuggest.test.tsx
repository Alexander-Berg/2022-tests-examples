jest.mock('auto-core/react/lib/gateApi', () => ({
    getResourcePublicApi: jest.fn().mockImplementation(() => Promise.resolve({ suggests: [] })),
}));

jest.mock('auto-core/lib/util/filters', () => {
    return {};
});

import React from 'react';
import { shallow } from 'enzyme';

import { getResourcePublicApi } from 'auto-core/react/lib/gateApi';

import ListingResetFiltersSuggest from './ListingResetFiltersSuggest';

class ListingResetFiltersSuggestChild extends ListingResetFiltersSuggest {
    renderContent() {
        return <div>content</div>;
    }
}

it('должен отрисовать контент, если он есть', () => {
    const tree = shallowRenderComponent();

    tree.find('InView').simulate('change', true);
    tree.setState({
        suggests: [
            {
                pagination: { page: 1, page_size: 30, total_page_count: 1, total_offers_count: 15 },
                resetKey: 'key1',
            },
        ],
    });

    expect(tree.find('InView').dive().children().text()).toEqual('content');
});

describe('должен не рендерить контент', () => {

    it('если ни разу не был во вьюпорте', () => {
        const tree = shallowRenderComponent();
        expect(tree.find('InView').dive()).toEqual({});
    });

    it('если данных нет', () => {
        const tree = shallowRenderComponent();

        tree.find('InView').simulate('change', true);
        tree.setState({
            suggests: [],
        });

        expect(tree.find('InView').dive()).toEqual({});
    });
});

describe('должен запросить данные', () => {

    it('только при первом попадании во вьюпорт', () => {
        const tree = shallowRenderComponent();

        tree.find('InView').simulate('change', true);
        tree.find('InView').simulate('change', false);
        tree.find('InView').simulate('change', true);

        expect(getResourcePublicApi).toHaveBeenCalledTimes(1);
    });

    it('после изменения пропсов при попадании во вьюпорт', () => {
        const tree = shallowRenderComponent();

        tree.find('InView').simulate('change', true);
        tree.find('InView').simulate('change', false);

        tree.setProps({ params: { pagination: {} }, offersCount: 7 });
        tree.find('InView').simulate('change', true);

        expect(getResourcePublicApi).toHaveBeenCalledTimes(2);
    });

    it('после изменения статуса получения листинга при попадании во вьюпорт', () => {
        const tree = shallowRenderComponent();
        tree.find('InView').simulate('change', true);
        tree.setProps({ params: { pagination: {} }, isPending: true });
        tree.setProps({ params: { pagination: {} }, isPending: false });

        expect(getResourcePublicApi).toHaveBeenCalledTimes(2);
    });

});

function shallowRenderComponent() {
    return shallow(
        <ListingResetFiltersSuggestChild
            params={{}}
            offersCount={ 10 }
        />,
        { context: { link: () => {}, metrika: {} } },
    );
}
