/**
 * Copyright 2011-2015 Yahoo Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.omid.tso;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.yahoo.omid.timestamp.storage.TimestampStorage;
import com.yahoo.omid.tso.TimestampOracleImpl.InMemoryTimestampStorage;

public class InMemoryTimestampStorageModule extends AbstractModule {

    @Override
    public void configure() {

        bind(TimestampStorage.class).to(InMemoryTimestampStorage.class).in(Singleton.class);

    }

}
