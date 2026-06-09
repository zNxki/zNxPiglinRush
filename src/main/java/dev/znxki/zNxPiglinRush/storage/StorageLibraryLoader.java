package dev.znxki.zNxPiglinRush.storage;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NonNull;

/**
 * Paper {@link PluginLoader} — executed BEFORE {@code #onEnable()}.
 *
 * <p>Downloads HikariCP and JDBC drivers (MySQL / MariaDB) from Maven Central
 * and injects them into the plugin classloader so they are already available
 * when {@link dev.znxki.zNxPiglinRush.PiglinRushPlugin} starts.
 *
 * <p>The libraries are cached by Paper in {@code libraries/} and are not
 * re-downloaded on every server startup.
 *
 * <p>Declared in {@code paper-plugin.yml} under the {@code loader:} key.
 */
@SuppressWarnings("UnstableApiUsage")
public final class StorageLibraryLoader implements PluginLoader {
    private static final String HIKARI_VERSION = "5.1.0";
    private static final String SLF4J_VERSION = "2.0.13";
    private static final String MYSQL_VERSION = "9.1.0";
    private static final String MARIADB_VERSION = "3.4.1";

    @Override
    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", "https://repo1.maven.org/maven2/").build());

        addDep(resolver, "com.zaxxer:HikariCP:" + HIKARI_VERSION);

        addDep(resolver, "org.slf4j:slf4j-api:" + SLF4J_VERSION);
        addDep(resolver, "org.slf4j:slf4j-simple:" + SLF4J_VERSION);

        addDep(resolver, "com.mysql:mysql-connector-j:" + MYSQL_VERSION);
        addDep(resolver, "org.mariadb.jdbc:mariadb-java-client:" + MARIADB_VERSION);

        classpathBuilder.addLibrary(resolver);
    }

    private static void addDep(@NonNull MavenLibraryResolver resolver, String gav) {
        resolver.addDependency(new Dependency(new DefaultArtifact(gav), null));
    }
}
