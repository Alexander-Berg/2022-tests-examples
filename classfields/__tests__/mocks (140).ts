import noop from 'lodash/noop';

export interface IGate {
    create(): Promise<void>;
}

export const GatePending: IGate = {
    create: () => new Promise(noop),
};

export const GateSuccess: IGate = {
    create: () => Promise.resolve(),
};

export const GateError: IGate = {
    create: () => Promise.reject(),
};
