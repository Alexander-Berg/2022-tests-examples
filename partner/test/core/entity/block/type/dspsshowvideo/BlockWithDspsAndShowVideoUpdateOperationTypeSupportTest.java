package ru.yandex.partner.core.entity.block.type.dspsshowvideo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformerImpl;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.all.BlockActionEdit;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.container.BlockContainer;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithDspsShowVideo;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.repository.type.BlockRepositoryTypeSupportFacade;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.block.service.BlockUpdateOperation;
import ru.yandex.partner.core.entity.block.service.BlockUpdateOperationFactory;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.type.base.BaseBlockRepositoryTypeSupport;
import ru.yandex.partner.core.entity.block.type.commonshowvideo.BlockWithCommonShowVideoRepositoryTypeSupport;
import ru.yandex.partner.core.entity.block.type.dsps.BlockWithDspsRepositoryTypeSupport;
import ru.yandex.partner.core.entity.dsp.model.Dsp;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multitype.repository.PartnerRepositoryTypeSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockWithDspsAndShowVideoUpdateOperationTypeSupportTest {
    @Autowired
    BlockWithDspsAndShowVideoUpdateOperationTypeSupport blockWithDspsAndShowVideoUpdateOperationTypeSupport;

    @Autowired
    BlockRepositoryTypeSupportFacade blockRepositoryTypeSupportFacade;

    @Autowired
    BlockTypedRepository blockTypedRepository;
    private BlockContainer blockContainer = BlockContainerImpl.create(OperationMode.EDIT);

    @Autowired
    BlockUpdateOperationFactory blockUpdateOperationFactory;

    @Autowired
    BlockService blockService;

    @Autowired
    ActionPerformerImpl actionPerformer;

    @Autowired
    RtbBlockEditFactory rtbBlockEditFactory;

    @Autowired
    BaseBlockRepositoryTypeSupport baseBlockRepositoryTypeSupport;
    @Autowired
    BlockWithCommonShowVideoRepositoryTypeSupport blockWithCommonShowVideoRepositoryTypeSupport;
    @Autowired
    BlockWithDspsRepositoryTypeSupport blockWithDspsRepositoryTypeSupport;

    @Test
    void noChangesWhenChangingFalseToFalse() {
        RtbBlock rtbBlock = blockTypedRepository.getSafely(List.of(347649081345L), RtbBlock.class).get(0);
        AppliedChanges<BlockWithDspsShowVideo> dontShowVideo =
                ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, false)
                        .applyTo(rtbBlock)
                        .castModelUp(BlockWithDspsShowVideo.class);

        blockWithDspsAndShowVideoUpdateOperationTypeSupport.onChangesApplied(
                blockContainer,
                List.of(dontShowVideo)
        );

        assertFalse(dontShowVideo.hasActuallyChangedProps());
    }

    @Test
    void setShowVideoAndDirectVideoDspIsAlreadyPresent() {
        RtbBlock rtbBlock = blockTypedRepository.getSafely(List.of(347649081345L), RtbBlock.class).get(0);
        AppliedChanges<BlockWithDspsShowVideo> showVideo =
                ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, true)
                        .applyTo(rtbBlock)
                        .castModelUp(BlockWithDspsShowVideo.class);

        blockWithDspsAndShowVideoUpdateOperationTypeSupport.onChangesApplied(
                blockContainer,
                List.of(showVideo)
        );

        assertTrue(showVideo.changed(RtbBlock.DSPS));
        assertThat(showVideo.getNewValue(RtbBlock.DSPS).size())
                .as("More video dsps added, even though direct is already present")
                .isGreaterThan(1);


        // changing true to true won't be effective
        AppliedChanges<BlockWithDspsShowVideo> showVideoAgain =
                ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, true)
                        .applyTo(rtbBlock)
                        .castModelUp(BlockWithDspsShowVideo.class);

        blockWithDspsAndShowVideoUpdateOperationTypeSupport.onChangesApplied(
                blockContainer,
                List.of(showVideoAgain)
        );

        assertFalse(showVideoAgain.hasActuallyChangedProps());
    }

    @Test
    void setShowVideoAndAddVideoDspAndReverse() {
        RtbBlock rtbBlock = blockTypedRepository.getSafely(List.of(347649081345L), RtbBlock.class).get(0);
        rtbBlock.setDsps(Collections.emptyList());
        AppliedChanges<BlockWithDspsShowVideo> showVideo =
                ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, true)
                        .applyTo(rtbBlock)
                        .castModelUp(BlockWithDspsShowVideo.class);

        blockWithDspsAndShowVideoUpdateOperationTypeSupport.onChangesApplied(
                blockContainer,
                List.of(showVideo)
        );

        assertTrue(showVideo.changed(RtbBlock.DSPS));
        assertThat(showVideo.getNewValue(RtbBlock.DSPS).size())
                .as("has multiple video dsps connected")
                .isGreaterThan(1);

        // the other way around, deleting video dsps
        AppliedChanges<BlockWithDspsShowVideo> dontShowVideo =
                ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, false)
                        .applyTo(rtbBlock)
                        .castModelUp(BlockWithDspsShowVideo.class);

        blockWithDspsAndShowVideoUpdateOperationTypeSupport.onChangesApplied(
                blockContainer,
                List.of(dontShowVideo)
        );

        assertTrue(dontShowVideo.changed(RtbBlock.DSPS));
        assertThat(dontShowVideo.getNewValue(RtbBlock.DSPS))
                .as("has no dsps left")
                .isEmpty();
    }

    @Test
    void readDspsWhenModelHasNullWithoutAction() {
        RtbBlock rtbBlock = new RtbBlock().withId(347649081345L);

        // убираем дспшки с блока
        BlockUpdateOperation deleteDspsFromBlockUpdateOperation = blockUpdateOperationFactory
                .createUpdateOperationWithPreloadedModels(
                        Applicability.FULL,
                        List.of(ModelChanges.build(rtbBlock, RtbBlock.DSPS, List.of()).castModel(BaseBlock.class)),
                        new IncomingFields(),
                        List.of(rtbBlock),
                        id -> new RtbBlock().withId(id)
                );
        deleteDspsFromBlockUpdateOperation.prepare().orElseGet(deleteDspsFromBlockUpdateOperation::apply);


        List<Dsp> dspsAfterDelete = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withProps(RtbBlock.DSPS)
        ).get(0).getDsps();
        assertThat(dspsAfterDelete).isEmpty();

        // взводим флаг show_video
        assertThat(rtbBlock.getDsps()).isNull();
        BlockUpdateOperation setShowVideoTrueUpdateOperation = blockUpdateOperationFactory
                .createUpdateOperationWithPreloadedModels(
                        Applicability.FULL,
                        List.of(ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, true).castModel(BaseBlock.class)),
                        new IncomingFields(),
                        List.of(rtbBlock),
                        id -> new RtbBlock().withId(id)
                );
        setShowVideoTrueUpdateOperation.prepare().orElseGet(setShowVideoTrueUpdateOperation::apply);

        List<Dsp> dspsAfterSetVideoTrue = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withProps(RtbBlock.DSPS)
        ).get(0).getDsps();
        assertThat(dspsAfterSetVideoTrue).size().isGreaterThan(0);
    }

    @Test
    void readDspsWhenModelHasNullThroughAction() {
        RtbBlock rtbBlock = new RtbBlock().withId(347649081345L);

        // убираем дспшки с блока
        BlockActionEdit<RtbBlock> action = rtbBlockEditFactory.edit(
                List.of(ModelChanges.build(rtbBlock, RtbBlock.DSPS, List.of()))
        );
        actionPerformer.doActions(action);

        List<Dsp> dspsAfterDelete = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withProps(RtbBlock.DSPS)
        ).get(0).getDsps();
        assertThat(dspsAfterDelete).isEmpty();

        // включаем show_video
        action = rtbBlockEditFactory.edit(
                List.of(ModelChanges.build(rtbBlock, RtbBlock.SHOW_VIDEO, true))
        );
        actionPerformer.doActions(action);

        List<Dsp> dspsAfterSetVideoTrue = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withProps(RtbBlock.DSPS)
        ).get(0).getDsps();
        assertThat(dspsAfterSetVideoTrue).size().isGreaterThan(0);
    }

    @Test
    void updateSupportAffectsSamePropertiesAsRepository() {
        Set<ModelProperty<? extends Model, ?>> actual = blockWithDspsAndShowVideoUpdateOperationTypeSupport
                .getAffectedProps();

        List<? extends PartnerRepositoryTypeSupport<? extends BaseBlock, BlockContainer, BlockContainer>> supports =
                blockRepositoryTypeSupportFacade.getSupportsByClass(BlockWithDspsShowVideo.class);

        assertThat(supports).hasAtLeastOneElementOfType(BaseBlockRepositoryTypeSupport.class);
        assertThat(supports).hasAtLeastOneElementOfType(BlockWithCommonShowVideoRepositoryTypeSupport.class);
        assertThat(supports).hasAtLeastOneElementOfType(BlockWithDspsRepositoryTypeSupport.class);

        Set<ModelProperty<?, ?>> expected = new HashSet<>();
        expected.addAll(baseBlockRepositoryTypeSupport.getAffectedModelProperties());
        expected.addAll(blockWithCommonShowVideoRepositoryTypeSupport.getAffectedModelProperties());
        expected.addAll(blockWithDspsRepositoryTypeSupport.getAffectedModelProperties());

        assertThat(actual).containsAll(expected);
    }
}
