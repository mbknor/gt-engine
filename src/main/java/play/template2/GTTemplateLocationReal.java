package play.template2;

import java.io.File;

public class GTTemplateLocationReal extends GTTemplateLocation {

    public final File realFile;

    public GTTemplateLocationReal(String relativePath, File realFile) {
        super(relativePath);
        this.realFile = realFile;
    }

    @Override
    public String readSource() {
        return IO.readContentAsString(realFile);
    }

    @Override
    public String toString() {
        return "GTTemplateLocationReal{" +
                "realFile=" + realFile +
                "} " + super.toString();
    }
}
