import React, { useState, useEffect, useCallback, useContext, useMemo, useRef } from 'react';

import debounce from '@tinkoff/utils/function/debounce';
import { createSearchString } from '@trust/utils/url';
import { Checkbox } from '@yandex-lego/components/Checkbox/desktop/bundle';
import { Text } from '@yandex-lego/components/Text/desktop/bundle';
import { block } from 'bem-cn';

import { getPaymentSheet } from '../../../helpers/payment-sheet';
import { buttonContext } from '../../../store/button';
import { scenarioContext } from '../../../store/scenario';
import { testSettingsContext } from '../../../store/test-settings';

import { FrameCSP } from './FrameCSP';
import './index.css';

const b = block('test-settings');

const basePaymentSheet = getPaymentSheet();

const toAmount = (val: string): string => {
    const num = Number(val);

    return num ? num.toFixed(0) : '';
};

export function TestSettings(): JSX.Element {
    const buttonCtx = useContext(buttonContext);
    const scenarioCtx = useContext(scenarioContext);
    const tsCtx = useContext(testSettingsContext);

    const queryRef = useRef<HTMLInputElement>(null);

    const [iconSrc, setIconSrc] = useState('');
    const [totalAmount, setTotalAmount] = useState(() =>
        toAmount(scenarioCtx.sheet.data.order.total.amount),
    );

    const [coupon, setCoupon] = useState(false);

    const [readyCheck, setReadyCheck] = useState({ checkbox: false, status: 'loading' });

    useEffect(() => {
        const { YaPay } = window;

        YaPay!.Robokassa.getTabIcon().then(setIconSrc);
    }, []);

    useEffect(() => {
        const { YaPay } = window;
        const options = {
            merchantId: basePaymentSheet.merchant.id,
            paymentMethods: basePaymentSheet.paymentMethods,
            checkActiveCard: readyCheck.checkbox,
        };

        setReadyCheck((prev) => ({ ...prev, status: 'loading' }));

        YaPay!.readyToPayCheck(options).then((isReady) => {
            setReadyCheck((prev) => ({ ...prev, status: isReady ? 'ready' : 'not-ready' }));
        });
    }, [readyCheck.checkbox]);

    useEffect(() => {
        if (queryRef.current) {
            const query = createSearchString({
                sheet: JSON.stringify(scenarioCtx.sheet.data),
                'button-style': JSON.stringify(buttonCtx.style),
                'button-custom-style': JSON.stringify(buttonCtx.customStyle),
            });

            queryRef.current.value = `?${query}`;
        }
    }, [queryRef, scenarioCtx.sheet.data, buttonCtx.style, buttonCtx.customStyle]);

    const toggleReadyCheckCheckbox = useCallback(() => {
        setReadyCheck((prev) =>
            prev.status === 'loading' ? prev : { ...prev, checkbox: !prev.checkbox },
        );
    }, []);

    const toggleButtonGiftBadge = useCallback(() => {
        buttonCtx.setStyle('withGiftBadge10p', !buttonCtx.style.withGiftBadge10p);
    }, [buttonCtx]);

    const changeTotalAmount = useMemo(() => {
        const updateSheet = debounce(500, (amount) => {
            if (amount) {
                scenarioCtx.updateSheet({
                    order: {
                        ...scenarioCtx.sheet.data.order,
                        total: { amount },
                        items: [{ label: 'Item A', amount }],
                    },
                });
            }
        });

        return (event: React.ChangeEvent<HTMLInputElement>): void => {
            setTotalAmount(event.target.value);
            updateSheet(toAmount(event.target.value));
        };
    }, [scenarioCtx]);

    const changeCoupon = useMemo(() => {
        const updateSheet = debounce(500, (hasCoupon) => {
            scenarioCtx.replaceSheet({
                ...scenarioCtx.sheet.data,
                additionalFields: { coupons: hasCoupon },
            });
        });

        return (event: React.ChangeEvent<HTMLInputElement>): void => {
            setCoupon(event.target.checked);
            updateSheet(Boolean(event.target.checked));
        };
    }, [scenarioCtx]);

    return (
        <div className={b()}>
            <Text className={b('title').toString()} as="h2" typography="headline-l" weight="bold">
                Настройки для test.pay.yandex.ru
            </Text>
            <div className={b('row')}>
                <Text typography="headline-s" weight="regular">
                    YaPay.readyToPayCheck
                </Text>
                <div className={b('cell')}>
                    <Checkbox
                        label="{checkActiveCard: true}"
                        size="m"
                        view="default"
                        onChange={toggleReadyCheckCheckbox}
                        checked={readyCheck.checkbox}
                    />
                    <Text typography="headline-s" weight="regular">
                        {` --- STATUS: ${readyCheck.status}`}
                    </Text>
                </div>
            </div>
            <div className={b('row')}>
                <Text typography="headline-s" weight="regular">
                    TotalAmount
                </Text>
                <div className={b('cell')}>
                    <input onChange={changeTotalAmount} value={totalAmount} />
                </div>
            </div>
            <div className={b('row')}>
                <Text typography="headline-s" weight="regular">
                    Coupon
                </Text>
                <div className={b('cell')}>
                    <Checkbox size="m" view="default" onChange={changeCoupon} checked={coupon} />
                </div>
            </div>
            <div className={b('row')}>
                <Text typography="headline-s" weight="regular">
                    Tele2 gift badge
                </Text>
                <div className={b('cell')}>
                    <Checkbox
                        size="m"
                        view="default"
                        onChange={toggleButtonGiftBadge}
                        checked={buttonCtx.style.withGiftBadge10p === true}
                    />
                </div>
            </div>
            <div className={b('row')}>
                <Text typography="headline-s" weight="regular">
                    Эксперименты
                </Text>
                <div className={b('cell').mix(b('code')).toString()}>
                    <code>
                        <pre>{JSON.stringify(tsCtx.data.abt, null, 2)}</pre>
                    </code>
                </div>
            </div>
            {iconSrc ? (
                <div className={b('row')}>
                    <Text typography="headline-s" weight="regular">
                        Таб на Робокассе
                    </Text>
                    <img
                        alt="Robokassa tab icon"
                        className={b('cell').mix(b('rk-icon')).toString()}
                        src={iconSrc}
                    />
                </div>
            ) : null}

            <div className={b('row')}>
                <Text typography="headline-s" weight="regular">
                    Query
                </Text>
                <div className={b('cell')}>
                    <input ref={queryRef} readOnly />
                </div>
            </div>

            <hr />

            <FrameCSP />
        </div>
    );
}
