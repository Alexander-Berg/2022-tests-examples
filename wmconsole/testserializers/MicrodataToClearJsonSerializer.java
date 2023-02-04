package ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.testserializers;

import org.json.JSONException;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.Entity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.EntitySerializer;

/**
 * Created by aleksart on 28.04.14.
 */
public class MicrodataToClearJsonSerializer implements EntitySerializer {
    @Override
    public String serialize(Entity e) throws JSONException {
        if(e.getTag().equals("microdata")){
            return EntityToClearJsonSerializer.toClearJson(e).toString(1);
        }
        else{
            return "";
        }
    }
}
