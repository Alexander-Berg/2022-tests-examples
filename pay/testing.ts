import BaseCore from '../../core/base-core';
import TestingConfig from '../testing';

export default class TestingCoreConfig extends TestingConfig {
    core: BaseCore;

    constructor(core: BaseCore) {
        super();

        this.core = core;
    }
}
