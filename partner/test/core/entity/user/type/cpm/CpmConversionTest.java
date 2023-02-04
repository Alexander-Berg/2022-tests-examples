package ru.yandex.partner.core.entity.user.type.cpm;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import NPartner.Page.TPartnerPage.TBlock;
import NPartner.Page.TPartnerPage.TBlock.TGeo;
import NPartner.Page.TPartnerPage.TBlock.TPICategoryIAB;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.Brand;
import ru.yandex.partner.core.entity.block.model.Geo;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.model.PiCategory;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.tasks.convertcpm.ConvertCpmTask;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.entity.user.service.UserService;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.libs.bs.json.BkDataConverter;

import static java.math.RoundingMode.FLOOR;
import static ru.yandex.partner.core.CoreConstants.Strategies.MIN_CPM_STRATEGY_ID;
import static ru.yandex.partner.core.CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID;


@CoreTest
@ExtendWith(MySqlRefresher.class)
class CpmConversionTest {
    @Autowired
    CpmConversionFactory cpmConversionFactory;

    @Autowired
    UserService userService;

    @Autowired
    BlockService blockService;

    BkDataConverter bkDataConverter = new BkDataConverter();

    RtbBlock block_41443_2() {
        return blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(41443L))
                .withFilter(BlockFilters.BLOCK_ID.eq(2L))
        ).get(0);
    }

    @Test
    void userCurrencyConversionTest() {
        Supplier<User> userSupplier = () -> userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(UserFilters.ID.eq(54519301L))
        ).get(0);
        User user = userSupplier.get();
        Assertions.assertEquals("RUB", user.getCurrentCurrency(),
                "User must have RUB currency before conversion"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(30L), 54519301L
        ));
        cpmConversion.convert();


        User userAfter = userSupplier.get();
        Assertions.assertEquals("USD", userAfter.getCurrentCurrency(),
                "Currency must change to USD"
        );
    }


    @Test
    void brandCpmConversionTest() {
        RtbBlock block = block_41443_2();
        Assertions.assertFalse(block.getBrands().isEmpty(),
                "Block under cpm-conversion must have brands"
        );

        Brand brand = block.getBrands().stream()
                .filter(b -> b.getBid().equals(420L))
                .findFirst().orElseThrow();
        Assertions.assertEquals(BigDecimal.valueOf(150.).setScale(3, FLOOR), brand.getCpm(),
                "Expected brand with cpm=150.0"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(3L), 1009L
        ));
        cpmConversion.convert();


        RtbBlock blockAfter = block_41443_2();
        Brand brandAfter = blockAfter.getBrands().stream()
                .filter(b -> b.getBid().equals(420L))
                .findFirst().orElseThrow();
        Assertions.assertEquals(BigDecimal.valueOf(50.).setScale(3, FLOOR), brandAfter.getCpm(),
                "Brand cpm must be 150.0 / 3 = 50.0 after conversion"
        );
    }

    @Test
    void geoCpmConversionTest() {
        RtbBlock block = block_41443_2();
        Assertions.assertFalse(block.getGeo().isEmpty(),
                "Block under cpm-conversion must have geos"
        );

        Geo geo = block.getGeo().stream()
                .filter(g -> g.getId().equals(225L))
                .findFirst().orElseThrow();
        Assertions.assertEquals(BigDecimal.valueOf(500L), geo.getCpm(),
                "Expected geo with cpm=500"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(50L), 1009L
        ));
        cpmConversion.convert();


        RtbBlock blockAfter = block_41443_2();
        Geo geoAfter = blockAfter.getGeo().stream()
                .filter(g -> g.getId().equals(225L))
                .findFirst().orElseThrow();
        Assertions.assertEquals(BigDecimal.valueOf(10L).setScale(3, FLOOR), geoAfter.getCpm(),
                "Geo cpm must be 500 / 50 = 10 after conversion"
        );
    }

    @Test
    void piCategoriesCpmConversionTest() {
        Supplier<RtbBlock> blockSupplier = () -> blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(41443L))
                .withFilter(BlockFilters.BLOCK_ID.eq(1L))
        ).get(0);
        RtbBlock block = blockSupplier.get();
        Assertions.assertFalse(block.getPiCategories().isEmpty(),
                "Block under cpm-conversion must have pi categories"
        );

        PiCategory piCategory = block.getPiCategories().stream()
                .filter(c -> c.getId().equals(53L))
                .findFirst().orElseThrow();
        Assertions.assertEquals(BigDecimal.valueOf(8.0).setScale(3, FLOOR), piCategory.getCpm(),
                "Expected piCategory with cpm=8.0"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(2L), 1009L
        ));
        cpmConversion.convert();


        RtbBlock blockAfter = blockSupplier.get();
        PiCategory piCategoryAfter = blockAfter.getPiCategories().stream()
                .filter(c -> c.getId().equals(53L))
                .findFirst().orElseThrow();
        Assertions.assertEquals(BigDecimal.valueOf(4.0).setScale(3, FLOOR), piCategoryAfter.getCpm(),
                "Geo cpm must be 8.0 / 2 = 4.0 after conversion"
        );
    }

    @Test
    void convertMincpmStrategyTest() {
        Supplier<MobileRtbBlock> blockSupplier = () -> blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(43569L))
                .withFilter(BlockFilters.BLOCK_ID.eq(5L))
        ).get(0);
        MobileRtbBlock block = blockSupplier.get();
        Assertions.assertEquals(MIN_CPM_STRATEGY_ID, block.getStrategyType(),
                "Expected block with mincpm strategy"
        );
        Assertions.assertEquals(BigDecimal.valueOf(50.).setScale(3, FLOOR), block.getMincpm(),
                "Expected block with mincpm=50.0"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(5L), 1011L
        ));
        cpmConversion.convert();


        MobileRtbBlock blockAfter = blockSupplier.get();
        Assertions.assertEquals(BigDecimal.valueOf(10.).setScale(3, FLOOR), blockAfter.getMincpm(),
                "Mincpm must be 50 / 5 = 10 after cpm conversion"
        );
    }

    @Test
    void convertDecimalMincpmStrategyTest() {
        Supplier<MobileRtbBlock> blockSupplier = () -> blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(43569L))
                .withFilter(BlockFilters.BLOCK_ID.eq(5L))
        ).get(0);
        MobileRtbBlock block = blockSupplier.get();
        Assertions.assertEquals(MIN_CPM_STRATEGY_ID, block.getStrategyType(),
                "Expected block with mincpm strategy"
        );
        Assertions.assertEquals(BigDecimal.valueOf(50.).setScale(3, FLOOR), block.getMincpm(),
                "Expected block with mincpm=50.0"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(3.3), 1011L
        ));
        cpmConversion.convert();


        MobileRtbBlock blockAfter = blockSupplier.get();
        Assertions.assertEquals(BigDecimal.valueOf(15.151).setScale(3, FLOOR), blockAfter.getMincpm(),
                "Mincpm must be 50 / 3.3 = 15.151 after cpm conversion"
        );
    }

    @Test
    void convertSeparateCpmStrategyTest() {
        Supplier<MobileRtbBlock> blockSupplier = () -> blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(43569L))
                .withFilter(BlockFilters.BLOCK_ID.eq(4L))
        ).get(0);
        MobileRtbBlock block = blockSupplier.get();
        Assertions.assertEquals(SEPARATE_CPM_STRATEGY_ID, block.getStrategyType(),
                "Expected block with separate cpm strategy"
        );
        Assertions.assertEquals(BigDecimal.valueOf(50.).setScale(3, FLOOR), block.getTextCpm(),
                "Expected block with text_cpm=50.0"
        );
        Assertions.assertEquals(BigDecimal.valueOf(50.).setScale(3, FLOOR), block.getMediaCpm(),
                "Expected block with media_cpm=50.0"
        );
        Assertions.assertNull(block.getVideoCpm(),
                "Expected block WITHOUT video_cpm"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(5L), 1011L
        ));
        cpmConversion.convert();


        MobileRtbBlock blockAfter = blockSupplier.get();
        Assertions.assertEquals(BigDecimal.valueOf(10.).setScale(3, FLOOR), blockAfter.getTextCpm(),
                "text_cpm must be 50 / 5 = 10 after cpm conversion"
        );
        Assertions.assertEquals(BigDecimal.valueOf(10.).setScale(3, FLOOR), blockAfter.getMediaCpm(),
                "media_cpm must be 50 / 5 = 10 after cpm conversion"
        );
        Assertions.assertNull(blockAfter.getVideoCpm(),
                "video_cpm must be null after cpm conversion"
        );
    }

    RtbBlock block_41443_64() {
        return blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(BlockFilters.PAGE_ID.eq(41443L))
                .withFilter(BlockFilters.BLOCK_ID.eq(64L))
        ).get(0);
    }

    @Test
    void customBkDataCpmCurrencyAndDspInfoTest() throws JsonProcessingException {
        var block = block_41443_64();
        Assertions.assertTrue(block.getIsCustomBkData(), "expected god mod block");

        TBlock tBlock = bkDataConverter.convertBlockJsonToProto(block.getBkData()).getMessage();
        Assertions.assertEquals("RUB", tBlock.getCPMCurrency(), "expect rub currency on block");

        TBlock.TDSPInfo direct = tBlock.getDSPInfo(0);
        Assertions.assertEquals(1000, direct.getCPM(), "expect block with direct with cpm=1000");


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(10L), 1009L
        ));
        cpmConversion.convert();


        RtbBlock blockAfter = block_41443_64();
        TBlock tBlockAfter = bkDataConverter.convertBlockJsonToProto(blockAfter.getBkData()).getMessage();
        Assertions.assertEquals("USD", tBlockAfter.getCPMCurrency(), "bk_data currency must be converted");

        TBlock.TDSPInfo directAfter = tBlockAfter.getDSPInfo(0);
        Assertions.assertEquals(100, directAfter.getCPM(), "bk_data cpm must be 1000 / 10 = 100");
    }

    @Test
    void customBkDataTest() throws JsonProcessingException {
        var block = block_41443_64();
        Assertions.assertTrue(block.getIsCustomBkData(), "expected god mod block");

        TBlock tBlock = bkDataConverter.convertBlockJsonToProto(block.getBkData()).getMessage();
        TGeo geo = tBlock.getGeoList().stream()
                .filter(g -> g.getGeoID() == 213)
                .findFirst().orElseThrow();
        Assertions.assertEquals(2000, geo.getValue(),
                "expect bkdata with geo cpm = 2000"
        );

        TBlock.TBrand brand = tBlock.getBrandList().stream()
                .filter(b -> b.getBrandID() == 189)
                .findFirst().orElseThrow();
        Assertions.assertEquals(4000, brand.getValue(),
                "expect bkdata with brand cpm = 4000"
        );

        TPICategoryIAB category = tBlock.getPICategoryIABList().stream()
                .filter(c -> c.getCategoryID() == 55)
                .findFirst().orElseThrow();
        Assertions.assertEquals(3000, category.getMediaCreativeReach(),
                "expect bkdata with media cpm = 3000"
        );
        Assertions.assertEquals(3000, category.getMediaImageReach(),
                "expect bkdata with image cpm = 3000"
        );
        Assertions.assertEquals(0, category.getVideoCreativeReach(),
                "expected bkdata video cpm = 0"
        );


        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(10L), 1009L
        ));
        cpmConversion.convert();


        RtbBlock blockAfter = block_41443_64();
        TBlock tBlockAfter = bkDataConverter.convertBlockJsonToProto(blockAfter.getBkData()).getMessage();
        TGeo geoAfter = tBlockAfter.getGeoList().stream()
                .filter(g -> g.getGeoID() == 213)
                .findFirst().orElseThrow();
        Assertions.assertEquals(200, geoAfter.getValue(),
                "bkdata geo cpm must be 2000 / 10 = 200"
        );

        TBlock.TBrand brandAfter = tBlockAfter.getBrandList().stream()
                .filter(b -> b.getBrandID() == 189)
                .findFirst().orElseThrow();
        Assertions.assertEquals(400, brandAfter.getValue(),
                "bkdata brand cpm must be 4000 / 10 = 400"
        );

        TPICategoryIAB categoryAfter = tBlockAfter.getPICategoryIABList().stream()
                .filter(c -> c.getCategoryID() == 55)
                .findFirst().orElseThrow();
        Assertions.assertEquals(300, categoryAfter.getMediaCreativeReach(),
                "bkdata media cpm must be 3000 / 10 = 300"
        );
        Assertions.assertEquals(300, categoryAfter.getMediaImageReach(),
                "expect bkdata with image cpm = 3000"
        );
        Assertions.assertEquals(0, categoryAfter.getVideoCreativeReach(),
                "bkdata video cpm must stay unchanged"
        );
    }

    @Test
    void convertedSimpleCpmObjectsTest() {
        CpmConversion cpmConversion = cpmConversionFactory.fromPayload(new ConvertCpmTask.Payload(
                "USD", BigDecimal.valueOf(60L), 1009L
        ));

        List<TGeo> geos = cpmConversion.convertedSimpleCpmObjects(
                List.of(
                        TGeo.newBuilder().setValue(120L).build(),
                        TGeo.newBuilder().setValue(180L).build()
                ),
                TGeo.class
        );

        Assertions.assertEquals(2L, geos.get(0).getValue());
        Assertions.assertEquals(3L, geos.get(1).getValue());
    }
}
