/**
    Copyright (C) 2019 Forrest Guice
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

package com.forrestguice.suntimeswidget.cards;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.InsetDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesCalculatorDescriptor;
import com.forrestguice.suntimeswidget.calculator.SuntimesData;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public class CardAdapter extends RecyclerView.Adapter<CardViewHolder>
{
    private static SuntimesUtils utils = new SuntimesUtils();

    private WeakReference<Context> contextRef;
    private boolean supportsGoldBlue = false;
    private boolean showSeconds = false;
    private boolean showWarnings = false;

    private boolean[] showFields = null;
    private boolean showActual = true;
    private boolean showCivil = true;
    private boolean showNautical = true;
    private boolean showAstro = true;
    private boolean showNoon = true;
    private boolean showGold = false;
    private boolean showBlue = false;

    private int color_textTimeDelta, color_enabled, color_disabled, color_pressed;

    public CardAdapter(Context context, SuntimesCalculatorDescriptor calculatorDesc) {
        initTheme(context);
        initOptions(context, calculatorDesc);
    }

    private void initTheme(Context context)
    {
        int[] attrs = new int[] { android.R.attr.textColorPrimary, R.attr.buttonPressColor, R.attr.text_disabledColor };
        TypedArray a = context.obtainStyledAttributes(attrs);
        color_textTimeDelta = ContextCompat.getColor(context, a.getResourceId(0, Color.WHITE));
        color_enabled = color_textTimeDelta;
        color_pressed = ContextCompat.getColor(context, a.getResourceId(1, R.color.btn_tint_pressed_dark));
        color_disabled = ContextCompat.getColor(context, a.getResourceId(2, R.color.text_disabled_dark));
        a.recycle();
    }

    public void initOptions(Context context, SuntimesCalculatorDescriptor calculatorDesc)
    {
        contextRef = new WeakReference<>(context);
        supportsGoldBlue = calculatorDesc.hasRequestedFeature(SuntimesCalculator.FEATURE_GOLDBLUE);
        showSeconds = WidgetSettings.loadShowSecondsPref(context, 0);
        showWarnings = AppSettings.loadShowWarningsPref(context);

        showFields = AppSettings.loadShowFieldsPref(context);
        showActual = showFields[AppSettings.FIELD_ACTUAL];
        showCivil = showFields[AppSettings.FIELD_CIVIL];
        showNautical = showFields[AppSettings.FIELD_NAUTICAL];
        showAstro = showFields[AppSettings.FIELD_ASTRO];
        showNoon = showFields[AppSettings.FIELD_NOON];
        showGold = showFields[AppSettings.FIELD_GOLD];
        showBlue = showFields[AppSettings.FIELD_BLUE];
    }

    public static final int MAX_POSITIONS = 365;                       // 365 slots [today - 182, today + 182]
    public static final int TODAY_POSITION = (MAX_POSITIONS / 2) + 1;  // middle position is today
    public static final int INITIAL_POSITION_WINDOW = 2;               // initial data window; today +- 2 days
    private HashMap<Integer, SuntimesRiseSetDataset> sunData = new HashMap<>(MAX_POSITIONS);
    private HashMap<Integer, SuntimesMoonData> moonData = new HashMap<>(MAX_POSITIONS);

    @Override
    public int getItemCount() {
        return MAX_POSITIONS;
    }

    public void initData(Context context)
    {
        sunData.clear();
        moonData.clear();
        WidgetSettings.DateInfo dateInfo = WidgetSettings.loadDatePref(context, 0);
        for (int i = TODAY_POSITION - INITIAL_POSITION_WINDOW; i < TODAY_POSITION + INITIAL_POSITION_WINDOW + 1; i++) {
            initData(context, dateInfo, i);
        }
        notifyDataSetChanged();
    }

    protected void initData(Context context, WidgetSettings.DateInfo dateInfo, int position)
    {
        Calendar date = Calendar.getInstance();
        date.set(dateInfo.getYear(), dateInfo.getMonth(), dateInfo.getDay());
        date.add(Calendar.DAY_OF_YEAR, position - TODAY_POSITION);

        SuntimesRiseSetDataset sun = new SuntimesRiseSetDataset(context);
        sun.setTodayIs(date);
        sun.calculateData();
        sunData.put(position, sun);

        SuntimesMoonData moon = new SuntimesMoonData(context, 0, "moon");
        moon.setTodayIs(date);
        moon.calculate();
        moonData.put(position, moon);
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater layout = LayoutInflater.from(parent.getContext());
        View view = layout.inflate(R.layout.info_time_card1, parent, false);
        CardViewHolder holder = new CardViewHolder(view);

        holder.row_actual.setVisible(showActual);
        holder.row_civil.setVisible(showCivil);
        holder.row_nautical.setVisible(showNautical);
        holder.row_astro.setVisible(showAstro);
        holder.row_solarnoon.setVisible(showNoon);

        showGold = showGold && supportsGoldBlue;
        showBlue = showBlue && supportsGoldBlue;
        holder.row_blue8.setVisible(showBlue);
        holder.row_blue4.setVisible(showBlue);
        holder.row_gold.setVisible(showGold);

        Context context = contextRef.get();
        if (themeOverride != null && context != null) {
            themeCardViews(context, themeOverride, holder);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position)
    {
        Context context = (contextRef != null ? contextRef.get() : null);
        if (context == null) {
            Log.w("CardAdapter", "onBindViewHolder: null context!");
            return;
        }
        if (holder == null) {
            Log.w("CardAdapter", "onBindViewHolder: null view holder!");
            return;
        }

        SuntimesRiseSetDataset sun = sunData.get(position);
        SuntimesMoonData moon = moonData.get(position);
        if (sun == null || moon == null)
        {
            initData(context, WidgetSettings.loadDatePref(context, 0), position);
            sun = sunData.get(position);
            moon = moonData.get(position);
        }

        themeCardViews(context, holder);

        holder.resetHighlight();
        if (timeFields != null && (position == TODAY_POSITION || position == TODAY_POSITION + 1)) {
            initTimeFields(timeFields, holder, position != TODAY_POSITION);
        }

        // sun fields
        if (sun != null && sun.isCalculated())
        {
            if (showActual) {
                SuntimesUtils.TimeDisplayText sunriseString_actualTime = utils.calendarTimeShortDisplayString(context, sun.dataActual.sunriseCalendarToday(), showSeconds);
                SuntimesUtils.TimeDisplayText sunsetString_actualTime = utils.calendarTimeShortDisplayString(context, sun.dataActual.sunsetCalendarToday(), showSeconds);
                holder.row_actual.updateFields(sunriseString_actualTime.toString(), sunsetString_actualTime.toString());
            }

            if (showCivil) {
                SuntimesUtils.TimeDisplayText sunriseString_civilTime = utils.calendarTimeShortDisplayString(context, sun.dataCivil.sunriseCalendarToday(), showSeconds);
                SuntimesUtils.TimeDisplayText sunsetString_civilTime = utils.calendarTimeShortDisplayString(context, sun.dataCivil.sunsetCalendarToday(), showSeconds);
                holder.row_civil.updateFields(sunriseString_civilTime.toString(), sunsetString_civilTime.toString());
            }

            if (showNautical) {
                SuntimesUtils.TimeDisplayText sunriseString_nauticalTime = utils.calendarTimeShortDisplayString(context, sun.dataNautical.sunriseCalendarToday(), showSeconds);
                SuntimesUtils.TimeDisplayText sunsetString_nauticalTime = utils.calendarTimeShortDisplayString(context, sun.dataNautical.sunsetCalendarToday(), showSeconds);
                holder.row_nautical.updateFields(sunriseString_nauticalTime.toString(), sunsetString_nauticalTime.toString());
            }

            if (showAstro) {
                SuntimesUtils.TimeDisplayText sunriseString_astroTime = utils.calendarTimeShortDisplayString(context, sun.dataAstro.sunriseCalendarToday(), showSeconds);
                SuntimesUtils.TimeDisplayText sunsetString_astroTime = utils.calendarTimeShortDisplayString(context, sun.dataAstro.sunsetCalendarToday(), showSeconds);
                holder.row_astro.updateFields(sunriseString_astroTime.toString(), sunsetString_astroTime.toString());
            }

            if (showNoon) {
                SuntimesUtils.TimeDisplayText noonString = utils.calendarTimeShortDisplayString(context, sun.dataNoon.sunriseCalendarToday(), showSeconds);
                holder.row_solarnoon.updateFields(noonString.toString());
            }

            if (showBlue) {
                String sunriseString_blue8 = utils.calendarTimeShortDisplayString(context, sun.dataBlue8.sunriseCalendarToday(), showSeconds).toString();
                String sunsetString_blue8 = utils.calendarTimeShortDisplayString(context, sun.dataBlue8.sunsetCalendarToday(), showSeconds).toString();
                holder.row_blue8.updateFields(sunriseString_blue8, sunsetString_blue8);

                String sunriseString_blue4 = utils.calendarTimeShortDisplayString(context, sun.dataBlue4.sunriseCalendarToday(), showSeconds).toString();
                String sunsetString_blue4 = utils.calendarTimeShortDisplayString(context, sun.dataBlue4.sunsetCalendarToday(), showSeconds).toString();
                holder.row_blue4.updateFields(sunriseString_blue4, sunsetString_blue4);
            }

            if (showGold) {
                String sunriseString_gold = utils.calendarTimeShortDisplayString(context, sun.dataGold.sunriseCalendarToday(), showSeconds).toString();
                String sunsetString_gold = utils.calendarTimeShortDisplayString(context, sun.dataGold.sunsetCalendarToday(), showSeconds).toString();
                holder.row_gold.updateFields(sunriseString_gold, sunsetString_gold);
            }

            updateDayLengthViews(context, holder.txt_daylength, sun.dataActual.dayLengthToday(), R.string.length_day);
            updateDayLengthViews(context, holder.txt_lightlength, sun.dataCivil.dayLengthToday(), R.string.length_light);

            // date field
            Calendar now = sun.now();
            Date data_date = sun.dataActual.date();
            DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context.getApplicationContext());   // Apr 11, 2016
            dateFormat.setTimeZone(sun.timezone());

            boolean showDateWarning = false;
            String thisString = context.getString(R.string.today);
            if (sun.dataActual.todayIsNotToday())
            {
                WidgetSettings.DateInfo nowInfo = new WidgetSettings.DateInfo(now);
                WidgetSettings.DateInfo dataInfo = new WidgetSettings.DateInfo(sun.dataActual.calendar());
                if (!nowInfo.equals(dataInfo))
                {
                    WidgetSettings.DateInfo diff = dataInfo.diff(nowInfo);
                    if (diff.getYear() == 0 && diff.getMonth() == 0 && (diff.getDay() == 1 || diff.getDay() == -1))
                    {
                        if (diff.getDay() == 1) {
                            thisString = context.getString(R.string.tomorrow);
                            showDateWarning = false;

                        } else {
                            thisString = context.getString(R.string.yesterday);
                            showDateWarning = false;
                        }

                    } else {
                        int diffDays = (int)((data_date.getTime() - now.getTimeInMillis()) / 1000L / 60L / 60L / 24L);
                        if (data_date.after(now.getTime())) {
                            thisString = context.getString(R.string.future_n, Integer.toString(diffDays + 1));
                            showDateWarning = true;

                        } else if (data_date.before(now.getTime())) {
                            thisString = context.getString(R.string.past_n, Integer.toString(Math.abs(diffDays)));
                            showDateWarning = true;
                        }
                    }
                }
            }

            ImageSpan dateWarningIcon = (showWarnings && showDateWarning) ? SuntimesUtils.createWarningSpan(context, holder.txt_date.getTextSize()) : null;
            String dateString = context.getString(R.string.dateField, thisString, dateFormat.format(data_date));
            SpannableStringBuilder dateSpan = SuntimesUtils.createSpan(context, dateString, SuntimesUtils.SPANTAG_WARNING, dateWarningIcon);
            holder.txt_date.setText(dateSpan);
            holder.txt_date.setContentDescription(dateString.replaceAll(Pattern.quote(SuntimesUtils.SPANTAG_WARNING), ""));

        } else {
            String notCalculated = context.getString(R.string.time_loading);
            holder.row_solarnoon.updateFields(notCalculated);
            holder.row_actual.updateFields(notCalculated, notCalculated);
            holder.row_civil.updateFields(notCalculated, notCalculated);
            holder.row_nautical.updateFields(notCalculated, notCalculated);
            holder.row_astro.updateFields(notCalculated, notCalculated);
            holder.row_gold.updateFields(notCalculated, notCalculated);
            holder.row_blue8.updateFields(notCalculated, notCalculated);
            holder.row_blue4.updateFields(notCalculated, notCalculated);
            holder.txt_daylength.setText("");
            holder.txt_lightlength.setText("");
            holder.txt_date.setText("\n\n");
        }

        // moon fields
        holder.sunsetHeader.measure(0, 0);      // adjust moonrise/moonset columns to match width of sunrise/sunset columns
        int sunsetHeaderWidth = holder.sunsetHeader.getMeasuredWidth();
        holder.moonrise.adjustColumnWidth(context, sunsetHeaderWidth);
        holder.moonphase.updateViews(context, moon);
        holder.moonrise.updateViews(context, moon);

        attachClickListeners(holder, position);
    }

    private void updateDayLengthViews(Context context, TextView textView, long dayLength, int labelID)
    {
        SuntimesUtils.TimeDisplayText dayLengthDisplay;
        if (dayLength <= 0)
            dayLengthDisplay = new SuntimesUtils.TimeDisplayText(String.format(SuntimesUtils.strTimeDeltaFormat, 0, (showSeconds ? SuntimesUtils.strSeconds : SuntimesUtils.strMinutes)), SuntimesUtils.strEmpty, SuntimesUtils.strEmpty);
        else if (dayLength >= SuntimesData.DAY_MILLIS)
            dayLengthDisplay = new SuntimesUtils.TimeDisplayText(String.format(SuntimesUtils.strTimeDeltaFormat, 24, SuntimesUtils.strHours), SuntimesUtils.strEmpty, SuntimesUtils.strEmpty);
        else dayLengthDisplay = utils.timeDeltaLongDisplayString(0, dayLength, showSeconds);

        dayLengthDisplay.setSuffix("");
        String dayLengthStr = dayLengthDisplay.toString();
        String dayLength_label = context.getString(labelID, dayLengthStr);
        textView.setText(SuntimesUtils.createBoldColorSpan(null, dayLength_label, dayLengthStr, color_textTimeDelta));
    }

    protected void themeCardViews(Context context, CardViewHolder holder)
    {
        if (themeOverride != null) {
            themeCardViews(context, themeOverride, holder);
        }
        ImageViewCompat.setImageTintList(holder.btn_flipperNext, SuntimesUtils.colorStateList(color_enabled, this.color_disabled, color_pressed));
        ImageViewCompat.setImageTintList(holder.btn_flipperPrev, SuntimesUtils.colorStateList(color_enabled, this.color_disabled, color_pressed));
    }

    protected void themeCardViews(Context context, @NonNull SuntimesTheme theme, CardViewHolder holder)
    {
        color_textTimeDelta = theme.getTimeColor();
        color_pressed = theme.getActionColor();
        int color_text = theme.getTextColor();
        int color_sunrise = theme.getSunriseTextColor();
        int color_sunset = theme.getSunsetTextColor();
        int color_action = theme.getActionColor();

        holder.txt_daylength.setTextColor(color_text);
        holder.txt_lightlength.setTextColor(color_text);

        holder.row_actual.getField(0).setTextColor(color_sunrise);
        holder.row_civil.getField(0).setTextColor(color_sunrise);
        holder.row_nautical.getField(0).setTextColor(color_sunrise);
        holder.row_astro.getField(0).setTextColor(color_sunrise);
        holder.row_gold.getField(1).setTextColor(color_sunrise);
        holder.row_blue8.getField(0).setTextColor(color_sunrise);
        holder.row_blue4.getField(0).setTextColor(color_sunset);

        holder.row_actual.getField(1).setTextColor(color_sunset);
        holder.row_civil.getField(1).setTextColor(color_sunset);
        holder.row_nautical.getField(1).setTextColor(color_sunset);
        holder.row_astro.getField(1).setTextColor(color_sunset);
        holder.row_solarnoon.getField(0).setTextColor(color_sunset);
        holder.row_gold.getField(0).setTextColor(color_sunset);
        holder.row_blue8.getField(1).setTextColor(color_sunset);
        holder.row_blue4.getField(1).setTextColor(color_sunrise);

        int labelColor = theme.getTitleColor();
        for (CardViewHolder.TimeFieldRow row : holder.rows) {
            row.label.setTextColor(labelColor);
        }

        holder.txt_date.setTextColor(SuntimesUtils.colorStateList(labelColor, color_disabled, color_action));

        int sunriseIconColor = theme.getSunriseIconColor();
        int sunriseIconColor2 = theme.getSunriseIconStrokeColor();
        int sunriseIconStrokeWidth = theme.getSunriseIconStrokePixels(context);
        SuntimesUtils.tintDrawable((InsetDrawable)holder.icon_sunrise.getBackground(), sunriseIconColor, sunriseIconColor2, sunriseIconStrokeWidth);
        holder.header_sunrise.setTextColor(color_sunrise);

        int sunsetIconColor = theme.getSunsetIconColor();
        int sunsetIconColor2 = theme.getSunsetIconStrokeColor();
        int sunsetIconStrokeWidth = theme.getSunsetIconStrokePixels(context);
        SuntimesUtils.tintDrawable((InsetDrawable)holder.icon_sunset.getBackground(), sunsetIconColor, sunsetIconColor2, sunsetIconStrokeWidth);
        holder.header_sunset.setTextColor(color_sunset);

        holder.moonrise.themeViews(context, theme);
        holder.moonphase.themeViews(context, theme);
        holder.moonlabel.setTextColor(labelColor);
    }

    public void initTimeFields(HashMap<SolarEvents.SolarEventField, TextView> timeFields, CardViewHolder holder, boolean tomorrow)
    {
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.SUNRISE, tomorrow), holder.row_actual.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.SUNSET, tomorrow), holder.row_actual.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_CIVIL, tomorrow), holder.row_civil.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_CIVIL, tomorrow), holder.row_civil.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_NAUTICAL, tomorrow), holder.row_nautical.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_NAUTICAL, tomorrow), holder.row_nautical.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_ASTRONOMICAL, tomorrow), holder.row_astro.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_ASTRONOMICAL, tomorrow), holder.row_astro.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.NOON, tomorrow), holder.row_solarnoon.getField(0));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_GOLDEN, tomorrow), holder.row_gold.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_GOLDEN, tomorrow), holder.row_gold.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_BLUE8, tomorrow), holder.row_blue8.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_BLUE8, tomorrow), holder.row_blue8.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_BLUE4, tomorrow), holder.row_blue4.getField(0));
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_BLUE4, tomorrow), holder.row_blue4.getField(1));

        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MOONRISE, tomorrow), holder.moonrise.getTimeViews(SolarEvents.MOONRISE)[0]);
        timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MOONSET, tomorrow), holder.moonrise.getTimeViews(SolarEvents.MOONSET)[0]);
    }

    public static class CardViewDecorator extends RecyclerView.ItemDecoration
    {
        private int marginPx = 8;

        public CardViewDecorator( Context context ) {
            marginPx = (int)context.getResources().getDimension(R.dimen.activity_margin);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
        {
            outRect.left = outRect.right = marginPx;
            outRect.top = outRect.bottom = 0;
        }
    }

    private SuntimesTheme themeOverride = null;
    public void setThemeOverride(@NonNull SuntimesTheme theme) {
        themeOverride = theme;
    }

    private HashMap<SolarEvents.SolarEventField, TextView> timeFields;
    public void setTimeFields(HashMap<SolarEvents.SolarEventField, TextView> fields) {
        this.timeFields = fields;
    }

    private CardAdapterListener adapterListener = new CardAdapterListener();
    public void setCardAdapterListener( @NonNull CardAdapterListener listener ) {
        adapterListener = listener;
    }

    private void attachClickListeners(@NonNull CardViewHolder holder, int position)
    {
        holder.txt_date.setOnClickListener(onDateClick(position));
        holder.txt_date.setOnLongClickListener(onDateLongClick(position));

        holder.sunriseHeader.setOnClickListener(onSunriseHeaderClick(position));
        holder.sunriseHeader.setOnLongClickListener(onSunriseHeaderLongClick(position));

        holder.sunsetHeader.setOnClickListener(onSunsetHeaderClick(position));
        holder.sunsetHeader.setOnLongClickListener(onSunsetHeaderLongClick(position));

        holder.moonClickArea.setOnClickListener(onMoonHeaderClick(position));
        holder.moonClickArea.setOnLongClickListener(onMoonHeaderLongClick(position));

        holder.btn_flipperNext.setOnClickListener(onNextClick(position));
        holder.btn_flipperPrev.setOnClickListener(onPrevClick(position));
    }

    private View.OnClickListener onDateClick(final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onDateClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onDateLongClick(final int position) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onDateLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onSunriseHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onSunriseHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onSunriseHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onSunriseHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onSunsetHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onSunsetHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onSunsetHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onSunsetHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onMoonHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onMoonHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onMoonHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onMoonHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onNextClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onNextClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onPrevClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onPrevClick(CardAdapter.this, position);
            }
        };
    }

    /**
     * CardAdapterListener
     */
    public static class CardAdapterListener
    {
        public void onDateClick(CardAdapter adapter, int position) {}
        public boolean onDateLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onSunriseHeaderClick(CardAdapter adapter, int position) {}
        public boolean onSunriseHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onSunsetHeaderClick(CardAdapter adapter, int position) {}
        public boolean onSunsetHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onMoonHeaderClick(CardAdapter adapter, int position) {}
        public boolean onMoonHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onNextClick(CardAdapter adapter, int position) {}
        public void onPrevClick(CardAdapter adapter, int position) {}
    }
}


