import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { Story, StoryContent, StorySlide } from '../../../../../code/api/entities/story/story'

describe(StorySlide, () => {
  it('should return null if irrelevant json type', () => {
    const item = JSONItemFromJSON([])
    const result = StorySlide.fromJSONItem(item)
    expect(result.isError()).toBe(true)
  })

  it('should return null if irrelevant json', () => {
    const item = JSONItemFromJSON({
      title: 'story slide title',
    })
    const result = StorySlide.fromJSONItem(item)
    expect(result.isError()).toBe(true)
  })

  it('should parse all fields', () => {
    const item = JSONItemFromJSON({
      title: 'story slide title',
      description: 'story slide description',
      backgroundColor: '#5377F4',
      background: 'https://example.yandex.net/bg.jpg',
      foreground: 'https://example.yandex.net/slide.png',
      duration: 6000,
      actionText: 'story action text',
      actionLink: 'actionlink://test',
    })
    const result = StorySlide.fromJSONItem(item).getValue()
    expect(result.title).toStrictEqual('story slide title')
    expect(result.description).toStrictEqual('story slide description')
    expect(result.backgroundColor).toStrictEqual('#5377F4')
    expect(result.background).toStrictEqual('https://example.yandex.net/bg.jpg')
    expect(result.foreground).toStrictEqual('https://example.yandex.net/slide.png')
    expect(result.duration).toStrictEqual(6000)
    expect(result.actionText).toStrictEqual('story action text')
    expect(result.actionLink).toStrictEqual('actionlink://test')
  })

  it('should be serialized', () => {
    const slide = new StorySlide(
      5000,
      'https://example.yandex.ru/bg.jpg',
      '#FF253F',
      'https://example.yandex.ru/slide.png',
      'slide title',
      'slide description',
      'slide action text',
      'actionlink://slide',
    )
    const jsonItem = slide.serialize()
    expect(jsonItem).toEqual(
      JSONItemFromJSON({
        title: 'slide title',
        description: 'slide description',
        backgroundColor: '#FF253F',
        background: 'https://example.yandex.ru/bg.jpg',
        foreground: 'https://example.yandex.ru/slide.png',
        duration: 5000,
        actionText: 'slide action text',
        actionLink: 'actionlink://slide',
      }),
    )
  })
})

describe(Story, () => {
  it('should return null if irrelevant json type', () => {
    const item = JSONItemFromJSON([])
    const result = Story.fromJSONItem(item)
    expect(result.isError()).toBe(true)
  })

  it('should return null if irrelevant json', () => {
    const item = JSONItemFromJSON({
      id: 'dark',
    })
    const result = Story.fromJSONItem(item)
    expect(result.isError()).toBe(true)
  })

  it('should return null on irrelevant excludedAccounts', () => {
    const item = JSONItemFromJSON({
      id: 'dark',
      title: 'Тёмная тема',
      markImage: 'https://example.yandex.net/mark.png',
      excludedAccounts: [0],
      slides: [],
    })
    const result = Story.fromJSONItem(item).getValue()
    expect(result.id).toStrictEqual('dark')
    expect(result.title).toStrictEqual('Тёмная тема')
    expect(result.markImage).toStrictEqual('https://example.yandex.net/mark.png')
    expect(result.excludedAccounts).toStrictEqual([])
    expect(result.slides).toStrictEqual([])
  })

  it('should parse all fields', () => {
    const item = JSONItemFromJSON({
      id: 'dark',
      title: 'Тёмная тема',
      markImage: 'https://example.yandex.net/mark.png',
      excludedAccounts: ['mailish'],
      slides: [
        {
          title: 'story slide title',
          description: 'story slide description',
          backgroundColor: '#5377F4',
          background: 'https://example.yandex.net/bg.jpg',
          foreground: 'https://example.yandex.net/slide.png',
          duration: 6000,
          actionText: 'story action text',
          actionLink: 'actionlink://test',
        },
      ],
    })
    const result = Story.fromJSONItem(item).getValue()
    expect(result.id).toStrictEqual('dark')
    expect(result.title).toStrictEqual('Тёмная тема')
    expect(result.markImage).toStrictEqual('https://example.yandex.net/mark.png')
    expect(result.excludedAccounts).toStrictEqual(['mailish'])
    expect(result.slides).toHaveLength(1)
  })

  it('should be deserialized', () => {
    const slide = new Story(
      'id',
      'title',
      'https://example.yandex.ru/mark.png',
      ['mailish'],
      [
        new StorySlide(
          5000,
          'https://example.yandex.ru/bg.jpg',
          '#FF253F',
          'https://example.yandex.ru/slide.png',
          'slide title',
          'slide description',
          'slide action text',
          'actionlink://slide',
        ),
      ],
    )
    const jsonItem = slide.serialize()
    expect(jsonItem).toEqual(
      JSONItemFromJSON({
        id: 'id',
        title: 'title',
        markImage: 'https://example.yandex.ru/mark.png',
        excludedAccounts: ['mailish'],
        slides: [
          {
            title: 'slide title',
            description: 'slide description',
            backgroundColor: '#FF253F',
            background: 'https://example.yandex.ru/bg.jpg',
            foreground: 'https://example.yandex.ru/slide.png',
            duration: 5000,
            actionText: 'slide action text',
            actionLink: 'actionlink://slide',
          },
        ],
      }),
    )
  })
})

describe(StoryContent, () => {
  it('should be deserialized', () => {
    const storyContent = new StoryContent(new Story('id', 'title', 'markImage', [], []), true)
    expect(storyContent.read).toBeTruthy()
  })
})
