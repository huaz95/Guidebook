package gigaherz.guidebook.guidebook.util;

import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import gigaherz.guidebook.guidebook.elements.LinkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class LinkHelper
{
    private static final Set<String> PROTOCOLS = Sets.newHashSet("http", "https");

    public interface ILinkable
    {
        void setLinkContext(LinkContext ctx);
    }

    public static void click(IBookGraphics nav, LinkContext context)
    {
        if (context.textTarget != null && context.textAction != null)
        {
            switch (context.textAction)
            {
                case "openUrl":
                    clickWeb(nav, context.textTarget);
                    break;
                case "copyText":
                    clickCopyToClipboard(nav, context.textTarget);
                    break;
                case "copyToChat":
                    clickCopyToChat(nav, context.textTarget);
                    break;
            }
        }
        if (context.target != null)
        {
            nav.navigateTo(context.target);
        }
    }

    public static void clickCopyToClipboard(IBookGraphics nav, String textTarget)
    {
        GuiScreen parent = (GuiScreen) nav.owner();
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiYesNo((result, id) -> {
            if (result)
            {
                GuiScreen.setClipboardString(textTarget);
                mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("text.copyToClipboard.success"));
            }
            mc.displayGuiScreen(parent);
        },
                I18n.format("text.copyToClipboard.line1"),
                I18n.format("text.copyToClipboard.line2"),
                0)
        {
            @Override
            public void drawScreen(int mouseX, int mouseY, float partialTicks)
            {
                parent.drawScreen(-1, -1, partialTicks);
                super.drawScreen(mouseX, mouseY, partialTicks);
            }
        });
    }

    public static void clickCopyToChat(IBookGraphics nav, String textTarget)
    {
        GuiScreen parent = (GuiScreen) nav.owner();
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiChat(textTarget){
            @Override
            public void drawScreen(int mouseX, int mouseY, float partialTicks)
            {
                parent.drawScreen(-1, -1, partialTicks);
                String text = "Temporary chat window open, press ESCAPE to cancel.";
                int textWidth = Math.max(fontRenderer.getStringWidth(text) + 40, width/2);
                drawRect((width - textWidth)/2, height/4, (width + textWidth)/2, height * 3/4, 0x7F000000);
                drawCenteredString(fontRenderer, text, width/2, (height-fontRenderer.FONT_HEIGHT)/2, 0xFFFFFFFF);
                super.drawScreen(mouseX, mouseY, partialTicks);
            }

            @Override
            protected void keyTyped(char typedChar, int keyCode) throws IOException
            {
                if (keyCode == Keyboard.KEY_ESCAPE)
                {
                    mc.displayGuiScreen(parent);
                }
                else if (keyCode == Keyboard.KEY_NUMPADENTER ||keyCode == Keyboard.KEY_RETURN)
                {
                    String s = this.inputField.getText().trim();

                    if (!s.isEmpty())
                    {
                        this.sendChatMessage(s);
                    }

                    this.mc.displayGuiScreen(parent);
                }
                else
                {
                    super.keyTyped(typedChar, keyCode);
                }
            }
        });
    }

    public static void clickWeb(IBookGraphics nav, String textTarget)
    {
        GuiScreen parent = (GuiScreen) nav.owner();
        Minecraft mc = Minecraft.getMinecraft();

        if (!mc.gameSettings.chatLinks)
        {
            return;
        }

        try
        {
            URI uri = new URI(textTarget);
            String s = uri.getScheme();

            if (s == null)
            {
                throw new URISyntaxException(textTarget, "Missing protocol");
            }

            if (!PROTOCOLS.contains(s.toLowerCase(Locale.ROOT)))
            {
                throw new URISyntaxException(textTarget, "Unsupported protocol: " + s.toLowerCase(Locale.ROOT));
            }

            if (mc.gameSettings.chatLinksPrompt)
            {
                ObfuscationReflectionHelper.setPrivateValue(GuiScreen.class, parent, uri, "field_175286_t");
                mc.displayGuiScreen(new GuiConfirmOpenLink(parent, textTarget, 31102009, false)
                {
                    @Override
                    public void drawScreen(int mouseX, int mouseY, float partialTicks)
                    {
                        parent.drawScreen(-1, -1, partialTicks);
                        super.drawScreen(mouseX, mouseY, partialTicks);
                    }
                });
            }
            else
            {
                openWebLink(uri);
            }
        }
        catch (URISyntaxException urisyntaxexception)
        {
            GuidebookMod.logger.error("Can't open url {}", textTarget, urisyntaxexception);
        }
    }

    private static void openWebLink(URI url)
    {
        try
        {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null);
            oclass.getMethod("browse", new Class[]{URI.class}).invoke(object, url);
        }
        catch (Throwable throwable1)
        {
            GuidebookMod.logger.error("Can't open url {}", url, throwable1);
        }
    }
}
