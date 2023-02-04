package ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.testserializers;

import org.json.JSONException;
import org.json.JSONObject;
import ru.yandex.common.util.collections.Cu;
import ru.yandex.common.util.functional.Function;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.BaseAttrValue;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.Entity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.attrs.*;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.EntitySerializer;

import java.util.List;

/**
 * Created by aleksart on 27.12.13.
 */
public class EntityToClearJsonSerializer implements EntitySerializer {

    private int gap = 0;

    @Override
    public String serialize(Entity e) throws JSONException {
        String content = toClearJson(e).toString(gap);
        return content;
    }
    public static final Function<BaseAttrValue, Object> ATTR_VALUE2JSON_VALUE = new Function<BaseAttrValue, Object>() {
        @Override
        public Object apply(final BaseAttrValue o) {
            if (o instanceof StringAttrValue) {
                return ((StringAttrValue) o).value;
            } else if (o instanceof LongAttrValue) {
                return ((LongAttrValue) o).value;
            } else if (o instanceof EntityAttrValue) {
                try {
                    return toClearJson(((EntityAttrValue) o).entity);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            throw new IllegalArgumentException(o.toString());
        }
    };

    public static JSONObject toClearJson(final Entity entity) throws JSONException {
        final JSONObject data = new JSONObject();
        for (final String key : entity.getAttributes()) {
            final List<BaseAttrValue> vals = entity.getValues(key);
            if (vals.size() == 1) {
                data.put(key, ATTR_VALUE2JSON_VALUE.apply(vals.get(0)));
            } else if (vals.size() > 1) {
                data.put(key, Cu.map(ATTR_VALUE2JSON_VALUE, vals));
            }
        }
        return data;
    }

    public void setGap(int gap){
        this.gap = gap;
    }
}
