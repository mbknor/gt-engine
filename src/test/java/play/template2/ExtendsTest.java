package play.template2;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ExtendsTest {

    // when tag uses extends, it should be as if the template calling the template extended it
    // the last one using extends wins
    // same behavour if a tag calls a tag


    // include and render behaves the same way as tags

    @Test
    public void testExtends() throws Exception {
                // first try with unix linefeeds
        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder( new File("src/test/resources/template_root/"))
                .build();

        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);

        Map<String,Object> args = new HashMap<String,Object>();

        GTJavaBase t = tr.getTemplateInstance(new GTTemplateLocation("templateUsingExtends.txt"));
        t.renderTemplate(args);
        assertThat( t.getAsString() ).isEqualTo("maintemplateUsingExtends");
        //assertThat( sr.renderSrc("#{tagUsingExtends/}template", args) ).isEqualTo("maintag1template");
        //assertThat( sr.renderSrc("#{tagUsingTagUsingExtends/}template", args) ).isEqualTo("maintag1tagxtemplate");
        //assertThat( sr.renderSrc("#{render 'templateUsingExtends.txt'/}template", args) ).isEqualTo("maintemplateUsingExtendstemplate");
        //assertThat( sr.renderSrc("#{include 'templateUsingExtends.txt'/}template", args) ).isEqualTo("maintemplateUsingExtendstemplate");



    }

}
