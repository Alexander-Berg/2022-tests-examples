import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { Story, StorySlide } from '../../../../code/api/entities/story/story'
import { MFlags } from '../../../../code/busilogics/flags/mflags'
import { StoryEntityFlag } from '../../../../code/busilogics/flags/story-flag'

describe(StoryEntityFlag, () => {
  const flag = MFlags.story('name', new Story('mailish', 'title', 'url', ['external_mail'], []))
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toEqual(new Story('mailish', 'title', 'url', ['external_mail'], []))
  })
  it('should parse value', () => {
    const story = new Story(
      'mailish',
      'title',
      'url',
      ['external_mail'],
      [new StorySlide(10, 'back', '#ffffff', 'foreground', 'title', 'desc', 'action', 'list')],
    )
    expect(
      flag.parse(
        JSONItemFromJSON({
          id: 'mailish',
          title: 'title',
          markImage: 'url',
          excludedAccounts: ['external_mail'],
          slides: [
            {
              backgroundColor: '#ffffff',
              title: 'title',
              description: 'desc',
              actionText: 'action',
              actionLink: 'list',
              background: 'back',
              foreground: 'foreground',
              duration: 10,
            },
          ],
        }),
      ),
    ).toEqual(story)
    expect(flag.parse(JSONItemFromJSON(1))).toBeNull()
    expect(flag.parse(JSONItemFromJSON({ id: 'mailish' }))).toBeNull()
  })
  it('should serialize value', () => {
    const story = new Story(
      'mailish',
      'title',
      'url',
      ['external_mail'],
      [new StorySlide(10, 'back', '#ffffff', 'foreground', 'title', 'desc', 'action', 'list')],
    )
    expect(flag.serialize(story)).toEqual(
      JSONItemFromJSON({
        id: 'mailish',
        title: 'title',
        markImage: 'url',
        excludedAccounts: ['external_mail'],
        slides: [
          {
            backgroundColor: '#ffffff',
            title: 'title',
            description: 'desc',
            actionText: 'action',
            actionLink: 'list',
            background: 'back',
            foreground: 'foreground',
            duration: 10,
          },
        ],
      }),
    )
  })
})
