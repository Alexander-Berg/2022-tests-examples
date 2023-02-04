import {SinonSpy, spy} from 'sinon';

/**
 * Sinon doesn't allow to create a separate spy with a name, that is pretty much useful in error messages.
 * This method allows to get it, although not without some hacking.
 * TODO: remove when sinon gets support of named spies
 * @param name spy name
 * @param impl optional implementation
 *
 * @return spied function
 */
export default function (name: string, impl: Function = () => {}): SinonSpy {
    return spy({[name]: impl}, name);
}
