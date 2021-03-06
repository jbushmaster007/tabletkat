package org.exalm.tabletkat.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentBreadCrumbs;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.TabletKatModule;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class MultiPaneSettingsMod implements IMod {
    private static int id_loading_container;
    private static int id_storage_color_bar;
    private static int id_tabs;

    @Override
    public void addHooks(ClassLoader cl) {
        Class dataUsageSummaryClass = XposedHelpers.findClass("com.android.settings.DataUsageSummary", cl);
//        final Class homeSettingsClass = XposedHelpers.findClass("com.android.settings.HomeSettings", cl);
        Class manageApplicationsClass = XposedHelpers.findClass("com.android.settings.applications.ManageApplications", cl);
        Class manageApplicationsTabInfoClass = XposedHelpers.findClass("com.android.settings.applications.ManageApplications.TabInfo", cl);
        final Class settingsClass = XposedHelpers.findClass("com.android.settings.Settings", cl);

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                String name = a.getResources().getResourceName(a.getThemeResId());
                if (name.endsWith(":style/Theme.Settings")) {
                    a.setTheme(TabletKatModule.shouldUseLightTheme()
                            ? android.R.style.Theme_DeviceDefault_Light
                            : android.R.style.Theme_DeviceDefault);
                }
            }
        });

        XposedHelpers.findAndHookMethod(settingsClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                View v = a.findViewById(android.R.id.list);

                View v2 = (View) v.getParent();

                int top = v2.getPaddingTop();
                int bottom = v2.getPaddingBottom();
                v.setPaddingRelative(v.getPaddingStart(), top, v.getPaddingEnd(), bottom);
                v2.setPaddingRelative(v2.getPaddingStart(), 0, v2.getPaddingEnd(), 0);
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onApplyThemeResource",
                Resources.Theme.class, int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                if (!settingsClass.isInstance(a)){
                    return;
                }
                ActionBar ab = a.getActionBar();
                Context newContext = new ContextThemeWrapper(a, android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
                XposedHelpers.setObjectField(ab, "mThemedContext", newContext);
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "setTitle", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                if (!settingsClass.isInstance(a)){
                    return;
                }
                PreferenceActivity p = (PreferenceActivity) a;
                if (!p.onIsMultiPane() || p.onIsHidingHeaders()) {
                    return;
                }
                Object o = XposedHelpers.getObjectField(p, "mFragmentBreadCrumbs");
                if (o == null) {
                    return;
                }
                param.args[0] = a.getResources().getIdentifier("settings_label", "string",
                        TabletKatModule.SETTINGS_PACKAGE);
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "setTitle", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                if (!settingsClass.isInstance(a)){
                    return;
                }
                PreferenceActivity p = (PreferenceActivity) a;
                if (!p.onIsMultiPane() || p.onIsHidingHeaders()) {
                    return;
                }
                Object o = XposedHelpers.getObjectField(p, "mFragmentBreadCrumbs");
                if (o == null) {
                    return;
                }
                param.args[0] = a.getString(a.getResources().getIdentifier("settings_label", "string",
                        TabletKatModule.SETTINGS_PACKAGE));
            }
        });

        XposedHelpers.findAndHookMethod(settingsClass, "isValidFragment", String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return true;
            }
        });
