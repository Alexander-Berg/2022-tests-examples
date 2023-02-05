import { FTSQuery, FullTextSearchQueriesProviderImpl } from '../../../../code/busilogics/fts/queries-provider'

describe(FullTextSearchQueriesProviderImpl, () => {
  describe('createFTSTableQuery', () => {
    it('should generate correct SQLite query for creating FTS table', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.createFTSTableQuery()).toBe(
        'CREATE VIRTUAL TABLE IF NOT EXISTS attach_and_body_fts USING fts4 (mid, type, ts, content, filename, subject, sender, notindexed=mid, notindexed=type, notindexed=ts, tokenize=icu, );',
      )
    })
    it('should generate correct SQLite query for creating offline saved suggests table', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.createSavedSuggestTableQuery()).toBe(
        'CREATE TABLE IF NOT EXISTS offline_suggest (suggest TEXT, ts INTEGER);',
      )
    })
    it('should generate correct SQLite query for clearing FTS table', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.clearFTSTableQuery()).toBe('DELETE FROM attach_and_body_fts;')
    })
    it('should generate correct SQLite query for trimming offline suggest to max number of suggests', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.trimSuggestWhereClause()).toBe(
        `ts == (SELECT MIN(ts) FROM offline_suggest) and (SELECT COUNT(*) FROM offline_suggest) > ${queryProvider.SUGGESTS_COUNT};`,
      )
    })
    it('should generate correct SQLite query for dropping FTS table', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.dropFTSTableQuery()).toBe('DROP TABLE IF EXISTS attach_and_body_fts;')
    })
    it('should generate correct SQLite query for dropping offline saved suggests table', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.dropSavedSuggestTableQuery()).toBe('DROP TABLE IF EXISTS offline_suggest;')
    })
    it('should generate correct SQLite query for getting saved offline suggests', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.savedFTSSuggestsQuery()).toBe('SELECT * FROM offline_suggest ORDER BY ts DESC;')
    })
    it('should generate correct SQLite query for getting all mids with timestamps ordered by timestamp desc', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.allIndexedMidsWithTsOrderedDescSelectStatement()).toBe(
        'SELECT mid, ts from attach_and_body_fts ORDER BY ts DESC;',
      )
    })
    it('should generate correct SQLite query for getting all mids with timestamps ordered by timestamp asc', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.allIndexedMidsWithTsOrderedAscSelectStatement()).toBe(
        'SELECT mid, ts from attach_and_body_fts ORDER BY ts ASC;',
      )
    })
    it('should generate correct SQLite search query template for offline full text search', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.searchQuery(new FTSQuery('examp', 20, 13))).toBe(
        // tslint:disable-next-line:quotemark
        "SELECT ts, mid, 0 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'sender:examp*' UNION SELECT ts, mid, 1 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'subject:examp*' UNION SELECT ts, mid, 2 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'content:examp*' UNION SELECT ts, mid, 3 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'filename:examp*' ORDER BY priority ASC, ts DESC LIMIT 20 OFFSET 13;",
      )
    })
    it('should generate correct SQLite search query template for offline full text search without offset', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.searchQuery(new FTSQuery('examp', 20, 0))).toBe(
        // tslint:disable-next-line:quotemark
        "SELECT ts, mid, 0 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'sender:examp*' UNION SELECT ts, mid, 1 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'subject:examp*' UNION SELECT ts, mid, 2 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'content:examp*' UNION SELECT ts, mid, 3 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'filename:examp*' ORDER BY priority ASC, ts DESC LIMIT 20;",
      )
    })
    it('should generate correct SQLite search query template for offline full text search without limit', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.searchQuery(new FTSQuery('examp', -1, 0))).toBe(
        // tslint:disable-next-line:quotemark
        "SELECT ts, mid, 0 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'sender:examp*' UNION SELECT ts, mid, 1 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'subject:examp*' UNION SELECT ts, mid, 2 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'content:examp*' UNION SELECT ts, mid, 3 AS priority FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'filename:examp*' ORDER BY priority ASC, ts DESC;",
      )
    })
    it('should generate correct SQLite suggest query template for offline suggest', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.suggestQuery(new FTSQuery('examp', 20, 13))).toBe(
        // tslint:disable-next-line:quotemark
        "SELECT ts, mid, 0 AS priority, subject AS snippet FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'subject:examp*' UNION SELECT ts, mid, 1 AS priority, subject AS snippet FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'content:examp*' UNION SELECT ts, mid, 2 AS priority, subject AS snippet FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'filename:examp*' ORDER BY priority ASC, ts DESC LIMIT 20;",
      )
    })
    it('should generate correct SQLite suggest query template for offline suggest without limit', () => {
      const queryProvider = new FullTextSearchQueriesProviderImpl('attach_and_body_fts', 'offline_suggest')
      expect(queryProvider.suggestQuery(new FTSQuery('examp', -1, 13))).toBe(
        // tslint:disable-next-line:quotemark
        "SELECT ts, mid, 0 AS priority, subject AS snippet FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'subject:examp*' UNION SELECT ts, mid, 1 AS priority, subject AS snippet FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'content:examp*' UNION SELECT ts, mid, 2 AS priority, subject AS snippet FROM attach_and_body_fts WHERE attach_and_body_fts MATCH 'filename:examp*' ORDER BY priority ASC, ts DESC;",
      )
    })
  })
})
