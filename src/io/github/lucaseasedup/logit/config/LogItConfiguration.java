/*
 * LogItConfiguration.java
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
package io.github.lucaseasedup.logit.config;

import io.github.lucaseasedup.logit.IniFile;
import io.github.lucaseasedup.logit.LogItCore;
import io.github.lucaseasedup.logit.LogItPlugin;
import it.sauronsoftware.base64.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * @author LucasEasedUp
 */
public final class LogItConfiguration extends PropertyObserver
{
    public LogItConfiguration(LogItPlugin plugin)
    {
        super(plugin.getCore());
        
        this.plugin = plugin;
    }
    
    /**
     * Loads settings from file and fills missing ones with default values.
     */
    public void load() throws IOException
    {
        plugin.reloadConfig();
        plugin.getConfig().options().header(null);
        
        File configDefFile = new File(plugin.getDataFolder(), "config-def.b64");
        String configDefString = Base64.decode(IOUtils.toString(new FileInputStream(configDefFile)));
        IniFile configDef = new IniFile(configDefString);
        Set<String> properties = configDef.getSections();
        
        for (String uuid : properties)
        {
            String path = configDef.getString(uuid, "path");
            PropertyType type = null;
            boolean requiresRestart = configDef.getBoolean(uuid, "requires_restart", true);
            Object defaultValue = null;
            PropertyValidator validator = null;
            PropertyObserver observer = null;
            
            String typeString = configDef.getString(uuid, "type");
            
            try
            {
                type = PropertyType.valueOf(typeString);
            }
            catch (IllegalArgumentException ex)
            {
                plugin.getLogger().log(Level.WARNING, "Unknown property type: " + typeString);
                
                continue;
            }
            
            String defaultValueString = configDef.getString(uuid, "default_value");
            
            switch (type)
            {
            case OBJECT:
                defaultValue = null;
                break;
            case BOOLEAN:
                defaultValue = Boolean.valueOf(defaultValueString);
                break;
            case COLOR:
                if (defaultValueString.equalsIgnoreCase("aqua"))
                    defaultValue = Color.AQUA;
                else if (defaultValueString.equalsIgnoreCase("black"))
                    defaultValue = Color.BLACK;
                else if (defaultValueString.equalsIgnoreCase("blue"))
                    defaultValue = Color.BLUE;
                else if (defaultValueString.equalsIgnoreCase("fuchsia"))
                    defaultValue = Color.FUCHSIA;
                else if (defaultValueString.equalsIgnoreCase("gray"))
                    defaultValue = Color.GRAY;
                else if (defaultValueString.equalsIgnoreCase("green"))
                    defaultValue = Color.GREEN;
                else if (defaultValueString.equalsIgnoreCase("lime"))
                    defaultValue = Color.LIME;
                else if (defaultValueString.equalsIgnoreCase("maroon"))
                    defaultValue = Color.MAROON;
                else if (defaultValueString.equalsIgnoreCase("navy"))
                    defaultValue = Color.NAVY;
                else if (defaultValueString.equalsIgnoreCase("olive"))
                    defaultValue = Color.OLIVE;
                else if (defaultValueString.equalsIgnoreCase("orange"))
                    defaultValue = Color.ORANGE;
                else if (defaultValueString.equalsIgnoreCase("purple"))
                    defaultValue = Color.PURPLE;
                else if (defaultValueString.equalsIgnoreCase("red"))
                    defaultValue = Color.RED;
                else if (defaultValueString.equalsIgnoreCase("silver"))
                    defaultValue = Color.SILVER;
                else if (defaultValueString.equalsIgnoreCase("teal"))
                    defaultValue = Color.TEAL;
                else if (defaultValueString.equalsIgnoreCase("white"))
                    defaultValue = Color.WHITE;
                else if (defaultValueString.equalsIgnoreCase("yellow"))
                    defaultValue = Color.YELLOW;
                else
                {
                    String[] rgb = defaultValueString.split(" ");
                    
                    if (rgb.length == 3)
                    {
                        defaultValue = Color.fromRGB(Integer.valueOf(rgb[0]), Integer.valueOf(rgb[1]), Integer.valueOf(rgb[2]));
                    }
                    else
                    {
                        defaultValue = Color.BLACK;
                    }
                }
                break;
            case DOUBLE:
                defaultValue = Double.valueOf(defaultValueString);
                break;
            case INT:
                defaultValue = Integer.valueOf(defaultValueString);
                break;
            case ITEM_STACK:
                defaultValue = null;
            case LONG:
                defaultValue = Long.valueOf(defaultValueString);
                break;
            case STRING:
                defaultValue = defaultValueString;
                break;
            case VECTOR:
                String[] axes = defaultValueString.split(" ");
                
                if (axes.length == 3)
                {
                    defaultValue = new Vector(Double.valueOf(axes[0]), Double.valueOf(axes[1]), Double.valueOf(axes[2]));
                }
                else
                {
                    defaultValue = new Vector(0, 0, 0);
                }
                
                break;
            case LIST:
            case BOOLEAN_LIST:
            case BYTE_LIST:
            case CHARACTER_LIST:
            case DOUBLE_LIST:
            case FLOAT_LIST:
            case INTEGER_LIST:
            case LONG_LIST:
            case MAP_LIST:
            case SHORT_LIST:
            case STRING_LIST:
                defaultValue = new ArrayList<>(0);
                break;
            default:
                throw new RuntimeException("Unknown property type.");
            }
            
            String validatorClassName = configDef.getString(uuid, "validator");
            
            try
            {
                if (validatorClassName != null && !validatorClassName.isEmpty())
                {
                    Class<PropertyValidator> validatorClass =
                            (Class<PropertyValidator>) Class.forName(validatorClassName);
                    
                    validator = validatorClass.getConstructor(LogItCore.class).newInstance();
                }

            }
            catch (ReflectiveOperationException ex)
            {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid property validator: " + validatorClassName + ". Stack trace:");
                ex.printStackTrace();
                
                continue;
            }
            
            String observerClassName = configDef.getString(uuid, "observer");
            
            try
            {
                if (observerClassName != null && !observerClassName.isEmpty())
                {
                    Class<PropertyObserver> observerClass =
                            (Class<PropertyObserver>) Class.forName(observerClassName);
                    
                    observer = observerClass.getConstructor(LogItCore.class).newInstance();
                }
            }
            catch (ReflectiveOperationException e)
            catch (ReflectiveOperationException ex)
            {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid property observer: " + observerClassName + ". Stack trace:");
                ex.printStackTrace();
                
                continue;
            }
            
            addProperty(path, type, requiresRestart, defaultValue, validator, observer);
        }
        
        save();
    }
    
