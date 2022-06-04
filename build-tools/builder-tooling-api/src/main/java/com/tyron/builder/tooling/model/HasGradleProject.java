/*
 * Copyright 2011 the original author or authors.
 *
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
 */

package com.tyron.builder.tooling.model;

import com.tyron.builder.tooling.model.ProjectIdentifier;

/**
 * An element that is associated with a Gradle project. Via the Gradle project you can access (list, run, etc.) Gradle tasks.
 */
public interface HasGradleProject extends ProjectModel {

    /**
     * Returns the identifier for the project that this model is associated with.
     * @since 3.3
     */
    @Override
    ProjectIdentifier getProjectIdentifier();

    /**
     * The associated Gradle project. Via the gradle project you can access (list, run, etc.) Gradle tasks.
     */
    GradleProject getGradleProject();
}