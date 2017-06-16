package com.jme3x.jfx.util.os;

/**
 * The type Operating system.
 *
 * @author JavaSaBr
 */
public class OperatingSystem {

    /**
     * Name of the operating system.
     */
    private String name;

    /**
     * Version of the operating system kernel.
     */
    private String version;

    /**
     * Operating system architecture.
     */
    private String arch;

    /**
     * Distribution name of the operating system.
     */
    private String distribution;

    /**
     * Instantiates a new Operating system.
     */
    public OperatingSystem() {
        final OperatingSystemResolver resolver = new OperatingSystemResolver();
        resolver.resolve(this);
    }

    /**
     * Gets arch.
     *
     * @return operating system architecture.
     */
    public String getArch() {
        return arch;
    }

    /**
     * Sets arch.
     *
     * @param arch operating system architecture.
     */
    public void setArch(final String arch) {
        this.arch = arch;
    }

    /**
     * Gets distribution.
     *
     * @return distribution name of the operating system.
     */
    public String getDistribution() {
        return distribution;
    }

    /**
     * Sets distribution.
     *
     * @param platform distribution name of the operating system.
     */
    public void setDistribution(final String platform) {
        this.distribution = platform;
    }

    /**
     * Gets name.
     *
     * @return name of the operating system.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name name of the operating system.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets version.
     *
     * @return version of the operating system kernel.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets version.
     *
     * @param version version of the operating system kernel.
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "OperatingSystem{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", arch='" + arch + '\'' +
                ", distribution='" + distribution + '\'' +
                '}';
    }
}
