export interface IFactory<T> {
    create(attributes: Partial<T>): Promise<T>;

    createMany(number: number, attributes: Array<Partial<T>>): Promise<T[]>;
}
