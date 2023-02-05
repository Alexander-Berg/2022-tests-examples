import { reject, resolve } from '../../../../../../common/xpromise-support'
import { int64, YSError } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { YSPair } from '../../../../../common/code/utils/tuples'
import { MessageMeta } from '../../../../../mapi/code/api/entities/message/message-meta'
import { MessageTypeFlags } from '../../../../../mapi/code/api/entities/message/message-type'
import { Labels } from '../../../../code/busilogics/labels/labels'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { SearchModel } from '../../../../code/busilogics/search/search-model'
import { SearchResultsSync } from '../../../../code/busilogics/search/search-results-sync'
import { MockStorage, MockWithinTransaction } from '../../../__helpers__/mock-patches'
import { clone, Writable } from '../../../../../common/__tests__/__helpers__/utils'

function MockSearch(patch: Partial<SearchModel>): SearchModel {
  return (patch as any) as SearchModel
}

function MockMessages(patch: Partial<Messages>): Messages {
  return (patch as any) as Messages
}

function MockLabels(patch: Partial<Labels>): Labels {
  return (patch as any) as Labels
}

describe(SearchResultsSync, () => {
  const SAMPLE_METAS = [
    new MessageMeta(
      int64(301),
      int64(101),
      null,
      [],
      true,
      null,
      '',
      '',
      '',
      false,
      false,
      null,
      int64(12345),
      false,
      null,
      MessageTypeFlags.people | MessageTypeFlags.tPeople,
    ),
    new MessageMeta(
      int64(302),
      int64(101),
      null,
      [],
      true,
      null,
      '',
      '',
      '',
      false,
      false,
      null,
      int64(23456),
      false,
      null,
      MessageTypeFlags.people | MessageTypeFlags.tPeople,
    ),
    new MessageMeta(
      int64(303),
      int64(101),
      null,
      [],
      true,
      null,
      '',
      '',
      '',
      false,
      false,
      null,
      int64(34567),
      false,
      null,
      MessageTypeFlags.people | MessageTypeFlags.tPeople,
    ),
  ]

  const SAMPLE_METAS_REMOTES = SAMPLE_METAS.slice(1)
  const SAMPLE_METAS_MODIFIED = SAMPLE_METAS_REMOTES.map((meta) => clone(meta) as Writable<Required<MessageMeta>>).map(
    (meta) => {
      meta.showFor = 'modified'
      return meta as MessageMeta
    },
  )

  const LOCAL_MIDS = [int64(500), int64(600)]

  it('should run within transaction', (done) => {
    const withinTransaction = MockWithinTransaction<any>().mockReturnValue(reject(new YSError('NO MATTER')))
    const searchResultsSync = new SearchResultsSync(
      MockStorage({ withinTransaction }),
      MockSearch({}),
      MockMessages({}),
      MockLabels({}),
    )
    expect.assertions(1)
    searchResultsSync.applySearchResults(SAMPLE_METAS, 's_id', false).failed((_) => {
      expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
      done()
    })
  })
  it('should apply search results', (done) => {
    const getDelta = jest.fn().mockReturnValue(resolve(new YSPair(LOCAL_MIDS, SAMPLE_METAS_REMOTES)))
    const modifyMessageMetaWithSearchId = jest.fn((searchId: string, meta: MessageMeta) => {
      const modifed = clone(meta) as Writable<Required<MessageMeta>>
      modifed.showFor = 'modified'
      return modifed as MessageMeta
    })
    const deleteSearchOnly = jest.fn().mockReturnValue(resolve(getVoid()))
    const clearMessagesShowFor = jest.fn().mockReturnValue(resolve(getVoid()))
    const insertMessages = jest.fn().mockReturnValue(resolve(getVoid()))
    const insertMessageLabels = jest.fn().mockReturnValue(resolve(getVoid()))
    const updateMessageAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
    const updateMessagesShowFor = jest.fn().mockReturnValue(resolve(getVoid()))
    const withinTransaction = MockWithinTransaction<any>()
    const searchResultsSync = new SearchResultsSync(
      MockStorage({ withinTransaction }),
      MockSearch({
        getDelta,
        updateMessagesShowFor,
        deleteSearchOnly,
        clearMessagesShowFor,
        modifyMessageMetaWithSearchId,
      }),
      MockMessages({ insertMessages, updateMessageAttaches }),
      MockLabels({ insertMessageLabels }),
    )
    expect.assertions(9)
    searchResultsSync.applySearchResults(SAMPLE_METAS, 's_id', false).then((_) => {
      expect(deleteSearchOnly).not.toBeCalled()
      expect(clearMessagesShowFor).not.toBeCalled()
      expect(modifyMessageMetaWithSearchId).toBeCalledTimes(2)
      expect(modifyMessageMetaWithSearchId.mock.calls[0]).toMatchObject(['s_id', SAMPLE_METAS_REMOTES[0]])
      expect(modifyMessageMetaWithSearchId.mock.calls[1]).toMatchObject(['s_id', SAMPLE_METAS_REMOTES[1]])
      expect(insertMessages).toBeCalledWith(SAMPLE_METAS_MODIFIED)
      expect(insertMessageLabels).toBeCalledWith(SAMPLE_METAS_REMOTES)
      expect(updateMessageAttaches).toBeCalledWith(SAMPLE_METAS)
      expect(updateMessagesShowFor).toBeCalledWith('s_id', LOCAL_MIDS)
      done()
    })
  })
  it('should apply search results and remove old results', (done) => {
    const getDelta = jest.fn().mockReturnValue(resolve(new YSPair(LOCAL_MIDS, SAMPLE_METAS_REMOTES)))
    const modifyMessageMetaWithSearchId = jest.fn((searchId: string, meta: MessageMeta) => {
      const modifed = clone(meta) as Writable<Required<MessageMeta>>
      modifed.showFor = 'modified'
      return modifed as MessageMeta
    })
    const deleteSearchOnly = jest.fn().mockReturnValue(resolve(getVoid()))
    const clearMessagesShowFor = jest.fn().mockReturnValue(resolve(getVoid()))
    const insertMessages = jest.fn().mockReturnValue(resolve(getVoid()))
    const insertMessageLabels = jest.fn().mockReturnValue(resolve(getVoid()))
    const updateMessageAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
    const updateMessagesShowFor = jest.fn().mockReturnValue(resolve(getVoid()))
    const withinTransaction = MockWithinTransaction<any>()
    const searchResultsSync = new SearchResultsSync(
      MockStorage({ withinTransaction }),
      MockSearch({
        getDelta,
        updateMessagesShowFor,
        deleteSearchOnly,
        clearMessagesShowFor,
        modifyMessageMetaWithSearchId,
      }),
      MockMessages({ insertMessages, updateMessageAttaches }),
      MockLabels({ insertMessageLabels }),
    )
    expect.assertions(9)
    searchResultsSync.applySearchResults(SAMPLE_METAS, 's_id', true).then((_) => {
      expect(deleteSearchOnly).toBeCalledWith(LOCAL_MIDS, 's_id')
      expect(clearMessagesShowFor).toBeCalled()
      expect(modifyMessageMetaWithSearchId).toBeCalledTimes(2)
      expect(modifyMessageMetaWithSearchId.mock.calls[0]).toMatchObject(['s_id', SAMPLE_METAS_REMOTES[0]])
      expect(modifyMessageMetaWithSearchId.mock.calls[1]).toMatchObject(['s_id', SAMPLE_METAS_REMOTES[1]])
      expect(insertMessages).toBeCalledWith(SAMPLE_METAS_MODIFIED)
      expect(insertMessageLabels).toBeCalledWith(SAMPLE_METAS_REMOTES)
      expect(updateMessageAttaches).toBeCalledWith(SAMPLE_METAS)
      expect(updateMessagesShowFor).toBeCalledWith('s_id', LOCAL_MIDS)
      done()
    })
  })
})
