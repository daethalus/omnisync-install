package net.omnisync.install;

import com.google.gson.*;
import net.omnisync.install.json.Artifact;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

public class Installer {

    public enum MachineProfile {
        LOW,
        HIGH
    }

    public static MachineProfile machineProfile() {
        OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean();

        if (bean.getTotalPhysicalMemorySize() <= 8589934592L) {
            return MachineProfile.LOW;
        }

        return MachineProfile.HIGH;
    }

    public static Gson GSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Artifact.class, new Artifact.Adapter())
            .create();

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String appDatFolder = System.getenv("APPDATA");
        File minecraftFolder = new File(appDatFolder, ".minecraft");

        JPanel contentPanel = new JPanel();

        JFrame frame = new JFrame();
        frame.setResizable(false);
        frame.setTitle("Omnisync installer");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setBounds(0, 0, 400, 120);
        frame.setContentPane(contentPanel);
        frame.setLocationRelativeTo(null);

        contentPanel.setLayout(null);

        JLabel jLabel = new JLabel("Minecraft folder: ");
        jLabel.setBounds(5, 10, 100, 20);
        contentPanel.add(jLabel);

        JTextField jTextField = new JTextField();
        jTextField.setText(minecraftFolder.getAbsolutePath());
        jTextField.setBounds(105, 10, 270, 20);

        contentPanel.add(jTextField);

        JButton btnInstall = new JButton("Install");
        btnInstall.setBounds(150, 40, 100, 20);
        btnInstall.addActionListener(e ->
        {
            install(new File(jTextField.getText()), frame);
        });

        contentPanel.add(btnInstall);

        frame.setVisible(true);
    }

    private static void install(File minecraftFolder, JFrame frame) {

        if (!minecraftFolder.exists()) {
            JOptionPane.showMessageDialog(frame, "folder " + minecraftFolder + " don't exists, please, open your launcher first");
            return;
        }

        File launcherProfiles = new File(minecraftFolder, "launcher_profiles.json");
        File versionRoot = new File(minecraftFolder, "versions");
        File librariesDir = new File(minecraftFolder, "libraries");

        File forgeFolder = new File(librariesDir, "net/minecraftforge/forge");
        if (!forgeFolder.exists()) {
            JOptionPane.showMessageDialog(frame, "minecraft forge is not installed, please, install forge first");
            return;
        }

        Profile profile;
        try {
            profile =
                    new Profile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "failed to load profile, please check  file profile.properties");
            return;
        }

        //copy version json file
        try (InputStream stream = Installer.class.getResourceAsStream("/" + profile.getJson())) {
            File json = new File(versionRoot, profile.getVersion() + '/' + profile.getVersion() + ".json");
            json.getParentFile().mkdirs();
            Files.copy(stream, json.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (InputStream stream = Installer.class.getResourceAsStream("/" + profile.getJar())) {
            File libFolder = new File(librariesDir, "net/omnisync/omnisync/" + profile.getJarVersion() + "/" + profile.getJar());
            libFolder.getParentFile().mkdirs();
            Files.copy(stream, libFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }

        injectProfile(profile, launcherProfiles);

        JOptionPane.showMessageDialog(frame, "omnisync installed successfully");

        frame.dispose();
    }

    private static boolean injectProfile(Profile profile, File target) {
        try {
            JsonObject json = null;
            try (InputStream stream = new FileInputStream(target)) {
                json = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                //error("Failed to read " + target);
                e.printStackTrace();
                return false;
            }

            JsonObject _profiles = json.getAsJsonObject("profiles");
            if (_profiles == null) {
                _profiles = new JsonObject();
                json.add("profiles", _profiles);
            }


            String javaHome = "";
            File javaHomeDir = new File(System.getProperty("java.home"));
            File javaWDir =  new File(new File(javaHomeDir, "bin"), "javaw.exe");

            JsonObject forgeProfile = _profiles.getAsJsonObject("forge");

            if (forgeProfile != null) {
                JsonElement eJavaDir = forgeProfile.get("javaDir");
                if (eJavaDir != null) {
                    javaHome = eJavaDir.getAsString();
                }
            }

            if (javaHome.isEmpty()) {
                javaHome = javaWDir.getPath();
            }

            MachineProfile machineProfile = machineProfile();

            String xmx = "-Xmx8G";
            if (machineProfile == MachineProfile.LOW) {
                xmx = "-Xmx6G";
            }

            JsonObject _profile = _profiles.getAsJsonObject(profile.getProfile());
            if (_profile == null) {
                _profile = new JsonObject();
                _profile.addProperty("name", profile.getProfile());
                _profile.addProperty("type", "custom");
                _profile.addProperty("javaDir", javaHome);
                _profile.addProperty("javaArgs", xmx + " -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=128M");
                _profiles.add(profile.getProfile(), _profile);
            }

            _profile.addProperty("lastVersionId", profile.getVersion());

            String icon = profile.getIcon();
            if (icon != null)
                _profile.addProperty("icon", icon);

            String jstring = GSON.toJson(json);
            Files.write(target.toPath(), jstring.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // error("There was a problem writing the launch profile,  is it write protected?");
            return false;
        }
        return true;
    }


}
