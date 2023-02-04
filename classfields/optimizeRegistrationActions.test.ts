import optimizeRegistrationActions from './optimizeRegistrationActions';

const REG_ACTIONS_1 = {
    date: '1212436800000',
    region: 'Москва',
    operation: 'Снятие с регистрации',
};

const REG_ACTIONS_2 = {
    date: '1212436800000',
    region: 'Москва',
    operation: 'Утилизация транспортного средства',
};

const REG_ACTIONS_3 = {
    date: '1212436800000',
    region: 'Орел',
    operation: 'Автомобиль не регистрировался для работы в каршеринге',
};

const REG_ACTIONS_4 = {
    date: '1213436800000',
    region: 'Москва',
    operation: 'В ГИБДД нет данных об авариях',
};

it('должен корректно склеить (и не склеить в других случаях) регистрационные действия', () => {
    const data = [ REG_ACTIONS_1, REG_ACTIONS_2, REG_ACTIONS_3, REG_ACTIONS_4 ];
    const result = optimizeRegistrationActions(data);
    const expected = [
        {
            region: 'Москва',
            date: '3 июня 2008',
            operation: [ 'Снятие с регистрации', 'Утилизация транспортного средства' ],
        },
        {
            region: 'Орел',
            date: '3 июня 2008',
            operation: [ 'Автомобиль не регистрировался для работы в каршеринге' ],
        },
        {
            region: 'Москва',
            date: '14 июня 2008',
            operation: [ 'В ГИБДД нет данных об авариях' ],
        },
    ];
    expect(result).toEqual(expected);
});
