package play.template2;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Tag Context (retrieve who call you)
 */
public class GTTagContext {

    static ThreadLocal<Stack<GTTagContext>> currentStack = new ThreadLocal<Stack<GTTagContext>>();

    public final String tagName;
    public final Map<String, Object> data = new HashMap<String, Object>();

    protected GTTagContext(String tagName) {
        this.tagName = tagName;
    }
    
    public static void init() {
        currentStack.set(new Stack<GTTagContext>());
        enterTag("ROOT");
    }
    
    public static void enterTag(String name) {
        currentStack.get().add(new GTTagContext(name));
    }
    
    public static void exitTag() {
        currentStack.get().pop();
    }
    
    public static GTTagContext current() {
        return currentStack.get().peek();
    }
    
    public static GTTagContext parent() {
        if(currentStack.get().size() < 2) {
            return null;
        }
        return currentStack.get().get(currentStack.get().size()-2);
    }
    
    public static boolean hasParentTag(String name) {
        for(int i=currentStack.get().size()-1; i>=0; i--) {
            if(name.equals(currentStack.get().get(i).tagName)) {
                return true;
            }
        }
        return false;
    }
    
    public static GTTagContext parent(String name) {
        for(int i=currentStack.get().size()-1; i>=0; i--) {
            if(name.equals(currentStack.get().get(i).tagName)) {
                return currentStack.get().get(i);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return tagName+""+data;
    }



}
