import { Int32, Int64, int64 } from '../../../../../../../common/ys'
import { EntityKind } from '../../../../../../mapi/code/api/entities/entity-kind'
import { IDSupport } from '../../../../../code/api/common/id-support'
import {
  DB,
  DBEntityConflictResolution,
  DBEntityDescriptor,
  DBEntityField,
  DBEntityFieldConstraint,
  DBEntityFieldDefaultConstraint,
  DBEntityFieldForeignKeyConstraint,
  DBEntityFieldNonNullConstraint,
  DBEntityFieldPrimaryKeyConstraint,
  DBEntityFieldType,
  DBEntityFieldUniqueConstraint,
  DBEntityPrimaryKeyConstraint,
  DBEntityUniqueConstraint,
} from '../../../../../code/api/storage/scheme-support/db-entity'
import { Polytype } from '../../../../../code/utils/polytype'
import { StringBuilder } from '../../../../../../mapi/code/utils/string-builder'
import { TestIDSupport } from '../../../../__helpers__/test-id-support'

type ConflictResolutions = 'rollback' | 'abort' | 'fail' | 'ignore' | 'replace'
function mapConflictResolution(resolution: ConflictResolutions): DBEntityConflictResolution {
  return ({
    rollback: DBEntityConflictResolution.rollback,
    abort: DBEntityConflictResolution.abort,
    fail: DBEntityConflictResolution.fail,
    ignore: DBEntityConflictResolution.ignore,
    replace: DBEntityConflictResolution.replace,
  } as Record<ConflictResolutions, DBEntityConflictResolution>)[resolution]
}
function f(name: string, isInteger: boolean, constraints: readonly DBEntityFieldConstraint[]): DBEntityField {
  return new DBEntityField(name, isInteger ? DBEntityFieldType.integer : DBEntityFieldType.string, constraints)
}
function u(resolution: ConflictResolutions): DBEntityFieldUniqueConstraint {
  return new DBEntityFieldUniqueConstraint(mapConflictResolution(resolution))
}
function pk(autoincrementing: boolean, resolution: ConflictResolutions): DBEntityFieldUniqueConstraint {
  return new DBEntityFieldPrimaryKeyConstraint(autoincrementing, mapConflictResolution(resolution))
}
function n(resolution: ConflictResolutions): DBEntityFieldUniqueConstraint {
  return new DBEntityFieldNonNullConstraint(mapConflictResolution(resolution))
}
function d(value: Int32 | Int64 | string): DBEntityFieldDefaultConstraint {
  let polyvalue: Polytype
  switch (typeof value) {
    case 'bigint':
      polyvalue = Polytype.int64(value)
      break
    case 'number':
      polyvalue = Polytype.int32(value)
      break
    case 'string':
      polyvalue = Polytype.string(value)
      break
    default:
      throw new TypeError(`Unsupported type for Polytype: ${typeof value}.`)
  }
  return new DBEntityFieldDefaultConstraint(polyvalue)
}
function fk(entity: EntityKind, field: string): DBEntityFieldForeignKeyConstraint {
  return new DBEntityFieldForeignKeyConstraint(entity, field)
}
function U(fields: readonly string[], resolution: ConflictResolutions): DBEntityUniqueConstraint {
  return new DBEntityUniqueConstraint(fields, mapConflictResolution(resolution))
}
function PK(fields: readonly string[], resolution: ConflictResolutions): DBEntityPrimaryKeyConstraint {
  return new DBEntityPrimaryKeyConstraint(fields, mapConflictResolution(resolution))
}

