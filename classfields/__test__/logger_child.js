const mockdate = require('mockdate');
mockdate.set('2020-10-18T17:00:50+03:00');

const logger = require('../logger');

logger.info('info');
logger.warn('warn');
logger.error('error');
logger.info({ foo: 'bar' }, 'info with obj');

const child = logger.child({ _context: 'context' });
child.info('child info');
