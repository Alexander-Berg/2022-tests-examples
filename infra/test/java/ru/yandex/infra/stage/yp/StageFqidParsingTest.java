package ru.yandex.infra.stage.yp;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StageFqidParsingTest {
    @Test
    void normalFqid() {
        assertThat(RelationControllerImpl.getStageId("yp|sas-test|stage|my_stage_id|d7c28a6b-a59aedd2-25eb200a-301468c4"), equalTo("my_stage_id"));
        assertThat(RelationControllerImpl.getStageId("yp|xdc|stage|another_one|1d457f2b-84fb4d5e-c7c2624-9d12e8b9"), equalTo("another_one"));
    }

    @Test
    void anotherScheme() {
        assertThrows(RuntimeException.class, () -> RelationControllerImpl.getStageId("yp2|sas-test|stage|my_stage_id|d7c28a6b-a59aedd2-25eb200a-301468c4"));
    }

    @Test
    void wrongObjectType() {
        assertThrows(RuntimeException.class, () -> RelationControllerImpl.getStageId("yp|sas-test|replica_set|my_stage_id|d7c28a6b-a59aedd2-25eb200a-301468c4"));
    }

    @Test
    void wrongLength() {
        assertThrows(RuntimeException.class, () -> RelationControllerImpl.getStageId("yp|sas-test|stage|my_stage_id|d7c28a6b-a59aedd2-25eb200a-301468c4|some_extended_info"));
    }

    @Test
    void wrongSeparator() {
        assertThrows(RuntimeException.class, () -> RelationControllerImpl.getStageId("yp.sas-test.stage.my_stage_id.d7c28a6b-a59aedd2-25eb200a-301468c4"));
    }
}
