export function ioredis() {
    return {
        get: () => Promise.resolve(undefined),
        set: () => Promise.resolve(undefined),
        del: () => Promise.resolve(undefined),
    };
}

ioredis.Cluster = function () {
    return {
        get: () => Promise.resolve(undefined),
        set: () => Promise.resolve(undefined),
        del: () => Promise.resolve(undefined),
    };
};
