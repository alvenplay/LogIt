/*
 * BlockEventListener.java
 *
 * Copyright (C) 2012-2014 LucasEasedUp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.lucaseasedup.logit.listener;

import io.github.lucaseasedup.logit.LogItCoreObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public final class BlockEventListener extends LogItCoreObject implements Listener
{
    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlace(BlockPlaceEvent event)
    {
        if (!getConfig().getBoolean("force-login.prevent.block-place"))
            return;
        
        Player player = event.getPlayer();
        
        if (!getSessionManager().isSessionAlive(player) && getCore().isPlayerForcedToLogIn(player))
        {
            event.setCancelled(true);
            
            if (getConfig().getBoolean("force-login.prompt-on.block-place"))
            {
                getCore().sendForceLoginMessage(player);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    private void onBreak(BlockBreakEvent event)
    {
        if (!getConfig().getBoolean("force-login.prevent.block-break"))
            return;
        
        Player player = event.getPlayer();
        
        if (!getSessionManager().isSessionAlive(player) && getCore().isPlayerForcedToLogIn(player))
        {
            event.setCancelled(true);
            
            if (getConfig().getBoolean("force-login.prompt-on.block-break"))
            {
                getCore().sendForceLoginMessage(player);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    private void onHangingBreak(HangingBreakByEntityEvent event)
    {
        if (!getConfig().getBoolean("force-login.prevent.block-break")
                || !(event.getRemover() instanceof Player))
            return;
        
        Player player = (Player) event.getRemover();
        
        if (!getSessionManager().isSessionAlive(player) && getCore().isPlayerForcedToLogIn(player))
        {
            event.setCancelled(true);
            
            if (getConfig().getBoolean("force-login.prompt-on.block-break"))
            {
                getCore().sendForceLoginMessage(player);
            }
        }
    }
}
