package play.template2;

import play.template2.compile.GTPreCompiler;
import play.template2.exceptions.*;

import java.util.ArrayList;
import java.util.List;

public class GTExceptionFixer {

    private final GTTemplateRepo templateRepo;

    public GTExceptionFixer(GTTemplateRepo templateRepo) {
        this.templateRepo = templateRepo;
    }

    // converts stacktrace-elements referring to generated template code into pointin to the correct template-file and line

    protected static class FixedStackTrace {
        public final GTTemplateRepo.TemplateInfo errorTI;
        public final int errorLine;
        public final StackTraceElement[] stackTraceElements;

        public FixedStackTrace(GTTemplateRepo.TemplateInfo errorTI, int errorLine, StackTraceElement[] stackTraceElements) {
            this.errorTI = errorTI;
            this.errorLine = errorLine;
            this.stackTraceElements = stackTraceElements;
        }
    }

    protected FixedStackTrace fixStackTrace(StackTraceElement[] seList) {
        GTTemplateRepo.TemplateInfo prevTi = null;
        GTTemplateRepo.TemplateInfo errorTI = null;
        int errorLine = 0;
        
        List<StackTraceElement> newSElist = new ArrayList<StackTraceElement>();
        for ( StackTraceElement se : seList) {
            StackTraceElement orgSe = se;
            String clazz = se.getClassName();
            int lineNo=0;

            GTTemplateRepo.TemplateInfo ti = null;

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

                ti = templateRepo.classname2TemplateInfo.get(clazz);

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
        
        return new FixedStackTrace(errorTI, errorLine, newStackTranceArray);
        
    }

    public GTRuntimeException fixException(Throwable e) {

        if ( e instanceof GTRuntimeExceptionForwarder) {
            e = e.getCause();
        }
        
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

        FixedStackTrace fixedStackTrace = fixStackTrace( seList);

        GTRuntimeException newE = null;
        
        if ( e instanceof GTRuntimeExceptionWithSourceInfo) {
            newE = (GTRuntimeExceptionWithSourceInfo)e;
        } else if ( fixedStackTrace.errorTI != null) {
            // The top-most error is a template error and we have the source.
            // generate GTRuntimeExceptionWithSourceInfo
            newE = new GTRuntimeExceptionWithSourceInfo(e.getMessage(), e, fixedStackTrace.errorTI.templateLocation, fixedStackTrace.errorLine);
        } else {
            // The topmost error is not inside a template - wrap it in GTRuntimeException
            newE = new GTRuntimeException(e.getMessage(), e);
        }

        newE.setStackTrace( fixedStackTrace.stackTraceElements);

        // fix the rest of the stacktraces
        Throwable cause = newE.getCause();
        while ( cause != null) {
            cause.setStackTrace( fixStackTrace( cause.getStackTrace()).stackTraceElements );
            cause = cause.getCause();
        }

        return newE;
    }


}
