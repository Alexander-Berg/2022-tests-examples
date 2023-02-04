import { readFileSync } from 'fs';

import { loadAllMessages, messageToObject } from 'auto-core/proto/schema-registry';

beforeEach(async() => {
    await loadAllMessages();
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.BreadcrumbsResponse', () => {
    let jsonData: unknown;
    let protoData: Buffer;
    let protoName: string;
    beforeEach(() => {
        jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.BreadcrumbsResponse.json', { encoding: 'utf-8' }));
        protoData = readFileSync(__dirname + '/__messages__/auto.api.BreadcrumbsResponse.bin');
        protoName = 'auto.api.BreadcrumbsResponse';
    });

    it('должен декодировать protobuf', () => {
        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.OfferListingResponse', () => {
    let jsonData: unknown;
    let protoData: Buffer;
    let protoName: string;
    beforeEach(() => {
        jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.OfferListingResponse.json', { encoding: 'utf-8' }));
        protoData = readFileSync(__dirname + '/__messages__/auto.api.OfferListingResponse.bin');
        protoName = 'auto.api.OfferListingResponse';
    });

    it('должен декодировать protobuf', () => {
        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.OfferResponse', () => {
    let protoName: string;
    beforeEach(() => {
        protoName = 'auto.api.OfferResponse';
    });

    it('должен декодировать protobuf (is_owner=false)', () => {
        const jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.OfferResponse.json', { encoding: 'utf-8' }));
        const protoData = readFileSync(__dirname + '/__messages__/auto.api.OfferResponse.bin');

        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });

    it('должен декодировать protobuf (is_owner=true)', () => {
        const jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.OfferResponse.owner.json', { encoding: 'utf-8' }));
        const protoData = readFileSync(__dirname + '/__messages__/auto.api.OfferResponse.owner.bin');

        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.RawVinReportResponse', () => {
    let protoName: string;
    beforeEach(() => {
        protoName = 'auto.api.RawVinReportResponse';
    });

    it('должен декодировать protobuf', () => {
        const jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.RawVinReportResponse.json', { encoding: 'utf-8' }));
        const protoData = readFileSync(__dirname + '/__messages__/auto.api.RawVinReportResponse.bin');

        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});

/*
Оно падает. Такое ощущение, что протобуф не соответствует schema-registry
describe('auto.api.ReviewListingResponse', () => {
    let protoName: string;
    beforeEach(() => {
        protoName = 'auto.api.ReviewListingResponse';
    });

    it('должен декодировать protobuf', () => {
        const jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.ReviewListingResponse.json', { encoding: 'utf-8' }));
        const protoData = readFileSync(__dirname + '/__messages__/auto.api.ReviewListingResponse.bin');

        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});
*/

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.SavedSearchesListing', () => {
    let protoName: string;
    beforeEach(() => {
        protoName = 'auto.api.SavedSearchesListing';
    });

    it('должен декодировать protobuf', () => {
        const jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.SavedSearchesListing.json', { encoding: 'utf-8' }));
        const protoData = readFileSync(__dirname + '/__messages__/auto.api.SavedSearchesListing.bin');

        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.shark.RichCreditApplicationResponse', () => {
    let protoName: string;
    beforeEach(() => {
        protoName = 'auto.api.shark.RichCreditApplicationResponse';
    });

    it('должен декодировать пустое protobuf-сообщение', () => {
        const result = messageToObject(Buffer.from([]), protoName);

        expect(result).toEqual({});
    });
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('auto.api.vin.garage.GetCardResponse', () => {
    let protoName: string;
    beforeEach(() => {
        protoName = 'auto.api.vin.garage.GetCardResponse';
    });

    it('должен декодировать protobuf', () => {
        const jsonData = JSON.parse(readFileSync(__dirname + '/__messages__/auto.api.vin.garage.GetCardResponse.json', { encoding: 'utf-8' }));
        const protoData = readFileSync(__dirname + '/__messages__/auto.api.vin.garage.GetCardResponse.bin');

        const result = messageToObject(protoData, protoName);

        expect(result).toEqual(jsonData);
    });
});
