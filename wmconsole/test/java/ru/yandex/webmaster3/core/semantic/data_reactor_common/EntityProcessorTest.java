package ru.yandex.webmaster3.core.semantic.data_reactor_common;

import junit.framework.TestCase;
import org.json.JSONObject;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.attrs.StringAttrValue;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.Json2EntityConvertor;

import java.util.*;

/**
 * Created by aleksart on 23.12.13.
 */
public class EntityProcessorTest extends TestCase {
    EntityProcessor entityProcessor = new EntityProcessor() {
        @Override
        public Collection<Entity> process(Entity e) {

            Set<String> str = new HashSet<>(e.getAttributes());
            List<BaseAttrValue> vals = e.getValues("testKey");
            for( BaseAttrValue val : vals){
                StringAttrValue sVal = (StringAttrValue)val;
                str.add(sVal.value);
            }

            System.out.println(str);

            return Collections.EMPTY_SET;
        }
    };
    public void testProcess() throws Exception {

        System.out.println("Hello from test Process");
    }

    public void testApply() throws Exception {

        System.out.println("Hello from test Apply");

        JSONObject jsonObject = new JSONObject();
        JSONObject dataObject = new JSONObject().put("testKey","testValue");
        jsonObject.put("data",dataObject);
        Entity entity = Json2EntityConvertor.fromJson(jsonObject);
        entityProcessor.apply(entity);

    }
}
