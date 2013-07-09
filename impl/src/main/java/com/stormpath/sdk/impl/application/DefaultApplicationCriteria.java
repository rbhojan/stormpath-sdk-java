/*
 * Copyright 2013 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.impl.application;

import com.stormpath.sdk.application.ApplicationCriteria;
import com.stormpath.sdk.application.ApplicationOptions;
import com.stormpath.sdk.impl.query.DefaultCriteria;

/**
 * @since 0.8
 */
public class DefaultApplicationCriteria extends DefaultCriteria<ApplicationCriteria, ApplicationOptions> implements ApplicationCriteria {

    public DefaultApplicationCriteria() {
        super(new DefaultApplicationOptions());
    }

    @Override
    public ApplicationCriteria orderByName() {
        return orderBy(DefaultApplication.NAME);
    }

    @Override
    public ApplicationCriteria orderByDescription() {
        return orderBy(DefaultApplication.DESCRIPTION);
    }

    @Override
    public ApplicationCriteria orderByStatus() {
        return orderBy(DefaultApplication.STATUS);
    }

    public ApplicationCriteria expandAccounts() {
        getOptions().expandAccounts();
        return this;
    }

    public ApplicationCriteria expandAccounts(int limit) {
        getOptions().expandAccounts(limit);
        return this;
    }

    public ApplicationCriteria expandAccounts(int limit, int offset) {
        getOptions().expandAccounts(limit, offset);
        return this;
    }

    public ApplicationCriteria expandGroups() {
        getOptions().expandGroups();
        return this;
    }

    public ApplicationCriteria expandGroups(int limit) {
        getOptions().expandGroups(limit);
        return this;
    }

    public ApplicationCriteria expandGroups(int limit, int offset) {
        getOptions().expandGroups(limit, offset);
        return this;
    }

    public ApplicationCriteria expandTenant() {
        getOptions().expandTenant();
        return this;
    }
}
