import bankMock from 'auto-core/react/dataDomain/credit/mocks/bank.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';
import productIDs from 'auto-core/react/dataDomain/credit/dicts/productIDs';

import { ClaimState } from 'auto-core/types/TCreditBroker';

import sortCreditProducts from './sortCreditProducts';

const banks = [
    bankMock().withBankID('bank-1').value(),
    bankMock().withBankID('bank-2').value(),
    bankMock().withBankID('bank-3').value(),
    bankMock().withBankID('bank-4').value(),
    bankMock().withBankID('bank-5').value(),
    bankMock().withBankID('bank-6').value(),
    bankMock().withBankID('bank-7').value(),
    bankMock().withBankID('bank-8').value(),
    bankMock().withBankID('bank-9').value(),
];

it('сортирует продукты по  группам статусов', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').withBankID('bank-1').value(),
        creditProductMock().withID('product-2').withBankID('bank-2').value(),
        creditProductMock().withID('product-3').withBankID('bank-3').value(),
        creditProductMock().withID('product-4').withBankID('bank-4').value(),
        creditProductMock().withID('product-5').withBankID('bank-5').value(),
    ];

    const claims = [
        creditApplicationClaimMock().withProductID('product-1').withState(ClaimState.REJECT).value(),
        creditApplicationClaimMock().withProductID('product-2').withState(ClaimState.NEW).value(),
        creditApplicationClaimMock().withProductID('product-3').withState(ClaimState.CANCELED_DRAFT).value(),
        creditApplicationClaimMock().withProductID('product-4').withState(ClaimState.DRAFT).value(),
        creditApplicationClaimMock().withProductID('product-5').withState(ClaimState.APPROVED).value(),
    ];

    const creditApplication = creditApplicationMock().withClaims(claims).value();

    const resultProductsIDsOrder = sortCreditProducts({ creditApplication, creditProducts, banks })
        .map(product => product.id);

    expect(resultProductsIDsOrder).toEqual([ 'product-4', 'product-5', 'product-3', 'product-2', 'product-1' ]);
});

it('внутри одной группы статусов, сортирует продукты по приоритету', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').withBankID('bank-1').value(),
        creditProductMock().withID('product-2').withBankID('bank-2').withPriority(1).value(),
        creditProductMock().withID('product-3').withBankID('bank-3').withPriority(2).value(),
        creditProductMock().withID('product-4').withBankID('bank-4').withPriority(3).value(),
        creditProductMock().withID('product-5').withBankID('bank-5').value(),
    ];

    const claims = [
        creditApplicationClaimMock().withProductID('product-1').withState(ClaimState.REJECT).value(),
        creditApplicationClaimMock().withProductID('product-2').withState(ClaimState.NEW).value(),
        creditApplicationClaimMock().withProductID('product-3').withState(ClaimState.NEED_INFO).value(),
        creditApplicationClaimMock().withProductID('product-4').withState(ClaimState.NEW).value(),
        creditApplicationClaimMock().withProductID('product-5').withState(ClaimState.APPROVED).value(),
    ];

    const creditApplication = creditApplicationMock().withClaims(claims).value();

    const resultProductsIDsOrder = sortCreditProducts({ creditApplication, creditProducts, banks })
        .map(product => product.id);

    expect(resultProductsIDsOrder).toEqual([ 'product-5', 'product-2', 'product-3', 'product-4', 'product-1' ]);
});

it('внутри одной группы статусов и одного приоритета сортирует продукты по ставке', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').withBankID('bank-1').value(),
        creditProductMock().withID('product-2').withBankID('bank-2').withInterestRateRange({ from: 14, to: 16 }).value(),
        creditProductMock().withID('product-3').withBankID('bank-3').withInterestRateRange({ from: 11, to: 16 }).value(),
        creditProductMock().withID('product-4').withBankID('bank-4').withInterestRateRange({ from: 12, to: 16 }).value(),
        creditProductMock().withID('product-5').withBankID('bank-5').value(),
    ];

    const claims = [
        creditApplicationClaimMock().withProductID('product-1').withState(ClaimState.REJECT).value(),
        creditApplicationClaimMock().withProductID('product-2').withState(ClaimState.NEW).value(),
        creditApplicationClaimMock().withProductID('product-3').withState(ClaimState.NEED_INFO).value(),
        creditApplicationClaimMock().withProductID('product-4').withState(ClaimState.NEW).value(),
        creditApplicationClaimMock().withProductID('product-5').withState(ClaimState.APPROVED).value(),
    ];

    const creditApplication = creditApplicationMock().withClaims(claims).value();

    const resultProductsIDsOrder = sortCreditProducts({ creditApplication, creditProducts, banks })
        .map(product => product.id);

    expect(resultProductsIDsOrder).toEqual([ 'product-5', 'product-3', 'product-4', 'product-2', 'product-1' ]);
});

it('обеспечивает сбербанку позицию в топ-4, если его там нет', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').value(),
        creditProductMock().withID('product-2').value(),
        creditProductMock().withID('product-3').value(),
        creditProductMock().withID('product-4').value(),
        creditProductMock().withID(productIDs.sberbank1).value(),
    ];

    const creditApplication = creditApplicationMock().value();
    const resultProductsIDsOrder = sortCreditProducts({ creditApplication, creditProducts, banks })
        .map(product => product.id);

    expect(resultProductsIDsOrder.indexOf(productIDs.sberbank1)).toEqual(3);
});

it('не перемещает сбербанк, если он и так в топ-4', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').value(),
        creditProductMock().withID(productIDs.sberbank1).value(),
        creditProductMock().withID('product-2').value(),
        creditProductMock().withID('product-3').value(),
        creditProductMock().withID('product-4').value(),
    ];

    const creditApplication = creditApplicationMock().value();
    const resultProductsIDsOrder = sortCreditProducts({ creditApplication, creditProducts, banks })
        .map(product => product.id);

    expect(resultProductsIDsOrder.indexOf(productIDs.sberbank1)).toEqual(1);
});

it('ставит сбербанк на первое место в эксклюзиве', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').value(),
        creditProductMock().withID(productIDs.sberbank1).withPriorityTags([ 'EXCLUSIVE' ]).value(),
        creditProductMock().withID('product-2').value(),
        creditProductMock().withID('product-3').value(),
        creditProductMock().withID('product-4').value(),
    ];

    const creditApplication = creditApplicationMock().value();
    const resultProductsIDsOrder = sortCreditProducts({ creditApplication, creditProducts, banks })
        .map(product => product.id);

    expect(resultProductsIDsOrder.indexOf(productIDs.sberbank1)).toEqual(0);
});

it('сортирует продукты по ставке и приоритету, если нет данных о заявке', () => {
    const creditProducts = [
        creditProductMock().withID('product-1').withBankID('bank-1').withPriority(4).value(),
        creditProductMock().withID('product-2').withBankID('bank-2').withPriority(1).withInterestRateRange({ from: 14, to: 16 }).value(),
        creditProductMock().withID('product-3').withBankID('bank-3').withPriority(1).withInterestRateRange({ from: 11, to: 16 }).value(),
        creditProductMock().withID('product-4').withBankID('bank-4').withPriority(2).withInterestRateRange({ from: 12, to: 16 }).value(),
        creditProductMock().withID('product-5').withBankID('bank-5').withPriority(3).value(),
    ];

    const resultProductsIDsOrder = sortCreditProducts({ creditProducts })
        .map(product => product.id);

    expect(resultProductsIDsOrder).toEqual([ 'product-3', 'product-2', 'product-4', 'product-5', 'product-1' ]);
});