    public void save()
    {
        plugin.saveConfig();
    }
    
    public Map<String, Property> getProperties()
    {
        return properties;
    }
    
    public Property getProperty(String path)
    {
        return properties.get(path);
    }
    
    public boolean contains(String path)
    {
        return properties.containsKey(path);
    }
    
    public PropertyType getType(String path)
    {
        return properties.get(path).getType();
    }
    
    public String toString(String path)
    {
        return properties.get(path).toString();
    }
    
    public ConfigurationSection getConfigurationSection(String path)
    {
        return plugin.getConfig().getConfigurationSection(path);
    }
    
    public Object get(String path)
    {
        return properties.get(path);
    }
    
    public boolean getBoolean(String path)
    {
        return (Boolean) properties.get(path).getValue();
    }
    
    public Color getColor(String path)
    {
        return (Color) properties.get(path).getValue();
    }
    
    public double getDouble(String path)
    {
        return (Double) properties.get(path).getValue();
    }
    
    public int getInt(String path)
    {
        return (Integer) properties.get(path).getValue();
    }
    
    public ItemStack getItemStack(String path)
    {
        return (ItemStack) properties.get(path).getValue();
    }
    
    public long getLong(String path)
    {
        return (Long) properties.get(path).getValue();
    }
    
    public String getString(String path)
    {
        return (String) properties.get(path).getValue();
    }
    
    public Vector getVector(String path)
    {
        return (Vector) properties.get(path).getValue();
    }
    
    @SuppressWarnings("rawtypes")
    public List getList(String path)
    {
        return (List) properties.get(path).getValue();
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path)
    {
        return (List<String>) properties.get(path).getValue();
    }
    
    public void set(String path, Object value) throws InvalidPropertyValueException
    {
        properties.get(path).set(value);
    }
    
    @Override
    public void update(Property p)
    {
        plugin.getConfig().set(p.getPath(), p.getValue());
        plugin.saveConfig();
        
        plugin.getLogger().log(Level.INFO, LogItPlugin.getMessage("CONFIG_PROPERTY_SET_LOG", new String[]{
            "%path%", p.getPath(),
            "%value%", p.toString(),
        }));
    }
    
    protected void addProperty(String path,
                               PropertyType type,
                               boolean changeRequiresRestart,
                               Object defaultValue,
                               PropertyValidator validator,
                               PropertyObserver obs)
    {
        Property property =
                new Property(path, type, changeRequiresRestart, plugin.getConfig().get(path, defaultValue), validator);
        
        if (obs != null)
            property.addObserver(obs);
        
        property.addObserver(this);
        
        plugin.getConfig().set(property.getPath(), property.getValue());
        properties.put(property.getPath(), property);
    }
    
    protected void addProperty(String path,
                               PropertyType type,
                               boolean changeRequiresRestart,
                               Object defaultValue,
                               PropertyObserver obs)
    {
        addProperty(path, type, changeRequiresRestart, defaultValue, null, obs);
    }
    
    protected void addProperty(String path,
                               PropertyType type,
                               boolean changeRequiresRestart,
                               Object defaultValue,
                               PropertyValidator validator)
    {
        addProperty(path, type, changeRequiresRestart, defaultValue, validator, null);
    }
    
    protected void addProperty(String path,
                               PropertyType type,
                               boolean changeRequiresRestart,
                               Object defaultValue)
    {
        addProperty(path, type, changeRequiresRestart, defaultValue, null, null);
    }
    
    private final LogItPlugin plugin;
    private final Map<String, Property> properties = new LinkedHashMap<>();
}
