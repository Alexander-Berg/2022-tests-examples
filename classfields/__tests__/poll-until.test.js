const { pollUntil, wait } = require('../poll-until');

function *generatorCreator() {
    let z = 0;

    while (z < 100) {
        yield z++;
    }

    return z;
}

const promiseCreator = async generator => {
    await wait(100);
    return generator.next().value;
};

const promiseRejector = async generator => {
    await wait(100);
    throw generator.next().value;
};

const promiseCreatorWithRejections = async generator => {
    await wait(100);
    const value = generator.next().value;

    if (value % 2 === 0) {
        throw value;
    }

    return value;
};

describe('pollUntil', () => {
    it('polls 5 times and resolves with the last promise result when no results satisfy checker function', async() => {
        const generator = generatorCreator();

        const resolved = await pollUntil(
            promiseCreator,
            [ generator ],
            result => result === 99999,
            200,
            5
        );

        expect(resolved).toEqual(4);
        expect(generator.next().value).toEqual(5);
    });

    it('polls 3 times and resolves when the 3rd out of 10 result satisfies checker function', async() => {
        const generator = generatorCreator();

        const resolved = await pollUntil(
            promiseCreator,
            [ generator ],
            result => result === 2,
            200,
            10
        );

        expect(resolved).toEqual(2);
        expect(generator.next().value).toEqual(3);
    });

    it('polls 5 times and rejects with the result of the last rejected promise when every promise is rejected',
        async() => {
            const generator = generatorCreator();
            let rejected;

            try {
                await pollUntil(
                    promiseRejector,
                    [ generator ],
                    result => result === 99999,
                    200,
                    5
                );
            } catch (e) {
                rejected = e;
            }

            expect(rejected).toEqual(4);
            expect(generator.next().value).toEqual(5);
        });

    it('polls 5 times and resolves with the last resolved promise result when some promises were rejected', async() => {
        const generator = generatorCreator();

        const resolved = await pollUntil(
            promiseCreatorWithRejections,
            [ generator ],
            result => result === 99999,
            200,
            5
        );

        expect(resolved).toEqual(3);
        expect(generator.next().value).toEqual(5);
    });
});
