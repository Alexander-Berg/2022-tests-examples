import dayjs from '@realty-front/dayjs';

import { IPriceStatisticsRoomItem } from 'realty-core/types/priceStatistics';

import styles from './styles.module.css';

export const defaultProps = {
    chartHeight: 400,
    xAxisHeight: 20,
    referenceLabelOffset: 5,
    labelOffset: 20,
    avgLabelWidth: 45,
    referenceLabelClassName: styles.label,
} as const;

export const getData = (dateStr: string, count: number, reverse?: boolean): IPriceStatisticsRoomItem[] => {
    const date = dayjs(dateStr);
    const result: IPriceStatisticsRoomItem[] = [];
    const startValue = 5000000;
    const startValuePerM = 100000;

    for (let i = 0; i < count; i++) {
        const timestamp = reverse ? date.subtract(i, 'M').toISOString() : date.add(i, 'M').toISOString();
        const item = {
            timestamp,
            meanPrice: String(startValue + 100000 * i),
            meanPricePerM2: String(startValuePerM + 10000 * i),
        };

        if (reverse) {
            result.unshift(item);
        } else {
            result.push(item);
        }
    }

    return result;
};

// REALTYFRONT-12974
export const case12974Data: IPriceStatisticsRoomItem[] = [
    {
        meanPrice: '165382620',
        meanPricePerM2: '955805',
        timestamp: '2019-09-30T00:00:00Z',
    },
    {
        meanPrice: '165744634',
        meanPricePerM2: '958392',
        timestamp: '2019-10-31T00:00:00Z',
    },
    {
        meanPrice: '165599701',
        meanPricePerM2: '957366',
        timestamp: '2019-11-30T00:00:00Z',
    },
    {
        meanPrice: '165599701',
        meanPricePerM2: '957366',
        timestamp: '2019-12-31T00:00:00Z',
    },
    {
        meanPrice: '192465870',
        meanPricePerM2: '1174229',
        timestamp: '2021-08-31T00:00:00Z',
    },
];