describe('CREATE TABLE script generation', () => {
  describe(DBEntityFieldPrimaryKeyConstraint, () => {
    it('should generate primary key constraint on column', () => {
      expect(pk(false, 'abort').asScript()).toBe('PRIMARY KEY')
      expect(pk(false, 'fail').asScript()).toBe('PRIMARY KEY ON CONFLICT FAIL')
      expect(pk(false, 'ignore').asScript()).toBe('PRIMARY KEY ON CONFLICT IGNORE')
      expect(pk(false, 'replace').asScript()).toBe('PRIMARY KEY ON CONFLICT REPLACE')
      expect(pk(false, 'rollback').asScript()).toBe('PRIMARY KEY ON CONFLICT ROLLBACK')

      expect(pk(true, 'abort').asScript()).toBe('PRIMARY KEY AUTOINCREMENT')
      expect(pk(true, 'fail').asScript()).toBe('PRIMARY KEY ON CONFLICT FAIL AUTOINCREMENT')
      expect(pk(true, 'ignore').asScript()).toBe('PRIMARY KEY ON CONFLICT IGNORE AUTOINCREMENT')
      expect(pk(true, 'replace').asScript()).toBe('PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT')
      expect(pk(true, 'rollback').asScript()).toBe('PRIMARY KEY ON CONFLICT ROLLBACK AUTOINCREMENT')
    })
  })
  describe(DBEntityFieldUniqueConstraint, () => {
    it('should generate unique constraint on column', () => {
      expect(u('abort').asScript()).toBe('UNIQUE')
      expect(u('fail').asScript()).toBe('UNIQUE ON CONFLICT FAIL')
      expect(u('ignore').asScript()).toBe('UNIQUE ON CONFLICT IGNORE')
      expect(u('replace').asScript()).toBe('UNIQUE ON CONFLICT REPLACE')
      expect(u('rollback').asScript()).toBe('UNIQUE ON CONFLICT ROLLBACK')
    })
  })
  describe(DBEntityFieldNonNullConstraint, () => {
    it('should generate not null constraint on column', () => {
      expect(n('abort').asScript()).toBe('NOT NULL')
      expect(n('fail').asScript()).toBe('NOT NULL ON CONFLICT FAIL')
      expect(n('ignore').asScript()).toBe('NOT NULL ON CONFLICT IGNORE')
      expect(n('replace').asScript()).toBe('NOT NULL ON CONFLICT REPLACE')
      expect(n('rollback').asScript()).toBe('NOT NULL ON CONFLICT ROLLBACK')
    })
  })
  describe(DBEntityFieldDefaultConstraint, () => {
    it('should generate default constraint on column', () => {
      expect(d(100).asScript()).toBe('DEFAULT 100')
      expect(d('hello').asScript()).toBe('DEFAULT "hello"')
    })
  })
  describe(DBEntityFieldForeignKeyConstraint, () => {
    it('should generate references constraint on column', () => {
      expect(fk(EntityKind.message, 'id').asScript()).toBe('REFERENCES message(id)')
    })
  })
  describe(DBEntityUniqueConstraint, () => {
    it('should generate unique constraint on table', () => {
      expect(U([], 'abort').asScript()).toBe('')
      expect(U(['id1'], 'abort').asScript()).toBe('UNIQUE (id1)')
      expect(U(['id1', 'id2'], 'abort').asScript()).toBe('UNIQUE (id1, id2)')
      expect(U(['id1'], 'fail').asScript()).toBe('UNIQUE (id1) ON CONFLICT FAIL')
      expect(U(['id1', 'id2'], 'fail').asScript()).toBe('UNIQUE (id1, id2) ON CONFLICT FAIL')
      expect(U(['id1'], 'ignore').asScript()).toBe('UNIQUE (id1) ON CONFLICT IGNORE')
      expect(U(['id1', 'id2'], 'ignore').asScript()).toBe('UNIQUE (id1, id2) ON CONFLICT IGNORE')
      expect(U(['id1'], 'replace').asScript()).toBe('UNIQUE (id1) ON CONFLICT REPLACE')
      expect(U(['id1', 'id2'], 'replace').asScript()).toBe('UNIQUE (id1, id2) ON CONFLICT REPLACE')
      expect(U(['id1'], 'rollback').asScript()).toBe('UNIQUE (id1) ON CONFLICT ROLLBACK')
      expect(U(['id1', 'id2'], 'rollback').asScript()).toBe('UNIQUE (id1, id2) ON CONFLICT ROLLBACK')
    })
  })
  describe(DBEntityPrimaryKeyConstraint, () => {
    it('should generate primary key constraint on table', () => {
      expect(PK([], 'abort').asScript()).toBe('')
      expect(PK(['id1'], 'abort').asScript()).toBe('PRIMARY KEY (id1)')
      expect(PK(['id1', 'id2'], 'abort').asScript()).toBe('PRIMARY KEY (id1, id2)')
      expect(PK(['id1'], 'fail').asScript()).toBe('PRIMARY KEY (id1) ON CONFLICT FAIL')
      expect(PK(['id1', 'id2'], 'fail').asScript()).toBe('PRIMARY KEY (id1, id2) ON CONFLICT FAIL')
      expect(PK(['id1'], 'ignore').asScript()).toBe('PRIMARY KEY (id1) ON CONFLICT IGNORE')
      expect(PK(['id1', 'id2'], 'ignore').asScript()).toBe('PRIMARY KEY (id1, id2) ON CONFLICT IGNORE')
      expect(PK(['id1'], 'replace').asScript()).toBe('PRIMARY KEY (id1) ON CONFLICT REPLACE')
      expect(PK(['id1', 'id2'], 'replace').asScript()).toBe('PRIMARY KEY (id1, id2) ON CONFLICT REPLACE')
      expect(PK(['id1'], 'rollback').asScript()).toBe('PRIMARY KEY (id1) ON CONFLICT ROLLBACK')
      expect(PK(['id1', 'id2'], 'rollback').asScript()).toBe('PRIMARY KEY (id1, id2) ON CONFLICT ROLLBACK')
    })
  })
  describe(DBEntityDescriptor, () => {
    it('should generate table creation script', () => {
      const entity = new DBEntityDescriptor(
        EntityKind.message,
        [
          f('id', true, [pk(true, 'replace')]),
          f('name', false, [u('fail')]),
          f('age', true, [n('abort'), d(18)]),
          f('gender', false, [d('female')]),
          f('address', false, []),
          f('manager', true, [fk(EntityKind.message, 'id'), n('rollback')]),
        ],
        [],
        true,
      )
      expect(entity.asScript()).toBe(
        new StringBuilder()
          .addLine('CREATE TABLE IF NOT EXISTS message (')
          .addLine('id INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT,')
          .addLine('name TEXT UNIQUE ON CONFLICT FAIL,')
          .addLine('age INTEGER NOT NULL DEFAULT 18,')
          .addLine('gender TEXT DEFAULT "female",')
          .addLine('address TEXT,')
          .addLine('manager INTEGER REFERENCES message(id) NOT NULL ON CONFLICT ROLLBACK')
          .add(');')
          .build(),
      )
    })
    it('should generate table creation script with table constraints', () => {
      const entity = new DBEntityDescriptor(
        EntityKind.message,
        [f('id1', true, []), f('id2', false, []), f('lhs', true, []), f('rhs', false, [])],
        [PK(['id1', 'id2'], 'rollback'), U(['lhs', 'rhs'], 'replace')],
        false,
      )
      expect(entity.asScript()).toBe(
        new StringBuilder()
          .addLine('CREATE TABLE message (')
          .addLine('id1 INTEGER,')
          .addLine('id2 TEXT,')
          .addLine('lhs INTEGER,')
          .addLine('rhs TEXT,')
          .addLine('PRIMARY KEY (id1, id2) ON CONFLICT ROLLBACK,')
          .addLine('UNIQUE (lhs, rhs) ON CONFLICT REPLACE')
          .add(');')
          .build(),
      )
    })
  })
})

