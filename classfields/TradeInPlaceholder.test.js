jest.mock('./img/tradeInPlaceholder.png', () => 'placeholderImage');
const TradeInPlacholder = require('./TradeInPlaceholder');

it('TradeInPlacholder тест: должен вернуть корректный компонент', () => {
    expect(TradeInPlacholder({ text: '<div>text</div>', className: 'className' })).toMatchSnapshot();
});
