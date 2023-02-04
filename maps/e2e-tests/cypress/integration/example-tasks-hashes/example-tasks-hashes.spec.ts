import { TaskTypeEnum } from '../../../src/utils/constants';

const failMessage = 'Hash of relevant example does not match known hash';

describe('Hashes of examples for planning', () => {
  beforeEach(function () {
    cy.fixture('example-task-hashes.json').as('hashes');
    cy.fixture('parent-example-task-hashes').as('parentHashes');
  });

  it('Hash of the demo example', function () {
    const demoHash = this.hashes[TaskTypeEnum.DEMO];
    expect(demoHash).to.equal(this.parentHashes[TaskTypeEnum.DEMO], failMessage);
  });

  it('Hash of a simple example in Russian', function () {
    const simpleRuHash = this.hashes[TaskTypeEnum.SIMPLE_RU];
    expect(simpleRuHash).to.equal(this.parentHashes[TaskTypeEnum.SIMPLE_RU], failMessage);
  });

  it('Hash of a simple example in English', function () {
    const simpleEnHash = this.hashes[TaskTypeEnum.SIMPLE_EN];
    expect(simpleEnHash).to.equal(this.parentHashes[TaskTypeEnum.SIMPLE_EN], failMessage);
  });

  it('Hash of a simple example in Spanish (es)', function () {
    const simpleEnHash = this.hashes[TaskTypeEnum.SIMPLE_ES];
    expect(simpleEnHash).to.equal(this.parentHashes[TaskTypeEnum.SIMPLE_ES], failMessage);
  });

  it('Hash of an extended simple example in Russian', function () {
    const extendedRuHash = this.hashes[TaskTypeEnum.EXTENDED_RU];
    expect(extendedRuHash).to.equal(this.parentHashes[TaskTypeEnum.EXTENDED_RU], failMessage);
  });

  it('Hash of an extended simple example in English', function () {
    const extendedEnHash = this.hashes[TaskTypeEnum.EXTENDED_EN];
    expect(extendedEnHash).to.equal(this.parentHashes[TaskTypeEnum.EXTENDED_EN], failMessage);
  });

  it('Hash of an extended simple example in Spanish (es)', function () {
    const extendedEnHash = this.hashes[TaskTypeEnum.EXTENDED_ES];
    expect(extendedEnHash).to.equal(this.parentHashes[TaskTypeEnum.EXTENDED_ES], failMessage);
  });
});
