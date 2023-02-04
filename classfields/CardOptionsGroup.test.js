const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CardOptionsGroup = require('./CardOptionsGroup');

const BASE_OPTIONS = [
    {
        code: 'rear-seats-heat',
        name: 'Подогрев задних сидений',
    },
    {
        code: 'wheel-heat',
        name: 'Обогрев рулевого колеса',
    },
    {
        code: 'auto-park',
        name: 'Система автоматической парковки',
    },
    {
        code: '360-camera',
        name: 'Камера 360°',
    },
    {
        code: 'programmed-block-heater',
        name: 'Программируемый предпусковой отопитель',
    },
    {
        code: 'electro-trunk',
        name: 'Электропривод крышки багажника',
    },
    {
        code: 'hcc',
        name: 'Система помощи при старте в гору',
    },
];

const ADDITIONAL_OPTIONS = [
    {
        code: 'windscreen-heat',
        name: 'Электрообогрев лобового стекла',
        group: 'Обзор',
    },
    {
        code: 'high-beam-assist',
        name: 'Система управления дальним светом',
        group: 'Обзор',
    },
    {
        code: '19-inch-wheels',
        name: 'Легкосплавные диски 19"',
        group: 'Элементы экстерьера',
    },
    {
        code: 'alarm',
        name: 'Штатная сигнализация',
        group: 'Защита от угона',
    },
];

it('должен отрендерить пустую строку, если нет опций', () => {
    const tree = shallow(<CardOptionsGroup title="Комфорт"/>);
    expect(shallowToJson(tree)).toEqual('');
});

it('должен отрендерить список дополнительных опций, если они есть', () => {
    const tree = shallow(
        <CardOptionsGroup
            title="Комфорт"
            baseOptions={ BASE_OPTIONS }
            additionalOptions={ ADDITIONAL_OPTIONS }
        />,
    );
    expect(
        tree.find('.CardOptionsGroup__additionalOptions'),
    ).toHaveLength(1);
});

it('не должен отрендерить список дополнительных опций, если их нет', () => {
    const tree = shallow(
        <CardOptionsGroup
            title="Комфорт"
            baseOptions={ BASE_OPTIONS }
        />,
    );
    expect(
        tree.find('.CardOptionsGroup__additionalOptions'),
    ).toHaveLength(0);
});

it('должен отрендерить правильный заголовок для списка дополнительных опций', () => {
    const tree = shallow(
        <CardOptionsGroup
            title="Комфорт"
            baseOptions={ BASE_OPTIONS }
            additionalOptions={ ADDITIONAL_OPTIONS }
        />,
    );
    expect(
        tree.find('.CardOptionsGroup__additionalOptionsTitle').text(),
    ).toEqual('Могут быть установлены дополнительно');
});
