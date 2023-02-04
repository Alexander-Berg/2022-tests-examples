import catalogFilterToJournalTags from './catalogFilterToJournalTags';

describe('должен правильно перевести catalogFilter в тэги журнала', () => {
    it('с указанной маркой', () => {
        const catalogFilter = {
            mark: 'AUDI',
        };

        expect(catalogFilterToJournalTags(catalogFilter)).toEqual('AUDI');
    });

    it('с указанной маркой и моделью', () => {
        const catalogFilter = {
            mark: 'AUDI',
            model: 'A4',
        };

        expect(catalogFilterToJournalTags(catalogFilter)).toEqual('AUDI-A4');
    });

    it('с указанной ничем', () => {
        const catalogFilter = {};

        expect(catalogFilterToJournalTags(catalogFilter)).toBeUndefined();
    });
});
