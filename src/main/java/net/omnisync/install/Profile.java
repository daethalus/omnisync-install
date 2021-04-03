package net.omnisync.install;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Profile {
    private final String profile;
    private final String version;
    private final String icon;
    private final String json;
    private final String jar;
    private final String jarVersion;

    public Profile() throws IOException {

        InputStream input = Profile.class.getResourceAsStream("/profile.properties");

        Properties prop = new Properties();
        prop.load(input);

        profile = prop.getProperty("profile.name");
        version = prop.getProperty("profile.version");
        icon = prop.getProperty("profile.icon");
        json = prop.getProperty("profile.json");
        jar = prop.getProperty("profile.jar");
        jarVersion = prop.getProperty("profile.jarVersion");
    }

    public Profile(String profile, String version, String icon, String json, String jar, String jarVersion) {
        this.profile = profile;
        this.version = version;
        this.icon = icon;
        this.json = json;
        this.jar = jar;
        this.jarVersion = jarVersion;
    }

    public String getProfile() {
        return profile;
    }


    public String getVersion() {
        return version;
    }


    public String getIcon() {
        return icon;
    }


    public String getJson() {
        return json;
    }

    public String getJar() {
        return jar;
    }

    public String getJarVersion() {
        return jarVersion;
    }
}
