---
to:  <%= cwd %>/<%= name %>/configs/testing/node.js
---
const { REDIS_HOSTS, REDIS_CLUSTER_PORT, REDIS_CLUSTER_NAME, REDIS_PASSWORD } = process.env;

module.exports = {
    cache: {
        type: 'redis',
        options: {
            generation: '1',
            defaultKeyTTL: 1000 * 60 * 30,
            cacheTTL: 1000 * 60 * 60 * 24,
            readTimeout: 100,
            useCluster: true,
            clusterNodes: REDIS_HOSTS.split(','),
            redisCluster: {
                scaleReads: 'all',
                redisOptions: {
                    port: REDIS_CLUSTER_PORT,
                    name: REDIS_CLUSTER_NAME,
                    password: REDIS_PASSWORD,
                    family: 6,
                    commandTimeout: 250,
                    retryStrategy: () => {
                        return 60 * 1000; // в случае разрыва соединения пытаемся переподключаться с интервалом в 1 минуту.
                    }
                }
            }
        }
    },
    mailListSlug: 'C5WYBOT3-VK9'
};
