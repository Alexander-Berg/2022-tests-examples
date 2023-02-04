package ru.yandex.partner.core.entity.user.multistate;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@CoreTest
class CanChangeRequisitesCheckTest {

    @Autowired
    CanChangeRequisitesCheck canChangeRequisitesCheck;

    @Test
    void hasLiveContractParsed() {
        var contract = """
                {
                 "is_signed": "2022-03-19T00:00:00",
                 "client_id": 95101321,
                 "is_faxed": null,
                 "dt": "2022-03-19",
                 "id": 6898377,
                 "update_dt": "2022-03-19T12:21:24",
                 "is_cancelled": "",
                 "create_dt": "2022-03-19",
                 "person_id": 17924144,
                 "test_mode": 0,
                 "contract_type": 9
                }
                """;

        assertTrue(canChangeRequisitesCheck.hasLiveContract(List.of(contract)));
    }
}
