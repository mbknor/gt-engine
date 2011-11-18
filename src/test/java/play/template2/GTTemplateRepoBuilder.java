package play.template2;

import play.template2.compile.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class GTTemplateRepoBuilder {

    private static class GTPreCompilerFactoryImpl implements GTPreCompilerFactory {

        public GTTemplateRepo templateRepo;

        public GTPreCompiler createCompiler(GTTemplateRepo templateRepo) {
            return new GTPreCompiler(templateRepo) {
                @Override
                public Class<? extends GTJavaBase> getJavaBaseClass() {
                    return GTJavaBaseTesterImpl.class;
                }
            };
        }

    }


    private GTTemplateRepo createTemplateRepo() {

        GTGroovyPimpTransformer.gtJavaExtensionMethodResolver = new GTJavaExtensionMethodResolver() {
            public Class findClassWithMethod(String methodName) {
                return null;
            }
        };

        GTJavaCompileToClass.typeResolver = new GTTypeResolver() {
            public byte[] getTypeBytes(String name) {

                try {
                    InputStream in = getClass().getClassLoader().getResourceAsStream( name.replaceAll("\\.", "/") + ".class");
                    if ( in==null) {
                        return null;
                    }

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while ( (bytesRead = in.read(buffer))>0 ) {
                        out.write(buffer, 0, bytesRead);
                    }

                    return out.toByteArray();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final GTPreCompilerFactoryImpl preCompilerFactory = new GTPreCompilerFactoryImpl();
        final GTTemplateRepo templateRepo = new GTTemplateRepo(getClass().getClassLoader(), false, preCompilerFactory, false, null);
        preCompilerFactory.templateRepo = templateRepo;
        return templateRepo;
    }



    public GTTemplateRepo build() {
        return createTemplateRepo();
    }
}
