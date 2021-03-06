package act.db.di;

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

import act.app.App;
import act.db.Dao;
import act.inject.param.ParamValueLoaderService;
import act.util.ActContext;
import org.osgl.inject.ValueLoader;
import org.osgl.util.*;

import java.util.Collection;

public class FindBy extends ValueLoader.Base {

    private String bindName;
    private Dao dao;
    private StringValueResolver resolver;
    private boolean findOne;
    private boolean byId;
    private String querySpec;
    private Class<?> rawType;

    @Override
    protected void initialized() {
        App app = App.instance();

        rawType = spec.rawType();
        findOne = !(Collection.class.isAssignableFrom(rawType));
        dao = app.dbServiceManager().dao(findOne ? rawType : (Class) spec.typeParams().get(0));

        byId = (Boolean) options.get("byId");
        resolver = app.resolverManager().resolver(byId ? dao.idType() : (Class) options.get("fieldType"));
        if (null == resolver) {
            throw new IllegalArgumentException("Cannot find String value resolver for type: " + dao.idType());
        }
        bindName = S.string(value());
        if (S.blank(bindName)) {
            bindName = ParamValueLoaderService.bindName(spec);
        }
        if (!byId) {
            querySpec = S.string(options.get("value"));
            if (S.blank(querySpec)) {
                querySpec = bindName;
            }
        }
    }

    @Override
    public Object get() {
        ActContext ctx = ActContext.Base.currentContext();
        E.illegalStateIf(null == ctx);
        String value = resolve(bindName, ctx);
        if (null == value) {
            return null;
        }
        Object by = resolver.resolve(value);
        if (null == by) {
            return null;
        }
        Collection col = findOne ? null : (Collection) App.instance().getInstance(rawType);
        if (byId) {
            Object bean = dao.findById(by);
            if (findOne) {
                return bean;
            } else {
                col.add(bean);
                return col;
            }
        } else {
            if (findOne) {
                return dao.findOneBy(Keyword.of(querySpec).javaVariable(), by);
            } else {
                col.addAll(C.list(dao.findBy(Keyword.of(querySpec).javaVariable(), by)));
                return col;
            }
        }
    }

    private static String resolve(String bindName, ActContext ctx) {
        String value = ctx.paramVal(bindName);
        if (S.notBlank(value)) {
            return value;
        }

        Keyword keyword = Keyword.of(bindName);
        if (keyword.tokens().size() > 1) {
            return resolve(keyword, ctx);
        } else {
            keyword = Keyword.of(bindName + " id");
            return resolve(keyword, ctx);
        }
    }

    private static String resolve(Keyword keyword, ActContext ctx) {
        String value = ctx.paramVal(keyword.underscore());
        if (S.notBlank(value)) {
            return value;
        }
        value = ctx.paramVal(keyword.javaVariable());
        if (S.notBlank(value)) {
            return value;
        }
        return null;
    }

}
