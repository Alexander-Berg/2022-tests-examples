import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { resolve } from '../../../../../../common/xpromise-support'
import { Nullable } from '../../../../../../common/ys'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../common/__tests__/__helpers__/mock-patches'
import { FileSystem } from '../../../../../common/code/file-system/file-system'
import { JSONItem, MapJSONItem } from '../../../../../common/code/json/json-types'
import { CustomNetworkProvider } from '../../../../../common/code/network/custom-network-provider'
import { Network } from '../../../../../common/code/network/network'
import { NetworkResponseBody } from '../../../../../common/code/network/network-response'
import { getVoid, Result } from '../../../../../common/code/result/result'
import { MockNetworkResponse } from '../../../../../mapi/__tests__/__helpers__/mock-patches'
import { Flag } from '../../../../../xflags/code/flag'
import { FlagsProviderSharedInstance } from '../../../../../xflags/code/flags-provider'
import { Story, StorySlide } from '../../../../code/api/entities/story/story'
import { MFlags } from '../../../../code/busilogics/flags/mflags'
import { Stories } from '../../../../code/busilogics/stories/stories'
import { ActionTimeTracker } from '../../../../code/busilogics/trackers/action-time-tracker'
import { CountingTracker } from '../../../../code/busilogics/trackers/counting-tracker'
import { MockHighPrecisionTimer } from '../../../__helpers__/mock-patches'

class TestCustomNetworkProvider implements CustomNetworkProvider {
  private readonly body: Nullable<NetworkResponseBody>
  public constructor(body: Nullable<NetworkResponseBody>) {
    this.body = body
  }
  public provideNetwork(baseUrl: string): Network {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue(this.body),
    })
    return MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
  }
}

class TestArrayBuffer implements ArrayBuffer {
  public readonly [Symbol.toStringTag]: string
  public readonly byteLength: number = 0

  public slice(begin: number, end?: number): ArrayBuffer {
    return this
  }
}

function createStories(
  networkProvider: CustomNetworkProvider = createTestCustomNetworkProvider(),
  fileSystem: FileSystem = createFileSystem(),
): Stories {
  const serializer = MockJSONSerializer({
    serialize: jest.fn().mockReturnValue(new Result<string>('', null)),
    deserialize: jest.fn().mockReturnValue(new Result<JSONItem>(new MapJSONItem(), null)),
  })
  return new Stories(
    networkProvider,
    fileSystem,
    new MockSharedPreferences(),
    serializer,
    new CountingTracker(new MockSharedPreferences()),
    new ActionTimeTracker(new MockSharedPreferences(), MockHighPrecisionTimer()),
  )
}

function createTestCustomNetworkProvider() {
  return new TestCustomNetworkProvider({
    string: () => 'body',
    bytes: () => new TestArrayBuffer(),
  })
}

function createFileSystem(exists: boolean = true) {
  return MockFileSystem({
    exists: jest.fn().mockReturnValue(resolve(exists)),
    listDirectory: jest.fn().mockImplementation((path) => {
      if (path === 'DOCUMENTS/stories/images') {
        return resolve([
          'DOCUMENTS/stories/images/story_mark_id_1',
          'DOCUMENTS/stories/images/slide_bg_id_1',
          'DOCUMENTS/stories/images/slide_fg_id_1',
        ])
      } else {
        return resolve(['DOCUMENTS/stories/images', 'DOCUMENTS/stories/id_1', 'DOCUMENTS/stories/id_2'])
      }
    }),
    readAsString: jest.fn().mockReturnValue(resolve('')),
    writeAsString: jest.fn().mockReturnValue(resolve(getVoid())),
    writeArrayBuffer: jest.fn().mockReturnValue(resolve(getVoid())),
    delete: jest.fn().mockReturnValue(resolve(getVoid())),
  })
}

function mockStoriesFlags(stories: Story[] = createTestStories(['id_1', 'id_2'])) {
  FlagsProviderSharedInstance.instance = {
    getValueForFlag: jest.fn().mockImplementation((flag: Flag<any>, log: boolean) => {
      switch (flag.name) {
        case MFlags.storiesIteration.name:
          return 1
        case MFlags.storiesShows.name:
          return 5
        case MFlags.stories.name:
          return stories.map((story) => story.id)
        default:
          return stories.find((story) => story.id === flag.name)
      }
    }),
    getFlagEditorForFlag: jest.fn(),
    getRuntimeEditorForFlag: jest.fn(),
  }
}

function createTestStories(ids: string[]): Story[] {
  return ids.map((id) => createTestStory(id))
}

