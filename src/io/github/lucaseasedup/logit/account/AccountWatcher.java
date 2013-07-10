/*
 * AccountWatcher.java
 *
 * Copyright (C) 2012-2013 LucasEasedUp
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
package io.github.lucaseasedup.logit.account;

import io.github.lucaseasedup.logit.LogItCore;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

/**
 * @author LucasEasedUp
 */
public class AccountWatcher implements Runnable
{
    public AccountWatcher(LogItCore core, AccountManager accountManager)
    {
        this.core = core;
        this.accountManager = accountManager;
    }
    
    @Override
    public void run()
    {
        if (core.getConfig().getInt("days-of-absence-to-unregister") < 0)
            return;
        
        Set<String> usernames = Collections.synchronizedSet(accountManager.getRegisteredUsernames());
        int now = (int) (System.currentTimeMillis() / 1000L);
        
        for (String username : usernames)
        {
            int lastActiveDate = accountManager.getLastActiveDate(username);
            
            if (lastActiveDate == 0)
                continue;
            
            //                                                                                   days to seconds
            if ((now - lastActiveDate) >= (core.getConfig().getInt("days-of-absence-to-unregister") * 86400))
            {
                try
                {
                    accountManager.removeAccount(username);
                }
                catch (SQLException | UnsupportedOperationException ex)
                {
                }
            }
        }
    }
    
    private final LogItCore core;
    private final AccountManager accountManager;
}
