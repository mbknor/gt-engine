package play.template2;

import play.template2.compile.GTCompiler;
import play.template2.compile.GTJavaCompileToClass;
import play.template2.compile.GTPreCompiler;
import play.template2.compile.GTPreCompilerFactory;
import play.template2.exceptions.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GTTemplateRepo {

    public final ClassLoader parentClassLoader;
    public final boolean checkForChanges;
    public final GTPreCompilerFactory preCompilerFactory;
    public final boolean preCompiledMode;
    public final File folderToDumpClassesIn;



    private Map<String, TemplateInfo> loadedTemplates = new ConcurrentHashMap<String, TemplateInfo>();
    private Map<String, TemplateInfo> classname2TemplateInfo = new ConcurrentHashMap<String, TemplateInfo>();


    public static class TemplateInfo {
        public final GTTemplateLocation templateLocation;
        public final long fileSize;
        public final long fileDate;
        public final GTTemplateInstanceFactory templateInstanceFactory;

        private TemplateInfo(GTTemplateLocation templateLocation, GTTemplateInstanceFactory templateInstanceFactory) {
            this.templateLocation = templateLocation;

            if ( templateLocation instanceof GTTemplateLocationReal) {
                GTTemplateLocationReal real = (GTTemplateLocationReal)templateLocation;
                // store fileSize and time so we can detect changes.
                File file = real.realFile;
                fileSize = file.length();
                fileDate = file.lastModified();
            } else {
                fileSize = 0;
                fileDate = 0;
            }
            this.templateInstanceFactory = templateInstanceFactory;
        }

        public boolean isModified() {

            if ( !(templateLocation instanceof GTTemplateLocationReal) ) {
                // Cannot check for changes - does not have a file
                return false;
            }

            File freshFile = new File(((GTTemplateLocationReal)templateLocation).realFile.getAbsolutePath());
            if (!freshFile.exists() || !freshFile.isFile()) {
                return true;
            }
            if (fileSize != freshFile.length()) {
                return true;
            }

            if ( fileDate != freshFile.lastModified()) {
                return true;
            }

            return false;
        }

        public Class<? extends GTJavaBase> getTemplateClass() {
            return templateInstanceFactory.getTemplateClass();
        }

        public GTLineMapper getGroovyLineMapper() {
            try {
                return (GTLineMapper)getTemplateClass().getDeclaredMethod("getGroovyLineMapper").invoke(null);
            } catch ( Exception e) {
                throw new RuntimeException(e);
            }
        }

        public GTLineMapper getJavaLineMapper() {
            try {
                return (GTLineMapper)getTemplateClass().getDeclaredMethod("getJavaLineMapper").invoke(null);
            } catch ( Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public GTTemplateRepo(ClassLoader parentClassLoader, boolean checkForChanges, GTPreCompilerFactory preCompilerFactory, boolean preCompiledMode, File folderToDumpClassesIn) {
        this.parentClassLoader = parentClassLoader;
        if (parentClassLoader== null) {
            throw new GTException("parentClassLoader cannot be null");
        }
        this.checkForChanges = checkForChanges;

        this.preCompilerFactory = preCompilerFactory;
        if ( preCompilerFactory ==null ) {
            throw new GTException("preCompilerFactory cannot be null");
        }

        this.preCompiledMode = preCompiledMode;
        this.folderToDumpClassesIn = folderToDumpClassesIn;
    }


    public boolean templateExists( GTTemplateLocation templateLocation) {

        try {
            getTemplateInstance( templateLocation, false);
            return true;
        } catch( Exception e) {
            // nop
            return false;
        }
    }

    private void removeTemplate ( String templatePath ) {
        TemplateInfo ti = loadedTemplates.remove( templatePath);
        if ( ti!=null) {
            classname2TemplateInfo.remove(ti.getTemplateClass().getName());
        }
    }

    private void addTemplate ( String templatePath, TemplateInfo ti) {
        loadedTemplates.put(templatePath, ti);
        classname2TemplateInfo.put(ti.getTemplateClass().getName(), ti);
    }

    public GTJavaBase getTemplateInstance( final GTTemplateLocation templateLocation) throws GTTemplateNotFound {
        return getTemplateInstance(templateLocation, true);
    }

    protected GTJavaBase getTemplateInstance( final GTTemplateLocation templateLocation, boolean doCompile) throws GTTemplateNotFound {

        // Is this a loaded template ?
        TemplateInfo ti = loadedTemplates.get(templateLocation.relativePath);
        if ( ti == null || checkForChanges ) {
            synchronized(loadedTemplates) {

                ti = loadedTemplates.get(templateLocation.relativePath);

                if ( ti == null ) {
                    // look for compiled class (precompiled class or class in cache.)
                    ti = lookForPreCompiledOrCached(templateLocation);
                }

                if ( ti != null) {
                    // is it changed on disk?
                    if (ti.isModified()) {
                        // remove it
                        removeTemplate( templateLocation.relativePath);
                        ti = null;
                    }
                }

                if (ti == null ) {
                    // new or modified - must compile it

                    if (templateLocation instanceof GTTemplateLocationReal) {
                        File file = ((GTTemplateLocationReal)templateLocation).realFile;

                        if ( !file.exists() || !file.isFile()) {
                            throw new GTTemplateNotFound( templateLocation.relativePath);
                        }
                    }

                    if ( !doCompile) {
                        // We know the compile exists - skip compiling.
                        return null;
                    }

                    ti = compileTemplate(templateLocation);

                }

                if ( ti != null) {
                    // store it
                    addTemplate(templateLocation.relativePath, ti);

                }
            }
        }

        if ( ti == null) {
            throw new GTTemplateNotFound(templateLocation.relativePath);
        }

        // already compile and unchanged - lets return the template instance
        GTJavaBase templateInstance = ti.templateInstanceFactory.create(this);

        return templateInstance;
    }

    // If running in precompiled mode, we look in parent classloader,
    // if not we're looking for class on disk.
    private TemplateInfo lookForPreCompiledOrCached(GTTemplateLocation templateLocation) {
        String templateClassName = GTPreCompiler.generatedPackageName + "." + GTPreCompiler.generateTemplateClassname( templateLocation.relativePath);
        if (preCompiledMode) {
            // compiled template classes are loaded by framework as regular classes....
            // look for it
            try {
                Class<? extends GTJavaBase> templateClass = (Class<? extends GTJavaBase>)parentClassLoader.loadClass(templateClassName);
                // found it
                return new TemplateInfo( templateLocation, new GTTemplateInstanceFactoryRegularClass(templateClass));
            } catch (ClassNotFoundException e) {
                // nop..
            }
        } else if(folderToDumpClassesIn != null){

            return loadTemplateFromDisk(templateLocation, templateClassName);
        }

        return null;
    }

    private TemplateInfo loadTemplateFromDisk(GTTemplateLocation templateLocation, String templateClassName) {
        // generate filename
        final String classFilenameWithPath = templateClassName.replace('.','/') + ".class";
        final File file = new File(folderToDumpClassesIn, classFilenameWithPath);
        if ( !file.exists()) {
            return null;
        }

        // create templateLocationReal to actuall template src file
        GTTemplateLocationReal templateLocationReal = GTFileResolver.impl.getTemplateLocationFromRelativePath(templateLocation.relativePath);

        if ( templateLocationReal == null) {
            // could not fund the corresponding template-source-file
            file.delete();
            return null;
        }

        // check if class file and template-src have the same lastModified date
        if ( templateLocationReal.realFile.lastModified() != file.lastModified()) {
            // cached classes are old. cannot use them. delete it so we don't find it again
            file.delete();
            return null;
        }


        // found the main template class file - must load all classes for this template - which all starts with the same name..
        final File folder = file.getParentFile();
        final String simpleFilename = file.getName().substring(0, file.getName().length() - 6); // remove ".class"
        File[] allClassFiles = folder.listFiles( new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.startsWith( simpleFilename);
            }
        });

        GTJavaCompileToClass.CompiledClass[] compiledClasses = new GTJavaCompileToClass.CompiledClass[allClassFiles.length];
        int i=0;
        for ( File classFile : allClassFiles) {
            byte[] bytes = IO.readContent( classFile);
            String className = GTPreCompiler.generatedPackageName + "." + classFile.getName().substring(0, classFile.getName().length() - 6); // remove ".class";

            compiledClasses[i++] = new GTJavaCompileToClass.CompiledClass(className, bytes);
        }

        GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler.CompiledTemplate(templateClassName, compiledClasses);


        return new TemplateInfo( templateLocationReal, new GTTemplateInstanceFactoryLive(parentClassLoader, compiledTemplate) );
        

    }

    public void removeTemplate(GTTemplateLocation templateLocation) {
        synchronized(loadedTemplates) {
            removeTemplate( templateLocation.relativePath);
        }
    }

    public TemplateInfo compileTemplate(GTTemplateLocation templateLocation) throws GTException {
        TemplateInfo ti;
        try {
            // compile it
            GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler(parentClassLoader, this, preCompilerFactory, true).compile( templateLocation);

            if (folderToDumpClassesIn != null && templateLocation instanceof GTTemplateLocationReal) {
                // Must dump these classes in folder...

                long templateLastModified = ((GTTemplateLocationReal)templateLocation).realFile.lastModified();

                for ( GTJavaCompileToClass.CompiledClass compiledClass : compiledTemplate.compiledJavaClasses) {
                    String filename = compiledClass.classname.replace('.','/') + ".class";
                    File file = new File(folderToDumpClassesIn, filename);
                    file.getParentFile().mkdirs();
                    IO.write( compiledClass.bytes, file);
                    // set lastModified date on file equal to the one from the template src - then we can check if cache is valid later..
                    file.setLastModified(templateLastModified);
                }

            }

            GTTemplateInstanceFactory templateInstanceFactory = new GTTemplateInstanceFactoryLive(parentClassLoader, compiledTemplate);

            ti = new TemplateInfo(templateLocation, templateInstanceFactory);
        } catch(GTTemplateNotFound e) {
            throw e;
        } catch(GTCompilationExceptionWithSourceInfo e) {
            throw e;
        } catch (Exception e) {
            // Must only store it if no error occurs
            throw new GTCompilationException(e);
        }
        return ti;
    }

    // converts stacktrace-elements referring to generated template code into pointin to the correct template-file and line
    public GTRuntimeException fixException(Throwable e) {
        TemplateInfo prevTi = null;
        TemplateInfo errorTI = null;
        int errorLine = 0;

        StackTraceElement[] seList = e.getStackTrace();

        if ( e instanceof GTTemplateRuntimeException) {
            // we must skip all stack-trace-elements in front until we find one with a generated classname
            int i=0;
            while ( i < seList.length) {
                String clazz = seList[i].getClassName();
                if ( clazz.startsWith(GTPreCompiler.generatedPackageName)) {
                    // This is a generated class
                    // This is our new start index
                    StackTraceElement[] l = new StackTraceElement[seList.length-i];
                    for ( int n = i; n< seList.length; n++) {
                        l[n-i] = seList[n];
                    }
                    seList = l;

                    break;
                }
                i++;
            }

            // unwrap this exception
            if ( e.getCause() != null) {
                e = e.getCause();
            }
        }

        List<StackTraceElement> newSElist = new ArrayList<StackTraceElement>();
        for ( StackTraceElement se : seList) {
            StackTraceElement orgSe = se;
            String clazz = se.getClassName();
            int lineNo=0;

            TemplateInfo ti = null;

            if ( clazz.startsWith(GTPreCompiler.generatedPackageName)) {
                // This is a generated template class

                int i = clazz.indexOf("$");
                if ( i > 0 ) {
                    clazz = clazz.substring(0, i);
                }

                boolean groovy = false;
                if ( clazz.endsWith("G")) {
                    // groovy class
                    groovy = true;
                    // Remove the last G in classname
                    clazz = clazz.substring(0,clazz.length()-1);
                }

                ti = classname2TemplateInfo.get(clazz);

                if (se.getMethodName().equals("_renderTemplate")) {
                    se = null;
                } else if (ti != null) {

                    if ( ti == prevTi ) {
                        // same template again - skip it
                        se = null;
                    } else {
                        prevTi = ti;

                        if ( groovy) {
                            lineNo = ti.getGroovyLineMapper().translateLineNo(se.getLineNumber());
                        } else {
                            // java
                            lineNo = ti.getJavaLineMapper().translateLineNo(se.getLineNumber());
                        }
                        se = new StackTraceElement(ti.templateLocation.relativePath, "", "line", lineNo);
                    }
                } else {
                    // just leave it as is
                }
            } else {
                // remove if groovy or reflection code
                if (clazz.startsWith("org.codehaus.groovy.") || clazz.startsWith("groovy.") || clazz.startsWith("sun.reflect.") || clazz.startsWith("java.lang.reflect.")) {
                    // remove it
                    se = null;
                }
            }

            if ( se != null) {
                if ( newSElist.isEmpty() && se != orgSe) {
                    // The topmost error is in a template
                    errorTI = ti;
                    errorLine = lineNo;
                }
                newSElist.add(se);
            }

        }

        StackTraceElement[] newStackTranceArray = newSElist.toArray(new StackTraceElement[]{});

        if ( errorTI != null) {
            // The top-most error is a template error and we have the source.
            // generate GTRuntimeExceptionWithSourceInfo
            GTRuntimeExceptionWithSourceInfo newE = new GTRuntimeExceptionWithSourceInfo(e.getMessage(), e, errorTI.templateLocation, errorLine);
            newE.setStackTrace( newStackTranceArray);
            // also update the stacktrace of the cause-exception
            e.setStackTrace( newStackTranceArray);
            return newE;
        } else {
            // The topmost error is not inside a template - wrap it in GTRuntimeException
            GTRuntimeException newE = new GTRuntimeException(e.getMessage(), e);
            newE.setStackTrace(newStackTranceArray);
            // also update the stacktrace of the cause-exception
            e.setStackTrace( newStackTranceArray);
            return newE;
        }
    }

}
