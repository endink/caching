/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching;


public class PostEvictionCallbackRegistration {
    private IPostEvictionCallback getEvictionCallback;
    private Object state;

    public PostEvictionCallbackRegistration(IPostEvictionCallback getEvictionCallback, Object state) {
        this.getEvictionCallback = getEvictionCallback;
        this.state = state;
    }

    public IPostEvictionCallback getGetEvictionCallback() {
        return getEvictionCallback;
    }

    public void setGetEvictionCallback(IPostEvictionCallback getEvictionCallback) {
        this.getEvictionCallback = getEvictionCallback;
    }

    public Object getState() {
        return state;
    }

    public void setState(Object state) {
        this.state = state;
    }
}
