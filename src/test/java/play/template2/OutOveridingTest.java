package play.template2;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OutOveridingTest {

    @Test
    public void testLegacyOutOverriding() {
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");
        

    }
}
