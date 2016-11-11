package org.jrx.jira.instrumentation.provider.engine;

import com.atlassian.jira.config.properties.JiraProperties;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.modzdetector.IOUtils;
import com.atlassian.plugin.IllegalPluginStateException;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 26.10.2016.
 */
@Component
public class AgentInstaller {

    private static final Logger log = LoggerFactory.getLogger(AgentInstaller.class);

    private final JiraHome jiraHome;
    private final JiraProperties jiraProperties;

    @Autowired
    public AgentInstaller(
        @ComponentImport JiraHome jiraHome,
        @ComponentImport JiraProperties jiraProperties
    ) {
        this.jiraHome = jiraHome;
        this.jiraProperties = jiraProperties;
    }


    private static File getInstrumentationDirectory(JiraHome jiraHome) throws IOException {
        final File dataDirectory = jiraHome.getDataDirectory();
        final File instrFolder = new File(dataDirectory, "instrumentation");
        if (!instrFolder.exists()) {
            Files.createDirectory(instrFolder.toPath());
        }
        return instrFolder;
    }

    private static File loadFileFromCurrentJar(File destination, String fileName) throws IOException {
        try (InputStream resourceAsStream = AgentInstaller.class.getResourceAsStream("/lib/" + fileName)) {
            final File existingFile = new File(destination, fileName);
            if (!existingFile.exists() || !isCheckSumEqual(new FileInputStream(existingFile), resourceAsStream)) {
                Files.deleteIfExists(existingFile.toPath());
                existingFile.createNewFile();
                try (OutputStream os = new FileOutputStream(existingFile)) {
                    IOUtils.copy(resourceAsStream, os);
                }
            }
            return existingFile;
        }
    }

    private static boolean isCheckSumEqual(InputStream existingFileStream, InputStream newFileStream) {
        try (InputStream oldIs = existingFileStream; InputStream newIs = newFileStream) {
            return Arrays.equals(getMDFiveDigest(oldIs), getMDFiveDigest(newIs));
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error to compare checksum for streams {},{}", existingFileStream, newFileStream);
            return false;
        }
    }

    private static byte[] getMDFiveDigest(InputStream is) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        md.digest(IOUtils.toByteArray(is));
        return md.digest();
    }

    public void install() throws PluginException {
        try {
            log.trace("Trying to install tools and agent");
            if (!isProperAgentLoaded()) {
                log.info("Instrumentation agent is not installed yet or has wrong version");
                final String pid = getPid();
                log.debug("Current VM PID={}", pid);
                final URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                log.debug("System classLoader={}", systemClassLoader);
                final Class<?> virtualMachine = getVirtualMachineClass(
                    systemClassLoader,
                    "com.sun.tools.attach.VirtualMachine",
                    true
                );
                log.debug("VM class={}", virtualMachine);
                Method attach = virtualMachine.getMethod("attach", String.class);
                Method loadAgent = virtualMachine.getMethod("loadAgent", String.class);
                Method detach = virtualMachine.getMethod("detach");
                Object vm = null;
                try {
                    log.trace("Attaching to VM with PID={}", pid);
                    vm = attach.invoke(null, pid);
                    final File agentFile = getAgentFile();
                    log.debug("Agent file: {}", agentFile);
                    loadAgent.invoke(vm, agentFile.getAbsolutePath());
                } finally {
                    tryToDetach(vm, detach);
                }
            } else {
                log.info("Instrumentation agent is already installed");
            }
        } catch (Exception e) {
            throw new IllegalPluginStateException("Failed to load: agent and tools are not installed properly", e);
        }
    }

    private boolean isProperAgentLoaded() {
        try {
            ClassLoader.getSystemClassLoader().loadClass(InstrumentationProvider.INSTRUMENTATION_CLASS_NAME);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void tryToDetach(Object vm, Method detach) {
        try {
            if (vm != null) {
                log.trace("Detaching from VM: {}", vm);
                detach.invoke(vm);
            } else {
                log.warn("Failed to detach, vm is null");
            }
        } catch (Exception e) {
            log.warn("Failed to detach", e);
        }
    }

    private String getPid() {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        return nameOfRunningVM.split("@", 2)[0];
    }

    private Class<?> getVirtualMachineClass(URLClassLoader systemClassLoader, String className, boolean tryLoadTools) throws Exception {
        log.trace("Trying to get VM class, loadingTools={}", tryLoadTools);
        try {
            return systemClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            if (tryLoadTools) {
                final OS os = getRunningOs();
                os.tryToLoadTools(systemClassLoader, jiraHome);
                return getVirtualMachineClass(systemClassLoader, className, false);
            } else {
                throw new ReflectiveOperationException("Failed to load VM class", e);
            }
        }
    }

    private OS getRunningOs() {
        final String osName = jiraProperties.getSanitisedProperties().get("os.name");
        log.debug("OS name: {}", osName);
        if (Pattern.compile(".*[Ll]inux.*").matcher(osName).matches()) {
            return OS.LINUX;
        } else if (Pattern.compile(".*[Ww]indows.*").matcher(osName).matches()) {
            return OS.WINDOWS;
        } else {
            throw new IllegalStateException("Unknown OS running");
        }
    }

    private File getAgentFile() throws IOException {
        final File agent = loadFileFromCurrentJar(getInstrumentationDirectory(jiraHome), "instrumentation-agent.jar");
        agent.deleteOnExit();
        return agent;
    }

    private enum OS {
        WINDOWS {

            @Override
            protected String getToolsFilename() {
                return "tools-windows.jar";
            }

            @Override
            protected String getAttachLibFilename() {
                return "attach.dll";
            }
        },
        LINUX {

            @Override
            protected String getToolsFilename() {
                return "tools-linux.jar";
            }

            @Override
            protected String getAttachLibFilename() {
                return "libattach.so";
            }
        };

        public void tryToLoadTools(URLClassLoader systemClassLoader, JiraHome jiraHome) throws Exception {
            log.trace("Trying to load tools");
            final File instrumentationDirectory = getInstrumentationDirectory(jiraHome);
            if (!isLibraryAvailable()) {
                appendLibPath(instrumentationDirectory.getAbsolutePath());
                loadFileFromCurrentJar(instrumentationDirectory, getAttachLibFilename());
                resetCache();
            }
            final File tools = loadFileFromCurrentJar(instrumentationDirectory, getToolsFilename());
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(systemClassLoader, tools.toURI().toURL());
        }

        private void resetCache() throws NoSuchFieldException, IllegalAccessException {
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        }

        private void appendLibPath(String instrumentationDirectory) {
            if (System.getProperty("java.library.path") != null) {
                System.setProperty("java.library.path",
                    instrumentationDirectory + System.getProperty("path.separator")
                        + System.getProperty("java.library.path"));
            } else {
                System.setProperty("java.library.path", instrumentationDirectory);
            }
        }

        private boolean isLibraryAvailable() {
            try {
                System.loadLibrary(FilenameUtils.removeExtension(getAttachLibFilename()));
                return true;
            } catch (UnsatisfiedLinkError e) {
                log.debug("Attach lib is not available", e);
                return false;
            }
        }
        protected abstract String getToolsFilename();
        protected abstract String getAttachLibFilename();
    }
}
