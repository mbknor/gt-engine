package play.template2;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class GTRenderingResult {

    protected List<StringWriter> allOuts = new ArrayList<StringWriter>();

    public GTRenderingResult() {
    }

    public GTRenderingResult(List<StringWriter> allOuts) {
        this.allOuts = allOuts;
    }

    public void writeOutput(OutputStream os, String encoding) {
        for ( StringWriter s : allOuts) {
            try {
                os.write(s.getBuffer().toString().getBytes(encoding));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * returns the rendering-output as string, but remember:
     * when dumping output to an outputstream, it is better to use writeOutput()
     *
     * @return the rendering-output as string
     */
    public String getAsString() {
        StringBuilder sb = new StringBuilder();
        for ( StringWriter s : allOuts) {
            sb.append( s.toString());
        }
        return sb.toString();
    }

}
