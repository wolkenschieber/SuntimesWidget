/**
    Copyright (C) 2017-2021 Forrest Guice
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

package com.forrestguice.suntimeswidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class LightMapDialog extends BottomSheetDialogFragment
{
    private static SuntimesUtils utils = new SuntimesUtils();

    private TextView dialogTitle;
    private View sunLayout;

    private TextView sunAzimuth, sunAzimuthRising, sunAzimuthSetting, sunAzimuthAtNoon, sunAzimuthLabel;
    private TextView sunElevation, sunElevationAtNoon, sunElevationLabel;
    private ImageView riseIcon, setIcon;
    private TextView sunShadowObj, sunShadowLength, sunShadowLengthAtNoon;

    private LightMapView lightmap;
    private LightMapKey field_night, field_astro, field_nautical, field_civil, field_day;
    private int colorNight, colorAstro, colorNautical, colorCivil, colorDay;
    private int colorRising, colorSetting;
    private int colorLabel;
    private boolean showSeconds = true;
    private int decimalPlaces = 1;
    private View dialogContent = null;

    private SuntimesRiseSetDataset data;
    public void setData( SuntimesRiseSetDataset data )
    {
        this.data = data;
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(onShowDialogListener);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedState)
    {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(getActivity(), AppSettings.loadTheme(getContext()));    // hack: contextWrapper required because base theme is not properly applied
        View dialogContent = inflater.cloneInContext(contextWrapper).inflate(R.layout.layout_dialog_lightmap, parent, false);

        SuntimesUtils.initDisplayStrings(getActivity());
        initViews(dialogContent);
        if (savedState != null) {
            Log.d("DEBUG", "LightMapDialog onCreate (restoreState)");
        }
        themeViews(getContext());
        return dialogContent;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        expandSheet(getDialog());
    }

    private void expandSheet(DialogInterface dialog)
    {
        if (dialog != null) {
            BottomSheetDialog bottomSheet = (BottomSheetDialog) dialog;
            FrameLayout layout = (FrameLayout) bottomSheet.findViewById(android.support.design.R.id.design_bottom_sheet);  // for AndroidX, resource is renamed to com.google.android.material.R.id.design_bottom_sheet
            if (layout != null) {
                BottomSheetBehavior behavior = BottomSheetBehavior.from(layout);
                behavior.setHideable(false);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    private void initPeekHeight(DialogInterface dialog)
    {
        if (dialog != null) {
            BottomSheetDialog bottomSheet = (BottomSheetDialog) dialog;
            FrameLayout layout = (FrameLayout) bottomSheet.findViewById(android.support.design.R.id.design_bottom_sheet);  // for AndroidX, resource is renamed to com.google.android.material.R.id.design_bottom_sheet
            if (layout != null)
            {
                BottomSheetBehavior behavior = BottomSheetBehavior.from(layout);
                ViewGroup dialogLayout = (ViewGroup) bottomSheet.findViewById(R.id.dialog_lightmap_layout);
                View divider1 = bottomSheet.findViewById(R.id.info_time_lightmap);
                if (dialogLayout != null && divider1 != null)
                {
                    Rect headerBounds = new Rect();
                    divider1.getDrawingRect(headerBounds);
                    dialogLayout.offsetDescendantRectToMyCoords(divider1, headerBounds);
                    behavior.setPeekHeight(headerBounds.bottom + (int)getResources().getDimension(R.dimen.dialog_margin));

                } else {
                    behavior.setPeekHeight(-1);
                }
            }
        }
    }

    private DialogInterface.OnShowListener onShowDialogListener = new DialogInterface.OnShowListener()
    {
        @Override
        public void onShow(DialogInterface dialog)
        {
            startUpdateTask();
            dialogTitle.post(new Runnable() {
                @Override
                public void run() {
                    initPeekHeight(getDialog());
                }
            });
        }
    };

    private void startUpdateTask()
    {
        stopUpdateTask();
        if (sunElevation != null)
            sunElevation.post(updateTask);
    }
    private void stopUpdateTask()
    {
        if (sunElevation != null)
            sunElevation.removeCallbacks(updateTask);
    }

    @Override
    public void onStop()
    {
        stopUpdateTask();
        super.onStop();
    }

    public static final int UPDATE_RATE = 3000;
    private Runnable updateTask = new Runnable()
    {
        @Override
        public void run()
        {
            if (data != null)
            {
                updateLightmapViews(data);
                updateSunPositionViews(data);
            }
            if (sunElevation != null)
                sunElevation.postDelayed(this, UPDATE_RATE);
        }
    };

    public void initViews(View dialogView)
    {
        dialogTitle = (TextView)dialogView.findViewById(R.id.sundialog_title);
        lightmap = (LightMapView)dialogView.findViewById(R.id.info_time_lightmap);

        sunLayout = dialogView.findViewById(R.id.info_sun_layout);
        sunElevation = (TextView)dialogView.findViewById(R.id.info_sun_elevation_current);
        sunElevationAtNoon = (TextView)dialogView.findViewById(R.id.info_sun_elevation_atnoon);
        sunElevationLabel = (TextView)dialogView.findViewById(R.id.info_sun_elevation_current_label);

        sunAzimuth = (TextView)dialogView.findViewById(R.id.info_sun_azimuth_current);
        sunAzimuthRising = (TextView)dialogView.findViewById(R.id.info_sun_azimuth_rising);
        sunAzimuthAtNoon = (TextView)dialogView.findViewById(R.id.info_sun_azimuth_atnoon);
        sunAzimuthSetting = (TextView)dialogView.findViewById(R.id.info_sun_azimuth_setting);
        sunAzimuthLabel = (TextView)dialogView.findViewById(R.id.info_sun_azimuth_current_label);

        View shadowLayout = dialogView.findViewById(R.id.info_shadow_layout);
        if (shadowLayout != null) {
            shadowLayout.setOnClickListener(onShadowLayoutClick);
        }

        sunShadowObj = (TextView)dialogView.findViewById(R.id.info_shadow_height);
        sunShadowLength = (TextView)dialogView.findViewById(R.id.info_shadow_length);
        sunShadowLengthAtNoon = (TextView)dialogView.findViewById(R.id.info_shadow_length_atnoon);

        field_night = new LightMapKey(dialogView, R.id.info_time_lightmap_key_night_icon, R.id.info_time_lightmap_key_night_label, R.id.info_time_lightmap_key_night_duration);
        field_astro = new LightMapKey(dialogView, R.id.info_time_lightmap_key_astro_icon, R.id.info_time_lightmap_key_astro_label, R.id.info_time_lightmap_key_astro_duration);
        field_nautical = new LightMapKey(dialogView, R.id.info_time_lightmap_key_nautical_icon, R.id.info_time_lightmap_key_nautical_label, R.id.info_time_lightmap_key_nautical_duration);
        field_civil = new LightMapKey(dialogView, R.id.info_time_lightmap_key_civil_icon, R.id.info_time_lightmap_key_civil_label, R.id.info_time_lightmap_key_civil_duration);
        field_day = new LightMapKey(dialogView, R.id.info_time_lightmap_key_day_icon, R.id.info_time_lightmap_key_day_label, R.id.info_time_lightmap_key_day_duration);

        riseIcon = (ImageView)dialogView.findViewById(R.id.sundialog_riseicon);
        setIcon = (ImageView)dialogView.findViewById(R.id.sundialog_seticon);
    }

    private View.OnClickListener onShadowLayoutClick =  new View.OnClickListener()
    {
        @Override
        public void onClick(@NonNull View v)
        {
            Context context = getContext();
            if (context != null) {
                showShadowObjHeightPopup(context, v);
            }
        }
    };

    protected void showShadowObjHeightPopup(@NonNull final Context context, @NonNull View v)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        if (inflater != null)
        {
            PopupWindow popupWindow = new PopupWindow(createShadowObjHeightPopupView(context), LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)));
            popupWindow.setOutsideTouchable(true);
            popupWindow.showAsDropDown(v);
        }
    }
    protected View createShadowObjHeightPopupView(@NonNull final Context context)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        if (inflater != null)
        {
            View popupView = inflater.inflate(R.layout.layout_dialog_objheight, null);
            if (popupView != null)
            {
                SeekBar seekbar = (SeekBar) popupView.findViewById(R.id.seek_objheight);
                if (seekbar != null)
                {
                    int centimeters = (int) (WidgetSettings.loadObserverHeightPref(context, 0) * 100) + 1;
                    centimeters = (centimeters < 1 ? 1 : Math.min(centimeters, SEEK_CENTIMETERS_MAX));
                    seekbar.setMax(SEEK_CENTIMETERS_MAX);
                    if (Build.VERSION.SDK_INT >= 24) {
                        seekbar.setProgress(centimeters, false);
                    } else {
                        seekbar.setProgress(centimeters);
                    }
                    seekbar.setOnSeekBarChangeListener(onObjectHeightSeek);
                }
                ImageButton moreButton = (ImageButton) popupView.findViewById(R.id.btn_more);
                if (moreButton != null) {
                    moreButton.setOnClickListener(onObjectHeightMoreLess(true));
                }
                ImageButton lessButton = (ImageButton) popupView.findViewById(R.id.btn_less);
                if (lessButton != null) {
                    lessButton.setOnClickListener(onObjectHeightMoreLess(false));
                }
            }
            return popupView;
        }
        return null;
    }
    private View.OnClickListener onObjectHeightMoreLess( final boolean more ) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context != null) {
                    float currentHeight = WidgetSettings.loadObserverHeightPref(context, 0);
                    WidgetSettings.saveObserverHeightPref(getContext(), 0, currentHeight + ((more ? 1 : -1) * (SEEK_CENTIMETERS_INC / 100f)));
                    updateViews();
                }
            }
        };
    }
    private SeekBar.OnSeekBarChangeListener onObjectHeightSeek = new SeekBar.OnSeekBarChangeListener()
    {
        @Override
        public void onProgressChanged(SeekBar seekBar, int centimeters, boolean fromUser)
        {
            Context context = getContext();
            if (fromUser && context != null)
            {
                if (centimeters < 1) {
                    centimeters = 1;
                }
                WidgetSettings.saveObserverHeightPref(getContext(), 0, (centimeters / 100f));
                updateViews();
            }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };
    private static final int SEEK_CENTIMETERS_MAX = 5 * 100;
    private static final int SEEK_CENTIMETERS_INC = 1;

    @SuppressWarnings("ResourceType")
    public void themeViews(Context context)
    {
        if (themeOverride == null)
        {
            int[] colorAttrs = { R.attr.graphColor_night,   // 0
                    R.attr.graphColor_astronomical,         // 1
                    R.attr.graphColor_nautical,             // 2
                    R.attr.graphColor_civil,                // 3
                    R.attr.graphColor_day,                  // 4
                    R.attr.sunriseColor,                    // 5
                    R.attr.sunsetColor                      // 6
            };
            TypedArray typedArray = context.obtainStyledAttributes(colorAttrs);
            int def = R.color.transparent;
            colorNight = ContextCompat.getColor(context, typedArray.getResourceId(0, def));
            colorAstro = ContextCompat.getColor(context, typedArray.getResourceId(1, def));
            colorNautical = ContextCompat.getColor(context, typedArray.getResourceId(2, def));
            colorCivil = ContextCompat.getColor(context, typedArray.getResourceId(3, def));
            colorDay = ContextCompat.getColor(context, typedArray.getResourceId(4, def));
            colorRising = ContextCompat.getColor(context, typedArray.getResourceId(5, def));
            colorSetting = ContextCompat.getColor(context, typedArray.getResourceId(6, def));
            typedArray.recycle();

        } else {
            int titleColor = themeOverride.getTitleColor();
            float textSizeSp = themeOverride.getTextSizeSp();
            float titleSizeSp = themeOverride.getTitleSizeSp();
            float timeSizeSp = themeOverride.getTimeSizeSp();
            float suffixSizeSp = themeOverride.getTimeSuffixSizeSp();

            dialogTitle.setTextColor(titleColor);
            dialogTitle.setTextSize(titleSizeSp);
            dialogTitle.setTypeface(dialogTitle.getTypeface(), (themeOverride.getTitleBold() ? Typeface.BOLD : Typeface.NORMAL));

            sunElevationLabel.setTextColor(titleColor);
            sunElevationLabel.setTextSize(suffixSizeSp);

            sunAzimuthLabel.setTextColor(titleColor);
            sunAzimuthLabel.setTextSize(suffixSizeSp);

            lightmap.themeViews(context, themeOverride);
            colorNight = themeOverride.getNightColor();
            colorDay = themeOverride.getDayColor();
            colorAstro = themeOverride.getAstroColor();
            colorNautical = themeOverride.getNauticalColor();
            colorCivil = themeOverride.getCivilColor();
            colorRising = themeOverride.getSunriseTextColor();
            colorSetting = themeOverride.getSunsetTextColor();

            field_astro.themeViews(themeOverride);
            field_nautical.themeViews(themeOverride);
            field_civil.themeViews(themeOverride);
            field_day.themeViews(themeOverride);
            field_night.themeViews(themeOverride);

            sunAzimuth.setTextColor(themeOverride.getTimeColor());
            sunAzimuth.setTextSize(timeSizeSp);
            sunAzimuthRising.setTextSize(timeSizeSp);
            sunAzimuthSetting.setTextSize(timeSizeSp);

            sunElevation.setTextColor(themeOverride.getTimeColor());
            sunElevation.setTextSize(timeSizeSp);
            sunElevationAtNoon.setTextSize(timeSizeSp);

            sunAzimuthAtNoon.setTextColor(themeOverride.getTimeColor());
            sunAzimuthAtNoon.setTextSize(timeSizeSp);

            sunShadowObj.setTextColor(themeOverride.getTitleColor());
            sunShadowObj.setTextSize(timeSizeSp);

            sunShadowLength.setTextColor(themeOverride.getTimeColor());
            sunShadowLength.setTextSize(timeSizeSp);

            sunShadowLengthAtNoon.setTextColor(themeOverride.getSunsetTextColor());
            sunShadowLengthAtNoon.setTextSize(timeSizeSp);

            SuntimesUtils.tintDrawable((InsetDrawable)riseIcon.getBackground(), themeOverride.getSunriseIconColor(), themeOverride.getSunriseIconStrokeColor(), themeOverride.getSunriseIconStrokePixels(context));
            SuntimesUtils.tintDrawable((InsetDrawable)setIcon.getBackground(), themeOverride.getSunsetIconColor(), themeOverride.getSunsetIconStrokeColor(), themeOverride.getSunsetIconStrokePixels(context));
        }

        SuntimesUtils.colorizeImageView(field_night.icon, colorNight);
        SuntimesUtils.colorizeImageView(field_astro.icon, colorAstro);
        SuntimesUtils.colorizeImageView(field_nautical.icon, colorNautical);
        SuntimesUtils.colorizeImageView(field_civil.icon, colorCivil);
        SuntimesUtils.colorizeImageView(field_day.icon, colorDay);

        colorLabel = field_night.label.getTextColors().getColorForState(new int[] { -android.R.attr.state_enabled }, Color.BLUE); // field_night.label.getCurrentTextColor()
    }

    private SuntimesTheme themeOverride = null;
    public void themeViews(Context context, @Nullable SuntimesTheme theme)
    {
        if (theme != null) {
            themeOverride = theme;
            if (lightmap != null) {
                themeViews(context);
            }
        }
    }

    public void updateViews()
    {
        if (data != null)
            updateViews(data);
    }

    protected void updateViews( @NonNull SuntimesRiseSetDataset data )
    {
        stopUpdateTask();
        updateLightmapViews(data);
        updateSunPositionViews(data);
        startUpdateTask();
    }

    protected void updateLightmapViews(@NonNull SuntimesRiseSetDataset data)
    {
        if (lightmap != null)
        {
            Context context = getContext();
            field_civil.updateInfo(context, createInfoArray(data.civilTwilightLength()));
            field_civil.highlight(false);

            field_nautical.updateInfo(context, createInfoArray(data.nauticalTwilightLength()));
            field_nautical.highlight(false);

            field_astro.updateInfo(context, createInfoArray(data.astroTwilightLength()));
            field_astro.highlight(false);

            field_night.updateInfo(context, createInfoArray(new long[] {data.nightLength()}));
            field_night.highlight(false);

            long dayDelta = data.dayLengthOther() - data.dayLength();
            field_day.updateInfo(context, createInfoArray(data.dayLength(), dayDelta, colorDay));
            field_day.highlight(false);

            lightmap.updateViews(data);
            //Log.d("DEBUG", "LightMapDialog updated");
        }
    }

    private void styleAzimuthText(TextView view, double azimuth, Integer color, int places)
    {
        SuntimesUtils.TimeDisplayText azimuthText = utils.formatAsDirection2(azimuth, places, false);
        String azimuthString = utils.formatAsDirection(azimuthText.getValue(), azimuthText.getSuffix());
        SpannableString azimuthSpan = null;
        if (color != null) {
            //noinspection ConstantConditions
            azimuthSpan = SuntimesUtils.createColorSpan(azimuthSpan, azimuthString, azimuthString, color);
        }
        azimuthSpan = SuntimesUtils.createRelativeSpan(azimuthSpan, azimuthString, azimuthText.getSuffix(), 0.7f);
        azimuthSpan = SuntimesUtils.createBoldSpan(azimuthSpan, azimuthString, azimuthText.getSuffix());
        view.setText(azimuthSpan);

        SuntimesUtils.TimeDisplayText azimuthDesc = utils.formatAsDirection2(azimuth, places, true);
        view.setContentDescription(utils.formatAsDirection(azimuthDesc.getValue(), azimuthDesc.getSuffix()));
    }

    private CharSequence styleElevationText(double elevation, Integer color, int places)
    {
        SuntimesUtils.TimeDisplayText elevationText = utils.formatAsElevation(elevation, places);
        String elevationString = utils.formatAsElevation(elevationText.getValue(), elevationText.getSuffix());
        SpannableString span = null;
        //noinspection ConstantConditions
        span = SuntimesUtils.createRelativeSpan(span, elevationString, elevationText.getSuffix(), 0.7f);
        span = SuntimesUtils.createColorSpan(span, elevationString, elevationString, color);
        return (span != null ? span : elevationString);
    }

    private CharSequence styleLengthText(@NonNull Context context, double meters, WidgetSettings.LengthUnit units)
    {
        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        if (meters < Double.POSITIVE_INFINITY)
            return SuntimesUtils.formatAsDistance(context, SuntimesUtils.formatAsHeight(context, meters, units, 2, true));
        else return formatter.format(meters);
    }

    private int getColorForPosition(SuntimesCalculator.SunPosition position, SuntimesCalculator.SunPosition noonPosition)
    {
        if (position.elevation >= 0)
            return (SuntimesRiseSetDataset.isRising(position, noonPosition) ? colorRising : colorSetting);

        if (position.elevation >= -6)
            return colorCivil;

        if (position.elevation >= -12)  //if (elevation >= -18)   // share color
            return colorAstro;

        return colorLabel;
    }

    private void highlightLightmapKey(double elevation)
    {
        if (elevation >= 0)
            field_day.highlight(true);

        else if (elevation >= -6)
            field_civil.highlight(true);

        else if (elevation >= -12)
            field_nautical.highlight(true);

        else if (elevation >= -18)
            field_astro.highlight(true);

        else field_night.highlight(true);
    }

    protected void updateSunPositionViews(@NonNull SuntimesRiseSetDataset data)
    {
        SuntimesCalculator calculator = data.calculator();
        if (sunLayout != null)
        {
            SuntimesRiseSetData noonData = data.dataNoon;
            Calendar noonTime = (noonData != null ? noonData.sunriseCalendarToday() : null);
            SuntimesCalculator.SunPosition noonPosition = (noonTime != null && calculator != null ? calculator.getSunPosition(noonTime) : null);
            SuntimesCalculator.SunPosition currentPosition = (calculator != null ? calculator.getSunPosition(data.nowThen(data.calendar())) : null);

            if (currentPosition != null)
            {
                styleAzimuthText(sunAzimuth, currentPosition.azimuth, null, 2);
                sunElevation.setText(styleElevationText(currentPosition.elevation, getColorForPosition(currentPosition, noonPosition),2));
                highlightLightmapKey(currentPosition.elevation);

            } else {
                sunAzimuth.setText("");
                sunAzimuth.setContentDescription("");
                sunElevation.setText("");
            }

            SuntimesRiseSetData riseSetData = data.dataActual;
            Calendar riseTime = (riseSetData != null ? riseSetData.sunriseCalendarToday() : null);
            SuntimesCalculator.SunPosition positionRising = (riseTime != null && calculator != null ? calculator.getSunPosition(riseTime) : null);
            if (positionRising != null) {
                styleAzimuthText(sunAzimuthRising, positionRising.azimuth, colorRising, decimalPlaces);

            } else {
                sunAzimuthRising.setText("");
                sunAzimuthRising.setContentDescription("");
            }

            Calendar setTime = (riseSetData != null ? riseSetData.sunsetCalendarToday() : null);
            SuntimesCalculator.SunPosition positionSetting = (setTime != null && calculator != null ? calculator.getSunPosition(setTime) : null);
            if (positionSetting != null) {
                styleAzimuthText(sunAzimuthSetting, positionSetting.azimuth, colorSetting, decimalPlaces);

            } else {
                sunAzimuthSetting.setText("");
                sunAzimuthSetting.setContentDescription("");
            }

            if (noonPosition != null)
            {
                sunElevationAtNoon.setText(styleElevationText(noonPosition.elevation, colorSetting, decimalPlaces));
                styleAzimuthText(sunAzimuthAtNoon, noonPosition.azimuth, null, decimalPlaces);

            } else {
                sunElevationAtNoon.setText("");
                sunAzimuthAtNoon.setText("");
                sunAzimuthAtNoon.setContentDescription("");
            }

            Context context = getContext();
            if (context != null && calculator != null)
            {
                double objectHeight = WidgetSettings.loadObserverHeightPref(context, 0);
                if (objectHeight > 0)
                {
                    WidgetSettings.LengthUnit units = WidgetSettings.loadLengthUnitsPref(context, 0);

                    if (sunShadowObj != null) {
                        sunShadowObj.setText(styleLengthText(context, objectHeight, units));
                    }
                    if (sunShadowLength != null) {
                        double shadowLength = calculator.getShadowLength(objectHeight, data.now());
                        sunShadowLength.setText((shadowLength >= 0) ? styleLengthText(context, shadowLength, units) : "");
                    }
                    if (sunShadowLengthAtNoon != null && noonTime != null) {
                        double shadowLengthAtNoon = calculator.getShadowLength(objectHeight, noonTime );
                        sunShadowLengthAtNoon.setText((shadowLengthAtNoon >= 0) ? styleLengthText(context, shadowLengthAtNoon, units) : "");
                    }
                }
            }

            showSunPosition(currentPosition != null);
        }
    }

    private void showSunPosition(boolean show)
    {
        if (sunLayout != null)
        {
            int updatedVisibility = (show ? View.VISIBLE : View.GONE);
            if (sunLayout.getVisibility() != updatedVisibility)
            {
                sunLayout.setVisibility(updatedVisibility);
                if (dialogContent != null) {
                    dialogContent.requestLayout();
                }
            }
        }
    }

    /**
     * LightMapKey
     */
    private class LightMapKey
    {
        protected ImageView icon;
        protected TextView label;
        protected TextView text;

        public LightMapKey(ImageView icon, TextView label, TextView duration)
        {
            this.icon = icon;
            this.label = label;
            this.text = duration;
        }

        public LightMapKey(@NonNull View parent, int iconRes, int labelRes, int durationRes)
        {
            icon = (ImageView)parent.findViewById(iconRes);
            label = (TextView)parent.findViewById(labelRes);
            text = (TextView)parent.findViewById(durationRes);
        }

        public void themeViews(SuntimesTheme theme)
        {
            if (theme != null)
            {
                label.setTextColor(theme.getTextColor());
                label.setTextSize(theme.getTextSizeSp());
                text.setTextColor(theme.getTimeColor());
                text.setTextSize(theme.getTimeSizeSp());
                text.setTypeface(text.getTypeface(), (theme.getTimeBold() ? Typeface.BOLD : Typeface.NORMAL));
            }
        }

        public void setVisible(boolean visible)
        {
            int visibility = (visible ? View.VISIBLE : View.GONE);
            if (label != null) {
                label.setVisibility(visibility);
            }
            if (text != null) {
                text.setVisibility(visibility);
            }
            if (icon != null) {
                icon.setVisibility(visibility);
            }
        }

        public void highlight(boolean highlight)
        {
            if (label != null)
            {
                label.setTypeface(null, (highlight ? Typeface.BOLD : Typeface.NORMAL));
                if (highlight)
                    label.setPaintFlags(label.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                else label.setPaintFlags(label.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            }

            //if (text != null)
                //text.setTypeface(null, (highlight ? Typeface.BOLD : Typeface.NORMAL));
        }

        public void updateInfo(Context context, LightMapKeyInfo[] info)
        {
            if (text == null || info == null || context == null)
                return;

            if (info.length == 1)
            {
                String duration = info[0].durationString(showSeconds);
                if (info[0].delta > 0) {
                    String s = context.getString(R.string.length_twilight1e_pos, duration, info[0].deltaString(showSeconds));
                    if (info[0].durationColor != null)
                        text.setText(SuntimesUtils.createColorSpan(null, s, duration, info[0].durationColor));
                    else text.setText(new SpannableString(s));

                } else if (info[0].delta < 0) {
                    String s = context.getString(R.string.length_twilight1e_neg, duration, info[0].deltaString(showSeconds));
                    if (info[0].durationColor != null)
                        text.setText(SuntimesUtils.createColorSpan(null, s, duration, info[0].durationColor));
                    else text.setText(new SpannableString(s));

                } else {
                    String s = context.getString(R.string.length_twilight1, duration);
                    if (info[0].durationColor != null)
                        text.setText(SuntimesUtils.createColorSpan(null, s, duration, info[0].durationColor));
                    else text.setText(new SpannableString(s));
                }
                setVisible(true);

            } else if (info.length >= 2) {
                String s = context.getString(R.string.length_twilight2, info[0].durationString(showSeconds), info[1].durationString(showSeconds));
                String delimiter = context.getString(R.string.length_delimiter);
                text.setText(SuntimesUtils.createBoldColorSpan(null, s, delimiter, colorDay));
                setVisible(true);

            } else {
                text.setText(new SpannableString(""));
                setVisible(false);
            }
        }
    }

    /**
     * LightMapKeyInfo
     */
    public static class LightMapKeyInfo
    {
        public LightMapKeyInfo(long duration, long delta)
        {
            this.duration = duration;
            this.delta = delta;
        }

        public long duration = 0;
        public Integer durationColor = null;
        public String durationString(boolean showSeconds)
        {
            return utils.timeDeltaLongDisplayString(duration, showSeconds).toString();
        }

        public long delta = 0;
        public Integer deltaColor = null;
        public String deltaString(boolean showSeconds)
        {
            return utils.timeDeltaLongDisplayString(delta, showSeconds).toString();
        }
    }

    public static LightMapKeyInfo[] createInfoArray(long durations, long delta, int color)
    {
        if (durations != 0)
        {
            LightMapKeyInfo[] info = new LightMapKeyInfo[1];
            info[0] = new LightMapKeyInfo(durations, delta);
            info[0].durationColor = color;
            return info;

        } else {
            return new LightMapKeyInfo[0];
        }
    }

    public static LightMapKeyInfo[] createInfoArray(long[] durations)
    {
        ArrayList<LightMapKeyInfo> info = new ArrayList<>();
        for (int i=0; i<durations.length; i++)
        {
            if (durations[i] != 0) {
                info.add(new LightMapKeyInfo(durations[i], 0));
            }
        }
        return info.toArray(new LightMapKeyInfo[0]);
    }

}
