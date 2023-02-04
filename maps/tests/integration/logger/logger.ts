import {Logger, ConsoleHandler} from 'src/lib/logging';

const logger = new Logger(new ConsoleHandler({
    level: 'silly',
    colorize: false
}));

logger.silly('some silly message');
logger.debug('some debug message');
logger.verbose('some verbose message');
logger.info('some info message');
logger.warn('some warn message');
logger.error('some error message');
