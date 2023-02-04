import {
    creditInitialFee,
    creditPaymentFrom,
    creditPaymentTo,
    maxInitialFee,
} from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';

import { setInitialFee } from './setInitialFee';

const onChange = jest.fn();

describe('setInitialFee', () => {
    it('ничего не делает и выходит если, price <= maxAmount', () => {
        setInitialFee(
            10,
            0,
            0,
            1000,
            0,
            0,
            [ { value: 1 } ],
            onChange,
        );

        expect(onChange).toHaveBeenCalledTimes(0);
    });

    it('вызывает onChange(creditInitialFee)', () => {
        setInitialFee(
            1000 + maxInitialFee,
            0,
            0,
            100,
            0,
            0,
            [ { value: 1 } ],
            onChange,
        );

        expect(onChange).toHaveBeenCalledWith(
            maxInitialFee, { name: creditInitialFee },
        );
    });

    it('вызывает onChange c from если paymentFrom > paymentTo', () => {
        setInitialFee(
            1000,
            0,
            100,
            100,
            0,
            0,
            [ { value: 1 } ],
            onChange,
        );

        expect(onChange).toHaveBeenCalledWith(
            1, { name: creditPaymentFrom },
        );
    });

    it('вызывает onChange c to', () => {
        setInitialFee(
            1000,
            0,
            0,
            100,
            0,
            0,
            [ { value: 1 } ],
            onChange,
        );

        expect(onChange).toHaveBeenCalledWith(
            1, { name: creditPaymentTo },
        );
    });
});
