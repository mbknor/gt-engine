package play.template2;


import play.template2.compile.GTCompiler;
import play.template2.compile.GTJavaCompileToClass;
import play.template2.exceptions.GTException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;

public class GTTemplateInstanceFactoryLive extends GTTemplateInstanceFactory {

    private final Class<? extends GTJavaBase> templateClass;
    private final GTCompiler.CompiledTemplate compiledTemplate;
    private final CL cl;

    public static ProtectionDomain protectionDomain;

    public static class CL extends ClassLoader {

        private final Set<String> classNames;
        private final Map<String, byte[]> resource2bytes;
        private final ClassLoader parent;

        public CL(ClassLoader parent, GTJavaCompileToClass.CompiledClass[] compiledClasses) {
            this.parent = parent;

            this.classNames = new HashSet<String>(compiledClasses.length);
            this.resource2bytes = new HashMap<String, byte[]>(compiledClasses.length);

            for (GTJavaCompileToClass.CompiledClass cp : compiledClasses) {
                classNames.add(cp.classname);
                defineClass(cp.classname, cp.bytes, 0, cp.bytes.length, GTTemplateInstanceFactoryLive.protectionDomain);
                String resourceName = cp.classname.replace(".", "/") + ".class";
                resource2bytes.put(resourceName, cp.bytes);
            }
        }

        @Override
        public InputStream getResourceAsStream(String s) {

            if (resource2bytes.containsKey(s)) {
                return new ByteArrayInputStream(resource2bytes.get(s));
            } else {
                return parent.getResourceAsStream(s);
            }
        }

        @Override
        public Class<?> loadClass(String s) throws ClassNotFoundException {
            if ( !classNames.contains(s)) return parent.loadClass(s);
            return super.loadClass(s);
        }

        @Override
        public URL getResource(String s) {
            if ( !classNames.contains(s)) return parent.getResource(s);
            return super.getResource(s);
        }

        @Override
        public Enumeration<URL> getResources(String s) throws IOException {
            if ( !classNames.contains(s)) return parent.getResources(s);
            return super.getResources(s);
        }

        @Override
        public void setDefaultAssertionStatus(boolean b) {
            super.setDefaultAssertionStatus(b);
        }

        @Override
        public void setPackageAssertionStatus(String s, boolean b) {
            super.setPackageAssertionStatus(s, b);
        }

        @Override
        public void setClassAssertionStatus(String s, boolean b) {
            super.setClassAssertionStatus(s, b);
        }

        @Override
        public void clearAssertionStatus() {
            super.clearAssertionStatus();
        }
    }

    public GTTemplateInstanceFactoryLive(ClassLoader parentClassLoader, GTCompiler.CompiledTemplate compiledTemplate) {
        this.compiledTemplate = compiledTemplate;
        this.cl = new CL(parentClassLoader, compiledTemplate.compiledJavaClasses);
        try {
            this.templateClass = (Class<? extends GTJavaBase>)cl.loadClass(compiledTemplate.templateClassName);
        } catch (Exception e) {
            throw new GTException("Error creating template class instance", e);
        }
    }

    @Override
    public Class<? extends GTJavaBase> getTemplateClass() {
        return templateClass;
    }
}
