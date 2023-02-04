import { feeds } from './feeds';

export const getStore = ({ status }: { status?: string } = {}) => ({
    feeds: { ...feeds, status },
    config: {
        timeDelta: 0,
        serverTimeStamp: 1607945477604,
    },
    user: {
        crc: '0',
    },
});
