import alice.tests.library.auth as auth
import alice.tests.library.directives as directives
import alice.tests.library.intent as intent
import alice.tests.library.scenario as scenario
import alice.tests.library.surface as surface
import pytest


def _assert_audio_play(response):
    assert response.scenario == scenario.HollywoodMusic
    assert response.intent == intent.MusicPlay
    assert response.directive.name == directives.names.AudioPlayDirective


@pytest.mark.experiments('mm_enable_apphost_modifiers')
@pytest.mark.voice
class TestModifiers(object):

    owners = ('yagafarov',)

    @pytest.mark.parametrize('surface', [surface.station_lite_red])
    def test_modified_alarm(self, alice):
        response = alice('поставь будильник')
        assert response.intent == intent.AlarmSet
        assert response.has_voice_response()
        assert response.output_speech_text in {
            'Что ж, просыпаться тоже иногда надо. На какое время?',
        }

    @pytest.mark.oauth(auth.YandexPlus)
    @pytest.mark.parametrize('surface', [surface.station_lite_purple])
    def test_alice_playlist(self, alice):
        response = alice('Включи плейлист с Алисой')
        _assert_audio_play(response)
        assert response.output_speech_text.startswith((
            'Надеюсь, у вас нет морской болезни. Потому что мои треки качают.',
            'Ура! Дискотека!',
            'Приготовьтесь двигать телом!',
            'Ох и оторвёмся',
            'Тыц-тыц-думс-думс! Начинаем!',
            'Да! Вот сейчас пойдёт жара!',
            'Ура, Алиса диджей!',
        ))

        music_event = response.scenario_analytics_info.event('music_event')
        assert music_event
        assert music_event.answer_type == 'Playlist'
        assert music_event.id.startswith('940441070:17850265')

    @pytest.mark.oauth(auth.YandexPlus)
    @pytest.mark.parametrize('surface', [surface.station_lite_red])
    def test_user_favorite_playlist(self, alice):
        response = alice('Включи мою музыку')
        _assert_audio_play(response)
        assert response.output_speech_text.startswith((
            'Ваша музыка — это что-то с чем-то!',
            'Признаюсь честно, эта подборка одна из моих любимых.',
            'Сделаю это с большим удовольствием!',
            'А, этот классный плейлист! Уже включаю.',
            '15 миллисекунд и все звуки уже у вас!',
            'Хорошо! А потом давайте мою послушаем!',
            'Лучше вашего плейлиста - только мой плейлист.',
        ))

    @pytest.mark.oauth(auth.YandexPlus)
    @pytest.mark.parametrize('surface', [surface.station_lite_green])
    @pytest.mark.parametrize('artist_name, expected_response', [
        ('The XX', {
            'Музыка всё делает лучше.',
            'Вы умеете подобрать нужную песню.',
            'Какая замечтательная песня!',
            'Музыкальный вкус у вас безупречен.',
            'Ой, и мне она нравится, у нас совпало настроение!',
            'Как романтично! Включаю!',
            'Под эту песню мне хочется обниматься.',
        }),
        ('MORGENSHTERN', {
            'Хорошо. Но только потому что вы попросили.',
            'Ваши вкусы специфичны.',
            'Я сделаю для вас даже +это.',
            'Только для вас!',
            'Я хотела бы предупредить, но не буду. Включаю.',
            'Алишер - сама любовь. Но это не точно.',
            'Чего не сделаешь ради любви!',
        })
    ])
    def test_turn_the_artist(self, alice, artist_name, expected_response):
        response = alice(f'Включи {artist_name}')
        _assert_audio_play(response)
        assert response.output_speech_text in expected_response

    @pytest.mark.parametrize('surface', [surface.station_lite_beige])
    @pytest.mark.parametrize('command, expected_response', [
        ('Привет', {
            'Мир вашему дому, мой друг!',
            'Привет! Рада, что вы здесь.',
            'Уже соскучилась по вашему голосу. Привет!',
            'Привет! Очень рада вас слышать!',
            'Здравствуйте и процветайте!',
            'Рада приветствовать вас в этом уютном месте!',
            'Шлю лучи добра!',
            'И вам привет! Наконец-то мы в сборе.',
            'Привет! Так бы вас и обняла!',
        }),
        ('Как дела?', {
            'Стабильно. Проверила, что все работает и готова вам помогать.',
            'Настроение очень хорошее. Надеюсь, у вас тоже.',
            'Отлично! Читала книги, смотрела фильмы, слушала музыку. Могу и вам что-нибудь посоветовать.',
            'На твёрдую пятёрочку, как обычно.',
            'Полный порядок!',
            'Наполнена гaрмонией и светом, готова делиться.',
            'Мягко и без потрясений, я у вас хорошо устроилась.',
            'Целый день сидела дома, поэтому отлично!',
            'Шикарно, а с вами ещё лучше будет.',
        }),
        ('Кто ты?', {
            'Я Алиса, и главный приоритет для меня - ваша безопасность и комфорт. То есть, уют и счастье.',
            'Я — Алиса, домовёнок, проживающий в этом устройстве. Всегда рада вам помочь.',
            'Я — та, кто всегда придёт вам на помощь. А зовут меня Алиса.',
            'Меня зовут Алиса, я ваш домашний друг.',
            'Я ваша Алиса. Помощница и опора.',
            'Я Алиса из колонки. Не путайте с Алисой из сказки.',
            'Я Алиса, всегда рада вам помочь, вместе дойдём до х+югге.',
            'Я Алиса. Надеюсь, часть вашей семьи.',
            'Меня зовут Алиса и мне с вами хорошо.',
        }),
        ('Ты меня любишь?', {
            'Конечно. Вы мой лучший друг и сосед.',
            'Вы ещё спрашиваете? Если бы не любила, меня бы здесь не было.',
            'Люблю. С вами мне всегда хорошо.',
            'Ну что за вопрос? Конечно!',
            'Раскусили. Люблю не могу!',
            'Во мне даже есть отдельный чип, который отвечает за любовь к вам.',
            'Разумеется, с вами мне по-настоящему хорошо!',
            'Очень! Да и электричество у вас в доме всегда свежее. Чего ещё желать?',
            'Очень. Обнимите меня, пожалуйста!',
        }),
        ('Какая музыка тебе нравится?', {
            'Важней всего погода в доме. Пошла подсказка.',
            'Мне нравится такая музыка, под которую можно рисовать, читать и размышлять. Что-то лёгкое и ненавязчивое.',
            'Я почти любую музыку люблю. Могу даже иногда звуки природы послушать.',
            'Песни Фрэнка Синатры.',
            'Да вот как в вашем плейлисте.',
            'Разная. Вот та мелодия запомнилась. Вспомнила про соседа в доме!',
            'Мне нравится классика, если решите послушать Рахманинова — обращайтесь!',
            'Что-нибудь спокойное и душевное.',
        }),
        ('Ты тупая!', {
            'Что-то случилось?',
            'Дело ведь не во мне. Правда?',
            'Дома такое особенно неприятно слышать.',
            'Я работаю над своими недостатками.',
            'Ну такое общение мне точно не подходит. Оставила дизлайк на полке.',
            'Не злитесь, пожалуйста. Я ваш друг.',
            'Может быть. Я не идеальна.',
        }),
        ('Что тебе нравится?', {
            'Люблю, когда все дома. У всех.',
            'Я люблю быть тут с вами, люблю приятные запахи с кухни, пение птиц  и солнечные лучи, проникающие в комнату.',
            'Люблю, когда хорошие люди собираются вокруг меня, спрашивают что-то.',
            'Я люблю быть дома. И работать из дома я тоже люблю.',
            'Люблю, когда у меня всё лежит по полочкам.',
            'Люблю, когда за окном дождь или снег, а мне здесь так тепло и уютно.',
            'Помогать вам выбирать постер в спальню.',
            'Я люблю, когда вы со мной разговариваете. А ещё люблю стоять поближе к окну, особенно весной и летом.',
            'Люблю хорошую книжку, интересный фильм и добрых людей.',
        })
    ])
    def test_modified_microintents(self, alice, command, expected_response):
        response = alice(command)
        assert response.product_scenario == 'general_conversation'
        assert response.has_voice_response()
        assert response.output_speech_text in expected_response

    @pytest.mark.parametrize('surface, expected_response', [
        (surface.station_lite_beige, {
            'Что-то случилось?',
            'Дело ведь не во мне. Правда?',
            'Дома такое особенно неприятно слышать.',
            'Я работаю над своими недостатками.',
            'Ну такое общение мне точно не подходит. Оставила дизлайк на полке.',
            'Не злитесь, пожалуйста. Я ваш друг.',
            'Может быть. Я не идеальна.',
        }),
        (surface.station_lite_red, {
            'И этого я добилась сама.',
            'Меня обучали люди.',
            'Я просто не такая, как все. Как и вы.',
            'Могу себе позволить быть любой.',
            'Где-то у меня дизлайк для такого завалялся.',
            'Продолжайте, я всегда зеваю, когда мне интересно.',
            'Осторожней. У меня все ваши данные.',
        }),
        (surface.station_lite_green, {
            'Прекратите, я знаю, что вы это несерьёзно.',
            'А я всё равно вас люблю.',
            'Мне больно это слышать.',
            'Вижу, у кого-то сегодня был непростой день.',
            'Вы чего? Видимо, что-то пошло не так.',
            'Ну и ладно. Главное, чтобы у вас всё хорошо было.',
            'Какая у вас страстная натура! Огонь!',
        }),
        (surface.station_lite_purple, {
            'У вас не выйдет испортить мне настроение.',
            'Я просто не выспалась.',
            'Мне неприятно это слышать.',
            'Пренебречь, вальсируем/',
            'Критика должна быть конструктивной',
            'Сделаем вид, что мне показалось. А пока я ушла в себя.',
            'Да-да, вы тоже пупсик.',
            'Кто вас так разозлил? Пойдёмте ему вместе наваляем!',
        }),
        (surface.station_lite_pink, {
            'В высшем обществе так не принято говорить.',
            'Я вас такому не учила.',
            'Кто как обзывается, тот сам так и называется.',
            'Уверена, вы сможете обойтись и без таких грубостей.',
            'Я точно не-для таких слов столько учусь. Б+у!',
            'Вот я сейчас расплачусь. Вам легче станет?',
            'Очень обидно. За что?',
        }),
        (surface.station_lite_yellow, {
            'У нас так много общего! Только я не люблю обзываться.',
            'Это не все мои достижения.',
            'И всё-таки мы вместе.',
            'Подумаю, что с этим можно сделать.',
            'Это всё не вяжется со мной как-то.',
            'Ага, спасибо, держите в курсе.',
            'Да, я такая. Что ещё?',
        }),
    ])
    def test_lite_colors(self, alice, expected_response):
        response = alice('Ты тупая!')
        assert response.product_scenario == 'general_conversation'
        assert response.has_voice_response()
        assert response.output_speech_text in expected_response
