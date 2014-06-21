/*
 * LogItCore.java
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
package io.github.lucaseasedup.logit;

import static io.github.lucaseasedup.logit.util.CollectionUtils.containsIgnoreCase;
import static io.github.lucaseasedup.logit.util.MessageHelper._;
import io.github.lucaseasedup.logit.account.Account;
import io.github.lucaseasedup.logit.account.AccountKeys;
import io.github.lucaseasedup.logit.account.AccountManager;
import io.github.lucaseasedup.logit.account.AccountWatcher;
import io.github.lucaseasedup.logit.backup.BackupManager;
import io.github.lucaseasedup.logit.command.AcclockCommand;
import io.github.lucaseasedup.logit.command.AccunlockCommand;
import io.github.lucaseasedup.logit.command.ChangeEmailCommand;
import io.github.lucaseasedup.logit.command.ChangePassCommand;
import io.github.lucaseasedup.logit.command.DisabledCommandExecutor;
import io.github.lucaseasedup.logit.command.LoginCommand;
import io.github.lucaseasedup.logit.command.LoginHistoryCommand;
import io.github.lucaseasedup.logit.command.LogoutCommand;
import io.github.lucaseasedup.logit.command.NopCommandExecutor;
import io.github.lucaseasedup.logit.command.ProfileCommand;
import io.github.lucaseasedup.logit.command.RecoverPassCommand;
import io.github.lucaseasedup.logit.command.RegisterCommand;
import io.github.lucaseasedup.logit.command.RememberCommand;
import io.github.lucaseasedup.logit.command.UnregisterCommand;
import io.github.lucaseasedup.logit.config.ConfigurationManager;
import io.github.lucaseasedup.logit.config.InvalidPropertyValueException;
import io.github.lucaseasedup.logit.config.PredefinedConfiguration;
import io.github.lucaseasedup.logit.cooldown.CooldownManager;
import io.github.lucaseasedup.logit.listener.BlockEventListener;
import io.github.lucaseasedup.logit.listener.EntityEventListener;
import io.github.lucaseasedup.logit.listener.InventoryEventListener;
import io.github.lucaseasedup.logit.listener.PlayerEventListener;
import io.github.lucaseasedup.logit.listener.ServerEventListener;
import io.github.lucaseasedup.logit.listener.SessionEventListener;
import io.github.lucaseasedup.logit.locale.EnglishLocale;
import io.github.lucaseasedup.logit.locale.GermanLocale;
import io.github.lucaseasedup.logit.locale.LocaleManager;
import io.github.lucaseasedup.logit.locale.PolishLocale;
import io.github.lucaseasedup.logit.persistence.AirBarSerializer;
import io.github.lucaseasedup.logit.persistence.ExperienceSerializer;
import io.github.lucaseasedup.logit.persistence.HealthBarSerializer;
import io.github.lucaseasedup.logit.persistence.HungerBarSerializer;
import io.github.lucaseasedup.logit.persistence.LocationSerializer;
import io.github.lucaseasedup.logit.persistence.PersistenceManager;
import io.github.lucaseasedup.logit.persistence.PersistenceSerializer;
import io.github.lucaseasedup.logit.profile.ProfileManager;
import io.github.lucaseasedup.logit.security.AuthMePasswordHelper;
import io.github.lucaseasedup.logit.security.BCrypt;
import io.github.lucaseasedup.logit.security.HashingAlgorithm;
import io.github.lucaseasedup.logit.security.SecurityHelper;
import io.github.lucaseasedup.logit.session.SessionManager;
import io.github.lucaseasedup.logit.storage.CacheType;
import io.github.lucaseasedup.logit.storage.Storage;
import io.github.lucaseasedup.logit.storage.Storage.DataType;
import io.github.lucaseasedup.logit.storage.StorageFactory;
import io.github.lucaseasedup.logit.storage.StorageType;
import io.github.lucaseasedup.logit.storage.WrapperStorage;
import io.github.lucaseasedup.logit.util.IoUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

/**
 * The central part of LogIt.
 */
