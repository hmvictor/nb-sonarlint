package org.radar.radarlint;

import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        new FileOpenedNotifier().init();
    }

}
