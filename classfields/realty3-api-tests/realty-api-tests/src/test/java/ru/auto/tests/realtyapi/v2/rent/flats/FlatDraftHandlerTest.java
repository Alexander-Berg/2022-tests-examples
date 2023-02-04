package ru.auto.tests.realtyapi.v2.rent.flats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import lombok.extern.log4j.Log4j;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.RealtyTestApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiApiUpdateFlatDraftResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyPersonFullName;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiAssignedUser;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlat;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiGetFlatDraftResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentUser;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateFlatDraftRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateFlatDraftResponse;
import ru.auto.tests.realtyapi.v2.rent.AbstractHandlerTest;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("/rent/draft")
@RunWith(GuiceTestRunner.class)
@Log4j
@GuiceModules(RealtyTestApiModule.class)
public class FlatDraftHandlerTest extends AbstractHandlerTest {

    private RealtyRentApiFlat draft1;
    private RealtyRentApiRentUser rentUser1;

    @Inject
    private ApiClient apiV2;

    @Before
    public void before() {
        createAccounts();
    }

    @After
    public void after() {
        flatToDelete.add(draft1);
        usersToDelete.add(rentUser1);
    }

    @AfterClass
    public static void cleaning() {
        deleteDrafts(token1);
        deleteUsers(token1);
        deleteAccounts();
    }


    @Test
    @DisplayName("Создание черновика")
    public void createFlatDraftTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);

        RealtyApiApiUpdateFlatDraftResponse createFlatDraftResponse = apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()));

        RealtyRentApiUpdateFlatDraftResponse response = createFlatDraftResponse.getResponse();
        Assertions
                .assertThat(response)
                .describedAs("Ответ ручки создания черновика")
                .isNotNull();

        draft1 = response.getFlat();
        Assertions
                .assertThat(draft1)
                .describedAs("Созданная квартира")
                .isNotNull();

        Assertions
                .assertThat(draft1.getFlatId())
                .describedAs("ID квартиры")
                .isNotEmpty();

        Assertions
                .assertThat(draft1.getAddress().getAddress())
                .describedAs("Сохраненный адрес")
                .isEqualTo(address);

        Assertions
                .assertThat(draft1.getAddress().getFlatNumber())
                .describedAs("Сохраненный номер квартиры")
                .isEqualTo(request.getFlatNumber());

        Assertions
                .assertThat(draft1.getPhone())
                .describedAs("Сохраненный телефон")
                .isEqualTo(request.getPhone());

        Assertions
                .assertThat(draft1.getUserRole())
                .describedAs("Роль пользователя")
                .isEqualTo(RealtyRentApiFlat.UserRoleEnum.OWNER);

        Assertions
                .assertThat(draft1.getStatus())
                .describedAs("Статус квартиры")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.DRAFT);


        Assertions
                .assertThat(draft1.getAssignedUsers().size())
                .describedAs("Связанные пользователи")
                .isEqualTo(1);

        RealtyRentApiAssignedUser assignedUser = draft1.getAssignedUsers().get(0);

        Assertions
                .assertThat(assignedUser.getUserRole())
                .describedAs("Роль пользователя")
                .isEqualTo( RealtyRentApiAssignedUser.UserRoleEnum.OWNER);

        Assertions
                .assertThat(assignedUser.getUserId())
                .describedAs("ID пользователя")
                .isEqualTo(rentUser1.getUserId());
    }


    @Test
    @DisplayName("Cоздание черновика, передан некорректный/несуществующий uid")
    public void createFlatDraftWithInvalidUidTest() {
        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);

        String unknownUid = "uid:AAABBBCCC";
        apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(unknownUid)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(request)
                .execute(validatedWith(shouldBeCode(404)));
    }


    @Test
    @DisplayName("Получение черновика")
    public void getDraftTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);
        draft1 = rentApiAdaptor.createFlatDraft(token1, uid1).getFlat();
        draft1.setAssignedUsers(new ArrayList<>());

        RealtyRentApiGetFlatDraftResponse getDraftResponse = rentApiAdaptor.getFlatDraft(token1, uid1);

        Assertions
                .assertThat(getDraftResponse)
                .describedAs("Ответ получения черновика")
                .isNotNull();

        RealtyRentApiFlat flat = getDraftResponse.getFlat();
        Assertions
                .assertThat(flat)
                .describedAs("Найденный черновик")
                .isNotNull()
                .isEqualTo(draft1);

        Assertions
                .assertThat(flat.getFlatId())
                .describedAs("ID найденного черновика")
                .isEqualTo(draft1.getFlatId());
    }


    private RealtyRentApiUpdateFlatDraftRequest modifyDraft(RealtyRentApiFlat draft) {
        return new RealtyRentApiUpdateFlatDraftRequest()
                .email(rentUser1.getEmail() + ".com")
                .address(draft.getAddress().getAddress() + "123")
                .flatNumber(draft1.getAddress().getFlatNumber() + "456")
                .phone(draft1.getPhone().replaceAll("0", "9"))
                .person(
                        new RealtyPersonFullName()
                                .name(draft1.getPerson().getName() + "mod")
                                .patronymic(draft1.getPerson().getPatronymic() + "mod")
                                .surname(draft1.getPerson().getSurname() + "mod")
                );
    }

    @Test
    @DisplayName("Изменение черновика")
    public void updateDraftTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);
        draft1 = rentApiAdaptor.createFlatDraft(token1, uid1).getFlat();

        RealtyRentApiUpdateFlatDraftRequest request = modifyDraft(draft1);

        apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()));

        RealtyRentApiFlat updatedFlat = rentApiAdaptor.getFlatDraft(token1, uid1).getFlat();
        Assertions
                .assertThat(updatedFlat.getFlatId())
                .describedAs("Обновленный flatId не должен меняться")
                .isEqualTo(draft1.getFlatId());

        Assertions
                .assertThat(updatedFlat.getPhone())
                .describedAs("Обновленный телефон")
                .isEqualTo(request.getPhone());

        Assertions
                .assertThat(updatedFlat.getAddress().getFlatNumber())
                .describedAs("Обновленный номер квартиры")
                .isEqualTo(request.getFlatNumber());

        Assertions
                .assertThat(updatedFlat.getAddress().getAddress())
                .describedAs("Обновленный адрес квартиры")
                .isEqualTo(request.getAddress());

        Assertions
                .assertThat(updatedFlat.getPerson().getName())
                .describedAs("Обновленное имя пользователя")
                .isEqualTo(request.getPerson().getName());

        Assertions
                .assertThat(updatedFlat.getPerson().getSurname())
                .describedAs("Обновленная фамилия пользователя")
                .isEqualTo(request.getPerson().getSurname());

        Assertions
                .assertThat(updatedFlat.getPerson().getPatronymic())
                .describedAs("Обновленное отчество пользователя")
                .isEqualTo(request.getPerson().getPatronymic());
    }

}
