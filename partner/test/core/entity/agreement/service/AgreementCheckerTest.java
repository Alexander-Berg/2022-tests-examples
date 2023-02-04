package ru.yandex.partner.core.entity.agreement.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.partner.core.CoreConstants;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.block.BlockType;
import ru.yandex.partner.core.entity.user.actions.factories.UserEditFactory;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.libs.extservice.balance.BalanceResponseConverter;
import ru.yandex.partner.libs.extservice.balance.BalanceService;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class AgreementCheckerTest {
    @Autowired
    AgreementChecker agreementChecker;
    @Autowired
    BalanceService balanceService;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    UserEditFactory userEditFactory;

    private final LocalDate today = LocalDate.of(2016, 7, 7);
    private final LocalDate tomorrow = today.plusDays(1);

    @Test
    void tutbyPartnerNoAgreement() {
        var action = userEditFactory.edit(List.of(new ModelChanges<>(1024L, User.class)
                .process(true, User.IS_TUTBY)));

        actionPerformer.doActions(action);

        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(11024L, today, tomorrow);

        assertThat(result.values()).allMatch(Set::isEmpty);
    }

    @Test
    void tutbyPartnerWithAgreement() {
        var action = userEditFactory.edit(List.of(new ModelChanges<>(1025L, User.class)
                .process(true, User.IS_TUTBY)
                .process(true, User.HAS_TUTBY_AGREEMENT)));

        actionPerformer.doActions(action);

        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(11025L, today, tomorrow);

        assertThat(result).isEqualTo(Map.of(
                today, EnumSet.of(
                        BlockType.CONTEXT_ON_SITE_CONTENT,
                        BlockType.CONTEXT_ON_SITE_DIRECT,
                        BlockType.CONTEXT_ON_SITE_RTB,
                        BlockType.MOBILE_APP_RTB,
                        BlockType.SEARCH_ON_SITE_DIRECT,
                        BlockType.SEARCH_ON_SITE_PREMIUM,
                        BlockType.VIDEO_AN_SITE_INSTREAM,
                        BlockType.VIDEO_AN_SITE_INPAGE
                ),
                tomorrow, EnumSet.of(
                        BlockType.CONTEXT_ON_SITE_CONTENT,
                        BlockType.CONTEXT_ON_SITE_DIRECT,
                        BlockType.CONTEXT_ON_SITE_RTB,
                        BlockType.MOBILE_APP_RTB,
                        BlockType.SEARCH_ON_SITE_DIRECT,
                        BlockType.SEARCH_ON_SITE_PREMIUM,
                        BlockType.VIDEO_AN_SITE_INSTREAM,
                        BlockType.VIDEO_AN_SITE_INPAGE
                )
        ));
    }

    @Test
    void loginOfficengsru() throws IOException {
        stubContracts("/agreement/loginOfficengsru.json");

        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(11025L, today, tomorrow);

        var allTypes = Stream.of(BlockType.values())
                .filter(BlockType::isHasContract)
                .collect(Collectors.toSet());
        assertThat(result).isEqualTo(Map.of(
                today, allTypes,
                tomorrow, allTypes
        ));
    }

    @Test
    void loginMRuTextVip() {
        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(486246L, today, tomorrow);

        assertThat(result).isEqualTo(Map.of(
                today, EnumSet.of(
                        BlockType.CONTEXT_ON_SITE_DIRECT,
                        BlockType.SEARCH_ON_SITE_DIRECT,
                        BlockType.SEARCH_ON_SITE_PREMIUM,
                        BlockType.CONTEXT_ON_SITE_ADBLOCK
                ),
                tomorrow, EnumSet.of(
                        BlockType.CONTEXT_ON_SITE_DIRECT,
                        BlockType.SEARCH_ON_SITE_DIRECT,
                        BlockType.SEARCH_ON_SITE_PREMIUM,
                        BlockType.CONTEXT_ON_SITE_ADBLOCK
                )
        ));
    }

    @Test
    void loginAdinsideSpecial() throws IOException {
        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(CoreConstants.ADINSIDE_CLIENT_ID, today, tomorrow);

        assertThat(result).isEqualTo(Map.of(
                today, EnumSet.allOf(BlockType.class),
                tomorrow, EnumSet.allOf(BlockType.class)
        ));
    }

    @Test
    void yantestVip() {
        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(8933103L, today, tomorrow);

        assertThat(result).isEqualTo(Map.of(
                today, EnumSet.of(
                        BlockType.MOBILE_APP_RTB
                ),
                tomorrow, EnumSet.of(
                        BlockType.MOBILE_APP_RTB
                )
        ));
    }

    @Test
    void yantestNewVip() {
        Map<LocalDate, Set<BlockType>> result =
                agreementChecker.getDataForClient(9143013L, today, tomorrow);

        assertThat(result).isEqualTo(Map.of(
                today, EnumSet.of(
                        BlockType.VIDEO_AN_SITE_INPAGE,
                        BlockType.VIDEO_AN_SITE_INSTREAM,
                        BlockType.MOBILE_APP_RTB
                ),
                tomorrow, EnumSet.of(
                        BlockType.VIDEO_AN_SITE_INPAGE,
                        BlockType.VIDEO_AN_SITE_INSTREAM,
                        BlockType.MOBILE_APP_RTB
                )
        ));
    }

    private void stubContracts(String stubFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        Object[] response = mapper.readValue(
                getClass().getResource(stubFile),
                new TypeReference<Object[]>() {
                }
        );
        Mockito.when(
                balanceService.getPartnerContracts(11025L)
        ).thenReturn(Stream.of(response)
                .map(BalanceResponseConverter::convertPartnerContract)
                .collect(Collectors.toList()));
    }
}
