/*
 * This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
 * 
 * SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 * 
 * SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 * 
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk.scheduler;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import me.neatmonster.spacemodule.api.InvalidArgumentsException;
import me.neatmonster.spacemodule.api.UnhandledActionException;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.utilities.Utilities;

import org.json.simple.JSONValue;

public class Job extends TimerTask {
    public final Object[] actionArguments;
    public final String   actionName;
    public final String   timeArgument;
    private final Timer   timer = new Timer();
    public final String   timeType;

    @SuppressWarnings("deprecation")
    public Job(final String actionName, final Object[] actionArguments, final String timeType,
            final String timeArgument, final boolean loading) throws UnschedulableException, UnhandledActionException {
        if (!SpaceRTK.getInstance().actionsManager.contains(actionName)) {
            if (!loading) {
                final String result = Utilities.sendMethod("isSchedulable", "[\"" + actionName + "\"]");
                if (result == null || !result.equals("true"))
                    if (result == null || result.equals(""))
                        throw new UnhandledActionException();
                    else
                        throw new UnschedulableException("Action " + actionName + " isn't schedulable!");
            }
        } else if (!SpaceRTK.getInstance().actionsManager.isSchedulable(actionName))
            throw new UnschedulableException("Action " + actionName + " isn't schedulable!");
        this.actionName = actionName;
        this.actionArguments = actionArguments;
        this.timeType = timeType;
        this.timeArgument = timeArgument;
        if (timeType.equals("EVERYXHOURS"))
            timer.scheduleAtFixedRate(this, Integer.parseInt(timeArgument) * 3600000L,
                    Integer.parseInt(timeArgument) * 3600000L);
        else if (timeType.equals("EVERYXMINUTES"))
            timer.scheduleAtFixedRate(this, Integer.parseInt(timeArgument) * 60000L,
                    Integer.parseInt(timeArgument) * 60000L);
        else if (timeType.equals("ONCEPERDAYAT")) {
            Date nextOccurence = new Date();
            nextOccurence.setHours(Integer.parseInt(timeArgument.split(":")[0]));
            nextOccurence.setMinutes(Integer.parseInt(timeArgument.split(":")[1]));
            if (nextOccurence.before(new Date()))
                nextOccurence = new Date(nextOccurence.getTime() + 86400000L);
            timer.scheduleAtFixedRate(this, nextOccurence, 86400000L);
        } else if (timeType.equals("XMINUTESPASTEVERYHOUR")) {
            Date nextOccurence = new Date();
            nextOccurence.setMinutes(Integer.parseInt(timeArgument));
            if (nextOccurence.before(new Date()))
                nextOccurence = new Date(nextOccurence.getTime() + 3600000L);
            timer.scheduleAtFixedRate(this, nextOccurence, 3600000L);
        }
        Scheduler.saveJobs();
    }

    @Override
    public void run() {
        if (SpaceRTK.getInstance().actionsManager.contains(actionName))
            try {
                SpaceRTK.getInstance().actionsManager.execute(actionName, actionArguments);
            } catch (final InvalidArgumentsException e) {
                e.printStackTrace();
            } catch (final UnhandledActionException e) {
                e.printStackTrace();
            }
        else
            Utilities.sendMethod(actionName, JSONValue.toJSONString(Arrays.asList(actionArguments)).replace("[[", "[")
                    .replace("],[", ",").replace("]]", "]"));
    }
}