function createTestStory(
  id: string = 'id',
  markImage: string = 'https://mobmail.s3.yandex.net/global/stories/story/mark_' + id,
  background: string = 'https://mobmail.s3.yandex.net/global/stories/slide/bg_' + id,
  foreground: string = 'https://mobmail.s3.yandex.net/global/stories/slide/fg_' + id,
): Story {
  return new Story(
    id,
    'title',
    markImage,
    [],
    [new StorySlide(5000, background, '#FFFFFF', foreground, 'title', 'description', 'actionText', 'actionLink')],
  )
}

describe(Stories, () => {
  it('shouldShowStories should return true', () => {
    mockStoriesFlags()
    const stories = createStories()
    expect(stories.shouldShowStories()).toBe(true)
  })
  it('shouldShowStories should return false', () => {
    mockStoriesFlags()
    const stories = createStories()
    for (let i = 0; i < MFlags.storiesShows.getValue(); i++) {
      stories.setStoriesShown()
    }
    expect(stories.shouldShowStories()).toBe(false)
    stories.resetStories()
    expect(stories.shouldShowStories()).toBe(true)
  })
  it('shouldShowStories should return false if was hidden', () => {
    const stories = createStories()
    stories.hideStories()
    expect(stories.shouldShowStories()).toBe(false)
    expect(stories.canShowStoriesSetting()).toBe(true)
    stories.removeHiddenFlag()
    expect(stories.shouldShowStories()).toBe(true)
    expect(stories.canShowStoriesSetting()).toBe(false)
  })
  it('markStoryRead', () => {
    const stories = createStories()
    stories.markStoryRead('1')
    stories.markStoryRead('2')
    const readStories = stories.getReadStories()
    expect(readStories).toEqual(new Set(['1', '2']))
  })
  it('markStoriesRead', () => {
    const stories = createStories()
    stories.markStoriesRead(['1', '2'])
    const readStories = stories.getReadStories()
    expect(readStories).toEqual(new Set(['1', '2']))
  })
  it('prepareStories should return void if successful', (done) => {
    mockStoriesFlags()
    const fileSystem = createFileSystem()
    const stories = createStories(createTestCustomNetworkProvider(), fileSystem)
    stories.prepareStories().then((value) => {
      expect(value).toBe(getVoid())
      expect(fileSystem.writeArrayBuffer).toBeCalled()
      done()
    })
  })
  it('prepareStories should return void if story without id', (done) => {
    mockStoriesFlags([createTestStory('')])
    const fileSystem = createFileSystem()
    const stories = createStories(createTestCustomNetworkProvider(), fileSystem)
    stories.prepareStories().then((value) => {
      expect(value).toBe(getVoid())
      expect(fileSystem.writeArrayBuffer).not.toBeCalled()
      done()
    })
  })
  it('prepareStories should return void if story with irrelevant url', (done) => {
    mockStoriesFlags([createTestStory('id', 'test', 'test', 'test')])
    const fileSystem = createFileSystem()
    const stories = createStories(createTestCustomNetworkProvider(), fileSystem)
    stories.prepareStories().then((value) => {
      expect(value).toBe(getVoid())
      expect(fileSystem.writeArrayBuffer).not.toBeCalled()
      done()
    })
  })
  it('prepareStories should create path if not created', (done) => {
    mockStoriesFlags([createTestStory('id', 'test', 'test', 'test')])
    const fileSystem = createFileSystem(false)
    const stories = createStories(createTestCustomNetworkProvider(), fileSystem)
    stories.prepareStories().then((value) => {
      expect(value).toBe(getVoid())
      expect(fileSystem.makeDirectory).toBeCalled()
      done()
    })
  })
  it('prepareStories should return void if images are not loaded', (done) => {
    mockStoriesFlags()
    const fileSystem = createFileSystem()
    const stories = createStories(new TestCustomNetworkProvider(null), fileSystem)
    stories.prepareStories().then((value) => {
      expect(value).toBe(getVoid())
      expect(fileSystem.writeArrayBuffer).not.toBeCalled()
      done()
    })
  })
  it('prepareStories should download only not loaded images', (done) => {
    mockStoriesFlags()
    const fileSystem = createFileSystem()
    const stories = createStories(createTestCustomNetworkProvider(), fileSystem)
    stories.prepareStories().then((value) => {
      expect(value).toBe(getVoid())
      expect(fileSystem.writeArrayBuffer).toBeCalledTimes(3)
      done()
    })
  })
  it('getCachedStories should return empty', (done) => {
    mockStoriesFlags()
    const stories = createStories()
    stories.getCachedStories().then((value) => {
      expect(value).toStrictEqual([])
      done()
    })
  })
})
