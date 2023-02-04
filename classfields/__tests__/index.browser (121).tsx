import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { getCardInfoByPan, banksAndPrefixes, getBrandByPan } from '@realty-front/payment-cards/app/libs';

import { PaymentCardItem } from '../';

const getAllBanksAndCards = (banks: typeof banksAndPrefixes) => {
    const result = {};

    Object.keys(banks.prefixes).forEach((prefix) => {
        const bankAlias = banks.prefixes[prefix];
        const cardNumber = `${prefix}0000005598`;
        const brand = getBrandByPan(cardNumber);

        result[bankAlias] = result[bankAlias] || {};
        result[bankAlias][brand] = cardNumber;
    });

    return result;
};

const allBanks = getAllBanksAndCards(banksAndPrefixes);

const render = async (component: React.ReactElement) => {
    await _render(component, { viewport: { width: 345, height: 600 } });

    expect(
        await takeScreenshot({
            fullPage: true,
        })
    ).toMatchImageSnapshot();
};

describe('PaymentCardItem', () => {
    it('Все поддерживаемые банки и карты', async () => {
        const cards: React.ReactNode[] = [];

        Object.keys(allBanks).forEach((bankAlias) => {
            Object.keys(allBanks[bankAlias]).forEach((brand) => {
                const pan = allBanks[bankAlias][brand];
                const cardInfo = getCardInfoByPan(pan);

                cards.push(
                    <div key={`${bankAlias}-${brand}`} style={{ marginBottom: '8px' }}>
                        <PaymentCardItem cardInfo={cardInfo} />
                    </div>
                );
            });
        });

        await render(<div>{cards}</div>);
    });

    it('Неизвестный банк и какрта', async () => {
        const cardInfo = getCardInfoByPan('000000******5598');

        await render(<PaymentCardItem cardInfo={cardInfo} />);
    });
});
