package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Destroyable;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import java.lang.annotation.Annotation;
import java.util.List;

public abstract class DestroyableBase implements Destroyable {

    private boolean destroyed;

    private List<Destroyable> subResources = C.newList();

    private volatile Class<? extends Annotation> scope;

    @Override
    public synchronized final void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        Destroyable.Util.destroyAll(subResources, scope());
        releaseResources();
    }

    @Override
    public final synchronized boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Recover the destry state. Use this API with cautious
     */
    protected void reload() {
        destroyed = false;
    }

    protected void releaseResources() {}

    public Class<? extends Annotation> scope() {
        if (null == scope) {
            synchronized (this) {
                if (null == scope) {
                    Class<?> c = getClass();
                    if (c.isAnnotationPresent(RequestScoped.class)) {
                        scope = RequestScoped.class;
                    } else if (c.isAnnotationPresent(SessionScoped.class)) {
                        scope = SessionScoped.class;
                    } else if (c.isAnnotationPresent(ApplicationScoped.class)) {
                        scope = ApplicationScoped.class;
                    } else {
                        scope = NormalScope.class;
                    }
                }
            }
        }
        return scope;
    }

    public synchronized void addSubResource(Destroyable object) {
        subResources.add(object);
    }

}
