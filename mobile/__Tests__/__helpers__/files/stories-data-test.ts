import { StoryWrapper } from '../../../code/stories'

export const testStories: StoryWrapper[] = [
  // Mailish
  {
    platforms: ['android', 'ios'],
    langs: ['ru', 'en'],
    id: 'mailish',
    conditions: 'applicationId == "ru.yandex.mail.beta"',
    excludedAccountType: ['external_mail'],
    title: {
      ru: 'Почтовые ящики',
      en: 'Mail boxes',
    },
    markImage: 'https://mobmail.s3.mds.yandex.net/global/stories/mailish/mark.jpg',
    slides: [
      {
        backgroundColor: '#ffeeee',
        background: 'https://mobmail.s3.mds.yandex.net/global/stories/mailish/bg.jpg',
        foreground: 'https://mobmail.s3.mds.yandex.net/global/stories/mailish/slide_1.png',
        duration: 10,
        title: {
          ru: 'Почтовые ящики',
          en: 'Mail boxes',
        },
        description: {
          ru: 'Почтовые ящики',
          en: 'Mail boxes',
        },
        actionLink: 'Go',
        actionText: 'Go Button',
      },
    ],
  },
  // End Mailish
]
