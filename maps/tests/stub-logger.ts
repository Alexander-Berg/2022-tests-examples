import {Logger, NullHandler} from 'src/lib/logging';

export const stubLogger = new Logger(new NullHandler());
