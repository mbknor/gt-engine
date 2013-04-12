package play.template2;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MetaMethod;
import org.apache.commons.lang.reflect.MethodUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import play.template2.exceptions.GTException;
import play.template2.exceptions.GTRuntimeExceptionForwarder;
import play.template2.exceptions.GTTemplateRuntimeException;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class GTJavaExtensionsInvoker {

    private static final Invoker regularArgsInvoker = new RegularArgsInvoker();
    private static final Invoker regularArgsWithObjectAsCollectionInvoker = new WithObjectAsCollectionInvoker(regularArgsInvoker);
    private static final Invoker withRealArgsAsArrayInvoker = new WithRealArgsAsArrayInvoker(regularArgsInvoker);
    private static final Invoker withRealArgsAsArrayAndObjectAsCollectionInvoker = new WithRealArgsAsArrayInvoker(regularArgsWithObjectAsCollectionInvoker);

    static final InvokeExecutor invokerExecutorMethod = new InvokerExecutorMethod();
    static final InvokeExecutor invokeExecutorGroovySupport = new InvokeExecutorGroovySupport();
    static final InvokeExecutor invokerExecutorRealMethod = new InvokerExecutorRealMethod();

    static final RealMethodInvoker realMethodInvoker = new RealMethodInvoker();

    static ConcurrentMap<InvocationSignatur, InvocationInfo> invocationInfoMap = new ConcurrentHashMap<InvocationSignatur, InvocationInfo>();

    private static final Invoker[] invokers = new Invoker[]{
            regularArgsInvoker,
            regularArgsWithObjectAsCollectionInvoker,
            withRealArgsAsArrayInvoker,
            withRealArgsAsArrayAndObjectAsCollectionInvoker
    };

    // Can be wrapping both java Method and groovy MetaMethod 
    static interface WrappedMethod {
        public Object invoke(Object o, Object[] args) throws Exception;
    }
    
    static class WrappedJavaMethod implements WrappedMethod {
        private final Method m;

        WrappedJavaMethod(Method m) {
            this.m = m;
        }

        @Override
        public Object invoke(Object o, Object[] args) throws Exception {
            return m.invoke(o, args);
        }
    }

    static class WrappedGroovyMetaMethod implements WrappedMethod {
        private final MetaMethod m;

        WrappedGroovyMetaMethod(MetaMethod m) {
            this.m = m;
        }

        @Override
        public Object invoke(Object o, Object[] args) throws Exception {
            return m.invoke(o, args);
        }
    }

    static interface Invoker {

        // generates args that should be used when invoking this kind
        public Object[] fixArgs(Object object, Object[] args);
        // finds a method on jeClass that matches this kind
        public WrappedMethod findMethod(Class jeClazz, String methodName, Object object, Object[] args);
    }



    static class RegularArgsInvoker implements Invoker {
        public Object[] fixArgs(Object object, Object[] args) {
            Object[] jeArgs = new Object[args.length+1];
            jeArgs[0] = object;
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    jeArgs[i+1] = arg;
                }
            }
            return jeArgs;
        }

        public WrappedMethod findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            Class[] jeArgsTypes = new Class[args.length+1];
            jeArgsTypes[0] = object.getClass();
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    jeArgsTypes[i+1] = arg.getClass();
                }
            }

            Method m = MethodUtils.getMatchingAccessibleMethod(jeClazz, methodName, jeArgsTypes);
            if ( m == null) {
                return null;
            }
            return new WrappedJavaMethod(m);
        }
    }

    static class WithObjectAsCollectionInvoker implements Invoker {

        private final Invoker baseInvoker;

        WithObjectAsCollectionInvoker(Invoker baseInvoker) {
            this.baseInvoker = baseInvoker;
        }

        public Object[] fixArgs(Object object, Object[] args) {
            // create list from object-array

            int arrayLength = Array.getLength(object);
            Collection objectCollection = new ArrayList(arrayLength);
            for ( int i=0; i < arrayLength; i++) {
                objectCollection.add( Array.get(object,i));
            }
            return baseInvoker.fixArgs(objectCollection, args);
        }

        public WrappedMethod findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            if ( !object.getClass().isArray()) {
                return null;
            }
            return baseInvoker.findMethod(jeClazz, methodName, new ArrayList(0), args);
        }
    }

    static class WithRealArgsAsArrayInvoker implements Invoker {

        private final Invoker baseInvoker;

        WithRealArgsAsArrayInvoker(Invoker baseInvoker) {
            this.baseInvoker = baseInvoker;
        }

        public Object[] fixArgs(Object object, Object[] args) {
            Class arrayType = args[0].getClass();

            // create new args-array with the array-type == the type of the first element in the real args
            Object argsArray = Array.newInstance(arrayType, args.length);
            for ( int i=0; i < args.length; i++) {
                Array.set(argsArray, i, args[i]);
            }

            return baseInvoker.fixArgs(object, new Object[]{argsArray});
        }

        public WrappedMethod findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            if ( args.length == 0) {
                return null;
            }

            Class arrayType = args[0].getClass();
            // create an empty array to get the type
            Object tmpArray = Array.newInstance(arrayType, 0);
            return baseInvoker.findMethod(jeClazz, methodName, object, new Object[]{tmpArray});
        }
    }

    static class RealMethodInvoker implements Invoker {
        public Object[] fixArgs(Object object, Object[] args) {
            return args;
        }

        public WrappedMethod findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            Class[] argsTypes = new Class[args.length];
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    argsTypes[i] = arg.getClass();
                }
            }
            // Look for it the regular way
            MetaMethod mm = InvokerHelper.getMetaClass(object).pickMethod(methodName, argsTypes);
            if ( mm != null ) {
                // found it
                return new WrappedGroovyMetaMethod(mm);
            }
            
            // if object is a class then Groovy won't find the static methods for us.
            // we must try to create an instance of the class and find the method that way.
            
            if ( object instanceof Class ) {
                // try to create an instance of the class
                try {
                    Object instance = ((Class)object).newInstance();
                    mm = InvokerHelper.getMetaClass(instance).pickMethod(methodName, argsTypes);
                    if ( mm != null ) {
                        // found it
                        return new WrappedGroovyMetaMethod(mm);
                    }
                } catch (Exception e) {
                    // Give up without logging anything
                } 
            }

            // As a last resort, try to find the method without using groovy
            Method m = MethodUtils.getMatchingAccessibleMethod(object.getClass(), methodName, argsTypes);
            if ( m != null) {
                // found it
                return new WrappedJavaMethod(m);
            }

            return null; // give up
        }
    }

    static interface InvokeExecutor {
        public Object doIt( WrappedMethod m, String methodName, Object object, Object[] args) throws Exception;
    }


    static class InvokerExecutorMethod implements InvokeExecutor {
        public Object doIt(WrappedMethod m, String methodName, Object object, Object[] args) throws Exception {
            return m.invoke(null, args);
        }
    }

    static class InvokerExecutorRealMethod implements InvokeExecutor {
        public Object doIt(WrappedMethod m, String methodName, Object object, Object[] args) throws Exception {
            return m.invoke(object, args);
        }
    }

    static class InvokeExecutorGroovySupport implements InvokeExecutor {

        public Object doIt(WrappedMethod m, String methodName, Object object, Object[] args) throws Exception {
            // This is a special groovy object - must special case
            GroovyObjectSupport gos = (GroovyObjectSupport)object;
            return gos.invokeMethod(methodName, args);
        }
    }

    static class InvocationSignatur {
        private final String methodName;
        private final Class objectType;
        private final Class[] argTypes;
        private final int hash;

        InvocationSignatur(String methodName, Object object, Object[] args) {
            this.methodName = methodName;
            this.objectType = object.getClass();
            this.argTypes = new Class[args.length];
            for ( int i=0;i<args.length;i++) {
                Object arg = args[i];
                if (arg!=null) {
                    argTypes[i] = args.getClass();
                }
            }
            // precalc hash for performance reasons
            this.hash = calcHashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InvocationSignatur that = (InvocationSignatur) o;

            if ( hash != that.hash) {
                return false;
            }

            if (!Arrays.equals(argTypes, that.argTypes)) return false;
            if (!methodName.equals(that.methodName)) return false;
            if (!objectType.equals(that.objectType)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        private int calcHashCode() {
            int result = methodName.hashCode();
            result = 31 * result + objectType.hashCode();
            result = 31 * result + Arrays.hashCode(argTypes);
            return result;
        }
    }

    static class InvocationInfo {
        public final WrappedMethod method;
        public final String methodName;
        public final InvokeExecutor invokeExecutor;
        public final Invoker invoker;

        InvocationInfo(WrappedMethod method, String methodName, InvokeExecutor invokeExecutor, Invoker invoker) {
            this.method = method;
            this.methodName = methodName;
            this.invokeExecutor = invokeExecutor;
            this.invoker = invoker;
        }
    }

    public static Object invoke( Class jeClazz, String methodName, Object object, Object[] args) {

        if ( object == null ) {
            return null;
        }

        try {

            InvocationSignatur invocationSignatur = new InvocationSignatur(methodName, object, args);

            // have we resolved this before?
            InvocationInfo invocationInfo = null;
            invocationInfo = invocationInfoMap.get(invocationSignatur);

            if ( invocationInfo != null ) {
                Invoker invoker = invocationInfo.invoker;
                return invocationInfo.invokeExecutor.doIt(invocationInfo.method, invocationInfo.methodName, object, (invoker!=null ? invoker.fixArgs(object, args) : null));
            }


            // start looking for JavaExtension

            // first we look for method with regular args..
            Invoker invoker = null;
            WrappedMethod m = null;
            InvokeExecutor invokerExecutor = null;
            for ( Invoker _invoker : invokers) {
                m = _invoker.findMethod(jeClazz, methodName, object, args);
                if ( m != null) {
                    invoker = _invoker;
                    invokerExecutor = invokerExecutorMethod;
                    break;
                }
            }

            if ( invokerExecutor == null) {
                if ( object instanceof GroovyObjectSupport) {
                    invokerExecutor = invokeExecutorGroovySupport;
                } else {

                    m = realMethodInvoker.findMethod(jeClazz, methodName, object, args);

                    if (m != null) {
                        invoker = realMethodInvoker;
                        invokerExecutor = invokerExecutorRealMethod;
                    }
                }
            }

            if (invokerExecutor != null) {
                invocationInfoMap.putIfAbsent(invocationSignatur, new InvocationInfo(m, methodName, invokerExecutor, invoker));
                Object res = invokerExecutor.doIt(m, methodName, object, (invoker!=null ? invoker.fixArgs(object, args) : args));
                return res;
            } else {
                throw new NoSuchMethodException(methodName);
            }

        } catch (Throwable e) {
            if ( e instanceof InvocationTargetException) {
                // must unwrap it..
                e = e.getCause();
            }
            throw new GTRuntimeExceptionForwarder(e);
        }
    }
}