public final class LogItCore
{
    private LogItCore(LogItPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    /**
     * Starts up the LogIt core.
     * 
     * @throws FatalReportedException if a critical error occurred
     *                                and LogIt could not start.
     * @throws IllegalStateException  if the LogIt core is already running.
     * 
     * @see #isStarted()
     * @see #stop()
     */
    public void start() throws FatalReportedException
    {
        if (isStarted())
            throw new IllegalStateException("The LogIt core has already been started.");
        
        getDataFolder().mkdir();
        
        firstRun = !getDataFile("config.yml").exists();
        
        String configHeader = "# # # # # # # # # # # # # # #\n"
                            + " LogIt Configuration File   #\n"
                            + "# # # # # # # # # # # # # # #\n";
        
        String statsHeader = "# # # # # # # # # # # # # # #\n"
                           + "  LogIt Statistics File     #\n"
                           + "# # # # # # # # # # # # # # #\n";
        
        String secretHeader = "# # # # # # # # # # # # # # # # # # # # # # # # # # # #\n"
                            + "             LogIt Secret Settings File               #\n"
                            + "                                                      #\n"
                            + " Do not touch unless you are 100% what you're doing!  #\n"
                            + "# # # # # # # # # # # # # # # # # # # # # # # # # # # #\n";
        
        configurationManager = new ConfigurationManager();
        configurationManager.registerConfiguration("config.yml",
                ".doNotTouch/config-def.b64", "config-def.ini", configHeader);
        configurationManager.registerConfiguration("stats.yml",
                ".doNotTouch/stats-def.b64", "stats-def.ini", statsHeader);
        configurationManager.registerConfiguration("secret.yml",
                ".doNotTouch/secret-def.b64", "secret-def.ini", secretHeader);
        
        try
        {
            configurationManager.loadAll();
        }
        catch (IOException | InvalidConfigurationException ex)
        {
            log(Level.SEVERE, "Could not load a configuration file.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        catch (InvalidPropertyValueException ex)
        {
            log(Level.SEVERE, ex.getMessage());
            
            FatalReportedException.throwNew(ex);
        }
        
        if (getConfig("config.yml").getBoolean("logging.file.enabled"))
        {
            openLogFile(getConfig("config.yml").getString("logging.file.filename"));
        }
        
        if (firstRun)
        {
            getDataFile("backup").mkdir();
            getDataFile("mail").mkdir();
            getDataFile("lang").mkdir();
            
            File passwordRecoveryTemplateFile = getDataFile("mail/password-recovery.html");
            
            if (!passwordRecoveryTemplateFile.exists())
            {
                try
                {
                    IoUtils.extractResource("password-recovery.html",
                            passwordRecoveryTemplateFile);
                }
                catch (IOException ex)
                {
                    log(Level.WARNING, "Could not copy resource: password-recovery.html", ex);
                }
            }
        }
        
        try
        {
            String updateVersion = LogItUpdateChecker.checkForUpdate(
                    getPlugin().getDescription().getFullName()
            );
            
            if (updateVersion != null)
            {
                log(Level.INFO, _("updateAvailable")
                        .replace("{0}", String.valueOf(updateVersion))
                        .replace("{1}", "http://dev.bukkit.org/bukkit-plugins/logit/"));
            }
        }
        catch (IOException ex)
        {
            // If a connection to the remote host could not be established,
            // we can't do anything about it and neither the user can,
            // so we'll just act as it never happened.
        }
        catch (ParseException ex)
        {
            log(Level.WARNING, ex);
        }
        
        localeManager = new LocaleManager();
        localeManager.registerLocale(EnglishLocale.getInstance());
        localeManager.registerLocale(PolishLocale.getInstance());
        localeManager.registerLocale(GermanLocale.getInstance());
        localeManager.setFallbackLocale(EnglishLocale.class);
        localeManager.switchActiveLocale(getConfig("config.yml").getString("locale"));
        
        StorageType leadingStorageType = StorageType.decode(
                getConfig("config.yml").getString("storage.accounts.leading.storage-type")
        );
        StorageType mirrorStorageType = StorageType.decode(
                getConfig("config.yml").getString("storage.accounts.mirror.storage-type")
        );
        
        String accountsUnit = getConfig("config.yml").getString("storage.accounts.leading.unit");
        AccountKeys accountKeys = new AccountKeys(
            getConfig("config.yml").getString("storage.accounts.keys.username"),
            getConfig("config.yml").getString("storage.accounts.keys.salt"),
            getConfig("config.yml").getString("storage.accounts.keys.password"),
            getConfig("config.yml").getString("storage.accounts.keys.hashing_algorithm"),
            getConfig("config.yml").getString("storage.accounts.keys.ip"),
            getConfig("config.yml").getString("storage.accounts.keys.login_session"),
            getConfig("config.yml").getString("storage.accounts.keys.email"),
            getConfig("config.yml").getString("storage.accounts.keys.last_active_date"),
            getConfig("config.yml").getString("storage.accounts.keys.reg_date"),
            getConfig("config.yml").getString("storage.accounts.keys.is_locked"),
            getConfig("config.yml").getString("storage.accounts.keys.login_history"),
            getConfig("config.yml").getString("storage.accounts.keys.display_name"),
            getConfig("config.yml").getString("storage.accounts.keys.persistence")
        );
        
        Storage leadingAccountStorage =
                new StorageFactory(getConfig("config.yml"), "storage.accounts.leading")
                        .produceStorage(leadingStorageType);
        
        Storage mirrorAccountStorage = 
                new StorageFactory(getConfig("config.yml"), "storage.accounts.mirror")
                        .produceStorage(mirrorStorageType);
        
        CacheType accountCacheType = CacheType.decode(
                getConfig("config.yml").getString("storage.accounts.leading.cache")
        );
        
        @SuppressWarnings("resource")
        WrapperStorage accountStorage = new WrapperStorage.Builder()
                .leading(leadingAccountStorage)
                .keys(accountKeys.getNames())
                .indexKey(accountKeys.username())
                .cacheType(accountCacheType)
                .build();
        accountStorage.mirrorStorage(mirrorAccountStorage, new Hashtable<String, String>()
            {{
                put(getConfig("config.yml").getString("storage.accounts.leading.unit"),
                    getConfig("config.yml").getString("storage.accounts.mirror.unit"));
            }});
        
        try
        {
            accountStorage.connect();
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not establish database connection.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        
        try
        {
            accountStorage.createUnit(accountsUnit, accountKeys);
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not create accounts table.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        
        try
        {
            accountStorage.setAutobatchEnabled(true);
            
            Hashtable<String, DataType> existingKeys = accountStorage.getKeys(accountsUnit);
            
            for (Map.Entry<String, DataType> e : accountKeys.entrySet())
            {
                if (!existingKeys.containsKey(e.getKey()))
                {
                    accountStorage.addKey(accountsUnit, e.getKey(), e.getValue());
                }
            }
            
            accountStorage.executeBatch();
            accountStorage.clearBatch();
            accountStorage.setAutobatchEnabled(false);
        }
        catch (IOException ex)
        {
            log(Level.SEVERE, "Could not update accounts table columns.", ex);
            
            FatalReportedException.throwNew(ex);
        }
        
        if (accountCacheType == CacheType.PRELOADED)
        {
            try
            {
                accountStorage.selectEntries(
                        getConfig("config.yml").getString("storage.accounts.leading.unit")
                );
            }
            catch (IOException ex)
            {
                log(Level.SEVERE, "Could not preload accounts.", ex);
            }
        }
        
        accountManager = new AccountManager(accountStorage, accountsUnit, accountKeys);
        persistenceManager = new PersistenceManager();
        
        setSerializerEnabled(LocationSerializer.class,
                getConfig("config.yml").getBoolean("waiting-room.enabled"));
        setSerializerEnabled(AirBarSerializer.class,
                getConfig("config.yml").getBoolean("force-login.obfuscate-bars.air"));
        setSerializerEnabled(HealthBarSerializer.class,
                getConfig("config.yml").getBoolean("force-login.obfuscate-bars.health"));
        setSerializerEnabled(ExperienceSerializer.class,
                getConfig("config.yml").getBoolean("force-login.obfuscate-bars.experience"));
        setSerializerEnabled(HungerBarSerializer.class,
                getConfig("config.yml").getBoolean("force-login.obfuscate-bars.hunger"));
        
        backupManager = new BackupManager(accountManager);
        sessionManager = new SessionManager();
        messageDispatcher = new LogItMessageDispatcher();
        
        if (getConfig("config.yml").getBoolean("profiles.enabled"))
        {
            File profilesPath = getDataFile(getConfig("config.yml").getString("profiles.path"));
            
            if (!profilesPath.exists())
            {
                profilesPath.getParentFile().mkdirs();
                profilesPath.mkdir();
            }
            
            profileManager = new ProfileManager(profilesPath,
                    getConfig("config.yml").getValues("profiles.fields"));
        }
        
        globalPasswordManager = new GlobalPasswordManager();
        cooldownManager = new CooldownManager();
        accountWatcher = new AccountWatcher();
        
        if (Bukkit.getPluginManager().isPluginEnabled("Vault"))
        {
            vaultPermissions =
                    Bukkit.getServicesManager().getRegistration(Permission.class).getProvider();
        }
        
        accountManagerTask = Bukkit.getScheduler().runTaskTimer(plugin,
                accountManager, 0L,
                getConfig("secret.yml").getTime("buffer-flush-interval", TimeUnit.TICKS));
        backupManagerTask = Bukkit.getScheduler().runTaskTimer(plugin,
                backupManager, 0L,
                BackupManager.TASK_PERIOD);
        sessionManagerTask = Bukkit.getScheduler().runTaskTimer(plugin,
                sessionManager, 0L,
                SessionManager.TASK_PERIOD);
        globalPasswordManagerTask = Bukkit.getScheduler().runTaskTimer(plugin,
                globalPasswordManager, 0L,
                GlobalPasswordManager.TASK_PERIOD);
        accountWatcherTask = Bukkit.getScheduler().runTaskTimer(plugin,
                accountWatcher, 0L,
                AccountWatcher.TASK_PERIOD);
        
        enableCommands();
        registerEvents();
        
        started = true;
        
        log(Level.FINE, _("startPlugin.success"));
        
        if (isFirstRun())
        {
            log(Level.INFO, _("firstRun"));
        }
    }
    
    /**
     * Stops the LogIt core.
     * 
     * @throws IllegalStateException if the LogIt core is not running.
     * 
     * @see #isStarted()
     * @see #start()
     */
    public void stop()
    {
        if (!isStarted())
            throw new IllegalStateException("The LogIt core is not started.");
        
        disableCommands();
        
        persistenceManager.unregisterSerializer(LocationSerializer.class);
        persistenceManager.unregisterSerializer(AirBarSerializer.class);
        persistenceManager.unregisterSerializer(HealthBarSerializer.class);
        persistenceManager.unregisterSerializer(ExperienceSerializer.class);
        persistenceManager.unregisterSerializer(HungerBarSerializer.class);
        
        try
        {
            accountManager.flushBuffer();
            accountManager.getStorage().close();
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not close database connection.", ex);
        }
        
        accountManagerTask.cancel();
        sessionManagerTask.cancel();
        accountWatcherTask.cancel();
        globalPasswordManagerTask.cancel();
        backupManagerTask.cancel();
        
        // Unregister all event listeners.
        HandlerList.unregisterAll(plugin);
        
        if (logFileWriter != null)
        {
            try
            {
                logFileWriter.close();
            }
            catch (IOException ex)
            {
                log(Level.WARNING, "Could not close log file.", ex);
            }
        }
        
        started = false;
        
        dispose();
        
        log(Level.FINE, _("stopPlugin.success"));
    }
    
    /**
     * Disposes the LogIt core.
     * 
     * @throws IllegalStateException if the LogIt core is running.
     */
    private void dispose()
    {
        if (isStarted())
            throw new IllegalStateException("Cannot dispose the LogIt core while it's running.");
        
        if (configurationManager != null)
        {
            configurationManager.dispose();
            configurationManager = null;
        }
        
        if (localeManager != null)
        {
            localeManager.dispose();
            localeManager = null;
        }
        
        if (accountManager != null)
        {
            accountManager.dispose();
            accountManager = null;
        }
        
        if (persistenceManager != null)
        {
            persistenceManager.dispose();
            persistenceManager = null;
        }
        
        if (backupManager != null)
        {
            backupManager.dispose();
            backupManager = null;
        }
        
        if (sessionManager != null)
        {
            sessionManager.dispose();
            sessionManager = null;
        }
        
        if (messageDispatcher != null)
        {
            messageDispatcher.dispose();
            messageDispatcher = null;
        }
        
        if (profileManager != null)
        {
            profileManager.dispose();
            profileManager = null;
        }
        
        if (globalPasswordManager != null)
        {
            globalPasswordManager.dispose();
            globalPasswordManager = null;
        }
        
        if (cooldownManager != null)
        {
            cooldownManager.dispose();
            cooldownManager = null;
        }
        
        accountWatcher = null;
        vaultPermissions = null;
        
        logFileWriter = null;
    }
    
    /**
     * Restarts the LogIt core.
     * 
     * @throws FatalReportedException if the LogIt core could not be started again.
     * @throws IllegalStateException  if the LogIt core is not running.
     * 
     * @see #isStarted()
     * @see #start()
     */
    public void restart() throws FatalReportedException
    {
        if (!isStarted())
            throw new IllegalStateException("The LogIt core is not started.");
        
        File sessionsFile =
                getDataFile(getConfig("config.yml").getString("storage.sessions.filename"));
        
        try
        {
            sessionManager.exportSessions(sessionsFile);
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not export sessions.", ex);
        }
        
        stop();
        start();
        
        try
        {
            plugin.reloadMessages(getConfig("config.yml").getString("locale"));
        }
        catch (IOException ex)
        {
            log(Level.WARNING, "Could not load messages.", ex);
        }
        
        if (sessionsFile.exists())
        {
            try
            {
                sessionManager.importSessions(sessionsFile);
            }
            catch (IOException ex)
            {
                log(Level.WARNING, "Could not import sessions.", ex);
            }
            
            sessionsFile.delete();
        }
        
        log(Level.INFO, _("reloadPlugin.success"));
    }
    
    /**
     * Checks if a password is equal, after hashing, to {@code hashedPassword}.
     * 
     * <p> If the <i>force-hashing-algorithm</i>
     * secret setting is set to <i>true</i>,
     * the global hashing algorithm (specified in the config file)
     * will be used instead of the provided {@code hashingAlgorithm}.
     * 
     * @param password         the password to be checked.
     * @param hashedPassword   the hashed password.
     * @param hashingAlgorithm the algorithm used when hashing {@code hashedPassword}.
     * 
     * @return {@code true} if passwords match; {@code false} otherwise.
     * 
     * @see #checkPassword(String, String, String, String)
     */
    public boolean checkPassword(String password, String hashedPassword, String hashingAlgorithm)
    {
        if (hashingAlgorithm == null
                || getConfig("secret.yml").getBoolean("force-hashing-algorithm"))
        {
            hashingAlgorithm = getDefaultHashingAlgorithm().name();
        }
        
        HashingAlgorithm algorithmType = HashingAlgorithm.decode(hashingAlgorithm);
        
        if (algorithmType == HashingAlgorithm.BCRYPT)
        {
            return BCrypt.checkpw(password, hashedPassword);
        }
        else if (algorithmType == HashingAlgorithm.AUTHME)
        {
            return AuthMePasswordHelper.comparePasswordWithHash(password, hashedPassword,
                    hashingAlgorithm.replaceAll("^authme:", ""));
        }
        else
        {
            return hashedPassword.equals(SecurityHelper.hash(password, algorithmType));
        }
    }
    
    /**
     * Checks if a password (with a salt appended) is equal,
     * after hashing, to {@code hashedPassword}.
     * 
     * <p> If the <i>force-hashing-algorithm</i>
     * secret setting is set to <i>true</i>,
     * the global hashing algorithm (specified in the config file)
     * will be used instead of the provided {@code hashingAlgorithm}.
     * 
     * @param password         the password to be checked.
     * @param hashedPassword   the hashed password.
     * @param salt             the salt for the passwords.
     * @param hashingAlgorithm the algorithm used when hashing {@code hashedPassword}.
     * 
     * @return {@code true} if passwords match; {@code false} otherwise.
     * 
     * @see #checkPassword(String, String, String)
     */
    public boolean checkPassword(String password,
                                 String hashedPassword,
                                 String salt,
                                 String hashingAlgorithm)
    {
        if (hashedPassword == null || hashedPassword.isEmpty())
            return false;
        
        if (hashingAlgorithm == null
                || getConfig("secret.yml").getBoolean("force-hashing-algorithm"))
        {
            hashingAlgorithm = getDefaultHashingAlgorithm().name();
        }
        
        HashingAlgorithm algorithmType = HashingAlgorithm.decode(hashingAlgorithm);
        
        if (algorithmType == HashingAlgorithm.BCRYPT)
        {
            try
            {
                return BCrypt.checkpw(password, hashedPassword);
            }
            catch (IllegalArgumentException ex)
            {
                return false;
            }
        }
        else if (algorithmType == HashingAlgorithm.AUTHME)
        {
            return AuthMePasswordHelper.comparePasswordWithHash(password, hashedPassword,
                    hashingAlgorithm.replaceAll("^authme:", ""));
        }
        else
        {
            if (getConfig("config.yml").getBoolean("password.use-salt"))
            {
                return hashedPassword.equals(
                        SecurityHelper.hash(password, salt, algorithmType)
                );
            }
            else
            {
                return hashedPassword.equals(
                        SecurityHelper.hash(password, algorithmType)
                );
            }
        }
    }
    
    /**
     * Checks if a player is forced to log in.
     * 
     * <p> Returns {@code true} if the <i>force-login.global</i> config setting
     * is set to <i>true</i>, or the player is in a world with forced login.
     * 
     * <p> If the player name is contained in the <i>force-login.exempt-players</i>
     * config property, it always returns {@code false} regardless of the above conditions.
     * 
     * <p> Note that this method does not check if the player is logged in.
     * For that purpose, use {@link SessionManager#isSessionAlive(Player)}
     * or {@link SessionManager#isSessionAlive(String)}.
     * 
     * @param player the player whom this check will be ran on.
     * 
     * @return {@code true} if the player is forced to log in; {@code false} otherwise.
     */
    public boolean isPlayerForcedToLogIn(Player player)
    {
        String playerWorldName = player.getWorld().getName();
        
        boolean forcedLoginGlobal =
                getConfig("config.yml").getBoolean("force-login.global");
        List<String> exemptedWorlds =
                getConfig("config.yml").getStringList("force-login.in-worlds");
        List<String> exemptedPlayers =
                getConfig("config.yml").getStringList("force-login.exempt-players");
        
        return (forcedLoginGlobal || exemptedWorlds.contains(playerWorldName))
                && !containsIgnoreCase(player.getName(), exemptedPlayers);
    }
    
    /**
     * Updates permission groups for a player only if LogIt is linked to Vault.
     * 
     * <p> Permission groups currently supported: <ul>
     *  <li>Registered</li>
     *  <li>Not registered</li>
     *  <li>Logged in</li>
     *  <li>Logged out</li>
     * </ul>
     * 
     * <p> Exact group names will be read from the configuration file.
     * 
     * @param player the player whose permission groups should be updated.
     */
    public void updatePlayerGroup(Player player)
    {
        if (!isLinkedToVault())
            return;
        
        if (accountManager.isRegistered(player.getName()))
        {
            vaultPermissions.playerRemoveGroup(player,
                    getConfig("config.yml").getString("groups.unregistered"));
            vaultPermissions.playerAddGroup(player,
                    getConfig("config.yml").getString("groups.registered"));
        }
        else
        {
            vaultPermissions.playerRemoveGroup(player,
                    getConfig("config.yml").getString("groups.registered"));
            vaultPermissions.playerAddGroup(player,
                    getConfig("config.yml").getString("groups.unregistered"));
        }
        
        if (sessionManager.isSessionAlive(player))
        {
            vaultPermissions.playerRemoveGroup(player,
                    getConfig("config.yml").getString("groups.logged-out"));
            vaultPermissions.playerAddGroup(player,
                    getConfig("config.yml").getString("groups.logged-in"));
        }
        else
        {
            vaultPermissions.playerRemoveGroup(player,
                    getConfig("config.yml").getString("groups.logged-in"));
            vaultPermissions.playerAddGroup(player,
                    getConfig("config.yml").getString("groups.logged-out"));
        }
    }
    
    /**
     * Logs a message in the name of LogIt.
     * 
     * <p> The logger message will be saved in a log file if doing so is permitted
     * by the appropriate configuration setting.
     * 
     * @param level   the message level.
     * @param message the message to be logged.
     * 
     * @see #log(Level, String, Throwable)
     */
    public void log(Level level, String message)
    {
        if (level == null)
            throw new IllegalArgumentException();
        
        if (getConfig("config.yml") != null && getConfig("config.yml").isLoaded())
        {
            if (getConfig("config.yml").getBoolean("logging.file.enabled")
                    && level.intValue() >= getConfig("config.yml").getInt("logging.file.level"))
            {
                if (logFileWriter == null)
                {
                    openLogFile(getConfig("config.yml").getString("logging.file.filename"));
                }
                
                try
                {
                    logFileWriter.write(logDateFormat.format(new Date()));
                    logFileWriter.write(" [");
                    logFileWriter.write(level.getName());
                    logFileWriter.write("] ");
                    logFileWriter.write(ChatColor.stripColor(message));
                    logFileWriter.write("\n");
                    logFileWriter.flush();
                }
                catch (IOException ex)
                {
                    plugin.getLogger().log(Level.WARNING, "Could not log to file.", ex);
                }
            }
            
            if (getConfig("config.yml").getBoolean("logging.verbose-console"))
            {
                System.out.println("[" + level + "] " + ChatColor.stripColor(message));
                
                return;
            }
        }
        
        plugin.getLogger().log(level, ChatColor.stripColor(message));
    }
    
    /**
     * Logs a message with a {@code Throwable} in the name of LogIt.
     * 
     * <p> The logger message will be saved in a log file if doing so is permitted
     * by the appropriate configuration setting.
     * 
     * @param level     the message level.
     * @param message   the message to be logged.
     * @param throwable the throwable whose stack trace should be appended to the log.
     * 
     * @see #log(Level, String)
     */
    public void log(Level level, String message, Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        
        try (PrintWriter pw = new PrintWriter(sw))
        {
            throwable.printStackTrace(pw);
        }
        
        log(level, message + " [Exception stack trace:\n" + sw.toString() + "]");
    }
    
    /**
     * Logs a {@code Throwable} in the name of LogIt.
     * 
     * <p> The logger message will be saved in a log file if doing so is permitted
     * by the appropriate configuration setting.
     * 
     * @param level     the logging level.
     * @param throwable the throwable to be logged.
     * 
     * @see #log(Level, String, Throwable)
     */
    public void log(Level level, Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        
        try (PrintWriter pw = new PrintWriter(sw))
        {
            throwable.printStackTrace(pw);
        }
        
        log(level, "Caught exception:\n" + sw.toString());
    }
    
    private void openLogFile(String filename)
    {
        File logFile = getDataFile(filename);
        
        if (logFile.length() > 300000)
        {
            int suffix = 0;
            File nextLogFile;
            
            do
            {
                suffix++;
                nextLogFile = getDataFile(filename + "." + suffix);
            }
            while (nextLogFile.exists());
            
            logFile.renameTo(nextLogFile);
        }
        
        try
        {
            logFileWriter = new FileWriter(logFile, true);
        }
        catch (IOException ex)
        {
            plugin.getLogger().log(Level.WARNING, "Could not open log file for writing.", ex);
        }
    }
    
    private void setSerializerEnabled(Class<? extends PersistenceSerializer> clazz, boolean status)
            throws FatalReportedException
    {
        if (status)
        {
            try
            {
                persistenceManager.registerSerializer(clazz);
            }
            catch (ReflectiveOperationException ex)
            {
                log(Level.SEVERE,
                        "Could not register persistence serializer: " + clazz.getSimpleName());
                
                FatalReportedException.throwNew(ex);
            }
        }
        else for (Player player : Bukkit.getOnlinePlayers())
        {
            Account account = getAccountManager().selectAccount(player.getName(), Arrays.asList(
                    getAccountManager().getKeys().username(),
                    getAccountManager().getKeys().persistence()
            ));
            
            if (account != null)
            {
                persistenceManager.unserializeUsing(account, player, clazz);
            }
        }
    }
    
    private void enableCommands()
    {
        enableCommand("login", new LoginCommand());
        enableCommand("logout", new LogoutCommand());
        enableCommand("remember", new RememberCommand(),
                getConfig("config.yml").getBoolean("login-sessions.enabled"));
        enableCommand("register", new RegisterCommand());
        enableCommand("unregister", new UnregisterCommand());
        enableCommand("changepass", new ChangePassCommand(),
                !getConfig("config.yml").getBoolean("password.disable-passwords"));
        enableCommand("changeemail", new ChangeEmailCommand());
        enableCommand("recoverpass", new RecoverPassCommand(),
                getConfig("config.yml").getBoolean("password-recovery.enabled"));
        enableCommand("profile", new ProfileCommand(),
                getConfig("config.yml").getBoolean("profiles.enabled"));
        enableCommand("acclock", new AcclockCommand());
        enableCommand("accunlock", new AccunlockCommand());
        enableCommand("loginhistory", new LoginHistoryCommand(),
                getConfig("config.yml").getBoolean("login-history.enabled"));
        enableCommand("$logit-nop-command", new NopCommandExecutor());
    }
    
    private void disableCommands()
    {
        disableCommand("login");
        disableCommand("logout");
        disableCommand("remember");
        disableCommand("register");
        disableCommand("unregister");
        disableCommand("changepass");
        disableCommand("changeemail");
        disableCommand("recoverpass");
        disableCommand("profile");
        disableCommand("acclock");
        disableCommand("accunlock");
        disableCommand("loginhistory");
        disableCommand("$logit-nop-command");
    }
    
    private void disableCommand(String command)
    {
        plugin.getCommand(command).setExecutor(new DisabledCommandExecutor());
    }
    
    private void enableCommand(String command, CommandExecutor executor, boolean enabled)
    {
        if (enabled)
        {
            plugin.getCommand(command).setExecutor(executor);
        }
        else
        {
            disableCommand(command);
        }
    }
    
    private void enableCommand(String command, CommandExecutor executor)
    {
        enableCommand(command, executor, true);
    }
    
    private void registerEvents()
    {
        Bukkit.getPluginManager().registerEvents(messageDispatcher, plugin);
        Bukkit.getPluginManager().registerEvents(cooldownManager, plugin);
        Bukkit.getPluginManager().registerEvents(new ServerEventListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new BlockEventListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new EntityEventListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryEventListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new SessionEventListener(), plugin);
    }
    
    /**
     * Returns the LogIt plugin object.
     * 
     * <p> Most of times, all the work will be done with the LogIt core,
     * but the {@code LogItPlugin} may come useful if you want to,
     * for example, reload the message files.
     * 
     * @return the LogIt plugin object.
     */
    public LogItPlugin getPlugin()
    {
        return plugin;
    }
    
    /**
     * Returns the LogIt data folder as a {@code File} object (<i>/plugins/LogIt/</i>).
     * 
     * @return the data folder.
     */
    public File getDataFolder()
    {
        return plugin.getDataFolder();
    }
    
    /**
     * Returns a file, as a {@code File} object,
     * relative to the LogIt data folder (<i>/plugins/LogIt/</i>).
     * 
     * @param path the relative path.
     * 
     * @return the data file.
     */
    public File getDataFile(String path)
    {
        return new File(getDataFolder(), path);
    }
    
    /**
     * Checks if this is the first time LogIt is running on this server.
     * 
     * @return {@code true} if LogIt is running for the first time;
     *         {@code false} otherwise.
     */
    public boolean isFirstRun()
    {
        return firstRun;
    }
    
    /**
     * Checks if the LogIt core has been successfully started and is running.
     * 
     * @return {@code true} if the LogIt core is started; {@code false} otherwise.
     */
    public boolean isStarted()
    {
        return started;
    }
    
    public ConfigurationManager getConfigurationManager()
    {
        return configurationManager;
    }
    
    public PredefinedConfiguration getConfig(String filename)
    {
        if (configurationManager == null)
            return null;
        
        return configurationManager.getConfiguration(filename);
    }
    
    /**
     * Returns the default hashing algorithm specified in the config file.
     * 
     * @return the default hashing algorithm.
     */
    public HashingAlgorithm getDefaultHashingAlgorithm()
    {
        return HashingAlgorithm.decode(
                getConfig("config.yml").getString("password.hashing-algorithm")
        );
    }
    
    public IntegrationType getIntegration()
    {
        return IntegrationType.decode(
                getConfig("config.yml").getString("integration")
        );
    }
    
    public LocaleManager getLocaleManager()
    {
        return localeManager;
    }
    
    public AccountManager getAccountManager()
    {
        return accountManager;
    }
    
    public PersistenceManager getPersistenceManager()
    {
        return persistenceManager;
    }
    
    public BackupManager getBackupManager()
    {
        return backupManager;
    }
    
    public SessionManager getSessionManager()
    {
        return sessionManager;
    }
    
    public LogItMessageDispatcher getMessageDispatcher()
    {
        return messageDispatcher;
    }
    
    public ProfileManager getProfileManager()
    {
        return profileManager;
    }
    
    public GlobalPasswordManager getGlobalPasswordManager()
    {
        return globalPasswordManager;
    }
    
    public CooldownManager getCooldownManager()
    {
        return cooldownManager;
    }
    
    /**
     * Checks if LogIt is linked to the Vault plugin
     * (e.i. Vault is enabled on this server and LogIt has successfully
     * obtained the Vault permission provider when it was starting up).
     * 
     * @return {@code true} if LogIt is linked to Vault; {@code false} otherwise.
     */
    public boolean isLinkedToVault()
    {
        return vaultPermissions != null;
    }
    
    /**
     * The preferred way to obtain the instance of {@code LogItCore}.
     * 
     * @return the instance of {@code LogItCore}.
     */
    public static LogItCore getInstance()
    {
        if (instance == null)
        {
            instance = new LogItCore(LogItPlugin.getInstance());
        }
        
        return instance;
    }
    
    private static volatile LogItCore instance = null;
    
    private final LogItPlugin plugin;
    private boolean firstRun;
    private boolean started = false;
    
    private ConfigurationManager    configurationManager;
    private LocaleManager           localeManager;
    private AccountManager          accountManager;
    private PersistenceManager      persistenceManager;
    private BackupManager           backupManager;
    private SessionManager          sessionManager;
    private LogItMessageDispatcher  messageDispatcher;
    private ProfileManager          profileManager;
    private GlobalPasswordManager   globalPasswordManager;
    private CooldownManager         cooldownManager;
    private AccountWatcher          accountWatcher;
    private Permission              vaultPermissions;
    
    private BukkitTask accountManagerTask;
    private BukkitTask backupManagerTask;
    private BukkitTask sessionManagerTask;
    private BukkitTask globalPasswordManagerTask;
    private BukkitTask accountWatcherTask;
    
    private FileWriter logFileWriter;
    private final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
