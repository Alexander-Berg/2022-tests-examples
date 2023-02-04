const getExtendedBalanceStats = require('./getExtendedBalanceStats');
const walletMock = require('www-cabinet/react/dataDomain/wallet/mocks/wallet.mock');
const balanceRechargeMock = require('www-cabinet/react/dataDomain/wallet/mocks/balanceRecharge.mock');
const MockDate = require('mockdate');

beforeEach(() => {
    MockDate.set('2018-02-07');
});

afterEach(() => {
    MockDate.reset();
});

it('должен заполнять интервал без метрик пустыми метриками', () => {
    const balanceStats = getExtendedBalanceStats({ wallet: walletMock });

    expect(balanceStats).toMatchSnapshot();
});

it('для сегодняшнего дня должен забирать статистику баланса из стора баланса', () => {
    const wallet = {
        ...walletMock,
        dateLimits: { from: '2018-02-03', to: '2018-02-07' },
    };

    const balanceStats = getExtendedBalanceStats({
        wallet: wallet,
        balanceRecharge: balanceRechargeMock,
    });

    const todayElement = balanceStats.find((item) => item.date === '2018-02-07');
    const todayElementSum = todayElement.products[0].sum;

    expect(todayElementSum).toBe(balanceRechargeMock.balance);
});
