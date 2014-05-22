/*
 * ProfileEditWizard.java
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
package io.github.lucaseasedup.logit.command.wizard;

import static io.github.lucaseasedup.logit.LogItPlugin.getMessage;
import io.github.lucaseasedup.logit.profile.field.Field;
import io.github.lucaseasedup.logit.profile.field.FloatField;
import io.github.lucaseasedup.logit.profile.field.IntegerField;
import io.github.lucaseasedup.logit.profile.field.SetField;
import io.github.lucaseasedup.logit.profile.field.StringField;
import java.util.List;
import org.bukkit.command.CommandSender;

public final class ProfileEditWizard extends Wizard
{
    public ProfileEditWizard(CommandSender sender, String playerName)
    {
        super(sender, Step.VIEW);
        
        this.playerName = playerName;
        this.fields = getCore().getProfileManager().getDefinedFields();
    }
    
    @Override
    protected void onCreate()
    {
        viewProfile(null);
        
        if (!fields.isEmpty())
        {
            updateStep(Step.CHOOSE_ACTION);
        }
        else
        {
            sendMessage(getMessage("wizard.orangeHorizontalLine"));
            cancelWizard();
        }
    }
    
    @Override
    protected void onMessage(String message)
    {
        if (getCurrentStep() == Step.CHOOSE_ACTION)
        {
            if (message.equalsIgnoreCase("done"))
            {
                sendMessage(getMessage("wizard.orangeHorizontalLine"));
                cancelWizard();
            }
            else if (message.equalsIgnoreCase("edit"))
            {
                sendMessage(getMessage("profile.edit.enterFieldNumber"));
                updateStep(Step.EDIT_FIELD);
            }
            else if (message.equalsIgnoreCase("erase"))
            {
                sendMessage(getMessage("profile.edit.enterFieldNumber"));
                updateStep(Step.ERASE_FIELD);
            }
        }
        else if (getCurrentStep() == Step.EDIT_FIELD || getCurrentStep() == Step.ERASE_FIELD)
        {
            int number;
            
            try
            {
                number = Integer.parseInt(message);
            }
            catch (NumberFormatException ex)
            {
                sendMessage(getMessage("profile.edit.unknownField"));
                
                return;
            }
            
            if (number > fields.size() || number <= 0)
            {
                sendMessage(getMessage("profile.edit.unknownField"));
                
                return;
            }
            
            field = fields.get(number - 1);
            
            if (getCurrentStep() == Step.EDIT_FIELD)
            {
                sendFieldEditingPrompt();
                
                updateStep(Step.ENTER_FIELD_VALUE);
            }
            else if (getCurrentStep() == Step.ERASE_FIELD)
            {
                getCore().getProfileManager().removeProfileObject(playerName, field.getName());
                
                viewProfile(field.getName());
                updateStep(Step.CHOOSE_ACTION);
            }
        }
        else if (getCurrentStep() == Step.ENTER_FIELD_VALUE)
        {
            if (field instanceof StringField)
            {
                StringField stringField = (StringField) field;
                
                if (message.length() < stringField.getMinLength())
                {
                    sendMessage(getMessage("profile.edit.stringTooShort")
                            .replace("{0}", field.getName())
                            .replace("{1}", String.valueOf(stringField.getMinLength())));
                    
                    return;
                }
                
                if (message.length() > stringField.getMaxLength())
                {
                    sendMessage(getMessage("profile.edit.stringTooLong")
                            .replace("{0}", field.getName())
                            .replace("{1}", String.valueOf(stringField.getMaxLength())));
                    
                    return;
                }
                
                getCore().getProfileManager().setProfileString(playerName,
                        field.getName(), message);
            }
            else if (field instanceof IntegerField)
            {
                IntegerField integerField = (IntegerField) field;
                int value;
                
                try
                {
                    value = Integer.parseInt(message);
                }
                catch (NumberFormatException ex)
                {
                    sendMessage(getMessage("profile.edit.invalidValue")
                            .replace("{0}", message));
                    
                    return;
                }
                
                if (value < integerField.getMinValue())
                {
                    sendMessage(getMessage("profile.edit.numberTooSmall")
                            .replace("{0}", field.getName())
                            .replace("{1}", String.valueOf(integerField.getMinValue())));
                    
                    return;
                }
                
                if (value > integerField.getMaxValue())
                {
                    sendMessage(getMessage("profile.edit.numberTooBig")
                            .replace("{0}", field.getName())
                            .replace("{1}", String.valueOf(integerField.getMaxValue())));
                    
                    return;
                }
                
                getCore().getProfileManager().setProfileInteger(playerName, field.getName(), value);
            }
            else if (field instanceof FloatField)
            {
                FloatField floatField = (FloatField) field;
                double value;
                
                try
                {
                    value = Double.parseDouble(message);
                }
                catch (NumberFormatException ex)
                {
                    sendMessage(getMessage("profile.edit.invalidValue")
                            .replace("{0}", message));
                    
                    return;
                }
                
                if (value < floatField.getMinValue())
                {
                    sendMessage(getMessage("profile.edit.numberTooSmall")
                            .replace("{0}", field.getName())
                            .replace("{1}", String.valueOf(floatField.getMinValue())));
                    
                    return;
                }
                
                if (value > floatField.getMaxValue())
                {
                    sendMessage(getMessage("profile.edit.numberTooBig")
                            .replace("{0}", field.getName())
                            .replace("{1}", String.valueOf(floatField.getMaxValue())));
                    
                    return;
                }
                
                getCore().getProfileManager().setProfileFloat(playerName, field.getName(), value);
            }
            else if (field instanceof SetField)
            {
                SetField setField = (SetField) field;
                String trimmedMessage = message.trim();
                
                if (!setField.isAccepted(trimmedMessage))
                {
                    sendMessage(getMessage("profile.edit.invalidValue")
                            .replace("{0}", message));
                    
                    return;
                }
                
                getCore().getProfileManager().setProfileString(playerName,
                        field.getName(), trimmedMessage);
            }
            else
            {
                throw new RuntimeException("Unknown field type: "
                        + field.getClass().getSimpleName());
            }
            
            viewProfile(field.getName());
            updateStep(Step.CHOOSE_ACTION);
        }
    }
    
    private void viewProfile(String updatedFieldName)
    {
        sendMessage("");
        sendMessage(getMessage("profile.edit.header")
                .replace("{0}", playerName));
        sendMessage(getMessage("wizard.orangeHorizontalLine"));
        
        if (fields.isEmpty())
        {
            sendMessage(getMessage("profile.edit.noFields"));
        }
        else
        {
            int i = 1;
            
            for (Field field : fields)
            {
                Object value = getCore().getProfileManager()
                        .getProfileObject(playerName, field.getName());
                
                if (value == null)
                {
                    value = "";
                }
                
                if (field.getName().equals(updatedFieldName))
                {
                    sendMessage(getMessage("profile.edit.updatedField")
                            .replace("{0}", String.valueOf(i))
                            .replace("{1}", field.getName())
                            .replace("{2}", value.toString()));
                }
                else
                {
                    sendMessage(getMessage("profile.edit.field")
                            .replace("{0}", String.valueOf(i))
                            .replace("{1}", field.getName())
                            .replace("{2}", value.toString()));
                    
                }
                
                i++;
            }
            
            sendMessage("");
            sendMessage(getMessage("profile.edit.chooseAction"));
        }
    }
    
    private void sendFieldEditingPrompt()
    {
        if (field instanceof StringField)
        {
            sendMessage(getMessage("profile.edit.enterFieldValue.string")
                    .replace("{0}", field.getName()));
        }
        else if (field instanceof IntegerField)
        {
            sendMessage(getMessage("profile.edit.enterFieldValue.integer")
                    .replace("{0}", field.getName()));
        }
        else if (field instanceof FloatField)
        {
            sendMessage(getMessage("profile.edit.enterFieldValue.float")
                    .replace("{0}", field.getName()));
        }
        else if (field instanceof SetField)
        {
            SetField setField = (SetField) field;
            StringBuilder values = new StringBuilder();
            
            for (String value : setField.getAcceptedValues())
            {
                if (values.length() > 0)
                {
                    values.append(getMessage("profile.edit.enterFieldValue.set.separator"));
                }
                
                values.append(getMessage("profile.edit.enterFieldValue.set.value")
                        .replace("{0}", value));
            }
            
            sendMessage(getMessage("profile.edit.enterFieldValue.set")
                    .replace("{0}", field.getName())
                    .replace("{1}", values.toString()));
        }
        else
        {
            throw new RuntimeException("Unknown field type: "
                    + field.getClass().getSimpleName());
        }
    }
    
    public static enum Step
    {
        VIEW, CHOOSE_ACTION,
        
        EDIT_FIELD, ENTER_FIELD_VALUE,
        
        ERASE_FIELD,
    }
    
    private final String playerName;
    private final List<Field> fields;
    private Field field;
}
