const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const GraphTooltip = require('./GraphTooltip').default;

it('не должен рендерить футер, если не передана общая сумма', () => {
    const tree = shallow(
        <GraphTooltip
            payload={ [ {
                payload: {
                    date: '2012-12-24',
                    products: [
                        { sum: 20, label: '20 кг', name: 'Апельсины', color: '#F46D43' },
                    ],
                },
            } ] }
        />,
    );

    const elem = tree.find('.GraphTooltip__footer');

    expect(shallowToJson(elem)).toBeNull();
});

it('не должен рендерить, если нет payload', () => {
    const tree = shallow(<GraphTooltip/>);

    expect(tree.isEmptyRender()).toBe(true);
});

it('не должен рендерить, если пустой payload', () => {
    const tree = shallow(<GraphTooltip payload={ [] }/>);

    expect(tree.isEmptyRender()).toBe(true);
});
