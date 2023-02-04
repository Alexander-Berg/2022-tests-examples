/* global expect, it */

import fs from 'fs';
import path from 'path';
import transformer from 'realty-core/app/lib/vos/transformer';

function requireAllFilesInFolder(folderPath) {
    const filesPathes = fs.readdirSync(path.join(__dirname, folderPath));

    return filesPathes
        .filter(filePath => filePath.endsWith('.json'))
        .map(filePath => require(`${folderPath}/${filePath}`));
}

const searcherOffers = requireAllFilesInFolder('./offers/searcher');
const vosOffers = requireAllFilesInFolder('./offers/vos');
const transformedVosOffers = vosOffers
    .map(vosCard => {
        try {
            return transformer.offerDataToPreview({ vosCard });
        } catch (e) {}
    })
    .filter(Boolean);

function findVosOffer(searcherOffer) {
    return transformedVosOffers.find(o => o.offerId === searcherOffer.offerId);
}

const defaultCompareWithVos = (searcherData, vosData) => expect(searcherData).toEqual(vosData);

// eslint-disable-next-line jest/no-export
module.exports = {
    everySearcherOffer(check, { checkVos = true } = {}) {
        searcherOffers.forEach(offer => {
            check(offer);
            if (checkVos) {
                const vosOffer = findVosOffer(offer);

                if (vosOffer) {
                    check(vosOffer);
                }
            }
        });
    },
    everySearcherOfferMatchSnapshot(getData, { checkVos = true, compareWithVos = defaultCompareWithVos, filter = () => true } = {}) {
        searcherOffers.filter(filter).forEach(offer => {
            it(`Offer ${offer.offerId}`, () => {
                const data = getData(offer);

                if (checkVos) {
                    const vosOffer = findVosOffer(offer);

                    if (vosOffer) {
                        const dataVos = getData(vosOffer);

                        compareWithVos(data, dataVos);
                    }
                }

                expect(data).toMatchSnapshot();
            });
        });
    }
};
