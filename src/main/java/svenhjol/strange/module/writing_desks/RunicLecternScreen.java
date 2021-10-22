package svenhjol.strange.module.writing_desks;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import svenhjol.charm.helper.ClientHelper;
import svenhjol.strange.Strange;
import svenhjol.strange.module.journals.JournalData;
import svenhjol.strange.module.journals.JournalHelper;
import svenhjol.strange.module.journals.Journals;
import svenhjol.strange.module.knowledge.KnowledgeBranch;
import svenhjol.strange.module.knowledge.KnowledgeClientHelper;
import svenhjol.strange.module.runestones.RunestoneItemTooltip;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("ConstantConditions")
public class RunicLecternScreen extends AbstractContainerScreen<RunicLecternMenu> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Strange.MOD_ID, "textures/gui/runic_lectern.png");

    private int midX;
    private int midY;

    private Button doneButton;
    private Button takeButton;
    private Button activateButton;

    private ItemStack tome;
    private String runes;
    private String author;
    private Item item;
    private KnowledgeBranch<?, ?> branch;

    public RunicLecternScreen(RunicLecternMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);

        this.passEvents = false;
        this.midX = 0;
        this.midY = 0;
    }

    @Override
    protected void init() {
        super.init();

        midX = width / 2;
        midY = height / 2;

        int buttonWidth = 90;

        doneButton = addRenderableWidget(new Button(midX - 140, midY + 90, buttonWidth, 20, CommonComponents.GUI_DONE, button -> {
            onClose();
        }));
        takeButton = addRenderableWidget(new Button(midX - 45, midY + 90, buttonWidth, 20, new TranslatableComponent("gui.strange.runic_lecterns.take_tome"), button -> {
            syncClickedButton(0);
            onClose();
        }));
        activateButton = addRenderableWidget(new Button(midX + 49, midY + 90, buttonWidth, 20, new TranslatableComponent("gui.strange.runic_lecterns.activate"), button -> {
            syncClickedButton(1);
            onClose();
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);
        renderBg(poseStack, delta, mouseX, mouseY);

        ItemStack providedItem = menu.slots.get(0).getItem();

        // if there is a sacrificial item, allow the activate button to be clicked
        activateButton.active = !providedItem.isEmpty();

        super.render(poseStack, mouseX, mouseY, delta);

        if (providedItem.isEmpty()) {
            renderItemHover(poseStack, mouseX, mouseY);
        } else {
            renderTooltip(poseStack, mouseX, mouseY);
        }

        // if tome has been sent from the server, update screen props
        if (WritingDesksClient.tomeHolder == null) {
            return;
        }

        if (tome == null) {
            tome = WritingDesksClient.tomeHolder.copy();
            RunicTomeItem.getRunes(tome).ifPresent(r -> runes = r);
            RunicTomeItem.getAuthor(tome).ifPresent(a -> author = a);
            RunicTomeItem.getBranch(tome).ifPresent(b -> branch = b);
            RunicTomeItem.getItem(tome).ifPresent(i -> this.item = i);
        }

        if (!runes.isEmpty()) {
            renderRunes(poseStack);
        }

//            renderItemClue(poseStack, mouseX, mouseY, journal);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float delta, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        setupTextureShaders();
        blit(poseStack, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // nah
    }

    protected void renderRunes(PoseStack poseStack) {
        KnowledgeClientHelper.renderRunesString(
            minecraft,
            poseStack,
            runes,
            midX - 48,
            midY - 68,
            10,
            13,
            10,
            4,
            0x997755,
            0xC0B0A0,
            false
        );
    }

    protected void renderItemHover(PoseStack poseStack, int mouseX, int mouseY) {
        if (minecraft.player == null) return;
        if (item == null) return;

        Optional<JournalData> optJournal = Journals.getJournalData(minecraft.player);
        if (optJournal.isEmpty()) return;

        JournalData journal = optJournal.get();
        int numberOfUnknownRunes = JournalHelper.getNumberOfUnknownRunes(runes, journal);
        if (numberOfUnknownRunes > 0) {
            return;
        }

        List<Component> text = Lists.newArrayList();
        NonNullList<ItemStack> items = NonNullList.create();
        ItemStack stack = new ItemStack(item);
        items.add(stack);

        text.add(new TranslatableComponent("gui.strange.clues.required_item"));
        text.add(item.getName(stack));

        int top = midY - 28;
        int left = midX - 8;
        int bottom = midY - 12;
        int right = midX + 8;

        if (mouseX > left && mouseX < right && mouseY > top && mouseY < bottom) {
            renderTooltip(poseStack, text, Optional.of(new RunestoneItemTooltip(items)), mouseX, mouseY);
        }
    }

    private void setupTextureShaders() {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
    }

    private void syncClickedButton(int r) {
        ClientHelper.getClient().ifPresent(mc -> mc.gameMode.handleInventoryButtonClick((this.menu).containerId, r));
    }
}
