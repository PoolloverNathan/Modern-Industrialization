/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.misc.version;

import aztech.modern_industrialization.MIConfig;
import aztech.modern_industrialization.ModernIndustrialization;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import me.shedaniel.cloth.api.common.events.v1.PlayerJoinCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public class VersionEvents {

    private static final String url = "https://api.cfwidget.com/minecraft/mc-mods/modern-industrialization";
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    private record Version(String name, String url, Date date) implements Comparable<Version> {
        @Override
        public int compareTo(@NotNull VersionEvents.Version o) {
            return o.date.compareTo(date);
        }
    }

    private static Version fetchVersion() {
        URLConnection connection;
        try {
            connection = new URL(url).openConnection();
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                PriorityQueue<Version> queue = new PriorityQueue<>();

                String response = scanner.useDelimiter("\\A").next();
                JsonParser jsonParser = new JsonParser();
                JsonObject jo = (JsonObject) jsonParser.parse(response);

                for (JsonElement file : jo.getAsJsonArray("files")) {
                    JsonObject fileAsJsonObject = (JsonObject) file;

                    String name = fileAsJsonObject.get("display").getAsString();
                    String url = fileAsJsonObject.get("url").getAsString();
                    String type = fileAsJsonObject.get("type").getAsString();
                    String date = fileAsJsonObject.get("uploaded_at").getAsString();

                    if (!type.equals("alpha")) {
                        queue.add(new Version(name, url, format.parse(date)));
                    }

                }

                if (!queue.isEmpty()) {
                    return queue.poll();
                }

            } catch (Exception e) {
                ModernIndustrialization.LOGGER.error(e.getMessage(), e);
            }

        } catch (IOException e) {
            ModernIndustrialization.LOGGER.error(e.getMessage(), e);
        }
        return null;

    }

    public static void init() {
        PlayerJoinCallback.EVENT.register((connection, player) -> {
            Optional<ModContainer> currentMod = FabricLoader.getInstance().getModContainer(ModernIndustrialization.MOD_ID);
            if (MIConfig.getConfig().newVersionMessage) {
                if (currentMod.isPresent()) {
                    ModContainer mod = currentMod.get();
                    String currentVersion = mod.getMetadata().getVersion().getFriendlyString();
                    Version lastVersion = fetchVersion();

                    if (lastVersion != null) {
                        String lastVersionString = lastVersion.name.replaceFirst("Modern Industrialization v", "").strip();

                        if (!lastVersionString.equals(currentVersion)) {
                            String url = lastVersion.url;

                            Style styleClick = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                    .withFormatting(Formatting.UNDERLINE).withFormatting(Formatting.GREEN).withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT, new TranslatableText("text.modern_industrialization.click_url")));

                            player.sendMessage(new TranslatableText("text.modern_industrialization.new_version", lastVersionString,
                                    new TranslatableText("text.modern_industrialization.curse_forge").setStyle(styleClick)), false);
                        }
                    }
                }

            } else {
                throw new IllegalStateException("Modern Industrialization is not loaded but loaded at the same time");
            }
        });
    }
}