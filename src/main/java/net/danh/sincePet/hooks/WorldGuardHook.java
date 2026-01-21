package net.danh.sincePet.hooks;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class WorldGuardHook {
    public static StateFlag PET_SPAWN;
    public static StateFlag PET_RIDE;
    public static StateFlag PET_FLY;
    public static StateFlag PET_ATTACK;
    public static StateFlag PET_BUFF;

    public void register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag spawnFlag = new StateFlag("pet-spawn", true);
            StateFlag rideFlag = new StateFlag("pet-ride", true);
            StateFlag flyFlag = new StateFlag("pet-fly", true);
            StateFlag attackFlag = new StateFlag("pet-attack", true);
            StateFlag buffFlag = new StateFlag("pet-buff", true);

            registry.register(spawnFlag);
            PET_SPAWN = spawnFlag;
            registry.register(rideFlag);
            PET_RIDE = rideFlag;
            registry.register(flyFlag);
            PET_FLY = flyFlag;
            registry.register(attackFlag);
            PET_ATTACK = attackFlag;
            registry.register(buffFlag);
            PET_BUFF = buffFlag;
        } catch (FlagConflictException e) {
            PET_SPAWN = (StateFlag) registry.get("pet-spawn");
            PET_RIDE = (StateFlag) registry.get("pet-ride");
            PET_FLY = (StateFlag) registry.get("pet-fly");
            PET_ATTACK = (StateFlag) registry.get("pet-attack");
            PET_BUFF = (StateFlag) registry.get("pet-buff");
        }
    }

    public RegionContainer getContainer() {
        return WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    public RegionQuery getQuery() {
        return getContainer().createQuery();
    }
}