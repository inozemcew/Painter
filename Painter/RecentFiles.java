package Painter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by aleksey on 11.10.16.
 */
public class RecentFiles extends Observable {
    private final static int MAX_RECENT = 5;
    private List<String> list = new LinkedList<>();
    private Preferences userPrefs;


    public RecentFiles(Object parent) {
        userPrefs = Preferences.userNodeForPackage(parent.getClass()).node("Recent files");
        try {
            for (String s : userPrefs.keys()) {
                list.add(userPrefs.get(s, ""));
            }
        } catch (BackingStoreException e) {
            list.clear();
        }
    }

    public void add(String s) {
        List<String> list = new LinkedList<>();
        try {
            for (String k : userPrefs.keys()) {
                list.add(userPrefs.get(k, ""));
            }
            if (list.contains(s)) list.remove(s);
            if (list.size() > MAX_RECENT) list.remove(list.size()-1);
            list.add(0, s);
            userPrefs.clear();
            int i = 0;
            for (String k :list)
                userPrefs.put(String.valueOf(i++), k);
            this.list = list;
        } catch (BackingStoreException e) {
            System.err.println("Cannot save list of recent files \n" + e.getMessage());
        }
        setChanged();
        notifyObservers();
    }


    public Iterator<String> getIterator() {
        return list.iterator();
    }

}
