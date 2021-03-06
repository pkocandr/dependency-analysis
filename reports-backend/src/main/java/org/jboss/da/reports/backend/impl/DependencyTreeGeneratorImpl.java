package org.jboss.da.reports.backend.impl;

import org.apache.maven.scm.ScmException;
import org.jboss.da.communication.indy.model.GAVDependencyTree;
import org.jboss.da.communication.pom.PomAnalysisException;
import org.jboss.da.communication.pom.model.MavenProject;
import org.jboss.da.communication.scm.api.SCMConnector;
import org.jboss.da.model.rest.GA;
import org.jboss.da.model.rest.GAV;
import org.jboss.da.reports.backend.api.DependencyTreeGenerator;
import org.jboss.da.reports.backend.api.GAVToplevelDependencies;
import org.jboss.da.reports.model.api.SCMLocator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Dustin Kut Moy Cheung &lt;dcheung@redhat.com&gt;
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@ApplicationScoped
public class DependencyTreeGeneratorImpl implements DependencyTreeGenerator {

    @Inject
    SCMConnector scmConnector;

    @Override
    public GAVDependencyTree getDependencyTree(SCMLocator scml) throws ScmException, PomAnalysisException {
        return scmConnector.getDependencyTreeOfRevision(
                scml.getScmUrl(),
                scml.getRevision(),
                scml.getPomPath(),
                scml.getRepositories());
    }

    @Override
    public GAVDependencyTree getDependencyTree(String url, String revision, GAV gav)
            throws ScmException, PomAnalysisException {
        return scmConnector.getDependencyTreeOfRevision(url, revision, gav);
    }

    @Override
    public GAVToplevelDependencies getToplevelDependencies(SCMLocator scml) throws ScmException, PomAnalysisException {
        Optional<MavenProject> pom = scmConnector.getPom(scml.getScmUrl(), scml.getRevision(), scml.getPomPath());
        GAV gav = pom.orElseThrow(() -> new ScmException("Failed to find specified pom: " + scml)).getGAV();

        Set<GAV> deps = scmConnector.getToplevelDependencyOfRevision(
                scml.getScmUrl(),
                scml.getRevision(),
                scml.getPomPath(),
                scml.getRepositories());
        return new GAVToplevelDependencies(gav, deps);
    }

    @Override
    public GAVToplevelDependencies getToplevelDependenciesFromModules(SCMLocator scml)
            throws ScmException, PomAnalysisException {
        Optional<MavenProject> pom = scmConnector.getPom(scml.getScmUrl(), scml.getRevision(), scml.getPomPath());
        GAV gav = pom.orElseThrow(() -> new ScmException("Failed to find specified pom: " + scml)).getGAV();

        Map<GA, Set<GAV>> dependenciesOfModules = scmConnector.getDependenciesOfModules(
                scml.getScmUrl(),
                scml.getRevision(),
                scml.getPomPath(),
                scml.getRepositories());
        Set<GAV> deps = dependenciesOfModules.values()
                .stream()
                .flatMap(Set::stream)
                .filter(g -> !dependenciesOfModules.containsKey(g.getGA()))
                .collect(Collectors.toCollection(HashSet::new));
        return new GAVToplevelDependencies(gav, deps);
    }

    @Override
    public GAVToplevelDependencies getToplevelDependencies(String url, String revision, GAV gav)
            throws ScmException, PomAnalysisException {
        Set<GAV> deps = scmConnector.getToplevelDependencyOfRevision(url, revision, gav);
        return new GAVToplevelDependencies(gav, deps);
    }
}
