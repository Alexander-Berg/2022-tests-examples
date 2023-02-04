package ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.testserializers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.common.util.functional.PartialFunction;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.BaseAttrValue;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.Entity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.BaseMutableEntity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.attrs.*;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.EntitySerializer;
import ru.yandex.webmaster3.core.semantic.review_business.biz.model.*;
import ru.yandex.webmaster3.core.semantic.review_business.biz.model.impl.json.BizReviewJsonConversions;
import ru.yandex.webmaster3.core.semantic.review_business.biz.model.impl.wrapper.*;
import ru.yandex.webmaster3.core.semantic.review_business.model.Author;
import ru.yandex.webmaster3.core.semantic.review_business.model.impl.wrapper.AuthorWrapper;
import ru.yandex.webmaster3.core.semantic.review_business.util.DateHelper;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.MicrodataUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.ComplexMicrodata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.struct_data.StructuredDataTransformers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by aleksart on 29.04.14.
 */
@Slf4j
public class OrgReviewFromSchemaSerializer implements EntitySerializer {

    public void setOriginalDoc(final AtomicReference<Entity> originalDoc) {
        this.originalDoc = originalDoc;
    }

    private AtomicReference<Entity> originalDoc;

    @Override
    public String serialize(Entity e) {
        if (e.getTag().equals("OrgReviewFromSchema")) {
            BizReviewWrapper review = getBizReviewFromOrg(e);
            try {
                String new_output = BizReviewJsonConversions.toJsonString(review).toString();
                List<String> testReviews = testReview(e.getUrl());
                StringBuilder sb = new StringBuilder();
                sb.append("\nnew:\n" + new_output + "\nold:\n");
                int maxLen = Integer.MAX_VALUE;
                String best = "";
                for (String rev : testReviews) {
                    int dist = StringUtils.getLevenshteinDistance(rev, new_output);
                    if (dist < maxLen) {
                        best = rev;
                        maxLen = dist;
                    }
                }
                sb.append(best + "\n");
                sb.append("difference:\n" + StringUtils.difference(best, new_output) + '\n');
                if (maxLen == 0) {
                    sb.append("OK\n");
                } else {
                    sb.append("Bad\n");
                }
                return sb.toString();
            } catch (JSONException e1) {
                log.error(e1.getMessage(),e1);
                return "";
            } catch (NullPointerException ne) {
                return "";
            } catch (Exception e1) {
                log.error(e1.getMessage(),e1);
                return "";
            }
        } else {
            return "";
        }
    }

    private BizReviewWrapper getBizReviewFromOrg(Entity e) {

        if (e.getFirstOrNull("items") instanceof EntityAttrValue) {
            Entity item = ((EntityAttrValue) e.getFirstOrNull("items")).entity;
            BizReviewWrapper review = BizReviewWrapper.newBizReview().setItem(getHcard(item));
            if (!e.getValues("url").isEmpty()) {
                review = review.setUrl(e.getStringValue("url"));
            }
            if (!e.getValues("pros").isEmpty()) {
                review = review.setPros(e.getStringValues("pros"));
            }
            if (!e.getValues("contras").isEmpty()) {
                review = review.setContras(e.getStringValues("contras"));
            }
            if (!e.getValues("description").isEmpty()) {
                review = review.setDescription(e.getStringValue("description"));
            }
            if (!e.getValues("rating").isEmpty()) {
                review = review.setRating(toFloat(e.getStringValue("rating")));
            }
            if (!e.getValues("best-rating").isEmpty()) {
                review = review.setBestRating(toFloat(e.getStringValue("best-rating")));
            }
            if (!e.getValues("worst-rating").isEmpty()) {
                review = review.setWorstRating(toFloat(e.getStringValue("worst-rating")));
            }
            if (!e.getValues("reviewsUrl").isEmpty()) {
                review = review.setReviewsUrl(e.getStringValue("reviewsUrl"));
            }
            BaseAttrValue author = e.getFirstOrNull("reviewer");
            review = review.setReviewer(getAuthor(author));
            String datePub = e.getStringValue("date-reviewed");
            if (datePub != null && !datePub.equals("")) {
                Date date = DateHelper.read(datePub);
                review = review.setReviewedDate(date);
            }
            String dateVis = e.getStringValue("date-visited");
            if (dateVis != null && !dateVis.equals("")) {
                Date date = DateHelper.read(dateVis);
                review = review.setReviewedDate(date);
            }

            review = review.setTags(Cu.mapWhereDefined(SCHEMA_RATING_TAG_TO_REVIEW, e.getValues("tags")));
            return review;
        }
        return null;
    }

