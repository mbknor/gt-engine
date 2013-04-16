package play.template2;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class VariableVisabilityTest {

    @Test
    public void testThatVariablesNotLeakbetweenScriptRuns() throws  Exception {
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        assertThat(r.renderSrc("${a}", args)).isEqualTo("");
        assertThat(r.renderSrc("%{a='x'}%${a}", args)).isEqualTo("x");
        assertThat(r.renderSrc("${a}", args)).isEqualTo("");
        
    }

    @Test
    public void testThatTagArgsToFirstTagIsNotAvailableInCalledTag() throws Exception {
        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder( new File("src/test/resources/template_root/"))
                .build();
        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);
        Map<String, Object> args = new HashMap<String, Object>();
        assertThat( sr.renderSrc("#{tagUsingSimpleTag 'data'/}", args) ).isEqualTo("data[from tag: ][from tag: argSpecified]");



    }
}
