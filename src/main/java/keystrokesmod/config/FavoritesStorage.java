package keystrokesmod.config;

import keystrokesmod.module.Module;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

public final class FavoritesStorage {
    private FavoritesStorage() {}

    private static File getFile() {
        File base = Minecraft.getMinecraft().mcDataDir;
        File dir = new File(base, "config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "keystrokesmod_favorites.properties");
    }

    public static void loadFavorites(Collection<Module> modules) {
        if (modules == null || modules.isEmpty()) return;
        File f = getFile();
        if (!f.exists()) return;

        Properties p = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            p.load(fis);
            for (Iterator<Module> it = modules.iterator(); it.hasNext(); ) {
                Module m = it.next();
                String name = m.getName();
                if (name == null) continue;
                String v = p.getProperty(name);
                if (v != null) {
                    m.setFavorite("true".equalsIgnoreCase(v));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (fis != null) try { fis.close(); } catch (Exception ignored) {}
        }
    }

    public static void saveFavorites(Collection<Module> modules) {
        if (modules == null) return;
        Properties p = new Properties();
        for (Iterator<Module> it = modules.iterator(); it.hasNext(); ) {
            Module m = it.next();
            String name = m.getName();
            if (name == null) continue;
            p.setProperty(name, m.isFavorite() ? "true" : "false");
        }

        File f = getFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            p.store(fos, "Favorites for keystrokesmod modules");
        } catch (Exception ignored) {
        } finally {
            if (fos != null) try { fos.close(); } catch (Exception ignored) {}
        }
    }
}
