import faker from 'faker/locale/ru';

export function generateList<T>(cb: { (): T }, length: number = faker.datatype.number(10)): T[] {
    return Array.from(Array(length)).map(() => cb());
}
