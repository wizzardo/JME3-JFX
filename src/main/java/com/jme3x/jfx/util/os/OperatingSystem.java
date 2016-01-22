package com.jme3x.jfx.util.os;

/**
 * @author Ronn
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

    public OperatingSystem() {
        final OperatingSystemResolver resolver = new OperatingSystemResolver();
        resolver.resolve(this);
    }

    /**
     * @return operating system architecture.
     */
    public String getArch() {
        return arch;
    }

    /**
     * @param arch operating system architecture.
     */
    public void setArch(final String arch) {
        this.arch = arch;
    }

    /**
     * @return distribution name of the operating system.
     */
    public String getDistribution() {
        return distribution;
    }

    /**
     * @param platform distribution name of the operating system.
     */
    public void setDistribution(final String platform) {
        this.distribution = platform;
    }

    /**
     * @return name of the operating system.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name name of the operating system.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return version of the operating system kernel.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version version of the operating system kernel.
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", version=" + version + ", arch=" + arch + ", distribution=" + distribution + "]";
    }
}
