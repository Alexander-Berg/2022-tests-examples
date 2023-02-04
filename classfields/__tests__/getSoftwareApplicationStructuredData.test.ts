import { SoftwareApplication, WithContext } from 'schema-dts';

import { getSoftwareApplicationStructuredData } from '../getSoftwareApplicationStructuredData';

describe('Должен вернуть разметку приложений', () => {
    it('IOS', () => {
        const iosSoftwareApplicationStructuredData = getSoftwareApplicationStructuredData()[0];
        const result: WithContext<SoftwareApplication> = {
            '@context': 'https://schema.org',
            '@type': 'SoftwareApplication',
            aggregateRating: {
                '@type': 'AggregateRating',
                ratingCount: 115954,
                ratingValue: 4.8,
            },
            applicationCategory: 'https://schema.org/BusinessApplication',
            name: 'Яндекс.Недвижимость',
            operatingSystem: 'IOS',
        };

        expect(iosSoftwareApplicationStructuredData).toMatchObject(result);
    });

    it('Android', () => {
        const androidSoftwareApplicationStructuredData = getSoftwareApplicationStructuredData()[1];
        const result: WithContext<SoftwareApplication> = {
            '@context': 'https://schema.org',
            '@type': 'SoftwareApplication',
            aggregateRating: {
                '@type': 'AggregateRating',
                ratingCount: 19273,
                ratingValue: 4.3,
            },
            applicationCategory: 'https://schema.org/BusinessApplication',
            name: 'Яндекс.Недвижимость',
            operatingSystem: 'Android',
        };

        expect(androidSoftwareApplicationStructuredData).toMatchObject(result);
    });
});
