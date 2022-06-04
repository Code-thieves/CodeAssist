/*
 * Copyright 2015 the original author or authors.
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
package com.tyron.builder.api.internal.artifacts.ivyservice;

import com.tyron.builder.api.internal.artifacts.configurations.ConfigurationInternal;
import com.tyron.builder.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactVisitor;
import com.tyron.builder.api.internal.artifacts.ivyservice.resolveengine.artifact.SelectedArtifactSet;
import com.tyron.builder.api.internal.artifacts.ivyservice.resolveengine.artifact.VisitedArtifactSet;
import com.tyron.builder.api.internal.artifacts.repositories.ResolutionAwareRepository;

import groovy.lang.Closure;

import com.tyron.builder.api.Action;
import com.tyron.builder.api.artifacts.Dependency;
import com.tyron.builder.api.artifacts.LenientConfiguration;
import com.tyron.builder.api.artifacts.ResolveException;
import com.tyron.builder.api.artifacts.ResolvedArtifact;
import com.tyron.builder.api.artifacts.ResolvedConfiguration;
import com.tyron.builder.api.artifacts.ResolvedDependency;
import com.tyron.builder.api.artifacts.UnresolvedDependency;
import com.tyron.builder.api.artifacts.component.ComponentIdentifier;
import com.tyron.builder.api.artifacts.result.DependencyResult;
import com.tyron.builder.api.artifacts.result.ResolutionResult;
import com.tyron.builder.api.artifacts.result.ResolvedComponentResult;
import com.tyron.builder.api.attributes.AttributeContainer;
import com.tyron.builder.api.internal.artifacts.ConfigurationResolver;
import com.tyron.builder.api.internal.artifacts.ResolveContext;
import com.tyron.builder.api.internal.artifacts.ResolverResults;

import com.tyron.builder.api.internal.attributes.AttributeContainerInternal;
import com.tyron.builder.api.internal.tasks.TaskDependencyResolveContext;
import com.tyron.builder.api.specs.Spec;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ErrorHandlingConfigurationResolver implements ConfigurationResolver {
    private final ConfigurationResolver delegate;

    public ErrorHandlingConfigurationResolver(ConfigurationResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ResolutionAwareRepository> getRepositories() {
        return delegate.getRepositories();
    }

    @Override
    public void resolveBuildDependencies(ConfigurationInternal configuration, ResolverResults results) {
        try {
            delegate.resolveBuildDependencies(configuration, results);
        } catch (Exception e) {
            results.failed(wrapException(e, configuration));
            BrokenResolvedConfiguration broken = new BrokenResolvedConfiguration(e, configuration);
            results.artifactsResolved(broken, broken);
        }
    }

    @Override
    public void resolveGraph(ConfigurationInternal configuration, ResolverResults results) throws ResolveException {
        try {
            delegate.resolveGraph(configuration, results);
        } catch (Exception e) {
            results.failed(wrapException(e, configuration));
            BrokenResolvedConfiguration broken = new BrokenResolvedConfiguration(e, configuration);
            results.artifactsResolved(broken, broken);
            return;
        }

        ResolutionResult wrappedResult = new ErrorHandlingResolutionResult(results.getResolutionResult(), configuration);
        results.graphResolved(wrappedResult, results.getResolvedLocalComponents(), results.getVisitedArtifacts());
    }

    @Override
    public void resolveArtifacts(ConfigurationInternal configuration, ResolverResults results) throws ResolveException {
        try {
            delegate.resolveArtifacts(configuration, results);
        } catch (Exception e) {
            BrokenResolvedConfiguration broken = new BrokenResolvedConfiguration(e, configuration);
            results.artifactsResolved(broken, broken);
            return;
        }

        ResolvedConfiguration wrappedConfiguration = new ErrorHandlingResolvedConfiguration(results.getResolvedConfiguration(), configuration);
        results.artifactsResolved(wrappedConfiguration, results.getVisitedArtifacts());
    }

    static ResolveException wrapException(Throwable e, ResolveContext resolveContext) {
        if (e instanceof ResolveException) {
            ResolveException resolveException = (ResolveException) e;
            return maybeAddHintToResolveException(resolveContext, resolveException);
        }
        return maybeAddHintToResolveException(resolveContext, new ResolveException(resolveContext.getDisplayName(), e));
    }

    private static ResolveException maybeAddHintToResolveException(ResolveContext resolveContext, ResolveException resolveException) {
        if (resolveContext instanceof ConfigurationInternal) {
            ConfigurationInternal config = (ConfigurationInternal) resolveContext;
            return config.maybeAddContext(resolveException);
        }
        return resolveException;
    }

    private static class ErrorHandlingLenientConfiguration implements LenientConfiguration {
        private final LenientConfiguration lenientConfiguration;
        private final ResolveContext resolveContext;

        private ErrorHandlingLenientConfiguration(LenientConfiguration lenientConfiguration, ResolveContext resolveContext) {
            this.lenientConfiguration = lenientConfiguration;
            this.resolveContext = resolveContext;
        }

        @Override
        public Set<ResolvedArtifact> getArtifacts() {
            try {
                return lenientConfiguration.getArtifacts();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<ResolvedArtifact> getArtifacts(Predicate<? super Dependency> dependencySpec) {
            try {
                return lenientConfiguration.getArtifacts(dependencySpec);
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<ResolvedDependency> getFirstLevelModuleDependencies() {
            try {
                return lenientConfiguration.getFirstLevelModuleDependencies();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<ResolvedDependency> getFirstLevelModuleDependencies(Predicate<? super Dependency> dependencySpec) {
            try {
                return lenientConfiguration.getFirstLevelModuleDependencies(dependencySpec);
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<ResolvedDependency> getAllModuleDependencies() {
            try {
                return lenientConfiguration.getAllModuleDependencies();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<UnresolvedDependency> getUnresolvedModuleDependencies() {
            try {
                return lenientConfiguration.getUnresolvedModuleDependencies();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<File> getFiles() {
            try {
                return lenientConfiguration.getFiles();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public Set<File> getFiles(Predicate<? super Dependency> dependencySpec) {
            try {
                return lenientConfiguration.getFiles(dependencySpec);
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }
    }

    private static class ErrorHandlingResolutionResult implements ResolutionResult {
        private final ResolutionResult resolutionResult;
        private final ResolveContext resolveContext;

        public ErrorHandlingResolutionResult(ResolutionResult resolutionResult, ResolveContext configuration) {
            this.resolutionResult = resolutionResult;
            this.resolveContext = configuration;
        }

        @Override
        public ResolvedComponentResult getRoot() {
            try {
                return resolutionResult.getRoot();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public void allDependencies(Action<? super DependencyResult> action) {
            resolutionResult.allDependencies(action);
        }

        @Override
        public Set<? extends DependencyResult> getAllDependencies() {
            try {
                return resolutionResult.getAllDependencies();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public void allDependencies(Closure closure) {
            resolutionResult.allDependencies(closure);
        }

        @Override
        public Set<ResolvedComponentResult> getAllComponents() {
            try {
                return resolutionResult.getAllComponents();
            } catch (Exception e) {
                throw wrapException(e, resolveContext);
            }
        }

        @Override
        public void allComponents(Action<? super ResolvedComponentResult> action) {
            resolutionResult.allComponents(action);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public void allComponents(Closure closure) {
            resolutionResult.allComponents(closure);
        }

        @Override
        public AttributeContainer getRequestedAttributes() {
            return resolutionResult.getRequestedAttributes();
        }
    }

    private static class ErrorHandlingResolvedConfiguration implements ResolvedConfiguration {
        private final ResolvedConfiguration resolvedConfiguration;
        private final ConfigurationInternal configuration;

        public ErrorHandlingResolvedConfiguration(ResolvedConfiguration resolvedConfiguration,
                                                  ConfigurationInternal configuration) {
            this.resolvedConfiguration = resolvedConfiguration;
            this.configuration = configuration;
        }

        @Override
        public boolean hasError() {
            return resolvedConfiguration.hasError();
        }

        @Override
        public LenientConfiguration getLenientConfiguration() {
            try {
                return new ErrorHandlingLenientConfiguration(resolvedConfiguration.getLenientConfiguration(), configuration);
            } catch (Exception e) {
                throw wrapException(e, configuration);
            }
        }

        @Override
        public void rethrowFailure() throws ResolveException {
            try {
                resolvedConfiguration.rethrowFailure();
            } catch (Exception e) {
                throw wrapException(e, configuration);
            }
        }

        @Override
        public Set<File> getFiles() throws ResolveException {
            try {
                return resolvedConfiguration.getFiles();
            } catch (ResolveException e) {
                throw wrapException(e, configuration);
            }
        }

        @Override
        public Set<File> getFiles(Predicate<? super Dependency> dependencySpec) throws ResolveException {
            try {
                return resolvedConfiguration.getFiles(dependencySpec);
            } catch (Exception e) {
                throw wrapException(e, configuration);
            }
        }

        @Override
        public Set<ResolvedDependency> getFirstLevelModuleDependencies() throws ResolveException {
            try {
                return resolvedConfiguration.getFirstLevelModuleDependencies();
            } catch (Exception e) {
                throw wrapException(e, configuration);
            }
        }

        @Override
        public Set<ResolvedDependency> getFirstLevelModuleDependencies(Predicate<? super Dependency> dependencySpec) throws ResolveException {
            try {
                return resolvedConfiguration.getFirstLevelModuleDependencies(dependencySpec);
            } catch (Exception e) {
                throw wrapException(e, configuration);
            }
        }

        @Override
        public Set<ResolvedArtifact> getResolvedArtifacts() throws ResolveException {
            try {
                return resolvedConfiguration.getResolvedArtifacts();
            } catch (Exception e) {
                throw wrapException(e, configuration);
            }
        }
    }

    private static class BrokenResolvedConfiguration implements ResolvedConfiguration, VisitedArtifactSet, SelectedArtifactSet {
        private final Throwable ex;
        private final ConfigurationInternal configuration;

        public BrokenResolvedConfiguration(Throwable ex, ConfigurationInternal configuration) {
            this.ex = ex;
            this.configuration = configuration;
        }

        @Override
        public boolean hasError() {
            return true;
        }

        @Override
        public LenientConfiguration getLenientConfiguration() {
            throw wrapException(ex, configuration);
        }

        @Override
        public void rethrowFailure() throws ResolveException {
            throw wrapException(ex, configuration);
        }

        @Override
        public Set<File> getFiles() throws ResolveException {
            throw wrapException(ex, configuration);
        }

        @Override
        public Set<File> getFiles(Predicate<? super Dependency> dependencySpec) throws ResolveException {
            throw wrapException(ex, configuration);
        }

        @Override
        public Set<ResolvedDependency> getFirstLevelModuleDependencies() throws ResolveException {
            throw wrapException(ex, configuration);
        }

        @Override
        public Set<ResolvedDependency> getFirstLevelModuleDependencies(Predicate<? super Dependency> dependencySpec) throws ResolveException {
            throw wrapException(ex, configuration);
        }

        @Override
        public Set<ResolvedArtifact> getResolvedArtifacts() throws ResolveException {
            throw wrapException(ex, configuration);
        }

        @Override
        public SelectedArtifactSet select(Predicate<? super Dependency> dependencySpec, AttributeContainerInternal requestedAttributes, Predicate<? super ComponentIdentifier> componentSpec, boolean allowNoMatchingVariant) {
            return this;
        }

        @Override
        public void visitDependencies(TaskDependencyResolveContext context) {
            context.visitFailure(ex);
        }

        @Override
        public void visitArtifacts(ArtifactVisitor visitor, boolean continueOnSelectionFailure) {
            visitor.visitFailure(ex);
        }

    }
}