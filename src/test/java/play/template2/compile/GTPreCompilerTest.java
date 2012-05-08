package play.template2.compile;

import org.junit.Test;
import play.template2.*;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class GTPreCompilerTest {

    @Test
    public void testNewLineInsideTagsExprEtc() throws Exception {

        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        assertThat(r.renderSrc("hello world", args)).isEqualTo("hello world");
        assertThat(r.renderSrc("${myData}", args)).isEqualTo("xxx");
        assertThat(r.renderSrc("${myData\n}", args)).isEqualTo("xxx");
        assertThat(r.renderSrc("${\r\nmyData\n}", args)).isEqualTo("xxx");
        assertThat(r.renderSrc("a${\nmyData\n}\nb${myData}", args)).isEqualTo("axxx\nbxxx");

        assertThat(r.renderSrc("#{set title:'a'/}", args)).isEqualTo("");
        assertThat(r.renderSrc("#{set 'title'}Q#{/set}", args)).isEqualTo("");

        assertThat(r.renderSrc("${ 1 == 1 ?\n 'true' : 'false'}", args)).isEqualTo("true");



    }

    // test some errors
    @Test
    public void testNewLineInsideTagsExprEtcError_1() throws Exception {

        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        GTCompilationExceptionWithSourceInfo ex = null;
        try {
            r.renderSrc("${myData", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found open $-declaration");

        ex = null;
        try {
            r.renderSrc("#{myTag", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found open #-declaration");

        ex = null;
        try {
            r.renderSrc("@@{'sss'", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found open @@-declaration");

        ex = null;
        try {
            r.renderSrc("#{aTag}", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found unclosed tag #{aTag}");

        
    }

    @Test
    public void testBracketsInsideExpr() throws Exception {

        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "123");

        assertThat(r.renderSrc("a${myData.each{x -> x}}b", args)).isEqualTo("a123b");
        assertThat(r.renderSrc("a${myData.each\n{x -> x}}b", args)).isEqualTo("a123b");
        assertThat(r.renderSrc("a${myData.each\n{x -> x}\n}b", args)).isEqualTo("a123b");



    }


}
