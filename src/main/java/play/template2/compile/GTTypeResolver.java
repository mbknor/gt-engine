package play.template2.compile;

public interface GTTypeResolver {

    public byte[] getTypeBytes(String name);
    
    public boolean isApplicationClass(String className);
}
