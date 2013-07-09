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
package com.stormpath.sdk.impl.group;

import com.stormpath.sdk.group.GroupOptions;
import com.stormpath.sdk.impl.query.DefaultOptions;

/**
 * @since 0.8
 */
public class DefaultGroupOptions extends DefaultOptions<GroupOptions> implements GroupOptions<GroupOptions> {

    @Override
    public GroupOptions expandDirectory() {
        return expand(DefaultGroup.DIRECTORY);
    }

    @Override
    public GroupOptions expandTenant() {
        return expand(DefaultGroup.TENANT);
    }

    @Override
    public GroupOptions expandAccounts() {
        return expand(DefaultGroup.ACCOUNTS);
    }

    @Override
    public GroupOptions expandAccounts(int limit) {
        return expand(DefaultGroup.ACCOUNTS, limit);
    }

    @Override
    public GroupOptions expandAccounts(int limit, int offset) {
        return expand(DefaultGroup.ACCOUNTS, limit, offset);
    }

    @Override
    public GroupOptions expandAccountMemberships() {
        return expand(DefaultGroup.ACCOUNT_MEMBERSHIPS);
    }

    @Override
    public GroupOptions expandAccountMemberships(int limit) {
        return expand(DefaultGroup.ACCOUNT_MEMBERSHIPS, limit);
    }

    @Override
    public GroupOptions expandAccountMemberships(int limit, int offset) {
        return expand(DefaultGroup.ACCOUNT_MEMBERSHIPS, limit, offset);
    }
}
