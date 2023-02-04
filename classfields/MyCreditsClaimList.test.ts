import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import bankMock from 'auto-core/react/dataDomain/credit/mocks/bank.mockchain';

import { BankID, ClaimState, CreditProductType } from 'auto-core/types/TCreditBroker';

import MyCreditsClaimList, { ALL_PRODUCTS_TYPE } from './MyCreditsClaimList';

const { getCreditProducts, getDerivedStateFromProps } = MyCreditsClaimList;
const props = {};
const creditProductTypes = [
    ALL_PRODUCTS_TYPE,
    CreditProductType.AUTO,
    CreditProductType.CONSUMER,
    CreditProductType.CREDIT_CARD,
    CreditProductType.REFINANCING,
];
const shouldRenderProductTypeSelector = true;

describe('MyCreditsClaimList', () => {
    it('была выключена единственная выбранная опция и это была опция "Все"', () => {
        const state = {
            creditProductTypes,
            selectedProductTypes: [],
            shouldRenderProductTypeSelector,
            previousSelectedProductTypes: [ ALL_PRODUCTS_TYPE ],
        };

        expect(getDerivedStateFromProps(props, state)?.selectedProductTypes).toEqual([ CreditProductType.AUTO ]);
    });

    it('была выключена единственная выбранная опция и это была не опция "Все"', () => {
        const state = {
            creditProductTypes,
            selectedProductTypes: [],
            shouldRenderProductTypeSelector,
            previousSelectedProductTypes: [ CreditProductType.CONSUMER ],
        };

        expect(getDerivedStateFromProps(props, state)?.selectedProductTypes).toEqual([ ALL_PRODUCTS_TYPE ]);
    });

    it('была выбрана опция "Все" и пользователь начал выбирать другие опции', () => {
        const state = {
            creditProductTypes,
            selectedProductTypes: [ ALL_PRODUCTS_TYPE, CreditProductType.CREDIT_CARD ],
            shouldRenderProductTypeSelector,
            previousSelectedProductTypes: [ ALL_PRODUCTS_TYPE ],
        };

        expect(getDerivedStateFromProps(props, state)?.selectedProductTypes).toEqual([ CreditProductType.CREDIT_CARD ]);
    });

    it('были выбраны какие-то опции и пользователь выбрал опцию "Все"', () => {
        const state = {
            creditProductTypes,
            selectedProductTypes: [ CreditProductType.CREDIT_CARD, CreditProductType.REFINANCING, ALL_PRODUCTS_TYPE ],
            shouldRenderProductTypeSelector,
            previousSelectedProductTypes: [ CreditProductType.CREDIT_CARD, CreditProductType.REFINANCING ],
        };

        expect(getDerivedStateFromProps(props, state)?.selectedProductTypes).toEqual([ ALL_PRODUCTS_TYPE ]);
    });

    it('были вручную выбраны все опции кроме "Все"', () => {
        const state = {
            creditProductTypes,
            selectedProductTypes: [
                CreditProductType.AUTO,
                CreditProductType.CONSUMER,
                CreditProductType.CREDIT_CARD,
                CreditProductType.REFINANCING,
            ],
            shouldRenderProductTypeSelector,
            previousSelectedProductTypes: [
                CreditProductType.AUTO,
                CreditProductType.CONSUMER,
                CreditProductType.CREDIT_CARD,
            ],
        };

        expect(getDerivedStateFromProps(props, state)?.selectedProductTypes).toEqual([ ALL_PRODUCTS_TYPE ]);
    });

    describe('список продуктов', () => {
        const availableOffer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .withSaleId('890-111')
            .withCreditPrecondition()
            .value();

        const ecreditOffer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .withSaleId('890-111')
            .withEcreditPrecondition()
            .withCreditPrecondition()
            .value();

        const creditApplicationClaims = [
            creditApplicationClaimMock()
                .withProductID('test-3')
                .withState(ClaimState.APPROVED)
                .value(),
        ];
        const creditApplication = creditApplicationMock()
            .withClaims(creditApplicationClaims)
            .value();
        const banks = [
            bankMock().withBankID(BankID.ALFABANK).value(),
            bankMock().withBankID(BankID.TINKOFF).value(),
            bankMock().withBankID(BankID.SBERBANK).value(),
            bankMock().withBankID(BankID.RAIFFEISEN).value(),
        ];
        const creditProducts = [
            creditProductMock()
                .withBankID(BankID.ALFABANK)
                .withID('test-1')
                .withType(CreditProductType.AUTO)
                .value(),
            creditProductMock()
                .withBankID(BankID.TINKOFF)
                .withID('test-2')
                .withType(CreditProductType.CONSUMER)
                .value(),
        ];

        const creditProductsAll = [
            ...creditProducts,
            creditProductMock()
                .withBankID(BankID.RAIFFEISEN)
                .withID('test-3')
                .withType(CreditProductType.CONSUMER)
                .value(),
            creditProductMock()
                .withBankID(BankID.GAZPROMBANK)
                .withID('test-4')
                .withType(CreditProductType.CONSUMER)
                .value(),
        ];

        const ecreditProduct = creditProductMock()
            .withID('test-ecredit')
            .withType(CreditProductType.DEALER)
            .value();

        it('попадают текущие и ранее добавленные продукты', () => {
            const props = {
                banks,
                creditApplication,
                creditProducts,
                creditProductsAll,
                offer: availableOffer,
            };

            const result = getCreditProducts(props);
            const productIDsFromResult = result.map(product => product.id);

            expect(productIDsFromResult).toEqual([ 'test-3', 'test-1', 'test-2' ]);
        });

        it('для е-кредита - только он и ранее добавленные продукты', () => {
            const props = {
                banks,
                creditApplication,
                creditProducts: [ ecreditProduct ],
                creditProductsAll,
                offer: ecreditOffer,
            };

            const result = getCreditProducts(props);
            const productIDsFromResult = result.map(product => product.id);

            expect(productIDsFromResult).toEqual([ 'test-3', 'test-ecredit',

            ]);
        });
    });
});
