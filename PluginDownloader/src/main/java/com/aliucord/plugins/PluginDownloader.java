/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.plugins.plugindownloader.Modal;
import com.aliucord.plugins.plugindownloader.PDUtil;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.lytefast.flexinput.R$b;
import com.lytefast.flexinput.R$h;

import java.util.regex.Pattern;


@SuppressWarnings("unused")
@SuppressLint("SetTextI18n")
public class PluginDownloader extends Plugin {
    private final int id = View.generateViewId();
    private final Pattern repoPattern = Pattern.compile("https?://github.com/([A-Za-z0-9\\-_.]+)/([A-Za-z0-9\\-_.]+)");
    private final Pattern zipPattern = Pattern.compile("https?://github.com/([A-Za-z0-9\\-_.]+)/([A-Za-z0-9\\-_.]+)/(raw|blob)/\\w+/(\\w+).zip");

    public PluginDownloader() {
        super();
        needsResources = true;
    }

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Adds message context menu items to quick download plugins";
        manifest.version = "1.1.2";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context context) {
        Drawable icon = ResourcesCompat.getDrawable(
                resources,
                resources.getIdentifier(
                        "ic_download",
                        "drawable",
                        "com.aliucord.plugins"),
                null);

        patcher.patch(WidgetChatListActions.class, "configureUI", new Class<?>[] {WidgetChatListActions.Model.class} , new PinePatchFn(callFrame -> {
            var _this = (WidgetChatListActions) callFrame.thisObject;
            var rootView = (NestedScrollView) _this.requireView();
            var layout = (LinearLayout) rootView.getChildAt(0);
            if (layout == null || layout.findViewById(id) != null) return;
            var ctx = layout.getContext();
            var msg = ((WidgetChatListActions.Model) callFrame.args[0]).getMessage();
            if (msg == null || msg.getContent() == null) return;
            String content = msg.getContent();
            long channelId = msg.getChannelId();

            if (channelId == Constants.PLUGIN_LINKS_UPDATES_CHANNEL_ID) {
                var matcher = zipPattern.matcher(content);
                while (matcher.find()) {
                    String author = matcher.group(1);
                    String repo = matcher.group(2);
                    String name = matcher.group(4);
                    var view = new TextView(ctx, null, 0, R$h.UiKit_Settings_Item_Icon);
                    view.setId(id);
                    view.setText("Download " + name);
                    if (icon != null) {
                        icon.setTint(ColorCompat.getThemedColor(ctx, R$b.colorInteractiveNormal));
                        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    }
                    view.setOnClickListener(e -> PDUtil.downloadPlugin(e.getContext(), author, repo, name, _this::dismiss));
                    layout.addView(view, 1);
                }
            } else if (channelId == Constants.PLUGIN_LINKS_CHANNEL_ID) {
                    var repoMatcher = repoPattern.matcher(content);
                    if (!repoMatcher.find()) return; // zzzzzzz don't post junk
                    String author = repoMatcher.group(1);
                    String repo = repoMatcher.group(2);

                    var modal = new Modal(author, repo);
                    var view = new TextView(ctx, null, 0, R$h.UiKit_Settings_Item_Icon);
                    view.setId(id);
                    view.setText("Open Plugin Downloader");
                    if (icon != null) {
                        icon.setTint(ColorCompat.getThemedColor(ctx, R$b.colorInteractiveNormal));
                        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    }
                    view.setOnClickListener(e -> {
                        Utils.openPageWithProxy(e.getContext(), modal);
                        _this.dismiss();
                    });
                    layout.addView(view, 1);
            }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
