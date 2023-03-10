package ru.auto.tests.amp.reviews;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AutoruFeatures.AMP)
@DisplayName("Страница отзыва")
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewTest {

    private final static String REVIEW_CAR = "29 июля 2012\n109\nГеморойный патриот.\n157 комментариев\n36821\nalleinikoff\nНа Авто.ру с 2010 года\nУАЗ Patriot I, 2012\n3163 2.7 MT (128 л.с.) 4WD, владею 1-2 года\nПосле продажи ниссан х трейла ( т30 дизель) назрел вопрос покупки другого авто.\n1/2\n30 июня 2013\nЭксплуатация пробег 15000\nВот читаю сайт уазовский - \" Уаз это современный и безопасный и т.д. автомобиль\"\n1/1\n21 июля 2013\nДополняю.\nПо жизни я умею делать все к чему прикасаюсь руками, за исключением довести до ума эту дрянь\n16 декабря 2015\nМашина подарила радость.\nУаз радует два раза - первый, когда покупаешь, второй, когда продаешь\n1/2\n16 декабря 2015\nСпециально для троллей.\nЖивое фото моего бывшего. На фото шнурок через просверленный капот, чтобы больше не открывался.\n16 декабря 2015\nСпециально для троллей.\nЖивое фото моего бывшего.\n1/1\nОценка автора\n4,2\nВнешний вид\n5\nКомфорт\n5\nБезопасность\n5\nХодовые качества\n5\nНадёжность\n1\nЛюбовь зла.. Наконец я его продал.\nПолюбишь и козла (патриота).. Блин, кто-то его ж купил.";
    private final static String REVIEW_MOTO = "18 августа 2018\n8\nЛегендарный CB 400 Super bold'or\n0 комментариев\n122\nЧёрный лимузин\nНа Авто.ру с 2018 года\nHonda CB 400 Super Four, 2005\nвладею 1-2 года\nЗдравствуй дорогой читатель! Хочу поделиться своим опытом владения этой техникой для того чтоб ты сделал правильный выбор. Начну с того что проехал я на нём 15тыс. км мотоцикл предназначен для города экономичный, легкий, узкий, маневренный, удобная посадка, достаточно мощности для комфортного передвижения но и за город в радиусе 250 км можно ехать без проблем, можно ехать и на дальняк (я ездил 1500 км в одиночку) но это не его стихия, грузоподъемность маленькая второй пассажир будет абузой, установка кофров для багажа перегрузит подвеску его максимум центральный кофр тупо прятать туда шлем и то под большим вопросом потому что это повлияет на его аэродинамические характеристики и управляемость. Его легкость из плюсов в городе превращается в минус в далёком путешествии, при порывах ветра мотоцикл может уходить с траектории, идеальный багаж это рюкзачок на спину. И хотя мощности мотоциклу хватает чтоб с легкостью разогнаться до 180 км\\ч + родной ветрозащитны не хватает и чтоб ехать на такой скорости необходимо лечь на бак что очень неудобно, т.к. я часто езжу по трассе я нашел для себя выход в покупке горбатого лобового стекла компании PUIG предназначенного для мотоцикла CB 1300 Super bold'or которое идеально подходит, при моём росте 182 см ветер отсекает выше шлема что обеспечивает комфортное передвижение на высоких скоростях. Можно также без проблем ехать по грунтовке на речку в сухую погоду например на рыбалку или просто искупаться. По сути мотоцикл весьма практичен и универсален является рабочей лошадкой на каждый день. Идеальным райдером для этого мотоцикла является человек 176-178 см роста 76-78 кг живого веса спортивного телосложения. С технической стороны всё очень хорошо сделано мотоцикл удобный двигатель работает ровно, тихо очень приятный звук, коробка мягенькая передачи включаются чётко, есть небольшие вибрации на 3500-4200 обр\\мин тяга с низов при такой небольшой мощности достаточная чтоб не глохнуть при трогании и динамично разгоняться но придется часто работать коробкой в поисках оптимального крутящего момента (но к этому быстро привыкаешь) мотоцикл очень дружелюбный прощает райдеру очень многие ошибки, тормоза не очень хорошие при перегреве задних колодок можно легко и неожиданно сорвать колесо в юз тут нужна тренировка, к тормозам нужно привыкать. По расходу топлива скажу так мне удавалось получить 4.5-4.8 л на 100км при оборотах не превышающих 7500 и спокойной езде по трассе со средней скоростью 95-115 км\\ч чем больше крутишь мотор тем больше расход, может уходить за 9л. при моей манере езды потребляет 5.7л на 100км, бака в 16л вполне хватает 250-300км. Цена на расходники в разумных пределах так например замена звёзд и цепи раз 25тыс. км обойдётся в 10-13тыс. руб хорошие колодки 12-15тыс руб за комплект масло фильтры свечи как у всех. В целом мотоцикл очень понравился доставил много приятных впечатлений от владения им.\n\nP.C. Не бывает идеальных универсальных мотоциклов но HONDA cb400 super bold'or очень близка к этому идеальному балансу.\n1/3\nОценка автора\n4,0\nВнешний вид\n4\nКомфорт\n4\nБезопасность\n3\nХодовые качества\n4\nНадёжность\n5\nНадежность. Управляемость. Расход топлива. Стоимость обслуживания. Качество сборки все\nПодвеска. Безопасность. Багажник";
    private final static String REVIEW_TRUCKS = "1 декабря 2014\n17\nОчень сожалею что купил китайца\n160 комментариев\n32926\nFOTON 1061\nНа Авто.ру с 2014 года\nFoton Aumark BJ10xx/11xx, 2012\nвладею 2-3 года\nФотон BJ1061 был куплен мною в 2012 году для работы на грузоперевозках, продавцы уверяли в том, что китайцы научились делать машины. С первым косяком столкнулся когда уже произвёл оплату за это корыто и собирался на нём уехать, да куда там - она ни в какую не хотела заводиться, механики фирмы продавца полдня провозились с машиной и с горем пополам я уехал с грустными мыслями и сожалением о приобретении этого чуда китайского \"гения\".\n\nИ действительно машина оказалась полным отстоем... электрика постоянно коротит (отчего машина может сгореть), проехав по мокрому асфальту, на панели приборов стрелки указателей и индикаторные лампочки начинают жить своей жизнью, одним словом - панель приборов превращается в цветомузыку. Двигатель через 10 тыс.км показал кулак дружбы и не помогла обещанная гарантия, всю вину свалили на меня, как потом выяснилось, они со всеми так поступают, потому что даже в гарантийный период вылазят такие косяки, которые у нормальных производителей появляются как минимум после пробега 300 тыс.км. Шпильки на колёсах этого драндулета постоянно обрывает, хотя гайки колёс хорошо затянуты и вес перевозимого груза был всего 3 тонны!!!\n\nС тормозами тоже вечные проблемы, что очень часто приводит к авариям, знаю это по своим знакомым так же как и я позарившимся на этот, с позволения сказать, автомобиль и по себе (смотрите фото). Не понятно почему приходится подводить колодки (вы не поверите) через каждую тысячу км - это просто жесть!!! Шестерни в коробке передач и в редукторе моста сделаны из сыромятины, замучился их менять, а стоимость запчастей сопоставима с запчастями на Мерседес, хотя качество у них вообще никакое и в наличии очень мало чего есть, приходится заказывать и ждать минимум месяц... \"А как же работа?\" - спросите вы. \"Да вот так и работаю на это корыто и всех клиентов растерял, а избавиться от этого китайского чуда не получается, потому что таких дураков как я уже больше нет, вот и приходится бесконечно ремонтировать этот Фотон и кое как выживать, а есть такое желание - пустить его с какого-нибудь обрыва и забыть о нём как о страшном сне.\n\nЛадно поехали дальше... пальцы шарниров рулевых тяг очень часто вырывает, сами тяги гнутся при езде по нашим колдобинам. О шинах вообще нет слов, если зимой машина постоит, особенно под грузом, то они становятся элипсоподобными и пока не нагреются едешь по ровной дороге и подпрыгиваешь, дорогу вообще не держат при торможении - ощущение как будто едешь на лыжах. Рессоры постоянно лопаются, даже без перегруза, обрывает кронштейны рессоры заднего моста. Рама - это вообще какое-то извращение, никакой геометрии, её постоянно гнёт, а потом вообще начинает рвать в самых непредсказуемых местах. Редуктор руля часто клинит, тоже сыромятина. Покраска кабины - это какое-то издевательство и извращение, краска вздувается пузырями, а металл кабины покрывается сквозными отверстиями от ржавчины.\n\nСамое интересное, что эти умники, которые пытаются вам втюхать эти чудо машины, оправдываются тем, что у нас с вами якобы руки растут не оттуда, а к их китайским чудомобилям необходим особый подход и более нежное отношение.))) Не ведитесь как я на сказки продавашек китайских чудо машин и тех бедолаг, которые уже пострадали и оказались в опе.\n1/5\nОценка автора\n1,2\nВнешний вид\n2\nКомфорт\n1\nБезопасность\n1\nХодовые качества\n1\nНадёжность\n1\nИздалека кажется что выглядит симпатично, но, подойдя ближе, сразу видишь все минусы как и во всех китайских авто.\nНе вздумайте покупать, а то потом 1000 раз пожалеете. Кстати, продать б/ушного китайца очень проблематично и практически не представляется возможным, дорога одна на вторчермет.. Ни в коем случае, даже врагу не пожелаю такой автомобиль, а правильней драндулет.";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String reviewMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String path;

    @Parameterized.Parameter(4)
    public String text;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "reviews/ReviewsAutoCars", "reviews/SearchCarsBreadcrumbsUazPatriot",
                        "/uaz/patriot/2309645/4014660/", REVIEW_CAR},
                {MOTO, "reviews/ReviewsAutoMoto", "reviews/SearchMotoBreadcrumbsHondaCb400",
                        "/motorcycle/honda/cb_400/8131894731002395272/", REVIEW_MOTO},
                {TRUCKS, "reviews/ReviewsAutoTrucks", "reviews/SearchTrucksBreadcrumbsFotonAumark10xx",
                        "/truck/foton/aumark_10xx/4033391/", REVIEW_TRUCKS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(reviewMock,
                breadcrumbsMock).post();

        urlSteps.testing().path(AMP).path(REVIEW).path(category).path(path).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Содержимое отзыва")
    public void shouldSeeContent() {
        basePageSteps.onReviewPage().reviewContent().waitUntil(hasText(text));
    }
}