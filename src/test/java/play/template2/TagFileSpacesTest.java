package play.template2;

import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import static org.fest.assertions.Assertions.assertThat;

public class TagFileSpacesTest {

    @Test
    public void testSpaces() throws Exception {
        // first try with unix linefeeds
        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder( new File("src/test/resources/template_root/"))
                .build();

        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);

        doLineFeedTests(sr);

        // then test with windows linefeeds
        tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder( new File("src/test/resources/template_root/"))
                .withFakeWindowsNewLines(true)
                .build();

        sr = new TemplateSourceRenderer(tr);

        doLineFeedTests(sr);
    }

    private void doLineFeedTests(TemplateSourceRenderer sr) throws UnsupportedEncodingException {
        Map<String, Object> args = new HashMap<String, Object>();
        assertThat( sr.renderSrc("beforeTag-#{simpleTag 'data'/}-afterTag", args) ).isEqualTo("beforeTag-[from tag: data]-afterTag");
        assertThat( sr.renderSrc("beforeTag-#{tagWithScriptBlock 'data'/}-afterTag", args) ).isEqualTo("beforeTag-[from tag: data]-afterTag");
        assertThat( sr.renderSrc("beforeTag-#{tagWithScriptBlockAndComment 'data'/}-afterTag", args) ).isEqualTo("beforeTag-[from tag: data]-afterTag");
    }
}