describe(DB, () => {
  it('should throw if IDSupport is not registered', () => {
    expect(() => DB.idField('ID')).toThrowError('ID Support must be registered with DB before use')
  })
  describe('fields', () => {
    beforeAll(() => DB.setIDSupport(new TestIDSupport()))
    it('should create string field', () => {
      expect(DB.stringField('NAME', [DB.nonNull(), DB.stringDefault('SAMPLE')])).toEqual(
        new DBEntityField('NAME', DBEntityFieldType.string, [DB.nonNull(), DB.stringDefault('SAMPLE')]),
      )
      expect(DB.stringField('NAME')).toEqual(new DBEntityField('NAME', DBEntityFieldType.string, []))
    })
    it('should create integer field', () => {
      expect(DB.integerField('NAME', [DB.nonNull(), DB.integerDefault(10)])).toEqual(
        new DBEntityField('NAME', DBEntityFieldType.integer, [DB.nonNull(), DB.integerDefault(10)]),
      )
      expect(DB.integerField('NAME')).toEqual(new DBEntityField('NAME', DBEntityFieldType.integer, []))
    })
    it('should create ID field', () => {
      const integerSpy = jest.spyOn(DB as any, 'idSupport').mockReturnValue({
        idColumnType: DBEntityFieldType.integer,
        fromCursor: jest.fn(),
        toDBValue: jest.fn(),
      } as IDSupport)
      expect(DB.idField('NAME', [DB.nonNull(), DB.primaryKey()])).toEqual(
        new DBEntityField('NAME', DBEntityFieldType.integer, [DB.nonNull(), DB.primaryKey()]),
      )
      integerSpy.mockRestore()

      const stringSpy = jest.spyOn(DB as any, 'idSupport').mockReturnValue({
        idColumnType: DBEntityFieldType.string,
        fromCursor: jest.fn(),
        toDBValue: jest.fn(),
      } as IDSupport)
      expect(DB.idField('NAME', [DB.nonNull(), DB.primaryKey()])).toEqual(
        new DBEntityField('NAME', DBEntityFieldType.string, [DB.nonNull(), DB.primaryKey()]),
      )
      stringSpy.mockRestore()
    })
    it('should generate default value constraints', () => {
      expect(DB.integerDefault(10)).toEqual(new DBEntityFieldDefaultConstraint(Polytype.int32(10)))
      expect(DB.stringDefault('DEF')).toEqual(new DBEntityFieldDefaultConstraint(Polytype.string('DEF')))
      expect(DB.idDefault(int64(10))).toEqual(new DBEntityFieldDefaultConstraint(Polytype.int64(int64(10))))
    })
    it('should generate references constraints', () => {
      expect(DB.references(EntityKind.folder, 'FK')).toEqual(
        new DBEntityFieldForeignKeyConstraint(EntityKind.folder, 'FK'),
      )
    })
    it('should generate non-null constraints (abort resolution constraint)', () => {
      expect(DB.nonNull()).toEqual(new DBEntityFieldNonNullConstraint(DBEntityConflictResolution.abort))
    })
    it('should generate PK constraints (abort resolution, no autoincrement)', () => {
      expect(DB.primaryKey()).toEqual(new DBEntityFieldPrimaryKeyConstraint(false, DBEntityConflictResolution.abort))
    })
    it('should generate PK constraints (autoincrement, abort resolution)', () => {
      expect(DB.primaryKeyAutoincrement()).toEqual(
        new DBEntityFieldPrimaryKeyConstraint(true, DBEntityConflictResolution.abort),
      )
    })
    it('should generate PK constraints (replace resolution, no autoincrement)', () => {
      expect(DB.primaryKeyReplace()).toEqual(
        new DBEntityFieldPrimaryKeyConstraint(false, DBEntityConflictResolution.replace),
      )
    })
    it('should generate PK constraints (ignore resolution, no autoincrement)', () => {
      expect(DB.primaryKeyIgnore()).toEqual(
        new DBEntityFieldPrimaryKeyConstraint(false, DBEntityConflictResolution.ignore),
      )
    })
  })
})
