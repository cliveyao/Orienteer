package org.orienteer.core.boot.loader;

import com.google.common.collect.Lists;
import org.apache.wicket.WicketRuntimeException;
import org.eclipse.aether.artifact.Artifact;
import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.boot.loader.util.InitUtils;
import org.orienteer.core.boot.loader.util.JarUtils;
import org.orienteer.core.boot.loader.util.metadata.MetadataUtil;
import org.orienteer.core.boot.loader.util.metadata.OModuleMetadata;
import org.orienteer.core.widget.AbstractWidget;
import org.orienteer.core.widget.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Vitaliy Gonchar
 */
public class OrienteerClassLoader extends URLClassLoader {
	
	private final MavenResolver resolver         = MavenResolver.get();
	private final Path modulesFolder             = InitUtils.getPathToModulesFolder();
    private final boolean dependenciesFromPomXml = InitUtils.isDependenciesResolveFromPomXml();

    private static final Logger LOG = LoggerFactory.getLogger(OrienteerClassLoader.class);

    private static OrienteerClassLoader orienteerClassLoader;

    public static OrienteerClassLoader create(ClassLoader parent) {
        orienteerClassLoader = new OrienteerClassLoader(parent);
        return orienteerClassLoader;
    }

    public static OrienteerClassLoader get() {
        return orienteerClassLoader;
    }

	private OrienteerClassLoader(ClassLoader parent) {
		super(new URL[0], parent);

        Map<Path, OModuleMetadata> modules = MetadataUtil.readModulesAsMap();
        List<Path> jars = JarUtils.readJarsInFolder(modulesFolder);

        List<OModuleMetadata> modulesForLoad;
        if (modules.isEmpty()) {
            modulesForLoad = createModules(jars);
        } else {
            List<OModuleMetadata> modulesWithoutDeps = getModulesWithoutDependencies(modules.values());
            if (!resolver.resolveModuleMetadata(modulesWithoutDeps)) {
                modulesForLoad = createModules(jars);

            } else modulesForLoad = getUpdateModules(jars, modules);
        }
        modulesForLoad = searchTrustyModules(modulesForLoad, parent);
        addModulesToClassLoaderResources(modulesForLoad);
    }

    private List<OModuleMetadata> searchTrustyModules(List<OModuleMetadata> unTrustedModules,
                                                      ClassLoader parent) {
        List<OModuleMetadata> trustyModules = Lists.newArrayList();
        OrienteerSandboxClassLoader sandboxClassLoader = new OrienteerSandboxClassLoader(parent);
        for (OModuleMetadata module : unTrustedModules) {
            boolean isTrusted = sandboxClassLoader.test(module);
            if (isTrusted) {
                trustyModules.add(module);
            } else {
                sandboxClassLoader = new OrienteerSandboxClassLoader(parent);
                sandboxClassLoader.loadResourcesInClassLoader(trustyModules);
            }
        }
        return trustyModules;
    }

    private void addModulesToClassLoaderResources(List<OModuleMetadata> modules) {
        for(OModuleMetadata metadata : modules) {
            try {
                addURL(metadata.getMainArtifact().getFile().toURI().toURL());
                for (Artifact artifact : metadata.getDependencies()) {
                    addURL(artifact.getFile().toURI().toURL());
                }
            } catch (MalformedURLException e) {
                LOG.error("Can't load dependency", e);
            }
        }
    }
	
	private List<OModuleMetadata> createModules(List<Path> jars) {
        List<OModuleMetadata> modulesForLoad = resolver.getResolvedModulesMetadata(jars, dependenciesFromPomXml);
        if (modulesForLoad.size() > 0) {
            MetadataUtil.createMetadata(modulesForLoad);
        } else MetadataUtil.deleteMetadata();

        return modulesForLoad;
    }
	
	private List<OModuleMetadata> getModulesWithoutDependencies(Collection<OModuleMetadata> modules) {
        List<OModuleMetadata> modulesWithoutDependencies = Lists.newArrayList();
        for (OModuleMetadata module : modules) {
            for (Artifact artifact : module.getDependencies()) {
                if (artifact.getFile() == null || !artifact.getFile().exists()) {
                    modulesWithoutDependencies.add(module);
                    break;
                }
            }
        }
        return modulesWithoutDependencies;
    }
	
    private List<OModuleMetadata> getUpdateModules(List<Path> jars, Map<Path, OModuleMetadata> modules) {
        List<OModuleMetadata> modulesForWrite = getModulesForAddToMetadata(jars, modules);
        List<OModuleMetadata> modulesForDelete = getModulesForDelete(jars, modules);

        if (modulesForDelete.size() > 0) {
            if (modulesForDelete.size() == modules.values().size()) {
                MetadataUtil.deleteMetadata();
            } else {
                MetadataUtil.deleteModulesFromMetadata(modulesForDelete);
            }
        }
        if (modulesForWrite.size() > 0) {
            MetadataUtil.addModulesToMetadata(modulesForWrite);
        }

        modules = MetadataUtil.readModulesAsMap();

        return getModulesForLoad(modules.values());
    }
    
    private List<OModuleMetadata> getModulesForAddToMetadata(List<Path> jars, Map<Path, OModuleMetadata> modules) {
        List<Path> modulesForWrite = Lists.newArrayList();
        Set<Path> jarsInMetadata = modules.keySet();
        for (Path pathToJar : jars) {
            if (!jarsInMetadata.contains(pathToJar)) {
                modulesForWrite.add(pathToJar);
            }
        }
        return resolver.getResolvedModulesMetadata(modulesForWrite, dependenciesFromPomXml);
    }

    private List<OModuleMetadata> getModulesForDelete(List<Path> jars, Map<Path, OModuleMetadata> modules) {
        List<OModuleMetadata> modulesForDelete = Lists.newArrayList();
        for (Path path : modules.keySet()) {
            if (!jars.contains(path)) {
                modulesForDelete.add(modules.get(path));
            }
        }
        return modulesForDelete;
    }

    private List<OModuleMetadata> getModulesForLoad(Collection<OModuleMetadata> modules) {
        List<OModuleMetadata> modulesForLoad = Lists.newArrayList();
        for (OModuleMetadata metadata : modules) {
            if (metadata.isLoad()) modulesForLoad.add(metadata);
        }
        return modulesForLoad;
    }

    private static class OrienteerSandboxClassLoader extends URLClassLoader {

        public OrienteerSandboxClassLoader(ClassLoader parent) {
            super(new URL[]{}, parent);
        }

        public void loadResourcesInClassLoader(List<OModuleMetadata> modules) {
            for (OModuleMetadata module : modules) {
                loadResourceInClassLoader(module);
            }
        }

        private void loadResourceInClassLoader(OModuleMetadata module) {
            try {
                addURL(module.getMainArtifact().getFile().toURI().toURL());
                for (Artifact artifact : module.getDependencies()) {
                    addURL(artifact.getFile().toURI().toURL());
                }
            } catch (MalformedURLException e) {
                LOG.error("Cannot load dependency.", e);
            }
        }

        public boolean test(OModuleMetadata module) {
            boolean trusted = false;
            try {
                loadResourceInClassLoader(module);
                loadClass(module.getInitializerName());
                trusted = true;
            } catch (ClassNotFoundException e) {
                LOG.warn("Cannot load init class for module: " + module);
                if (LOG.isDebugEnabled()) e.printStackTrace();
            }
            return trusted;
        }
    }
}
