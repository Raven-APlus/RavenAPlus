package keystrokesmod.clickgui;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.clickgui.components.IComponent;
import keystrokesmod.clickgui.components.impl.BindComponent;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.config.FavoritesStorage;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.Commands;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.render.GradientBlur;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClickGui extends GuiScreen {
    private ScheduledFuture<?> sf;
    private Timer aT;
    private Timer aL;
    private Timer aE;
    private Timer aR;
    private ScaledResolution sr;
    private GuiButtonExt s;
    private GuiTextField c;
    public static Map<Module.category, CategoryComponent> categories;
    public static List<Module.category> clickHistory;
    private Runnable delayedAction = null;

    private final GradientBlur blur = new GradientBlur(GradientBlur.Type.LR);

    private GuiButtonExt favoritesOnlyBtn;
    private boolean favoritesOnly = false;
    private String lastFilter = "";
    private GuiTextField searchField;
    private List<Module> searchResults = new ArrayList<>();
    private boolean showSearchResults = false;

    /**
     * to make smooth mouse scrolled
     */
    private int guiYMoveLeft = 0;

    public ClickGui() {
        int y = 5;
        Module.category[] values;
        int length = (values = Module.category.values()).length;

        categories = new HashMap<>(length);
        clickHistory = new ArrayList<>(length);
        for (int i = 0; i < length; ++i) {
            Module.category c = values[i];
            CategoryComponent f = new CategoryComponent(c);
            f.y(y);
            categories.put(c, f);
            clickHistory.add(c);
            y += 20;
        }
    }

    public static IFont getFont() {
        switch ((int) Gui.font.getInput()) {
            default:
            case 0:
                return FontManager.getMinecraft();
            case 1:
                return FontManager.productSans20;
            case 2:
                return FontManager.tenacity20;
        }
    }

    public void run(Runnable task) {
        delayedAction = task;
    }

    public void initMain() {
        (this.aT = this.aE = this.aR = new Timer(500.0F)).start();
        this.sf = Raven.getExecutor().schedule(() -> (this.aL = new Timer(650.0F)).start(), 650L, TimeUnit.MILLISECONDS);
    }

    public void initGui() {
        super.initGui();
        this.sr = new ScaledResolution(this.mc);
        (this.c = new GuiTextField(1, this.mc.fontRendererObj, 22, this.height - 100, 150, 20)).setMaxStringLength(256);
        this.buttonList.add(this.s = new GuiButtonExt(2, 22, this.height - 70, 150, 20, "Send"));
        this.s.visible = CommandLine.a;

        if (this.searchField == null) {
            this.searchField = new GuiTextField(3, this.mc.fontRendererObj, 8, 8, 160, 16);
            this.searchField.setMaxStringLength(64);
        }
        this.searchField.setFocused(false);

        int btnW = 82, btnH = 18;
        int btnX = this.width - (btnW + 8);
        int btnY = 6;
        if (this.favoritesOnlyBtn == null) {
            this.favoritesOnlyBtn = new GuiButtonExt(201, btnX, btnY, btnW, btnH, this.favoritesOnly ? "★ Favorites" : "☆ Favorites");
            this.buttonList.add(this.favoritesOnlyBtn);
        } else {
            this.favoritesOnlyBtn.xPosition = btnX;
            this.favoritesOnlyBtn.yPosition = btnY;
            this.favoritesOnlyBtn.width = btnW;
            this.favoritesOnlyBtn.height = btnH;
            this.favoritesOnlyBtn.displayString = this.favoritesOnly ? "★ Favorites" : "☆ Favorites";
            if (!this.buttonList.contains(this.favoritesOnlyBtn)) {
                this.buttonList.add(this.favoritesOnlyBtn);
            }
        }

        try {
            FavoritesStorage.loadFavorites(gatherAllModules());
        } catch (Exception ignored) {}

        applyFiltersToCategories();
    }

    public void drawScreen(int x, int y, float p) {
        int r;
        move:
        if (guiYMoveLeft != 0) {
            int step = (int) (guiYMoveLeft * 0.15);
            if (step == 0) {
                guiYMoveLeft = 0;
                break move;
            }
            for (CategoryComponent category : categories.values()) {
                category.y(category.getY() + step);
            }
            guiYMoveLeft -= step;
        }

        if (this.searchField != null) {
            this.searchField.drawTextBox();
            if (!this.searchField.isFocused() && (this.searchField.getText() == null || this.searchField.getText().trim().length() == 0)) {
                String hint = "Search modules...";
                int hx = this.searchField.xPosition + 4;
                int hy = this.searchField.yPosition + (this.searchField.height - this.fontRendererObj.FONT_HEIGHT) / 2;
                this.fontRendererObj.drawString(hint, hx, hy, 0x77FFFFFF);
            }
        }

        String q = (this.searchField != null && this.searchField.getText() != null) ? this.searchField.getText().trim() : "";
        if (!q.equals(this.lastFilter)) {
            this.lastFilter = q;
            applyFiltersToCategories();

            updateSearchResults(q);
        }

        if (ModuleManager.clientTheme.isEnabled() && ModuleManager.clientTheme.clickGui.isToggled()) {
            blur.update(0, 0, width, height);
            blur.render(0, 0, width, height, 1, 0.1f);
        } else {
            drawRect(0, 0, this.width, this.height, (int) (this.aR.getValueFloat(0.0F, 0.7F, 2) * 255.0F) << 24);
        }

        if (showSearchResults && !searchResults.isEmpty()) {
            drawSearchResults(x, y);
        }

        if (!Gui.removeWatermark.isToggled()) {
            int h = this.height / 4;
            int wd = this.width / 2;
            int w_c = 30 - this.aT.getValueInt(0, 30, 3);
            getFont().drawCenteredString("r", wd + 1 - w_c, h - 25, Utils.getChroma(2L, 1500L));
            getFont().drawCenteredString("a", wd - w_c, h - 15, Utils.getChroma(2L, 1200L));
            getFont().drawCenteredString("v", wd - w_c, h - 5, Utils.getChroma(2L, 900L));
            getFont().drawCenteredString("e", wd - w_c, h + 5, Utils.getChroma(2L, 600L));
            getFont().drawCenteredString("n", wd - w_c, h + 15, Utils.getChroma(2L, 300L));
            getFont().drawCenteredString("A+", wd + 1 + w_c, h + 30, Utils.getChroma(2L, 0L));
            this.drawVerticalLine(wd - 10 - w_c, h - 30, h + 43, Color.white.getRGB());
            this.drawVerticalLine(wd + 10 + w_c, h - 30, h + 43, Color.white.getRGB());
            if (this.aL != null) {
                r = this.aL.getValueInt(0, 20, 2);
                this.drawHorizontalLine(wd - 10, wd - 10 + r, h - 29, -1);
                this.drawHorizontalLine(wd + 10, wd + 10 - r, h + 42, -1);
            }
        }

        for (Module.category category : clickHistory) {
            CategoryComponent c = categories.get(category);
            c.rf(getFont());
            c.up(x, y);

            for (IComponent m : c.getModules()) {
                m.drawScreen(x, y);
            }
        }

        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        if (!Gui.removePlayerModel.isToggled()) {
            GuiInventory.drawEntityOnScreen(this.width + 15 - this.aE.getValueInt(0, 40, 2), this.height - 10, 40, (float) (this.width - 25 - x), (float) (this.height - 50 - y), this.mc.thePlayer);
        }

        if (CommandLine.a) {
            if (!this.s.visible) {
                this.s.visible = true;
            }

            r = CommandLine.animate.isToggled() ? CommandLine.an.getValueInt(0, 200, 2) : 200;
            if (CommandLine.b) {
                r = 200 - r;
                if (r == 0) {
                    CommandLine.b = false;
                    CommandLine.a = false;
                    this.s.visible = false;
                }
            }

            drawRect(0, 0, r, this.height, -1089466352);
            this.drawHorizontalLine(0, r - 1, this.height - 345, -1);
            this.drawHorizontalLine(0, r - 1, this.height - 115, -1);
            drawRect(r - 1, 0, r, this.height, -1);
            Commands.rc(getFont(), this.height, r, this.sr.getScaleFactor());
            int x2 = r - 178;
            this.c.xPosition = x2;
            this.s.xPosition = x2;
            this.c.drawTextBox();
            super.drawScreen(x, y, p);
        } else if (CommandLine.b) {
            CommandLine.b = false;
        }

        if (delayedAction != null)
            delayedAction.run();
        delayedAction = null;
    }

    private void updateSearchResults(String query) {
        searchResults.clear();
        showSearchResults = !query.isEmpty();

        if (showSearchResults) {
            for (Module module : gatherAllModules()) {
                if (module.getName().toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(module);
                }
            }
        }
    }

    private void drawSearchResults(int mouseX, int mouseY) {
        int panelX = this.searchField.xPosition;
        int panelY = this.searchField.yPosition + this.searchField.height + 2;
        int panelWidth = this.searchField.width;
        int panelHeight = Math.min(searchResults.size() * 12, 120);

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF000000);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFFFFFFFF);

        for (int i = 0; i < Math.min(searchResults.size(), 10); i++) {
            Module mod = searchResults.get(i);
            int resultY = panelY + (i * 12);
            boolean hovering = mouseX >= panelX && mouseX <= panelX + panelWidth &&
                    mouseY >= resultY && mouseY <= resultY + 12;

            if (hovering) {
                drawRect(panelX, resultY, panelX + panelWidth, resultY + 12, 0xFF333333);
            }

            getFont().drawString(mod.getName(), panelX + 2, resultY + 2,
                    mod.isEnabled() ? 0xFF00FF00 : 0xFFFFFFFF);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            this.mouseScrolled(dWheel);
        }
    }

    public void mouseScrolled(int dWheel) {
        if (dWheel > 0) {
            // up
            guiYMoveLeft += 30;
        } else if (dWheel < 0) {
            // down
            guiYMoveLeft -= 30;
        }
    }

    public void mouseClicked(int x, int y, int m) throws IOException {
        if (this.searchField != null) {
            this.searchField.mouseClicked(x, y, m);
        }

        if (showSearchResults && !searchResults.isEmpty()) {
            int panelX = this.searchField.xPosition;
            int panelY = this.searchField.yPosition + this.searchField.height + 2;
            int panelWidth = this.searchField.width;

            for (int i = 0; i < Math.min(searchResults.size(), 10); i++) {
                Module mod = searchResults.get(i);
                int resultY = panelY + (i * 12);

                if (x >= panelX && x <= panelX + panelWidth &&
                        y >= resultY && y <= resultY + 12) {
                    if (m == 0) {
                        mod.toggle();
                    } else if (m == 1) {
                        highlightModule(mod);
                    }
                    return;
                }
            }
        }

        Iterator<CategoryComponent> var4 = clickHistory.stream()
                .map(category -> categories.get(category))
                .iterator();

        while (true) {
            CategoryComponent category = null;
            do {
                do {
                    if (!var4.hasNext()) {
                        if (CommandLine.a) {
                            this.c.mouseClicked(x, y, m);
                            super.mouseClicked(x, y, m);
                        }

                        if (category != null) {
                            clickHistory.remove(category.categoryName);
                            clickHistory.add(category.categoryName);
                        }
                        return;
                    }

                    category = var4.next();
                    if (category.v(x, y) && !category.i(x, y) && !category.d(x, y) && m == 0) {
                        category.d(true);
                        category.dragStartX = x - category.getX();
                        category.dragStartY = y - category.getY();
                    }

                    if (category.d(x, y) && m == 0) {
                        category.mouseClicked(!category.fv());
                    }

                    if (category.i(x, y) && m == 0) {
                        category.cv(!category.p());
                    }
                } while (!category.fv());
            } while (category.getModules().isEmpty());

            for (IComponent c : category.getModules()) {
                c.onClick(x, y, m);
            }
        }
    }

    private void highlightModule(Module module) {
        for (CategoryComponent category : categories.values()) {
            if (category.categoryName != Module.category.favorites) {
                for (ModuleComponent comp : category.getModules()) {
                    if (comp.mod == module) {
                        category.fv(true);
                        return;
                    }
                }
            }
        }
    }

    public void mouseReleased(int x, int y, int s) {
        if (s == 0) {
            for (CategoryComponent category : categories.values()) {
                category.d(false);
                if (category.fv() && !category.getModules().isEmpty()) {
                    for (IComponent module : category.getModules()) {
                        module.mouseReleased(x, y, s);
                    }
                }
            }
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (this.searchField != null && this.searchField.textboxKeyTyped(t, k)) {
            this.lastFilter = this.searchField.getText() == null ? "" : this.searchField.getText().trim();
            applyFiltersToCategories();
            return;
        }

        if (k == Keyboard.KEY_ESCAPE && !binding()) {
            this.mc.displayGuiScreen(null);
        } else {
            for (CategoryComponent category : categories.values()) {
                if (category.fv() && !category.getModules().isEmpty()) {
                    for (IComponent module : category.getModules()) {
                        module.keyTyped(t, k);
                    }
                }
            }
            if (CommandLine.a) {
                String cm = this.c.getText();
                if (k == 28 && !cm.isEmpty()) {
                    Commands.rCMD(this.c.getText());
                    this.c.setText("");
                    return;
                }
                this.c.textboxKeyTyped(t, k);
            }
        }
    }

    public void actionPerformed(GuiButton b) {
        if (b == this.favoritesOnlyBtn) {
            this.favoritesOnly = !this.favoritesOnly;
            this.favoritesOnlyBtn.displayString = this.favoritesOnly ? "★ Favorites" : "☆ Favorites";
            applyFiltersToCategories();
            return;
        }

        if (b == this.s) {
            Commands.rCMD(this.c.getText());
            this.c.setText("");
        }
    }

    public void onGuiClosed() {
        try {
            FavoritesStorage.saveFavorites(gatherAllModules());
        } catch (Exception ignored) {}

        this.aL = null;
        if (this.sf != null) {
            this.sf.cancel(true);
            this.sf = null;
        }
        for (CategoryComponent c : categories.values()) {
            c.dragging = false;
            for (IComponent m : c.getModules()) {
                m.onGuiClosed();
            }
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories.values()) {
            for (ModuleComponent m : c.getModules()) {
                for (Component component : m.settings) {
                    if (component instanceof BindComponent && ((BindComponent) component).isBinding) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void resetPosition() {
        int xOffSet = 5;
        int yOffSet = 5;
        for(CategoryComponent category : categories.values()) {
            category.fv(false);
            category.x(xOffSet);
            category.y(yOffSet);
            xOffSet = xOffSet + 100;
            if (xOffSet > 400) {
                xOffSet = 5;
                yOffSet += 120;
            }
        }
    }

    private List<Module> gatherAllModules() {
        List<Module> all = new ArrayList<>();
        if (Raven.moduleManager != null) {
            all.addAll(Raven.moduleManager.getModules());
        }
        return all;
    }

    private void applyFiltersToCategories() {
        if (categories == null) return;
        String q = this.lastFilter == null ? "" : this.lastFilter.toLowerCase();
        for (CategoryComponent comp : categories.values()) {
            if (comp != null) {
                comp.setNameFilter(q);
                comp.setShowFavoritesOnly(this.favoritesOnly);
            }
        }
    }
}