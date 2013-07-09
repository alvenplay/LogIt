/*
 * LogoutCommand.java
 *
 * Copyright (C) 2012 LucasEasedUp
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
package io.github.lucaseasedup.logit.command;

import io.github.lucaseasedup.logit.LogItCore;
import static io.github.lucaseasedup.logit.LogItPlugin.getMessage;
import static io.github.lucaseasedup.logit.util.PlayerUtils.isPlayerOnline;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LogoutCommand extends AbstractCommandExecutor
{
    public LogoutCommand(LogItCore core)
    {
        super(core);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args)
    {
        Player p = null;
        
        try
        {
            p = (Player) s;
        }
        catch (ClassCastException ex)
        {
        }
        
        if (args.length > 0 && args[0].equals("-x") && args.length <= 2)
        {
            if (p != null && !p.hasPermission("logit.logout.others"))
            {
                s.sendMessage(getMessage("NO_PERMS"));
            }
            else if (args.length < 2)
            {
                s.sendMessage(getMessage("PARAM_MISSING").replace("%param%", "player"));
            }
            else if (!isPlayerOnline(args[1]))
            {
                s.sendMessage(getMessage("NOT_ONLINE").replace("%player%", args[1]));
            }
            else if (!core.getSessionManager().isSessionAlive(args[1]))
            {
                s.sendMessage(getMessage("START_SESSION_NOT_OTHERS").replace("%player%", args[1]));
            }
            else
            {
                core.getSessionManager().endSession(args[1]);

                s.sendMessage(getMessage("END_SESSION_SUCCESS_OTHERS").replace("%player%", args[1]));
            }
        }
        else if (args.length == 0)
        {
            if (p == null)
            {
                s.sendMessage(getMessage("ONLY_PLAYERS"));
            }
            else if (!p.hasPermission("logit.logout.self"))
            {
                p.sendMessage(getMessage("NO_PERMS"));
            }
            else if (!core.getSessionManager().isSessionAlive(p.getName()))
            {
                p.sendMessage(getMessage("START_SESSION_NOT_SELF"));
            }
            else
            {
                core.getSessionManager().endSession(p.getName());
            }
        }
        else
        {
            s.sendMessage(getMessage("INCORRECT_PARAMETER_COMBINATION"));
        }
        
        return true;
    }
}