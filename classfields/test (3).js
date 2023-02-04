module.exports = {
    environment: 'test',
    mysql: {
        host: '127.0.0.1',
        port: 3306,
        username: 'testing',
        password: 'testing',
        database: 'testing',
        timezone: '+03:00',
        logging: 'error',
        synchronize: true,
        dropSchema: true,
        // https://github.com/nestjs/typeorm/issues/61#issuecomment-455778208
        keepConnectionAlive: true,
    },
};
