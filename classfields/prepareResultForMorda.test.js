const resultMock = require('./result.mock');
const PrepareResultForMorda = require('www-search-app/lib/prepareResultForMorda');
const pseudoShuffle = (arr) => arr;

describe('PrepareResultForMorda должен', () => {
    describe('отдать результат для inserts', () => {
        it('desktop', () => {
            const opts = {
                platform: 'desktop',
                inserts: true,
                shuffle: false,
                experiments: [],
            };

            const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });

        it('touch', () => {
            const opts = {
                platform: 'touch',
                inserts: true,
                shuffle: false,
                experiments: [],
            };

            const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });
    });

    describe('отдать результат для div2', () => {
        it('прост', () => {
            const opts = {
                platform: 'desktop',
                inserts: false,
                shuffle: false,
                experiments: [],
            };

            const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });

        it('с экспом про ссылки для контрольной группы', () => {
            const opts = {
                platform: 'desktop',
                inserts: false,
                shuffle: false,
                experiments: [ 'listinglinks_etalon' ],
            };

            const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });

        it('с несколькими экспами и не поломаться', () => {
            const opts = {
                platform: 'desktop',
                inserts: false,
                shuffle: false,
                experiments: [ 'listinlinks', 'morelinks' ],
            };

            const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });

        it('с переименованным первым табом и подмешанным оффером из просмотренных', () => {
            const opts = {
                platform: 'desktop',
                inserts: false,
                shuffle: false,
                experiments: [],
            };

            const mock = {
                ...resultMock,
                recommended: {
                    ...resultMock.recommended,
                    offers: resultMock.recommended.offers.slice(0, 2),
                },
            };

            const preparer = new PrepareResultForMorda(mock, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });

    });

    describe('отдать результат для div2 в експе autoru-separated-view-tabs', () => {
        it('прост', () => {
            const opts = {
                platform: 'touch',
                inserts: false,
                shuffle: false,
                experiments: [ 'autoru-separated-view-tabs' ],
            };

            const resultMockImproved = {
                ...resultMock,
                recommended: {
                    ...resultMock.recommended,
                    offers: [
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                    ],
                } };
            const preparer = new PrepareResultForMorda(resultMockImproved, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });
    });

    describe('отдать результат для div2 в експе autoru-separated-view-tabs в inserts', () => {
        it('прост', () => {
            const opts = {
                platform: 'touch',
                inserts: true,
                shuffle: false,
                experiments: [ 'autoru-separated-view-tabs' ],
            };

            const resultMockImproved = {
                ...resultMock,
                recommended: {
                    ...resultMock.recommended,
                    offers: [
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                        ...resultMock.recommended.offers,
                    ],
                } };
            const preparer = new PrepareResultForMorda(resultMockImproved, opts, pseudoShuffle);

            const data = preparer.getJson();

            expect(data).toMatchSnapshot();
        });
    });

    it('Отдать listingUrl для всех табов', () => {
        const opts = {
            platform: 'touch',
            inserts: true,
            shuffle: false,
        };

        const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

        const data = preparer.getJson();

        expect(data.tabs.filter(tab => !tab.listingUrl)).toHaveLength(0);
    });

    it('Поставить пресет new-cars на первое место', () => {
        const opts = {
            platform: 'desktop',
            inserts: true,
            shuffle: false,
        };

        const resultMockImproved = {
            geo: [ ...resultMock.geo,
                {
                    alias: 'new-cars',
                    text: 'Новые авто',
                    offers: [ ...resultMock.recommended.offers ],
                },
            ],
            recommended: [],
        };

        const preparer = new PrepareResultForMorda(resultMockImproved, opts, pseudoShuffle);

        expect(preparer.getJson().tabs[0].title === 'Новые авто').toEqual(true);
    });
});
