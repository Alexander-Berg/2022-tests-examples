// TODO FIXME
declare class MockChain<T> {
    constructor(data: any);
    registerMock: (mockName: string, callback: (unknown: unknown) => unknown) => void;
    value: () => T;
}

export = MockChain;
