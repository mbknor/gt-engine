package play.template2;

public class GTTemplateLocation {

    /**
     * correct relative path in the app-context
     */
    public final String relativePath;

    public GTTemplateLocation(String relativePath) {
        this.relativePath = relativePath;
    }

    public String readSource() {
        GTTemplateLocationReal tl = GTFileResolver.impl.getTemplateLocationFromRelativePath(relativePath);
        return IO.readContentAsString(tl.realFileURL);
    }

    @Override
    public String toString() {
        return "GTTemplateLocation{" +
                "relativePath='" + relativePath + '\'' +
                '}';
    }
}
