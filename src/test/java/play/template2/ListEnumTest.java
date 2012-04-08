package play.template2;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ListEnumTest {

    public enum MyEnum {A,B,C};

    @Test
    public void testListingEnums() throws Exception {
        TemplateSourceRenderer r = new TemplateSourceRenderer(new GTTemplateRepoBuilder().build());

        Map<String, Object> args = new HashMap<String, Object>();

        assertThat(r.renderSrc("#{list play.template2.ListEnumTest.MyEnum, as: 'e'}${e}#{/list}", args)).isEqualTo("ABC");
    }
}
