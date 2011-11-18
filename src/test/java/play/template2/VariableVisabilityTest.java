package play.template2;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class VariableVisabilityTest {
    
    public void testThatVariablesNotLeakbetweenScriptRuns() throws  Exception {
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        assertThat(r.renderSrc("${a}", args)).isEqualTo("");
        assertThat(r.renderSrc("%{a='x'}%${a}", args)).isEqualTo("x");
        assertThat(r.renderSrc("${a}", args)).isEqualTo("");
        
    }
}
