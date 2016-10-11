package Painter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by aleksey on 11.10.16.
 */
public class RecentFiles {
    private final static int MAX_RECENT = 5;
    List<String> list = new LinkedList<>();
    Preferences userPrefs = Preferences.userNodeForPackage(RecentFiles.class).node("Recent files");

    public RecentFiles() {
        try {
            for (String s : userPrefs.keys()) {
                list.add(userPrefs.get(s, ""));
            }
        } catch (BackingStoreException e) {
            list.clear();
        }
    }

    public void add(String s) {
        if (list.contains(s)) list.remove(s);
        if (list.size() > MAX_RECENT) list.remove(list.size()-1);
        list.add(0, s);
        save();
    }

    public Iterator<String> getIterator() {
        return list.iterator();
    }

    private void save() {
        int i = 0;
        try {
            userPrefs.clear();
            for (String s:list)
                userPrefs.put(String.valueOf(i++),s);

        } catch (BackingStoreException e) {
            System.err.println("Cannot save list of recent files \n" + e.getMessage());
        }
    }
}