    private static final PartialFunction<BaseAttrValue, Tag> SCHEMA_RATING_TAG_TO_REVIEW =
            new PartialFunction<BaseAttrValue, Tag>() {
                @Override
                public Tag apply(BaseAttrValue attrValue) throws IllegalArgumentException {
                    if (attrValue instanceof EntityAttrValue) {
                        BaseMutableEntity tagEntity = (BaseMutableEntity) ((EntityAttrValue) attrValue).entity;
                        TagWrapper tag = TagWrapper.newTag();
                        tag = tag.setName(tagEntity.getStringValue("name"));
                        String typeString = tagEntity.getStringValue("type");
                        Tag.Type type = null;
                        if (typeString.equals("ATTITUDE")) {
                            type = Tag.Type.ATTITUDE;
                        }
                        if (typeString.equals("RATING10")) {
                            type = Tag.Type.RATING10;
                        }
                        if (typeString.equals("RATING5")) {
                            type = Tag.Type.RATING5;
                        }
                        tag = tag.setType(type);
                        tag = tag.setValue(toFloat(tagEntity.getStringValue("val")).toString());
                        return tag;
                    }
                    return null;
                }
            };

    private Author getAuthor(BaseAttrValue author) {
        if (author instanceof EntityAttrValue) {
            return AuthorWrapper.newAuthor().setFn(((EntityAttrValue) author).entity.getStringValue("fn")).setUrl(
                    ((EntityAttrValue) author).entity.getStringValue("url"));
        } else if (author instanceof StringAttrValue) {
            return AuthorWrapper.newAuthor().setFn(((StringAttrValue) author).getContent());
        } else if (author == null) {
            return null;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private HCard getHcard(Entity item) {
        if (item instanceof BaseMutableEntity) {
            try {

                String fn = item.getStringValue("fn");
                HCardWrapper card = HCardWrapper.newHCard().setFn(fn);
                List<Address> addresses = new ArrayList<Address>();
                for (BaseAttrValue addrsAttr : item.getValues("adrs")) {
                    if (addrsAttr instanceof EntityAttrValue) {
                        if (!((EntityAttrValue) addrsAttr).entity.getAttributes().isEmpty()) {
                            addresses.add(getAddress(addrsAttr));
                        }
                    } else {
                        addresses.add(getAddress(addrsAttr));
                    }
                }
                card = card.setAddresses(addresses);
                if (!item.getValues("geo").isEmpty()) {
                    BaseMutableEntity geoEntity = (BaseMutableEntity) item.getEntityValue("geo");
                    String lon = geoEntity.getStringValue("longitude");
                    String lat = geoEntity.getStringValue("latitude");
                    card = card.setGeo(GeoWrapper.newGeo().set(toFloat(lat), toFloat(lon)));
                }
                card = card.setWorkHours(item.getStringValue("opening"));
                card = card.setEmail(item.getStringValue("email"));
                card = card.setUrl(item.getStringValue("url"));
                card = card.setPhoneNumber(item.getStringValue("tel-voice"));
                card = card.setPhotoUrl(item.getStringValue("photo"));
                card = card.setLogoUrl(item.getStringValue("logo"));

                return card;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private AddressWrapper getAddress(BaseAttrValue adrs) {
        try {
            AddressWrapper address = AddressWrapper.newAddress();
            if (adrs instanceof StringAttrValue) {
                return address.setStreetAddress(((StringAttrValue) adrs).getContent());
            }
            if (adrs instanceof EntityAttrValue) {
                BaseMutableEntity adrsEntity = (BaseMutableEntity) ((EntityAttrValue) adrs).entity;
                address = address.setCountryName(adrsEntity.getStringValue("country-name")).setExtendedAddress(
                        adrsEntity.getStringValue("extended-address")).setLocality(
                        adrsEntity.getStringValue("locality")).setPostalCode(
                        adrsEntity.getStringValue("postal-code")).setRegion(
                        adrsEntity.getStringValue("region")).setStreetAddress(
                        adrsEntity.getStringValue("street-address"));
                return address;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    private static Float toFloat(final String best) {
        if (best == null) {
            return null;
        }
        try {
            return Float.valueOf(best.replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }


    public List<String> testReview(String address) throws Exception {

        final String url = originalDoc.get().url;
        final byte[] content = ((BlobAttrValue) originalDoc.get().getFirstOrNull("data")).byteArray;

        final List<Microdata> result =
                MicrodataUtils.extractMD(content, url.toString(), originalDoc.get().getStringValue("charset"), true,
                        false);
        List<String> results = new ArrayList<>();
        for (final Microdata data : result) {
            final BizReview org =
                    StructuredDataTransformers.fromSchemaOrgReview((ComplexMicrodata) data, url.toString());
            if (org != null) {
                results.add(BizReviewJsonConversions.toJsonString(org));
            }
        }
        return results;
    }
}


