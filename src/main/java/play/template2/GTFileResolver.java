package play.template2;

public class GTFileResolver {

    /**
     * This must be set by the framework with a working resolver
     */
    public static Resolver impl;


    public static interface Resolver {
        public GTTemplateLocationReal getTemplateLocationReal(String queryPath);
        public GTTemplateLocationReal getTemplateLocationFromRelativePath(String relativePath);
    }

}
