package ru.yandex.partner.core.entity.dsp.repository;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.common.editablefields.EditableFieldsService;
import ru.yandex.partner.core.entity.dsp.model.BaseDsp;
import ru.yandex.partner.core.entity.dsp.model.Dsp;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.partner.core.entity.dsp.DspConstants.EDIT_FORBIDDEN_MODEL_PROPERTIES;

@CoreTest
class DspTypedRepositoryTest {

    @Autowired
    DspTypedRepository dspTypedRepository;

    @Autowired
    EditableFieldsService<BaseDsp> editableFieldsService;

    @Test
    void getDspByIds() {
        BaseDsp dsp = dspTypedRepository.getDspById(2563070L);
        assertThat(dsp).isNotNull();
        assertThat(dsp.getId()).isEqualTo(2563070L);
        assertThat(dsp).isExactlyInstanceOf(Dsp.class);

        assertThat(((Dsp) dsp).getOwnerId()).isEqualTo(1014);
        assertThat(((Dsp) dsp).getLogin()).isEqualTo("mocked-dsp-manager");
        assertThat(((Dsp) dsp).getShortCaption()).isEqualTo("AWAPS Video");
        assertThat(((Dsp) dsp).getTag()).isEqualTo("awaps");
        assertThat(((Dsp) dsp).getTestUrl()).isEmpty();
        assertThat(((Dsp) dsp).getDataKey()).isEmpty();
        assertThat(((Dsp) dsp).getUrl())
                .isEqualTo("http://awaps.yandex.ru/11/1/0?r_host_id=7&charset=utf-8&rtb_as_ssi=1&empty_as_204=1");
        assertThat(((Dsp) dsp).getOwnerId()).isEqualTo(1014);

    }

    @Test
    void getEditableFields() {
        List<Dsp> dsp = dspTypedRepository.getSafely(List.of(2563070L), Dsp.class);
        assertThat(editableFieldsService.calculateEditableModelPropertiesHolder(dsp.get(0), null)
                .containsAnyPath(new HashSet<>(EDIT_FORBIDDEN_MODEL_PROPERTIES))
        ).isFalse();

    }
}
