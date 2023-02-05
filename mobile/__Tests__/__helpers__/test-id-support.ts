import { Nullable } from '../../../../common/ys'
import { ID, idFromString, idToString } from '../../../mapi/code/api/common/id'
import { IDSupport } from '../../code/api/common/id-support'
import { ColumnIndex, CursorValueExtractor } from '../../code/api/storage/cursor-value-extractor'
import { DBEntityFieldType } from '../../code/api/storage/scheme-support/db-entity'

export class TestIDSupport implements IDSupport {
  public readonly idColumnType = DBEntityFieldType.integer

  public fromCursor(cursor: CursorValueExtractor, index: ColumnIndex): Nullable<ID> {
    return cursor.isNull(index) ? null : idFromString(cursor.getString(index))
  }

  public toDBValue(id: ID): string {
    return idToString(id)!
  }
}
