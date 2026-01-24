package net.danh.sincePet.mythic;

import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicDropRegister implements Listener {

    @EventHandler
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        String dropName = event.getDropName().toLowerCase();

        if (dropName.equals("pet-xp") || dropName.equals("pet-exp")) {
            event.register(new PetExpDrop(event.getConfig()));
        }
    }
}