const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const _ = require('lodash');

const { nbsp } = require('auto-core/react/lib/html-entities');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingOutputTypeSwitcher = require('./ListingOutputTypeSwitcher');
const ListingOutputTypeSwitcherDumb = require('./ListingOutputTypeSwitcherDumb');

const LISTING_OUTPUT_TYPE = require('auto-core/data/listing/OutputTypes').default;

it('должен отрендерить компонент c отключенной группировкой по моделям и правильным тултипом', () => {
    const tree = shallow(
        <ListingOutputTypeSwitcherDumb
            disabledType={ LISTING_OUTPUT_TYPE.MODELS }
            onChange={ _.noop }
            isCars
        />,
        { context: contextMock },
    );

    const modelsRadioButton = tree.findWhere(item => item.name() === 'Radio' && item.prop('value') === LISTING_OUTPUT_TYPE.MODELS);
    expect(modelsRadioButton).toHaveProp('disabled', true);

    const hoveredTooltip = modelsRadioButton.find('HoveredTooltip');
    expect(hoveredTooltip).toHaveProp('tooltipContent', `Выберите регион, чтобы группировать по${ nbsp }моделям`);
});

it('должен отрендерить тултип с количестовм моделей при наведении', () => {
    const tree = shallow(
        <ListingOutputTypeSwitcherDumb
            type={ LISTING_OUTPUT_TYPE.LIST }
            onChange={ _.noop }
            groupedModelsCount={ 124 }
            isCars
        />,
        { context: contextMock },
    );
    const modelsRadioButton = tree.findWhere(item => item.name() === 'Radio' && item.prop('value') === LISTING_OUTPUT_TYPE.MODELS);
    const hoveredTooltip = modelsRadioButton.find('HoveredTooltip');
    expect(hoveredTooltip).toHaveProp('tooltipContent', `Показать 124${ nbsp }модели`);
});

it('должен отправить информацию о выборе дефолтного значения', () => {
    const onChangeMock = jest.fn();
    const tree = shallow(
        <ListingOutputTypeSwitcherDumb
            defaultType={ LISTING_OUTPUT_TYPE.MODELS }
            onChange={ onChangeMock }
            type={ LISTING_OUTPUT_TYPE.LIST }
            isCars
        />,
        { context: contextMock },
    );

    tree.find('RadioGroup').simulate('change', LISTING_OUTPUT_TYPE.MODELS);
    expect(onChangeMock).toHaveBeenCalledWith(LISTING_OUTPUT_TYPE.MODELS, true, undefined);
});

it('должен отправить информацию о выборе не дефолтного значения', () => {
    const onChangeMock = jest.fn();
    const tree = shallow(
        <ListingOutputTypeSwitcherDumb
            defaultType={ LISTING_OUTPUT_TYPE.MODELS }
            onChange={ onChangeMock }
            type={ LISTING_OUTPUT_TYPE.TABLE }
            isCars
        />,
        { context: contextMock },
    );

    tree.find('RadioGroup').simulate('change', LISTING_OUTPUT_TYPE.LIST);
    expect(onChangeMock).toHaveBeenCalledWith(LISTING_OUTPUT_TYPE.LIST, false, undefined);
});

it('должен вызвать onChange с кастомным методом, если он есть в пропсах', () => {
    const onChangeMock = jest.fn();
    const tree = shallow(
        <ListingOutputTypeSwitcherDumb
            defaultType={ LISTING_OUTPUT_TYPE.MODELS }
            onChange={ onChangeMock }
            type={ LISTING_OUTPUT_TYPE.TABLE }
            fetchMethod="random_shit_method"
            isCars
        />,
        { context: contextMock },
    );

    tree.find('RadioGroup').simulate('change', LISTING_OUTPUT_TYPE.LIST);
    expect(onChangeMock).toHaveBeenCalledWith(LISTING_OUTPUT_TYPE.LIST, false, 'random_shit_method');
});

describe('должен отправить метрику', () => {
    it('при появлении переключателя', () => {
        shallow(
            <ListingOutputTypeSwitcherDumb
                section="used"
                onChange={ _.noop }
                type={ LISTING_OUTPUT_TYPE.MODELS }
            />,
            { context: contextMock },
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'filter', 'output_type', 'load', 'used', LISTING_OUTPUT_TYPE.MODELS ]);
    });

    it('при смене значения переключателя', () => {
        const tree = shallow(
            <ListingOutputTypeSwitcherDumb
                section="used"
                onChange={ _.noop }
                type={ LISTING_OUTPUT_TYPE.MODELS }
            />,
            { context: contextMock },
        );

        tree.find('RadioGroup').simulate('change', LISTING_OUTPUT_TYPE.TABLE);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(2, [ 'filter', 'output_type', 'set', 'used', LISTING_OUTPUT_TYPE.TABLE ]);
    });
});

describe('дизейблим переключатель', () => {
    it('при обновлении обновление листинга', () => {
        const store = mockStore({
            cookies: {},
            listing: { data: {}, pending: true },
        });

        const tree = shallow(
            <Provider store={ store }>
                <ListingOutputTypeSwitcher onChange={ _.noop }/>
            </Provider>,
            { context: contextMock },
        ).dive().dive();

        const radioGroup = tree.find('RadioGroup');
        expect(radioGroup).toHaveProp('disabled', true);
    });
});

it('не будет рендерить опцию с табличным видом, если передан проп withoutTableView', () => {
    const tree = shallow(
        <ListingOutputTypeSwitcherDumb
            disabledType={ LISTING_OUTPUT_TYPE.MODELS }
            onChange={ _.noop }
            withoutTableView
        />,
        { context: contextMock },
    );

    const radioButtons = tree.find('Radio');
    expect(radioButtons).toHaveLength(2);
    expect(radioButtons.map(item => item.prop('value'))).toEqual([ LISTING_OUTPUT_TYPE.LIST, LISTING_OUTPUT_TYPE.CAROUSEL ]);
});
