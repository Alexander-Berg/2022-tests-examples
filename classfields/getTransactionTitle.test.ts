import getTransactionTitle from './getTransactionTitle';

it('преобразовать старое название "История размещение"', () => {
    const product = { alias: 'offer_history', name: 'offer_history', human_name: 'История размещений' };
    const result = getTransactionTitle([ product ]);
    expect(result).toEqual('Отчёт ПроАвто');
});

it('оставить как есть, если незнакомая строка', () => {
    const product = { alias: 'foo', name: 'bar', human_name: 'ХХХ контент' };
    const result = getTransactionTitle([ product ]);
    expect(result).toEqual('ХХХ контент');
});
