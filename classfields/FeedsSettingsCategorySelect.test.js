const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { Provider } = require('react-redux');
const _ = require('lodash');

const Tags = require('auto-core/react/components/islands/Tags');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const feedsMock = require('www-cabinet/react/dataDomain/feeds/mocks/feeds.mock');
const tariffsMock = require('www-cabinet/react/dataDomain/promoPopup/mocks/tariffs');

const getAvailableAutomaticCategories = require('www-cabinet/react/dataDomain/feeds/selectors/getAvailableAutomaticCategories');

const FeedsSettingsCategorySelect = require('./FeedsSettingsCategorySelect');

const initialState = {
    bunker: getBunkerMock([ 'cabinet/feeds', 'cabinet/categories_info' ]),
    feeds: feedsMock,
    promoPopup: { tariffs: tariffsMock },
};

const categories = getAvailableAutomaticCategories(initialState);

const store = mockStore(initialState);

it('должен сразу вызывать onChange с правильными параметрами для категории без чилдренов', () => {
    const onChangeHandler = jest.fn();

    const tree = shallow(
        <Provider store={ store }>
            <FeedsSettingsCategorySelect
                onChange={ onChangeHandler }
                categories={ categories }
            />
        </Provider>,
    ).dive().dive();

    tree.find(Tags).simulate('change', [ 'CARS_NEW' ]);

    expect(onChangeHandler).toHaveBeenCalledWith('CARS_NEW');
});

it('должен вызывать onChange с секцией родителя и категорией чилдрена', () => {
    const onChangeHandler = jest.fn();

    const tree = shallow(
        <Provider store={ store }>
            <FeedsSettingsCategorySelect
                onChange={ onChangeHandler }
                categories={ categories }
            />
        </Provider>,
    ).dive().dive();

    tree.find('Tags').simulate('change', [ 'COMMERCIAL_USED' ]);
    tree.find('Tags').at(1).simulate('change', [ 'TRUCK' ]);

    expect(onChangeHandler).toHaveBeenCalledWith('TRUCK_USED');
});

it('должен вызывать onChange с null и сбрасывать категорию чилдрена при смене категории родителя', () => {
    const onChangeHandler = jest.fn();

    const tree = shallow(
        <Provider store={ store }>
            <FeedsSettingsCategorySelect
                onChange={ onChangeHandler }
                categories={ categories }
            />
        </Provider>,
    ).dive().dive();

    tree.find('Tags').at(0).simulate('change', [ 'COMMERCIAL_USED' ]);
    tree.find('Tags').at(1).simulate('change', [ 'TRUCK' ]);
    tree.find('Tags').at(0).simulate('change', [ 'COMMERCIAL_NEW' ]);

    const props = tree.find('Tags').at(1).props();

    expect(onChangeHandler).toHaveBeenCalledWith(null);
    expect(props.value).toEqual([]);
});

it('должен отрендерить родительские категории', () => {
    const tree = shallow(
        <Provider store={ store }>
            <FeedsSettingsCategorySelect
                onChange={ _.noop }
                categories={ categories }
            />
        </Provider>,
    ).dive().dive();

    const parentTags = tree.find('Tags');

    expect(shallowToJson(parentTags)).toMatchSnapshot();
});

it('должен отрендерить чилдренов выбранной родительской категории', () => {
    const tree = shallow(
        <Provider store={ store }>
            <FeedsSettingsCategorySelect
                onChange={ _.noop }
                categories={ categories }
            />
        </Provider>,
    ).dive().dive();

    tree.find('Tags').simulate('change', [ 'COMMERCIAL_USED' ]);

    const childrenTags = tree.find('Tags').at(1);

    expect(shallowToJson(childrenTags)).toMatchSnapshot();
});