/*
        XposedHelpers.findAndHookMethod(settingsClass, "isValidFragment", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean b = (Boolean) param.getResult();
                param.setResult(b || param.args[0].equals(homeSettingsClass.getName()));
            }
        });
*/
        XposedHelpers.findAndHookMethod(settingsClass, "onIsMultiPane", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return isMultiPane((Resources) XposedHelpers.callMethod(methodHookParam.thisObject, "getResources"));
            }
        });

        XposedHelpers.findAndHookMethod(manageApplicationsClass, "onCreateView",
                LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup rootView = (ViewGroup) param.getResult();

                int i = (int) rootView.getContext().getResources().getDimension(com.android.internal.R.dimen.preference_fragment_padding_side); //TODO

                View tabs = rootView.findViewById(id_tabs);
                tabs.setPadding(i, 0, i, 0);
            }
        });

        XposedHelpers.findAndHookMethod(manageApplicationsTabInfoClass, "build",
                LayoutInflater.class, ViewGroup.class, View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup mRootView = (ViewGroup) param.getResult();
                View v = mRootView.findViewById(id_loading_container);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();

                int i = (int) v.getContext().getResources().getDimension(com.android.internal.R.dimen.preference_fragment_padding_side); //TODO

                params.leftMargin = i;
                params.rightMargin = i;

                v.setLayoutParams(params);
                v.requestLayout();

                DisplayMetrics d = v.getContext().getResources().getDisplayMetrics();
                int end = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, d);
                int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, d);

                View v2 = mRootView.findViewById(id_storage_color_bar);
                if (v2 != null) {
                    v2.setPaddingRelative(v2.getPaddingStart(), v2.getPaddingTop(), end, bottom);
                }
            }
        });

        XposedHelpers.findAndHookMethod(dataUsageSummaryClass, "onCreateView",
                LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ListView mListView = (ListView) XposedHelpers.getObjectField(param.thisObject, "mListView");
                View mHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mHeader");
                Object mAdapter = XposedHelpers.getObjectField(param.thisObject, "mAdapter");

                final boolean shouldInset = mListView.getScrollBarStyle()
                        == View.SCROLLBARS_OUTSIDE_OVERLAY;

                int mInsetSide = 0;
                if (shouldInset) {
                    mInsetSide = mListView.getResources().getDimensionPixelOffset(
                        com.android.internal.R.dimen.preference_fragment_padding_side); //TODO
                }
                XposedHelpers.setIntField(param.thisObject, "mInsetSide", mInsetSide);

                if (mInsetSide > 0) {
                    // inset selector and divider drawables
                    XposedHelpers.callMethod(param.thisObject, "insetListViewDrawables", mListView, mInsetSide);
                    mHeader.setPaddingRelative(mInsetSide, 0, mInsetSide, 0);
                }

                XposedHelpers.setIntField(mAdapter, "mInsetSide", mInsetSide);
            }
        });
}

    @Override
    public void initResources(XResources res, XModuleResources res2) {
        id_loading_container = res.getIdentifier("loading_container", "id", TabletKatModule.SETTINGS_PACKAGE);
        id_tabs = res.getIdentifier("tabs", "id", TabletKatModule.SETTINGS_PACKAGE);
        id_storage_color_bar = res.getIdentifier("storage_color_bar", "id", TabletKatModule.SETTINGS_PACKAGE);

        res.setReplacement(TabletKatModule.SETTINGS_PACKAGE, "dimen", "settings_side_margin",
                res2.fwd(com.android.internal.R.dimen.preference_fragment_padding_side)); //TODO
        res.setReplacement(TabletKatModule.SETTINGS_PACKAGE, "dimen", "pager_tabs_padding",
                res2.fwd(com.android.internal.R.dimen.preference_fragment_padding_side)); //TODO
    }

    private boolean isMultiPane(Resources r){
        return true;//r.getBoolean(com.android.internal.R.bool.preferences_prefer_dual_pane); //TODO
    }

    public static void hookBreadcrumbs(XResources res) {
        res.hookLayout("android", "layout", "breadcrumbs_in_fragment", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam layoutInflatedParam) throws Throwable {
                if (!TabletKatModule.shouldForceBreadcrumbs()) {
                    return;
                }
                ViewGroup v = (ViewGroup) layoutInflatedParam.view;
                if (v instanceof LinearLayout) {
                    return; //The breadcrumbs are already there
                }
                if (! (v.getContext() instanceof PreferenceActivity)) {
                    return; //Not a preference activity
                }
                try {
                    PreferenceActivity a = (PreferenceActivity) v.getContext();
                    if (!a.onIsMultiPane()) {
                        return; //Single pane
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                    return;
                }

                v.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                DisplayMetrics d = v.getResources().getDisplayMetrics();
                int padding = v.getResources().getDimensionPixelSize(
                        com.android.internal.R.dimen.preference_fragment_padding_side); //TODO
                int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, d);
                int top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, d);
                int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, d);
                int iheight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, d);

                LinearLayout l = new LinearLayout(v.getContext());
                l.setOrientation(LinearLayout.VERTICAL);
                l.setPadding(padding, 0, padding, 0);
                FrameLayout.LayoutParams lparams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                lparams.leftMargin = padding;
                lparams.rightMargin = padding;

                FragmentBreadCrumbs bc = new FragmentBreadCrumbs(l.getContext());
                bc.setId(android.R.id.title);
                bc.setPadding(0, top, 0, bottom);
                LinearLayout.LayoutParams bcparams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, height);
                bcparams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                l.addView(bc, bcparams);

                ImageView i = new ImageView(l.getContext());
                i.setImageDrawable(new ColorDrawable(0xFF404040));
                l.addView(i, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, iheight));

                v.addView(l);
                v.setVisibility(View.VISIBLE);

                v.requestLayout();
            }
        });
    }
}
