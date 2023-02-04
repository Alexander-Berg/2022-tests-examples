import {expect} from 'chai';

export function sortByRecordIds(items: any[]) {
    items.sort((a, b) => {
        return a.record_id.localeCompare(b.record_id);
    });
}

export function sortByPublicId(items: any[]) {
    items.sort((a, b) => {
        return a.public_id.localeCompare(b.public_id);
    });
}

export function expectArraysDeepEqual(actual: any[], expected: any[]) {
    expect(actual.length).to.be.equal(expected.length);

    // Output order is not guaranteed!
    sortByRecordIds(actual);
    sortByRecordIds(expected);

    expect(actual).to.be.deep.equal(expected);
}
