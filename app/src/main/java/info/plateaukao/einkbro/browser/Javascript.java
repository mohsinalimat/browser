package info.plateaukao.einkbro.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import info.plateaukao.einkbro.database.RecordDb;
import info.plateaukao.einkbro.unit.RecordUnit;

public class Javascript {
    private static final String FILE = "javaHosts.txt";
    private static final Set<String> hostsJS = new HashSet<>();
    private static final List<String> whitelistJS = new ArrayList<>();
    @SuppressLint("ConstantLocale")
    private static final Locale locale = Locale.getDefault();

    private static void loadHosts(final Context context) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AssetManager manager = context.getAssets();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(manager.open(FILE)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        hostsJS.add(line.toLowerCase(locale));
                    }
                } catch (IOException i) {
                    Log.w("browser", "Error loading hosts");
                }
            }
        });
        thread.start();
    }

    private synchronized static void loadDomains(Context context) {
        RecordDb action = new RecordDb(context);
        action.open(false);
        whitelistJS.clear();
        whitelistJS.addAll(action.listDomains(RecordUnit.TABLE_JAVASCRIPT));
        action.close();
    }

    private final Context context;

    public Javascript(Context context) {
        this.context = context;

        if (hostsJS.isEmpty()) {
            loadHosts(context);
        }
        loadDomains(context);
    }

    public boolean isWhite(String url) {
        for (String domain : whitelistJS) {
            if (url != null && url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addDomain(String domain) {
        RecordDb action = new RecordDb(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_JAVASCRIPT);
        action.close();
        whitelistJS.add(domain);
    }

    public synchronized void removeDomain(String domain) {
        RecordDb action = new RecordDb(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_JAVASCRIPT);
        action.close();
        whitelistJS.remove(domain);
    }

    public synchronized void clearDomains() {
        RecordDb action = new RecordDb(context);
        action.open(true);
        action.clearDomainsJS();
        action.close();
        whitelistJS.clear();
    }
}
