package ru.yandex.partner.core.entity.dsp;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.dsp.model.Dsp;
import ru.yandex.partner.core.entity.dsp.service.DspService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
class DspsServiceTest {

    @Autowired
    BlockTypedRepository blockTypedRepository;

    @Autowired
    DspService dspService;

    @Test
    void getAvailableDsps() {
        RtbBlock block = (RtbBlock) blockTypedRepository.getBlockByCompositeId(347649081345L);


        assertThat(block.getShowVideo()).isNotEqualTo(true);
        List<Dsp> availableDsps = dspService.getAvailableDsps(List.of(block),  x -> false).get(0);
        assertThat(availableDsps.size()).isEqualTo(7);

        block.setShowVideo(true);
        availableDsps = dspService.getAvailableDsps(List.of(block), x -> false).get(0);
        assertThat(availableDsps.size()).isEqualTo(18);
    }

    @Test
    void getDefaultDsps() {
        RtbBlock block = (RtbBlock) blockTypedRepository.getBlockByCompositeId(347649081345L);
        assertThat(block).isNotNull();
        block.setShowVideo(true);

        List<Dsp> dsps = dspService.getDefaultDsps(List.of(block)).get(0);
        assertThat(dsps.size()).isEqualTo(14);

        block.setShowVideo(false);
        dsps = dspService.getDefaultDsps(List.of(block)).get(0);
        assertThat(dsps.size()).isEqualTo(4);
    }

    @Test
    void getAllNotDeletedDsps() {
        List<Dsp> dspsFromAllNotDeleted = dspService.getAllNotDeletedDsps();

        List<Dsp> dspFromFindAll = dspService.findAll(QueryOpts.forClass(Dsp.class));
        assertThat(dspFromFindAll.size()).isEqualTo(dspsFromAllNotDeleted.size());
    }
}
