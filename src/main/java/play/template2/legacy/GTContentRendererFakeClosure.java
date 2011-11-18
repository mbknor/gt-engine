package play.template2.legacy;


import groovy.lang.Closure;
import groovy.lang.MetaClass;
import play.template2.GTContentRenderer;
import play.template2.GTJavaBase;
import play.template2.GTRenderingResult;

import java.io.PrintWriter;

public class GTContentRendererFakeClosure extends Closure {

    public final GTContentRenderer contentRenderer;
    private final GTJavaBase template;

    public GTContentRendererFakeClosure(GTJavaBase template, GTContentRenderer contentRenderer) {
        super(null, null);
        this.template = template;
        this.contentRenderer = contentRenderer;
    }

    public String renderToString() {
        GTRenderingResult res = contentRenderer.render();
        return res.getAsString();
    }

    @Override
    public String toString() {
        String s = renderToString();
        return s;
    }

    @Override
    public Object call() {
        // run the content

        PrintWriter customOut = (PrintWriter)contentRenderer.getRuntimeProperty("out");

        GTRenderingResult res = contentRenderer.render();

        // if someone has given us an alternative out (PrintWriter), then we must write
        // the result to that PrintWriter.
        if ( customOut != null) {
            customOut.write( res.getAsString());
        } else {
            // inject all the generated output into the output for the template
            template.insertOutput( res );
        }
        return null;
    }



    @Override
    public void setResolveStrategy(int resolveStrategy) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getProperty(String property) {
        return contentRenderer.getRuntimeProperty(property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        contentRenderer.setRuntimeProperty(property, newValue);
    }

    @Override
    public boolean isCase(Object candidate) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object call(Object[] args) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object call(Object arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setDelegate(Object delegate) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Class[] getParameterTypes() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getMaximumNumberOfParameters() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure asWritable() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void run() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure curry(Object[] arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure rcurry(Object[] arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure ncurry(int n, Object[] arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object clone() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setDirective(int directive) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        throw new RuntimeException("Not implemented");
    }
}
