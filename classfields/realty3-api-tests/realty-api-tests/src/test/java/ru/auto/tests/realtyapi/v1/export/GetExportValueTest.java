package ru.auto.tests.realtyapi.v1.export;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.mapper.XlsMapper.xlsMapper;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.export.GetExportTest.getBody;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.getDealStatus;

@Title("POST /export/offers.xls")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetExportValueTest {
    private static final int FIRST_SHEET = 0;
    private static final int VALUE_ROW = 1;
    private static final int ADDRESS_CELL = 0;
    private static final int LINK_CELL = 1;
    private static final int DESCRIPTION_CELL = 4;
    private static final int FLOOR_CELL = 5;
    private static final int DEAL_STATUS_CELL = 9;
    private static final int MORTGAGE_CELL = 10;

    private XSSFWorkbook response;
    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Before
    public void getResponse() {
        offerId = adaptor.getOfferIdFromSearcher();

        response = api.export().exportOffersRoute().reqSpec(authSpec())
                .body(getBody(offerId))
                .execute(validatedWith(shouldBe200Ok()))
                .as(XSSFWorkbook.class, xlsMapper());
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldOfferLinkIsValid() {
        Assertions.assertThat(response.getSheetAt(FIRST_SHEET).getRow(VALUE_ROW).getCell(LINK_CELL))
                .describedAs("Ответ должен содержать валидную ссылку")
                .hasStringCellValue(format("https://realty.yandex.ru/offer/%s", offerId));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldOfferHasValidDescription() {
        JsonElement offerDescription = adaptor.getOfferCard(offerId).get("description");
        String description = offerDescription == null ? "Нет описания" : offerDescription.getAsString();

        assertThat(response.getSheetAt(FIRST_SHEET).getRow(VALUE_ROW)
                .getCell(DESCRIPTION_CELL).getStringCellValue())
                .describedAs("Ответ должен содержать описание из карточки")
                .isEqualTo(description);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldOfferHasValidMortgage() {
        JsonObject response = adaptor.getOfferCard(offerId);
        String mortgage = Optional.ofNullable(response.getAsJsonObject("transactionConditionsMap"))
            .map(transactions -> transactions.get("MORTGAGE"))
            .map(JsonElement::getAsBoolean)
            .map(b -> b ? "Да" : "Нет")
            .orElse(StringUtils.EMPTY);

        Assertions.assertThat(this.response.getSheetAt(FIRST_SHEET).getRow(VALUE_ROW).getCell(MORTGAGE_CELL))
                .describedAs("Ответ должен иметь ипотеку из карточки")
                .hasStringCellValue(mortgage);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeValidFloor() {
        JsonObject offerCard = adaptor.getOfferCard(offerId);
        int floorsTotal = offerCard.get("floorsTotal").getAsInt();
        int floor = offerCard.getAsJsonArray("floorsOffered").get(0).getAsInt();
        String floorInfo = format("%d этаж из %d", floor, floorsTotal);

        assertThat(response.getSheetAt(FIRST_SHEET).getRow(VALUE_ROW)
                .getCell(FLOOR_CELL).getStringCellValue())
                .describedAs("Ответ должен иметь информацию о этаже из карточки")
                .contains(floorInfo);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeValidDealStatus() {
        JsonElement offerDealStatus = adaptor.getOfferCard(offerId).get("dealStatus");
        String deal = offerDealStatus == null ? null : getDealStatus().get(offerDealStatus.getAsString());

        assertThat(response.getSheetAt(FIRST_SHEET).getRow(VALUE_ROW)
                .getCell(DEAL_STATUS_CELL).getStringCellValue())
                .describedAs("Ответ должен иметь информацию о типе сделки из карточки")
                .isEqualTo(deal);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeValidAddress() {
        String address = adaptor.getOfferCard(offerId).getAsJsonObject("location")
                .get("address").getAsString();

        assertThat(response.getSheetAt(FIRST_SHEET).getRow(VALUE_ROW)
                .getCell(ADDRESS_CELL).getStringCellValue())
                .describedAs("Ответ должен иметь адрес из карточки")
                .contains(address);
    }
}
