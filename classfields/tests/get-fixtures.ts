// eslint-disable-next-line
export function getFixtures(fixtures: Record<any, any>): any {
    const testName = expect.getState().currentTestName;

    const testFixtures = fixtures[testName];

    if (!testFixtures) {
        throw Error(`Ошибка! Фикстуры теста "${testName}" не найдены`);
    }

    return fixtures[testName];
}
