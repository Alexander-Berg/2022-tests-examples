package ru.yandex.realty.searcher.response;

import com.google.protobuf.FloatValue;
import junit.framework.TestCase;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.picapica.MdsUrlBuilder;
import ru.yandex.realty.proto.unified.offer.images.MdsImageId;
import ru.yandex.realty.proto.unified.offer.images.RealtyPhotoInfo;
import ru.yandex.realty.proto.unified.offer.images.UnifiedImages;
import ru.yandex.realty.searcher.response.builders.PrefixImageUtil;

import static java.util.Arrays.asList;
import static ru.yandex.realty.proto.unified.offer.images.MdsImageId.KnownNamespace.REALTY;

public class PhotoPreviewsTest extends TestCase {
    private static final String AVATARNICA_PREFIX = "//avatarnica.test";

    private RealtyPhotoInfo.Builder mkImage(int group, String name, String preview, float relevance) {
        RealtyPhotoInfo.Builder b = RealtyPhotoInfo.newBuilder();
        b.setMdsId(MdsImageId.newBuilder().setKnownNamespace(REALTY).setGroup(group).setName(name));
        b.setPreview(preview);
        if (relevance != 0) {
            b.setRelevance(FloatValue.of(relevance));
        }
        return b;
    }

    public void testUrlParsing() {
        Offer offer = new Offer();
        offer.setPhotos(
                UnifiedImages.newBuilder()
                        .addImage(mkImage(1, "a", "a", 0))
                        .addImage(mkImage(2, "b", "b", 0))
                        .build(),
                100
        );

        assertEquals(
                asList(
                        PrefixImageUtil.PrefixImage$.MODULE$.apply(AVATARNICA_PREFIX + "/get-realty/1/a", "a"),
                        PrefixImageUtil.PrefixImage$.MODULE$.apply(AVATARNICA_PREFIX + "/get-realty/2/b", "b")
                ),
                PrefixImageUtil.getImages(offer, new MdsUrlBuilder(AVATARNICA_PREFIX))
        );
    }

}
