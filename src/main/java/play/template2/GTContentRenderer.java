package play.template2;

public interface GTContentRenderer {

    public GTRenderingResult render();

    // Sometimes when using body like it is done in CRUD, then we have to modify params in this
    // renderingResults context
    public Object getRuntimeProperty(String name);
    public void setRuntimeProperty(String name, Object value);
}
