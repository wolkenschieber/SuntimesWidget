/**
    Copyright (C) 2014 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/ 

package com.forrestguice.suntimeswidget.calculator.sunrisesunset_java;

import android.content.Context;
import android.util.Log;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.calculator.SuntimesCalculator;
import com.forrestguice.suntimeswidget.calculator.SuntimesCalculatorDescriptor;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * A thin wrapper around a SunriseSunsetCalculator instance (from sunrisesunsetlib-java) that
 * implements the interface used by the widget.
 */
public class SunriseSunsetSuntimesCalculator implements SuntimesCalculator
{
    public static final String NAME = "sunrisesunsetlib";
    public static final String REF = "com.forrestguice.suntimeswidget.calculator.sunrisesunset_java.SunriseSunsetSuntimesCalculator";
    public static final String LINK = "github.com/mikereedell/sunrisesunsetlib-java";
    SunriseSunsetCalculator calculator = null;
    TimeZone timezone;
    Location location;

    public SunriseSunsetSuntimesCalculator() { /* EMPTY */ }

    @Override
    public void init(WidgetSettings.Location locationSetting, String timezone)
    {
        init(locationSetting, TimeZone.getTimeZone(timezone));
    }

    @Override
    public void init(WidgetSettings.Location locationSetting, TimeZone timezone)
    {
        try
        {
            this.location = new Location(locationSetting.getLatitude(), locationSetting.getLongitude());

        } catch (NumberFormatException e) {
            Log.e("init", "location was invalid, falling back to default; " + e.toString());
            this.location = new Location(WidgetSettings.PREF_DEF_LOCATION_LATITUDE, WidgetSettings.PREF_DEF_LOCATION_LONGITUDE);
        }

        this.timezone = timezone;
        calculator = new SunriseSunsetCalculator(this.location, this.timezone);
    }

    @Override
    public String name()
    {
        return NAME;
    }

    @Override
    public Calendar getCivilSunriseCalendarForDate( Calendar date )
    {
        return calculator.getCivilSunriseCalendarForDate(date);
    }

    @Override
    public Calendar getNauticalSunriseCalendarForDate( Calendar date )
    {
         return calculator.getNauticalSunriseCalendarForDate(date);
    }

    @Override
    public Calendar getAstronomicalSunriseCalendarForDate( Calendar date )
    {
        return calculator.getAstronomicalSunriseCalendarForDate(date);
    }

    @Override
    public Calendar getOfficialSunriseCalendarForDate( Calendar date )
    {
        return calculator.getOfficialSunriseCalendarForDate(date);
    }

    @Override
    public Calendar getSolarNoonCalendarForDate(Calendar date)
    {
        Calendar sunriseCal = getOfficialSunriseCalendarForDate(date);
        Calendar sunsetCal = getOfficialSunsetCalendarForDate(date);
        if (sunriseCal != null && sunsetCal != null)
        {
            long sunriseTime = sunriseCal.getTimeInMillis();
            long sunsetTime = sunsetCal.getTimeInMillis();
            if (sunsetTime < sunriseTime)
                sunsetTime += (24 * 60 * 60 * 1000);  // bug workaround (sunset calendar set to wrong day; 24hrs off)

            long noonTime = sunriseTime + ((sunsetTime - sunriseTime) / 2L);
            Calendar noonCalendar = Calendar.getInstance(timezone);
            noonCalendar.setTimeInMillis(noonTime);
            return noonCalendar;

        } else {
            return null;
        }
    }

    @Override
    public Calendar getCivilSunsetCalendarForDate( Calendar date )
    {
        return calculator.getCivilSunsetCalendarForDate(date);
    }

    @Override
    public Calendar getNauticalSunsetCalendarForDate( Calendar date )
    {
        return calculator.getNauticalSunsetCalendarForDate(date);
    }

    @Override
    public Calendar getAstronomicalSunsetCalendarForDate( Calendar date )
    {
        return calculator.getAstronomicalSunsetCalendarForDate(date);
    }

    @Override
    public Calendar getVernalEquinoxForYear(Calendar date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.get(Calendar.YEAR), Calendar.MARCH, 20);   // TODO: implement, these lines are "debug stuff"
        return calendar;
    }

    @Override
    public Calendar getSummerSolsticeForYear(Calendar date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.get(Calendar.YEAR), Calendar.JUNE, 20);   // TODO: implement, these lines are "debug stuff"
        return calendar;
    }

    @Override
    public Calendar getAutumnalEquinoxForYear(Calendar date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.get(Calendar.YEAR), Calendar.SEPTEMBER, 22);   // TODO: implement, these lines are "debug stuff"
        return calendar;
    }

    @Override
    public Calendar getWinterSolsticeForYear(Calendar date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.get(Calendar.YEAR), Calendar.DECEMBER, 21);   // TODO: implement, these lines are "debug stuff"
        return calendar;
    }

    @Override
    public Calendar getOfficialSunsetCalendarForDate( Calendar date )
    {
        return calculator.getOfficialSunsetCalendarForDate(date);
    }

    public static SuntimesCalculatorDescriptor getDescriptor()
    {
        return new SuntimesCalculatorDescriptor(SunriseSunsetSuntimesCalculator.NAME, SunriseSunsetSuntimesCalculator.LINK, SunriseSunsetSuntimesCalculator.REF,
                R.string.calculator_displayString_sunrisesunsetlib);
    }

    @Override
    public Calendar[] getMorningBlueHourForDate(Calendar date)
    {
        return null;
    }

    @Override
    public Calendar[] getEveningBlueHourForDate(Calendar date)
    {
        return null;
    }

    @Override
    public Calendar[] getMorningGoldenHourForDate(Calendar date)
    {
        return null;
    }

    @Override
    public Calendar[] getEveningGoldenHourForDate(Calendar date)
    {
        return null;
    }

    @Override
    public boolean isDay(Calendar dateTime)
    {
        Calendar sunsetCal = getOfficialSunriseCalendarForDate(dateTime);
        if (sunsetCal == null)    // no sunset time, must be day
            return true;

        Calendar sunriseCal = getOfficialSunsetCalendarForDate(dateTime);
        if (sunriseCal == null)   // no sunrise time, must be night
            return false;

        Date time = dateTime.getTime();
        Date sunrise = sunriseCal.getTime();
        Date sunset = sunsetCal.getTime();
        return (time.after(sunrise) && time.before(sunset));
    }

}

