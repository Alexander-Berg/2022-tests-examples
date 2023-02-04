package ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.testserializers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import ru.yandex.common.util.functional.PartialFunction;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.BaseAttrValue;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.Entity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.attrs.EntityAttrValue;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.attrs.StringAttrValue;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.EntitySerializer;
import ru.yandex.webmaster3.core.semantic.review_business.auto.model.HProduct;
import ru.yandex.webmaster3.core.semantic.review_business.auto.model.agg.impl.json.AggAutoReviewJsonConversions;
import ru.yandex.webmaster3.core.semantic.review_business.auto.model.agg.impl.wrapper.AggAutoReviewWrapper;
import ru.yandex.webmaster3.core.semantic.review_business.auto.model.impl.wrapper.HProductWrapper;
import ru.yandex.webmaster3.core.semantic.review_business.model.Author;
import ru.yandex.webmaster3.core.semantic.review_business.model.impl.wrapper.AuthorWrapper;
import ru.yandex.webmaster3.core.semantic.review_business.util.DateHelper;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.MicrodataUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.ComplexMicrodata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microdata.data.Microdata;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.FrontEnd;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.struct_data.StructuredDataTransformers;

import java.net.URL;
import java.util.*;

import static ru.yandex.common.util.StringUtils.isEmpty;

/**
 * Created by aleksart on 11.07.14.
 */
@Slf4j
public class AutoReviewSerializer implements EntitySerializer {
    @Override
    public String serialize(Entity e) throws Exception {
        if (e.getTag().equals("AutoReviewOrgEntity")) {
            String eString = String.valueOf(EntityToClearJsonSerializer.toClearJson(e));

            AggAutoReviewWrapper aggAuto = getAggAutoRev(e);

            String aggAutoString = aggAuto==null?"":AggAutoReviewJsonConversions.toJsonString(aggAuto);

            StringBuilder sbg = new StringBuilder();
            sbg.append(eString).append("\n").append(aggAutoString);
            try {
                List<String> testReviews = testReview(e.getUrl());
                StringBuilder sb = new StringBuilder();
                sb.append("\nnew:\n" + aggAutoString + "\nold:\n");
                int maxLen = Integer.MAX_VALUE;
                String best = "";
                for (String rev : testReviews) {
                    int dist = StringUtils.getLevenshteinDistance(rev, aggAutoString);
                    if (dist < maxLen) {
                        best = rev;
                        maxLen = dist;
                    }
                }
                sb.append(best + "\n");
                sb.append("difference:\n" + StringUtils.difference(best, aggAutoString) + '\n');
                if (maxLen == 0||(isEmpty(aggAutoString)&&testReviews.isEmpty())) {
                    sb.append("OK\n");
                } else {
                    sb.append("Bad\n");
                }
                return sb.toString();
            } catch (JSONException e1) {
                log.error("Problem in json parsing", e1);
                return "";
            } catch (NullPointerException ne) {
                return "";
            } catch (Exception e1) {
                e1.printStackTrace();
                return "";
            }
        }
        return "";
    }

    private AggAutoReviewWrapper getAggAutoRev(Entity e) {
        AggAutoReviewWrapper review = AggAutoReviewWrapper.newReview();
        if (!e.containAttribute("item")) {
            return null;
        }
        review = review.setItem(AUTO_TO_REVIEW_HPRODUCT.apply(e.getEntityValue("item")));
        if (!e.getValues("url").isEmpty()) {
            review = review.setUrl(e.getStringValue("url"));
        }
        if (!e.getValues("pros").isEmpty()) {
            review = review.setPros(e.getStringValues("pros"));
        }
        if (!e.getValues("contras").isEmpty()) {
            review = review.setContras(e.getStringValue("contras"));
        }
        if (!e.getValues("review-body").isEmpty()) {
            review = review.setDescription(e.getStringValue("review-body"));
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
            review = review.setReviewsUrl(e.getStringValues("reviewsUrl"));
        }
        if (!e.getValues("name").isEmpty()) {
            review = review.setSummary(e.getStringValue("name"));
        }

        BaseAttrValue author = e.getFirstOrNull("review-author");
        review = review.setReviewer(getAuthor(author));
        String datePub = e.getStringValue("date-published");
        if (isEmpty(datePub)) {
            final Date date = DateHelper.read(datePub);
            review = review.setReviewedDate(date);
        }
        return review;
    }

    private Author getAuthor(BaseAttrValue author) {
        if (author instanceof EntityAttrValue) {
            return AuthorWrapper.newAuthor().setFn(((EntityAttrValue) author).entity.getStringValue("fn")).setUrl(
                    ((EntityAttrValue) author).entity.getStringValue("url"));
        } else if (author instanceof StringAttrValue) {
            return AuthorWrapper.newAuthor().setFn(((StringAttrValue) author).getContent());
        } else {
            throw new IllegalArgumentException();
        }
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

    private static final PartialFunction<Entity, HProduct> AUTO_TO_REVIEW_HPRODUCT =
            new PartialFunction<Entity, HProduct>() {

                @Override
                public HProduct apply(final Entity arg) throws IllegalArgumentException {
                    if (arg == null) {
                        throw new IllegalArgumentException();
                    }
                    if (!"AutoEntity".equals(arg.getTag())) {
                        throw new IllegalArgumentException();
                    }

                    HProductWrapper x =
                            HProductWrapper.newHProduct().setBodyType(arg.getStringValue("body-type")).setBrand(
                                    arg.getStringValue("brand")).setModel(arg.getStringValue("model")).setConfName(
                                    arg.getStringValue("config-name")).setDisplacement(
                                    arg.getStringValue("displacement")).setEngineType(
                                    arg.getStringValue("engine-type")).setFn(arg.getStringValue("name")).setGearType(
                                    arg.getStringValue("gear-type")).setProdyear(
                                    arg.getStringValue("prodyear")).setSteeringWheel(
                                    arg.getStringValue("steering-wheel")).setTransmission(
                                    arg.getStringValue("transmission")).setUrl(arg.getStringValue("url"));
                    return x;
                }
            };

    public List<String> testReview(String address) throws Exception {

        final URL url = new URL(address);
        final String content = (new FrontEnd()).downloadWithTimeout(url);

        final List<Microdata> result = MicrodataUtils.extractMD(content, url.toString(), true, false);
        List<String> results = new ArrayList<>();
        for (final Microdata data : result) {
            final AggAutoReviewWrapper org =
                    StructuredDataTransformers.fromSchemaOrgAutoReview((ComplexMicrodata) data, url.toString());
            if (org != null) {
                results.add(AggAutoReviewJsonConversions.toJsonString(org));
            }
        }
        return results;
    }
}
