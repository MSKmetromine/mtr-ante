package cn.zbx1425.mtrsteamloco.scripting.util.client;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.mixin.ClientCacheAccessor;
import mtr.client.ClientData;
import mtr.mappings.Utilities;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "11903"
import net.minecraft.core.registries.BuiltInRegistries;
#else
#endif
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import cn.zbx1425.mtrsteamloco.scripting.util.ScriptResourceUtil;

import java.awt.*;

@SuppressWarnings("unused")
public class ScriptResourceUtilClient extends ScriptResourceUtil {

    private static boolean hasNotoSansCjk = false;
    private static Font NOTO_SANS_MAYBE_CJK;
    private static final ResourceLocation NOTO_SANS_CJK_LOCATION = new ResourceLocation(mtr.MTR.MOD_ID, "font/noto-sans-cjk-tc-medium.otf");
    private static final ResourceLocation NOTO_SANS_LOCATION = new ResourceLocation(mtr.MTR.MOD_ID, "font/noto-sans-semibold.ttf");
    private static final ResourceLocation NOTO_SERIF_LOCATION = new ResourceLocation(mtr.MTR.MOD_ID, "font/noto-serif-cjk-tc-semibold.ttf");

    public static void init(ResourceManager resourceManager) {
        hasNotoSansCjk = hasResource(NOTO_SANS_CJK_LOCATION);
    }
    public static Font getSystemFont(String fontName) {
        ClientCacheAccessor clientCache = (ClientCacheAccessor) ClientData.DATA_CACHE;
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        switch (fontName) {
            case "Noto Sans" -> {
                if (NOTO_SANS_MAYBE_CJK == null) {
                    if (hasNotoSansCjk) {
                        try {
                            NOTO_SANS_MAYBE_CJK = Font.createFont(Font.TRUETYPE_FONT,
                                    Utilities.getInputStream(resourceManager.getResource(NOTO_SANS_CJK_LOCATION)));
                        } catch (Exception ex) {
                            Main.LOGGER.warn("Failed loading font", ex);
                        }
                    } else {
                        if (clientCache.getFont() == null) {
                            try {
                                clientCache.setFont(Font.createFont(Font.TRUETYPE_FONT,
                                        Utilities.getInputStream(resourceManager.getResource(NOTO_SANS_LOCATION))));
                            } catch (Exception ex) {
                                Main.LOGGER.warn("Failed loading font", ex);
                            }
                        }
                        NOTO_SANS_MAYBE_CJK = clientCache.getFont();
                    }
                }
                return NOTO_SANS_MAYBE_CJK;
            }
            case "Noto Serif" -> {
                if (clientCache.getFontCjk() == null) {
                    try {
                        clientCache.setFontCjk(Font.createFont(Font.TRUETYPE_FONT,
                                Utilities.getInputStream(resourceManager.getResource(NOTO_SERIF_LOCATION))));
                    } catch (Exception ex) {
                        Main.LOGGER.warn("Failed loading font", ex);
                    }
                }
                return clientCache.getFontCjk();
            }
            default -> {
                return new Font(fontName, Font.PLAIN, 1);
            }
        }
    }
}
