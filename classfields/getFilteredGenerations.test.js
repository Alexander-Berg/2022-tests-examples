const getFilteredGenerations = require('./getFilteredGenerations');

describe('фильтр по годам', () => {
    it('правильно фильтрует поколения, если не задан фильтр по годам', () => {
        const filter = { };
        const generations = [
            { entity: { year_from: 2020, year_to: 2021 } },
            { entity: { year_from: 2008, year_to: 2010 } },
        ];

        const result = getFilteredGenerations(filter, generations);

        expect(result).toEqual([
            generations[0],
            generations[1],
        ]);
    });

    it('правильно фильтрует поколения, если задан полный фильтр по годам', () => {
        const filter = { year_from: 2020, year_to: 2022 };
        const generations = [
            { entity: { year_from: 2020, year_to: 2021 } },
            { entity: { year_from: 2008, year_to: 2010 } },
            { entity: { year_from: 2008, year_to: 2023 } },
            { entity: { year_from: 2012, year_to: 2020 } },
            { entity: { year_from: 2021, year_to: 2025 } },
        ];

        const result = getFilteredGenerations(filter, generations);

        expect(result).toEqual([
            generations[0],
            generations[2],
            generations[3],
            generations[4],
        ]);
    });

    it('правильно фильтрует поколения, если year_from = year_to', () => {
        const filter = { year_from: 2020, year_to: 2020 };
        const generations = [
            { entity: { year_from: 2020, year_to: 2021 } },
            { entity: { year_from: 2008, year_to: 2010 } },
            { entity: { year_from: 2008, year_to: 2023 } },
            { entity: { year_from: 2012, year_to: 2020 } },
            { entity: { year_from: 2021, year_to: 2025 } },
        ];

        const result = getFilteredGenerations(filter, generations);

        expect(result).toEqual([
            generations[0],
            generations[2],
            generations[3],
        ]);
    });

    it('правильно фильтрует поколения, если задан не валидный фильтр по годам', () => {
        const filter = { year_from: 2020, year_to: 2010 };
        const generations = [
            { entity: { year_from: 2020, year_to: 2021 } },
            { entity: { year_from: 2008, year_to: 2010 } },
            { entity: { year_from: 2008, year_to: 2023 } },
            { entity: { year_from: 2012, year_to: 2020 } },
            { entity: { year_from: 2021, year_to: 2025 } },
        ];

        const result = getFilteredGenerations(filter, generations);

        expect(result).toEqual(generations);
    });

    it('правильно фильтрует поколения, если задан фильтр year_from', () => {
        const filter = { year_from: 2020 };
        const generations = [
            { entity: { year_from: 2020, year_to: 2021 } },
            { entity: { year_from: 2008, year_to: 2010 } },
            { entity: { year_from: 2012, year_to: 2021 } },
            { entity: { year_from: 2022, year_to: 2025 } },
        ];

        const result = getFilteredGenerations(filter, generations);

        expect(result).toEqual([
            generations[0],
            generations[2],
            generations[3],
        ]);
    });

    it('правильно фильтрует поколения, если задан фильтр year_to', () => {
        const filter = { year_to: 2020 };
        const generations = [
            { entity: { year_from: 2020, year_to: 2021 } },
            { entity: { year_from: 2008, year_to: 2010 } },
            { entity: { year_from: 2012, year_to: 2021 } },
            { entity: { year_from: 2022, year_to: 2025 } },
        ];

        const result = getFilteredGenerations(filter, generations);

        expect(result).toEqual([
            generations[0],
            generations[1],
            generations[2],
        ]);
    });
});
